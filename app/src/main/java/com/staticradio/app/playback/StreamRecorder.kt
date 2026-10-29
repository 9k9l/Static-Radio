package com.staticradio.app.playback

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * Records the live station by saving the raw stream bytes (no re-encoding, so
 * the file keeps the stream's own format: mp3 / aac / ogg ...) into the
 * device's public Download folder. Runs on its own connection, independent of
 * the ExoPlayer one, so it keeps going while playback is paused.
 */
class StreamRecorder(
    private val context: Context,
    private val repository: PlaybackRepository
) {
    private var job: Job? = null
    @Volatile var recordingUrl: String? = null
        private set

    val isRecording: Boolean get() = job?.isActive == true

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun start(
        scope: CoroutineScope,
        url: String,
        stationName: String,
        maxMinutes: Int,
        useSubfolder: Boolean
    ) {
        if (isRecording) return
        recordingUrl = url
        repository.updateRecordingState(true)
        repository.updateRecordingProgress(0, 0)
        job = scope.launch(Dispatchers.IO) {
            try {
                record(url, stationName, maxMinutes, useSubfolder)
            } finally {
                recordingUrl = null
                repository.updateRecordingState(false)
            }
        }
    }

    fun stop() {
        job?.cancel()
    }

    private suspend fun record(url: String, stationName: String, maxMinutes: Int, useSubfolder: Boolean) {
        var output: Output? = null
        var totalBytes = 0L
        val startedAt = System.currentTimeMillis()
        var reconnects = 0
        try {
            while (coroutineContext.isActive) {
                val request = Request.Builder().url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) StaticRadio/1.0")
                    // No ICY metadata: it would be interleaved into the audio bytes.
                    .header("Icy-MetaData", "0")
                    .build()
                val call = client.newCall(request)
                val handle = coroutineContext[Job]!!.invokeOnCompletion { call.cancel() }
                try {
                    call.execute().use { response ->
                        if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                        val contentType = response.header("Content-Type")?.substringBefore(';')?.trim()?.lowercase()
                        val extension = extensionFor(contentType)
                            ?: throw UnsupportedStreamException()
                        val body = response.body ?: throw IOException("empty body")
                        if (output == null) output = openOutput(stationName, extension, contentType, useSubfolder)
                        val out = output!!.stream
                        val buffer = ByteArray(16 * 1024)
                        val source = body.byteStream()
                        var lastReport = 0L
                        reconnects = 0
                        while (coroutineContext.isActive) {
                            val read = source.read(buffer)
                            if (read < 0) break
                            out.write(buffer, 0, read)
                            totalBytes += read
                            val now = System.currentTimeMillis()
                            if (now - lastReport >= 1000) {
                                lastReport = now
                                repository.updateRecordingProgress((now - startedAt) / 1000, totalBytes)
                            }
                            if (maxMinutes > 0 && now - startedAt >= maxMinutes * 60_000L) return@use
                        }
                    }
                } catch (e: UnsupportedStreamException) {
                    repository.emitRecordingMessage("تعذّر التسجيل: هذا النوع من البث (HLS/قوائم تشغيل) غير مدعوم")
                    return
                } catch (e: IOException) {
                    if (!coroutineContext.isActive) break
                    Log.w("StreamRecorder", "Stream interrupted", e)
                    if (++reconnects > 5) {
                        repository.emitRecordingMessage("انقطع البث أثناء التسجيل")
                        break
                    }
                    delay(2000)
                    continue
                } finally {
                    handle.dispose()
                }
                val elapsed = System.currentTimeMillis() - startedAt
                if (maxMinutes > 0 && elapsed >= maxMinutes * 60_000L) break
                // Server closed the stream cleanly: reconnect and keep appending.
                if (coroutineContext.isActive) delay(1000)
            }
        } catch (e: Exception) {
            if (e !is kotlinx.coroutines.CancellationException) {
                Log.e("StreamRecorder", "Recording failed", e)
                repository.emitRecordingMessage("فشل التسجيل: ${e.message ?: "خطأ غير معروف"}")
            }
        } finally {
            withContext(NonCancellable) {
                val finished = output
                if (finished != null) {
                    finished.finish(totalBytes > 0)
                    if (totalBytes > 0) {
                        repository.emitRecordingMessage("تم حفظ التسجيل في Download: ${finished.displayName}")
                    }
                }
            }
        }
    }

    private class UnsupportedStreamException : IOException()

    private fun extensionFor(contentType: String?): String? = when {
        contentType == null -> "mp3"
        contentType.contains("mpegurl") || contentType.contains("scpls") ||
            contentType.contains("text/") || contentType.contains("html") -> null
        contentType.contains("aac") -> "aac"
        contentType.contains("mp4") -> "m4a"
        contentType.contains("flac") -> "flac"
        contentType.contains("opus") -> "opus"
        contentType.contains("ogg") -> "ogg"
        contentType.contains("wav") -> "wav"
        else -> "mp3"
    }

    private fun mimeFor(extension: String, contentType: String?): String = when (extension) {
        "mp3" -> "audio/mpeg"
        "aac" -> "audio/aac"
        "m4a" -> "audio/mp4"
        "flac" -> "audio/flac"
        "opus", "ogg" -> "audio/ogg"
        "wav" -> "audio/wav"
        else -> contentType ?: "audio/mpeg"
    }

    private class Output(
        val stream: OutputStream,
        val displayName: String,
        private val onFinish: (keep: Boolean) -> Unit
    ) {
        fun finish(keep: Boolean) {
            runCatching { stream.flush() }
            runCatching { stream.close() }
            onFinish(keep)
        }
    }

    private fun openOutput(stationName: String, extension: String, contentType: String?, useSubfolder: Boolean): Output {
        val safeName = stationName.map { if (it.isLetterOrDigit() || it == ' ' || it == '-' || it == '_') it else '_' }
            .joinToString("").trim().ifEmpty { "radio" }.take(60)
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val displayName = "${safeName}_$timestamp.$extension"
        val mime = mimeFor(extension, contentType)
        val subfolder = if (useSubfolder) "/StaticRadio" else ""

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + subfolder)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("cannot create file in Downloads")
            val stream = resolver.openOutputStream(uri) ?: throw IOException("cannot open file")
            Output(stream, displayName) { keep ->
                if (keep) {
                    val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                    runCatching { resolver.update(uri, done, null, null) }
                } else {
                    runCatching { resolver.delete(uri, null, null) }
                }
            }
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), subfolder.trimStart('/'))
            if (!dir.exists() && !dir.mkdirs()) throw IOException("cannot create Download folder")
            val file = File(dir, displayName)
            Output(FileOutputStream(file), displayName) { keep ->
                if (keep) {
                    MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mime), null)
                } else {
                    file.delete()
                }
            }
        }
    }
}
