package com.xsytrance.vaib.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AudioPlayer(context: Context) {

    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _isPlaying   = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering

    private val _isEnded     = MutableStateFlow(false)
    val isEnded: StateFlow<Boolean> = _isEnded

    val currentPositionMs: Long get() = player.currentPosition
    val durationMs: Long        get() = player.duration

    // Available after prepare(); 0 means not yet assigned.
    val audioSessionId: Int get() = player.audioSessionId

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = playbackState == Player.STATE_BUFFERING
                _isEnded.value     = playbackState == Player.STATE_ENDED
            }
        })
    }

    fun loadTrack(uri: Uri) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.play()
    }

    /** Prepares the track without starting playback. Used for vAIb recall / restore. */
    fun prepareTrack(uri: Uri) {
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
    }

    /**
     * Play/pause with lazy-prepare support.
     *
     * [fallbackUri] is used when the player is in STATE_IDLE (e.g. an IA stream
     * that was restored from prefs but never prepared). In that case this method
     * loads and plays the URI in one step so the user doesn't have to re-enter
     * Discover or tap a card.
     *
     * STATE_ENDED is also handled: seeking to 0 replays a finished track.
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

    fun release() {
        player.release()
    }
}
