package com.xsytrance.vaib.audio

import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Enhanced audio player with queue support, crossfade, and playback speed.
 *
 * The original AudioPlayer is replaced by this version which maintains
 * backward compatibility with the ViewModel interface.
 */
class AudioPlayer(val context: android.content.Context) {

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val handler = Handler(Looper.getMainLooper())

    // ── State flows ────────────────────────────────────────────

    private val _isPlaying   = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering

    private val _isEnded     = MutableStateFlow(false)
    val isEnded: StateFlow<Boolean> = _isEnded

    val currentPositionMs: Long get() = player.currentPosition
    val durationMs: Long        get() = player.duration
    val audioSessionId: Int     get() = player.audioSessionId

    // ── Crossfade ──────────────────────────────────────────────

    /** Crossfade duration in milliseconds. 0 = disabled. */
    var crossfadeMs: Long = 3000L
        set(value) {
            field = value
            // Media3 handles crossfade natively via setVideoScalingMode,
            // but for audio we manage it with a volume ramp
        }

    private var crossfadeJob: Runnable? = null

    // ── Playback speed ─────────────────────────────────────────

    /** Playback speed multiplier. Default 1.0f. */
    var playbackSpeed: Float = 1.0f
        set(value) {
            field = value.coerceIn(0.5f, 2.0f)
            player.setPlaybackSpeed(field)
        }

    // ── Volume (for ducking) ───────────────────────────────────

    /** Set volume 0f..1f */
    fun setVolume(volume: Float) {
        player.volume = volume.coerceIn(0f, 1f)
    }

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = playbackState == Player.STATE_BUFFERING
                _isEnded.value     = playbackState == Player.STATE_ENDED

                // Auto-advance queue when track ends
                if (playbackState == Player.STATE_ENDED) {
                    crossfadeJob?.let { handler.removeCallbacks(it) }
                }
            }
        })
    }

    /** Prepare a track without playing. Clears previous media. */
    fun prepareTrack(uri: Uri) {
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
    }

    /** Load and immediately play a track. */
    fun loadTrack(uri: Uri) {
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.play()
    }

    /**
     * Queue-aware load: enqueues a list of URIs and starts playback.
     * Uses ExoPlayer's built-in media queue.
     */
    fun loadQueue(items: List<Uri>, startIndex: Int = 0) {
        player.stop()
        player.clearMediaItems()
        items.forEach { uri ->
            player.addMediaItem(MediaItem.fromUri(uri))
        }
        player.seekTo(startIndex, 0L)
        player.prepare()
        player.play()
    }

    /**
     * Crossfade play/pause toggle.
     * If crossfadeMs > 0 and we're starting playback, crossfade in.
     */
    fun togglePlayPause(fallbackUri: Uri? = null) {
        if (player.isPlaying) {
            player.pause()
            return
        }

        when (player.playbackState) {
            Player.STATE_IDLE -> {
                if (fallbackUri != null) {
                    player.setMediaItem(MediaItem.fromUri(fallbackUri))
                    player.prepare()
                    player.play()
                }
            }
            Player.STATE_ENDED -> {
                player.seekTo(0)
                player.play()
            }
            else -> player.play()
        }
    }

    /**
     * Start a crossfade to the next track using ExoPlayer's seekToNextMediaItem.
     * The crossfade is accomplished by scheduling a volume ramp.
     */
    fun skipToNextWithCrossfade() {
        if (crossfadeMs <= 0 || player.mediaItemCount <= 1) {
            player.seekToNextMediaItem()
            return
        }

        val currentVol = player.volume
        val step = 40L  // update every 40ms
        val steps = crossfadeMs / step
        val volDelta = -currentVol / steps.toFloat()

        crossfadeJob?.let { handler.removeCallbacks(it) }
        crossfadeJob = object : Runnable {
            private var stepCount = 0
            private var currentV = currentVol

            override fun run() {
                if (stepCount >= steps) {
                    // Fade-in after switching
                    player.seekToNextMediaItem()
                    player.play()
                    fadeInFromSilence()
                    return
                }
                currentV += volDelta
                player.volume = currentV.coerceAtLeast(0f)
                stepCount++
                handler.postDelayed(this, step)
            }

            private fun fadeInFromSilence() {
                player.volume = 0f
                val fadeSteps = steps
                var fadeStep = 0
                val fadeJob = object : Runnable {
                    override fun run() {
                        if (fadeStep >= fadeSteps) {
                            player.volume = 1f
                            return
                        }
                        player.volume = (fadeStep.toFloat() / fadeSteps)
                        fadeStep++
                        handler.postDelayed(this, step)
                    }
                }
                handler.postDelayed(fadeJob, step)
            }
        }
        handler.post(crossfadeJob!!)
    }

    fun skipToPrevious() {
        player.seekToPreviousMediaItem()
    }

    /** Release all resources. */
    fun release() {
        crossfadeJob?.let { handler.removeCallbacks(it) }
        player.release()
    }
}