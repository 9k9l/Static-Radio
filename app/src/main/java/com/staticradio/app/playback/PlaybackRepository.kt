package com.staticradio.app.playback

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton-scoped state bridge between the playback Service and the UI layer.
 * The Service pushes updates here; Compose screens collect from here.
 * Keeping this separate from the Service means the UI never binds directly
 * to Service internals — only to this repository via DI (Hilt/Koin/manual).
 */
class PlaybackRepository {

    private val _currentStationId = MutableStateFlow<String?>(null)
    val currentStationId: StateFlow<String?> = _currentStationId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _nowPlayingText = MutableStateFlow<String?>(null)
    val nowPlayingText: StateFlow<String?> = _nowPlayingText.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private val _sleepTimerEndAtMillis = MutableStateFlow<Long?>(null)
    val sleepTimerEndAtMillis: StateFlow<Long?> = _sleepTimerEndAtMillis.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0L)
    val recordingSeconds: StateFlow<Long> = _recordingSeconds.asStateFlow()

    private val _recordingBytes = MutableStateFlow(0L)
    val recordingBytes: StateFlow<Long> = _recordingBytes.asStateFlow()

    // One-shot user-facing messages about recording (saved / failed).
    private val _recordingMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val recordingMessages: SharedFlow<String> = _recordingMessages.asSharedFlow()

    fun updateRecordingState(recording: Boolean) {
        _isRecording.value = recording
        if (!recording) { _recordingSeconds.value = 0; _recordingBytes.value = 0 }
    }

    fun updateRecordingProgress(seconds: Long, bytes: Long) {
        _recordingSeconds.value = seconds
        _recordingBytes.value = bytes
    }

    fun emitRecordingMessage(message: String) {
        _recordingMessages.tryEmit(message)
    }

    fun updateCurrentStation(stationId: String?) {
        _currentStationId.value = stationId
        _nowPlayingText.value = null // clear stale metadata from previous station
    }

    fun updatePlayingState(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    fun updateBufferingState(isBuffering: Boolean) {
        _isBuffering.value = isBuffering
    }

    fun updateNowPlaying(text: String?) {
        _nowPlayingText.value = text
    }

    fun updateError(message: String?) {
        _playbackError.value = message
    }

    fun updateSleepTimerEndAt(endAtMillis: Long?) {
        _sleepTimerEndAtMillis.value = endAtMillis
    }

    companion object {
        // Simple manual singleton for the skeleton. Swap for Hilt @Singleton
        // once you wire up DI — recommended once the app grows past this stage.
        @Volatile private var instance: PlaybackRepository? = null

        fun getInstance(): PlaybackRepository =
            instance ?: synchronized(this) {
                instance ?: PlaybackRepository().also { instance = it }
            }
    }
}
