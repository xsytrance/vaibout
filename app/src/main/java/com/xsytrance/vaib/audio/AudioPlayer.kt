package com.xsytrance.vaib.audio

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val TAG = "VaibAudioPlayer"

class AudioPlayer(context: Context) {

    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _isPlaying   = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering

    private val _isEnded     = MutableStateFlow(false)
    val isEnded: StateFlow<Boolean> = _isEnded

    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionIdFlow: StateFlow<Int> = _audioSessionId

    val currentPositionMs: Long get() = player.currentPosition
    val durationMs: Long        get() = player.duration

    // Available after prepare(); 0 means not yet assigned.
    val audioSessionId: Int get() = player.audioSessionId

    /** Called when audio session ID changes — visualizer/EQ must reattach. */
    var onAudioSessionIdChanged: ((Int) -> Unit)? = null

    init {
        // Configure audio focus: music playback, handle focus automatically
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        player.setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
        player.setHandleAudioBecomingNoisy(true)
        Log.d(TAG, "Audio focus configured: USAGE_MEDIA, handleAudioFocus=true, becomingNoisy=true")

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = playbackState == Player.STATE_BUFFERING
                _isEnded.value     = playbackState == Player.STATE_ENDED
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                Log.d(TAG, "onAudioSessionIdChanged: $audioSessionId")
                _audioSessionId.value = audioSessionId
                if (audioSessionId != 0) {
                    onAudioSessionIdChanged?.invoke(audioSessionId)
                }
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
