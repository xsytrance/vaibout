package com.xsytrance.vaib.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AudioPlayer(context: Context) {

    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _isPlaying      = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isBuffering     = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering

    private val _isEnded         = MutableStateFlow(false)
    val isEnded: StateFlow<Boolean> = _isEnded

    private val _mediaMetadata   = MutableStateFlow(MediaMetadata.EMPTY)
    val mediaMetadata: StateFlow<MediaMetadata> = _mediaMetadata

    val currentPositionMs: Long get() = player.currentPosition
    val durationMs: Long        get() = player.duration
    val audioSessionId: Int     get() = player.audioSessionId

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = playbackState == Player.STATE_BUFFERING
                _isEnded.value     = playbackState == Player.STATE_ENDED
            }
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                _mediaMetadata.value = mediaMetadata
            }
        })
    }

    fun loadTrack(uri: Uri) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.play()
    }

    fun prepareTrack(uri: Uri) {
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
    }

    fun togglePlayPause(fallbackUri: Uri? = null) {
        if (player.isPlaying) { player.pause(); return }
        when (player.playbackState) {
            Player.STATE_IDLE -> if (fallbackUri != null) {
                player.setMediaItem(MediaItem.fromUri(fallbackUri))
                player.prepare()
                player.play()
            }
            Player.STATE_ENDED -> { player.seekTo(0); player.play() }
            else -> player.play()
        }
    }

    fun release() = player.release()
}
