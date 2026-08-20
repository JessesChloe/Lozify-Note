package com.witte.lozify.core.media

import android.content.Context
import android.media.MediaPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audio playback status enum.
 */
enum class AudioPlaybackStatus {
    IDLE,
    PLAYING,
    PAUSED,
    COMPLETED,
    ERROR
}

/**
 * State for global audio player.
 */
data class AudioPlayerState(
    val status: AudioPlaybackStatus = AudioPlaybackStatus.IDLE,
    val currentFilePath: String? = null,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val errorMessage: String? = null
)

/**
 * AudioPlayerManager - Global singleton audio player for note audio attachments.
 *
 * Stage 48: In-app music playback with play/pause/stop, real-time progress, and seek support.
 */
@Singleton
class AudioPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(AudioPlayerState())
    val playbackState: StateFlow<AudioPlayerState> = _playbackState.asStateFlow()

    /**
     * Play or pause an audio file.
     */
    fun playOrPause(file: File) {
        val current = _playbackState.value
        if (current.currentFilePath == file.absolutePath) {
            when (current.status) {
                AudioPlaybackStatus.PLAYING -> pause()
                AudioPlaybackStatus.PAUSED -> resume()
                else -> startNewPlayback(file)
            }
        } else {
            startNewPlayback(file)
        }
    }

    private fun startNewPlayback(file: File) {
        stop()
        if (!file.exists()) {
            _playbackState.value = AudioPlayerState(
                status = AudioPlaybackStatus.ERROR,
                currentFilePath = file.absolutePath,
                errorMessage = "音频文件不存在"
            )
            return
        }

        try {
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    _playbackState.value = _playbackState.value.copy(
                        status = AudioPlaybackStatus.COMPLETED,
                        currentPositionMs = duration.toLong()
                    )
                    stopProgressTracking()
                }
                setOnErrorListener { _, what, extra ->
                    _playbackState.value = AudioPlayerState(
                        status = AudioPlaybackStatus.ERROR,
                        errorMessage = "播放错误 ($what, $extra)"
                    )
                    stopProgressTracking()
                    true
                }
            }

            mediaPlayer = player
            player.start()

            _playbackState.value = AudioPlayerState(
                status = AudioPlaybackStatus.PLAYING,
                currentFilePath = file.absolutePath,
                currentPositionMs = 0L,
                durationMs = player.duration.toLong()
            )

            startProgressTracking()
        } catch (e: Exception) {
            _playbackState.value = AudioPlayerState(
                status = AudioPlaybackStatus.ERROR,
                errorMessage = e.message ?: "无法播放音频"
            )
        }
    }

    /**
     * Pause currently playing audio.
     */
    fun pause() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.pause()
                    _playbackState.value = _playbackState.value.copy(
                        status = AudioPlaybackStatus.PAUSED,
                        currentPositionMs = player.currentPosition.toLong()
                    )
                    stopProgressTracking()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Resume paused audio.
     */
    fun resume() {
        mediaPlayer?.let { player ->
            try {
                player.start()
                _playbackState.value = _playbackState.value.copy(
                    status = AudioPlaybackStatus.PLAYING
                )
                startProgressTracking()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Stop and reset audio playback.
     */
    fun stop() {
        stopProgressTracking()
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaPlayer = null
        _playbackState.value = AudioPlayerState(status = AudioPlaybackStatus.IDLE)
    }

    /**
     * Seek to specific position in milliseconds.
     */
    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { player ->
            try {
                player.seekTo(positionMs.toInt())
                _playbackState.value = _playbackState.value.copy(
                    currentPositionMs = positionMs
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startProgressTracking() {
        stopProgressTracking()
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            _playbackState.value = _playbackState.value.copy(
                                currentPositionMs = player.currentPosition.toLong(),
                                durationMs = player.duration.toLong()
                            )
                        }
                    } catch (e: Exception) {
                        // Player may be released
                    }
                }
                delay(100)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }
}
