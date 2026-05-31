package com.xsytrance.vaib.audio

import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.pow
import kotlin.math.sqrt

private const val TAG       = "VaibVisualizer"
const val NUM_BANDS         = 32

class AudioVisualizerAnalyzer {

    private var visualizer: Visualizer? = null

    private val _energy     = MutableStateFlow(0f)
    private val _beatPulse  = MutableStateFlow(0f)
    private val _freqBands  = MutableStateFlow(FloatArray(NUM_BANDS))

    val energy:    StateFlow<Float>      = _energy
    val beatPulse: StateFlow<Float>      = _beatPulse
    val freqBands: StateFlow<FloatArray> = _freqBands

    private var slowEnergy    = 0f
    private var beatDebounce  = 0
    private val smoothedBands = FloatArray(NUM_BANDS)

    fun start(audioSessionId: Int): Boolean {
        if (audioSessionId == 0) {
            Log.w(TAG, "start() rejected — audioSessionId is 0")
            return false
        }
        stop()
        return try {
            visualizer = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(1024)
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            vis: Visualizer, waveform: ByteArray, samplingRate: Int,
                        ) {
                            var sumSq = 0f
                            for (b in waveform) {
                                val s = (b.toInt() and 0xFF) - 128f
                                sumSq += s * s
                            }
                            val rms = sqrt(sumSq / waveform.size) / 128f
                            _energy.value = (_energy.value * 0.60f + rms * 0.40f).coerceIn(0f, 1f)
                            slowEnergy    = slowEnergy * 0.92f + rms * 0.08f
                            _beatPulse.value = (_beatPulse.value * 0.55f).coerceAtLeast(0f)
                            if (beatDebounce <= 0 && rms > 0.05f && rms > slowEnergy * 1.45f) {
                                _beatPulse.value = 1f
                                beatDebounce     = 3
                            } else if (beatDebounce > 0) {
                                beatDebounce--
                            }
                        }

                        override fun onFftDataCapture(
                            vis: Visualizer, fft: ByteArray, samplingRate: Int,
                        ) {
                            processFft(fft)
                        }
                    },
                    Visualizer.getMaxCaptureRate(),
                    waveform = true,
                    fft      = true,
                )
                enabled = true
            }
            Log.d(TAG, "Visualizer attached to session $audioSessionId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Visualizer attach failed: ${e.message}")
            visualizer = null
            false
        }
    }

    private fun processFft(fft: ByteArray) {
        val n       = fft.size
        val numBins = n / 2 - 1
        if (numBins <= 0) return
        for (band in 0 until NUM_BANDS) {
            val startBin = numBins.toDouble().pow(band.toDouble() / NUM_BANDS)
                .toInt().coerceAtLeast(1)
            val endBin   = numBins.toDouble().pow((band + 1.0) / NUM_BANDS)
                .toInt().coerceIn(startBin + 1, numBins)
            var mag   = 0f
            var count = 0
            for (bin in startBin until endBin) {
                val idx = bin * 2
                if (idx + 1 < n) {
                    val re = fft[idx].toFloat()
                    val im = fft[idx + 1].toFloat()
                    mag  += sqrt(re * re + im * im)
                    count++
                }
            }
            val raw = if (count > 0) (mag / count / 128f).coerceIn(0f, 1f) else 0f
            smoothedBands[band] = smoothedBands[band] * 0.65f + raw * 0.35f
        }
        _freqBands.value = smoothedBands.copyOf()
    }

    fun stop() {
        visualizer?.apply { enabled = false; release() }
        visualizer = null
        slowEnergy = 0f
        beatDebounce = 0
        smoothedBands.fill(0f)
        _energy.value    = 0f
        _beatPulse.value = 0f
        _freqBands.value = FloatArray(NUM_BANDS)
    }
}
