package com.xsytrance.vaib.audio

import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sqrt

private const val TAG = "VaibVisualizer"

/**
 * Lightweight wrapper around [Visualizer] that emits two reactive values:
 *  - [energy]    — normalised RMS amplitude (0..1), smoothed at ~10 Hz.
 *  - [beatPulse] — 1f on a detected transient, decaying to 0 within ~400ms.
 *
 * Beat detection: fires when instantaneous RMS rises ≥ 45 % above a slow
 * background EMA (τ ≈ 1.2 s), with a 300 ms debounce to suppress re-triggers.
 *
 * Lifecycle: call [start] after RECORD_AUDIO is granted and ExoPlayer has
 * called prepare() (audioSessionId must be non-zero). Call [stop] on exit.
 */
class AudioVisualizerAnalyzer {

    private var visualizer: Visualizer? = null

    private val _energy    = MutableStateFlow(0f)
    private val _beatPulse = MutableStateFlow(0f)

    val energy:    StateFlow<Float> = _energy
    val beatPulse: StateFlow<Float> = _beatPulse

    // Beat detection state — only touched in the Visualizer callback thread.
    private var slowEnergy    = 0f
    private var beatDebounce  = 0   // countdown in callback ticks (~100 ms each)
    private var logTickCounter = 0  // throttle periodic logging to ~every 3 s

    /** Returns true if the Visualizer attached successfully. */
    fun start(audioSessionId: Int): Boolean {
        Log.d(TAG, "start() audioSessionId=$audioSessionId")
        if (audioSessionId == 0) {
            Log.w(TAG, "start() rejected — audioSessionId is 0 (player not yet prepared)")
            return false
        }
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
                            // Waveform bytes are unsigned 0–255, centred at 128 (silence).
                            var sumSq = 0f
                            for (b in waveform) {
                                val s = (b.toInt() and 0xFF) - 128f
                                sumSq += s * s
                            }
                            val rms = sqrt(sumSq / waveform.size) / 128f

                            // Fast-smoothed energy for continuous visual modulation.
                            _energy.value = (_energy.value * 0.60f + rms * 0.40f)
                                .coerceIn(0f, 1f)

                            // Slow background EMA (τ ≈ 1.2 s at 10 Hz callbacks).
                            slowEnergy = slowEnergy * 0.92f + rms * 0.08f

                            // Decay the beat pulse each tick.
                            _beatPulse.value = (_beatPulse.value * 0.55f)
                                .coerceAtLeast(0f)

                            // Transient: sharp rise well above background, not silence.
                            if (beatDebounce <= 0
                                && rms > 0.05f
                                && rms > slowEnergy * 1.45f
                            ) {
                                _beatPulse.value = 1f
                                beatDebounce = 3   // ≈ 300 ms
                            } else if (beatDebounce > 0) {
                                beatDebounce--
                            }

                            // Periodic diagnostic log (~every 3 s at 10 Hz callbacks).
                            if (++logTickCounter % 30 == 0) {
                                Log.d(TAG, "energy=%.3f rms=%.3f beat=%.2f slowEnergy=%.3f waveformSize=${waveform.size}".format(
                                    _energy.value, rms, _beatPulse.value, slowEnergy
                                ))
                            }
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
            Log.d(TAG, "Visualizer attached successfully to session $audioSessionId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Visualizer attach failed: ${e.message}")
            visualizer = null
            false
        }
    }

    fun stop() {
        Log.d(TAG, "stop() releasing Visualizer")
        visualizer?.apply { enabled = false; release() }
        visualizer      = null
        slowEnergy      = 0f
        beatDebounce    = 0
        logTickCounter  = 0
        _energy.value    = 0f
        _beatPulse.value = 0f
    }
}
