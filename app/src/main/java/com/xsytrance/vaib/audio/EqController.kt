package com.xsytrance.vaib.audio

import android.media.audiofx.Equalizer
import android.util.Log

private const val TAG = "VaibEQ"

/**
 * Manages an [Equalizer] audio effect attached to an ExoPlayer audio session.
 * Safe to call even if the device does not support hardware EQ — all errors
 * are caught and logged, playback is never interrupted.
 */
class EqController {

    private var equalizer: Equalizer? = null

    /**
     * Attaches a new Equalizer to [audioSessionId] and applies [preset].
     * Releases any previously held Equalizer first.
     * Returns true on success, false if unsupported or session is unavailable.
     */
    fun apply(audioSessionId: Int, preset: EqPreset): Boolean {
        if (audioSessionId == 0) {
            Log.w(TAG, "apply() skipped — audioSessionId is 0")
            return false
        }
        release()
        return try {
            equalizer = Equalizer(0, audioSessionId).apply {
                val range = bandLevelRange          // ShortArray [min, max] in mB
                val minMb = range[0].toFloat()
                val maxMb = range[1].toFloat()
                val bandCount = numberOfBands.toInt()
                preset.bands.forEachIndexed { i, rawMb ->
                    if (i < bandCount) {
                        val clamped = rawMb.toFloat().coerceIn(minMb, maxMb).toInt().toShort()
                        setBandLevel(i.toShort(), clamped)
                    }
                }
                enabled = true
            }
            Log.d(TAG, "Applied preset=${preset.name} session=$audioSessionId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "EQ apply failed for preset=${preset.name}: ${e.message}")
            equalizer = null
            false
        }
    }

    fun release() {
        equalizer?.runCatching { enabled = false; release() }
        equalizer = null
    }
}
