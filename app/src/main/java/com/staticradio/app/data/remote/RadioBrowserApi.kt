package com.staticradio.app.data.remote

import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Talks to the public Radio Browser API (radio-browser.info), a community-run
 * directory of internet radio streams. No API key required.
 *
 * Previously hardcoded to a single mirror (de1.api.radio-browser.info). That
 * mirror is no longer in the current DNS pool returned by
 * all.api.radio-browser.info, so requests to it were failing/timing out —
 * search silently returned nothing, with no visible error, because a
 * connect failure and "zero results" looked identical to the caller.
 * Radio-browser.info's own docs say clients should resolve the mirror list
 * via a DNS lookup of all.api.radio-browser.info rather than hardcode one
 * host, since individual mirrors come and go. This does that lookup once,
 * lazily, and falls back through the discovered mirrors (then a couple of
 * long-lived known-good hosts as a last resort) if the first one fails.
 */
class RadioBrowserApi(
    private val client: OkHttpClient = OkHttpClient()
) {
    private val json = Json { ignoreUnknownKeys = true }

    // Last-resort hosts if DNS-based mirror discovery itself fails (e.g. no
    // DNS resolver reachable). These are long-running mirrors as of 2026 but
    // are only used as a fallback, never hardcoded as *the* endpoint.
    private val fallbackHosts = listOf(
        "de2.api.radio-browser.info",
        "nl1.api.radio-browser.info",
        "at1.api.radio-browser.info"
    )

    @Volatile
    private var cachedMirrors: List<String>? = null

    private suspend fun resolveMirrors(): List<String> = withContext(Dispatchers.IO) {
        cachedMirrors?.let { return@withContext it }
        val discovered = try {
            InetAddress.getAllByName("all.api.radio-browser.info")
                .mapNotNull { it.canonicalHostName }
                .filter { it.isNotBlank() && it != "all.api.radio-browser.info" }
                .distinct()
        } catch (e: UnknownHostException) {
            emptyList()
        }
        val mirrors = (discovered.ifEmpty { fallbackHosts })
        cachedMirrors = mirrors
        mirrors
    }

    /**
     * Tries each mirror in turn until one returns a successful response.
     * Only gives up (throwing) once every mirror has failed, so a single
     * dead/unreachable mirror doesn't silently look like "no results".
     */
    private suspend fun <T> queryWithFallback(
        buildUrl: (host: String) -> String,
        decode: (String) -> T
    ): T = withContext(Dispatchers.IO) {
        val mirrors = resolveMirrors()
        var lastError: Exception? = null
        for (host in mirrors) {
            val result: T? = try {
                val request = Request.Builder()
                    .url(buildUrl(host))
                    // radio-browser.info's usage guidelines ask for a descriptive User-Agent.
                    .header("User-Agent", "StaticRadioApp/0.1 (Android; personal weekend project)")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        lastError = IOException("HTTP ${response.code} from $host")
                        null
                    } else {
                        val body = response.body?.string()
                        if (body == null) {
                            lastError = IOException("Empty response from $host")
                            null
                        } else {
                            decode(body)
                        }
                    }
                }
            } catch (e: Exception) {
                lastError = e
                null
            }
            if (result != null) return@withContext result
        }
        throw lastError ?: IOException("No Radio Browser mirrors available")
    }

    suspend fun searchStations(query: String, limit: Int = 30): List<RadioBrowserStation> =
        queryWithFallback(
            buildUrl = { host ->
                "https://$host/json/stations/search".toHttpUrl().newBuilder()
                    .addQueryParameter("name", query)
                    .addQueryParameter("limit", limit.toString())
                    .addQueryParameter("hidebroken", "true")
                    .addQueryParameter("order", "clickcount")
                    .addQueryParameter("reverse", "true")
                    .build()
                    .toString()
            },
            decode = { body -> json.decodeFromString(body) }
        )

    /** Same advanced-search endpoint, filtered by community-provided tag instead of station name. */
    suspend fun searchStationsByTag(tag: String, limit: Int = 30): List<RadioBrowserStation> =
        queryWithFallback(
            buildUrl = { host ->
                "https://$host/json/stations/search".toHttpUrl().newBuilder()
                    .addQueryParameter("tag", tag)
                    .addQueryParameter("limit", limit.toString())
                    .addQueryParameter("hidebroken", "true")
                    .addQueryParameter("order", "clickcount")
                    .addQueryParameter("reverse", "true")
                    .build()
                    .toString()
            },
            decode = { body -> json.decodeFromString(body) }
        )
}
