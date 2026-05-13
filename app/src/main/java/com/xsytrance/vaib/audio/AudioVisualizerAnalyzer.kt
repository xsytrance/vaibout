package com.xsytrance.vaib.audio

import android.media.audiofx.Visualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sqrt

/**
 * Lightweight wrapper around [Visualizer] that computes a normalised RMS energy
 * value (0f..1f) from the waveform capture and emits it as a [StateFlow].
 *
 * Lifecycle: call [start] when entering Solo Dreamscape (after RECORD_AUDIO is
 * granted and a track is prepared). Call [stop] when leaving. [audioSessionId]
 * must be non-zero (ExoPlayer assigns a real ID only after prepare()).
 */
class AudioVisualizerAnalyzer {

    private var visualizer: Visualizer? = null

    private val _energy = MutableStateFlow(0f)
    val energy: StateFlow<Float> = _energy

    /** Returns true if the Visualizer was successfully attached. */
    fun start(audioSessionId: Int): Boolean {
        if (audioSessionId == 0) return false
        stop()
        return try {
            visualizer = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[0]
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            vis: Visualizer,
                            waveform: ByteArray,
                            samplingRate: Int,
                        ) {
                            // Waveform bytes: unsigned 0..255, silence centered at 128.
                            // Compute RMS of deviation from silence.
                            var sumSq = 0f
                            for (b in waveform) {
                                val sample = (b.toInt() and 0xFF) - 128f
                                sumSq += sample * sample
                            }
                            val rms = sqrt(sumSq / waveform.size) / 128f
                            // Exponential smoothing to avoid choppy jumps.
                            _energy.value = (_energy.value * 0.6f + rms * 0.4f)
                                .coerceIn(0f, 1f)
                        }

                        override fun onFftDataCapture(
                            vis: Visualizer,
                            fft: ByteArray,
                            samplingRate: Int,
                        ) = Unit
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    true,   // waveform
                    false,  // fft
                )
                enabled = true
            }
            true
        } catch (_: Exception) {
            visualizer = null
            false
        }
    }

    fun stop() {
        visualizer?.apply {
            enabled = false
            release()
        }
        visualizer = null
        _energy.value = 0f
    }
}
