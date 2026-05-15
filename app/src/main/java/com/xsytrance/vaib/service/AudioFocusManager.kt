package com.xsytrance.vaib.service

import android.content.Context
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.util.Log
import com.xsytrance.vaib.audio.AudioPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages audio focus (ducking, interruption handling) for vAIb.
 *
 * When vAIb has audio focus:
 *   - Other apps may [AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK] → we reduce volume
 *   - If they take [AUDIOFOCUS_LOSS] → we pause and wait for reclaim
 *   - When focus returns → we resume at the correct position
 *
 * This is the recommended Android pattern per the [AudioManager] docs.
 */
class AudioFocusManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var player: AudioPlayer? = null
    private var pausedByFocusLoss = false

    // Current ducked volume level (1.0 = full, 0.0 = silent)
    private val _duckVolume = MutableStateFlow(1f)
    val duckVolume: StateFlow<Float> = _duckVolume

    private val focusListener = OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "AUDIOFOCUS_GAIN — resuming")
                if (pausedByFocusLoss) {
                    player?.togglePlayPause()
                    pausedByFocusLoss = false
                }
                _duckVolume.value = 1f
                // Restore volume if we were ducking
                player?.setVolume(1f)
            }

            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> {
                Log.d(TAG, "AUDIOFOCUS_GAIN_TRANSIENT — resuming at ducked volume")
                if (pausedByFocusLoss) {
                    player?.togglePlayPause()
                    pausedByFocusLoss = false
                }
                _duckVolume.value = 1f
                player?.setVolume(1f)
            }

            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> {
                Log.d(TAG, "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK — ducking volume")
                _duckVolume.value = 0.3f
                player?.setVolume(0.3f)
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT — pausing temporarily")
                pausedByFocusLoss = player?.isPlaying() == true
                player?.pause()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK — ducking")
                _duckVolume.value = 0.3f
                player?.setVolume(0.3f)
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "AUDIOFOCUS_LOSS — pausing, will resume on reclaim")
                pausedByFocusLoss = player?.isPlaying() == true
                player?.pause()
                _duckVolume.value = 0f
            }
        }
    }

    /**
     * Must be called after [AudioPlayer] is initialized.
     */
    fun bindPlayer(player: AudioPlayer) {
        this.player = player
    }

    /**
     * Request audio focus before starting playback.
     * Returns true if focus was granted.
     */
    fun requestFocus(): Boolean {
        val result = audioManager.requestAudioFocus(
            focusListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN,
        )
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    /**
     * Abandon audio focus when playback stops or app closes.
     */
    fun abandonFocus() {
        audioManager.abandonAudioFocus(focusListener)
        pausedByFocusLoss = false
        _duckVolume.value = 1f
    }

    companion object {
        private const val TAG = "VaibFocus"
    }
}

// ── Convenience extensions ──────────────────────────────────

private fun AudioPlayer.isPlaying(): Boolean =
    this.player.isPlaying

private fun AudioPlayer.pause() {
    if (this.player.isPlaying) this.player.pause()
}

private fun AudioPlayer.setVolume(volume: Float) {
    // ExoPlayer volume is 0f–1f
    this.player.volume = volume
}