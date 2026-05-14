package com.xsytrance.vaib.core.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * VisualSignalEngine — centralizes visual energy for the vAIb out! UI.
 *
 * Consumes real audio data (energy, beatPulse) from AudioVisualizerAnalyzer,
 * adds simulated ambient breathing, and exposes clean signals for UI reactivity.
 *
 * Phase 1: Uses existing analyzer data + simulation. No new dependencies.
 * Phase 2: Can connect to real BPM/key detection when available.
 *
 * All signals are 0f..1f Float. Safe for Compose animations.
 */
class VisualSignalEngine(
    energyFlow: StateFlow<Float>,
    beatPulseFlow: StateFlow<Float>,
    private val isPlayingFlow: StateFlow<Boolean>,
) {

    /** Current audio energy (0..1). Smoothed RMS from analyzer. */
    val energy: StateFlow<Float> = energyFlow

    /** Beat pulse (0..1). 1f on transient, decays over ~400ms. */
    val beatPulse: StateFlow<Float> = beatPulseFlow

    /** Whether audio is actively painting the UI. */
    val isActive: StateFlow<Boolean> = isPlayingFlow

    private val _ambientBreath = MutableStateFlow(0.5f)
    private val _intensity = MutableStateFlow(0.3f)
    private val _beatPhase = MutableStateFlow(0f)

    /** Simulated ambient breath cycle (0..1). Slow sinusoidal. */
    val ambientBreath: StateFlow<Float> = _ambientBreath.asStateFlow()

    /** Combined intensity: energy + breath blend (0..1). */
    val intensity: StateFlow<Float> = _intensity.asStateFlow()

    /** Simulated beat clock phase (0..1). Used for visual sync. */
    val beatPhase: StateFlow<Float> = _beatPhase.asStateFlow()

    /** Update ambient breath value. Call from a coroutine ticker. */
    fun updateAmbientBreath(phase: Float) {
        val breath = (0.5f + 0.5f * sin(phase * 2.0 * PI)).toFloat()
        _ambientBreath.value = breath.coerceIn(0f, 1f)
        recalculateIntensity()
    }

    /** Update beat phase. Call from a coroutine ticker. */
    fun updateBeatPhase(phase: Float) {
        _beatPhase.value = phase.coerceIn(0f, 1f)
    }

    private fun recalculateIntensity() {
        val e = energy.value
        val b = _ambientBreath.value
        _intensity.value = (e * 0.6f + b * 0.4f).coerceIn(0f, 1f)
    }
}

// ── Compose helpers — safe wrappers for UI ────────────────────────────

/**
 * Remember a VisualSignalEngine from ViewModel flows.
 * Creates the engine once, updates with current data.
 */
@Composable
fun rememberVisualSignalEngine(
    energy: StateFlow<Float>,
    beatPulse: StateFlow<Float>,
    isPlaying: StateFlow<Boolean>,
): VisualSignalEngine {
    return remember { VisualSignalEngine(energy, beatPulse, isPlaying) }
}

/**
 * Ambient breathing animation — slow sinusoidal cycle.
 * Returns 0f..1f. Use for background opacity, glow pulsing.
 */
@Composable
fun ambientBreathAnimation(durationMs: Int = 4_000): Float {
    val transition = rememberInfiniteTransition(label = "ambientBreath")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMs, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "ambientBreath",
    )
    return (0.5f + 0.5f * sin(phase * 2.0 * PI)).toFloat().coerceIn(0f, 1f)
}

/**
 * Beat pulse animation — blends real beat with simulated periodic pulse.
 * Returns 0f..1f. Use for button glow, particle pop.
 */
@Composable
fun beatPulseAnimation(beatPulseFlow: StateFlow<Float>): Float {
    val beat by beatPulseFlow.collectAsState()
    val transition = rememberInfiniteTransition(label = "beatClock")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            tween(960, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "beatClock",
    )
    val simPulse = abs(sin(phase * PI * 2.0)).toFloat()
    return (beat * 0.7f + simPulse * 0.3f).coerceIn(0f, 1f)
}

/**
 * Energy-glow value for backgrounds and borders.
 * Blends real energy with ambient breath.
 * Returns 0f..1f.
 */
@Composable
fun energyGlow(energyFlow: StateFlow<Float>, breathValue: Float): Float {
    val energy by energyFlow.collectAsState()
    return (energy * 0.5f + breathValue * 0.3f + 0.2f).coerceIn(0f, 1f)
}

/**
 * Pulsing alpha for buttons and UI elements.
 * Base alpha + pulse boost when beat hits.
 * Returns 0f..1f.
 */
@Composable
fun pulsingAlpha(beatPulseFlow: StateFlow<Float>, baseAlpha: Float): Float {
    val beat by beatPulseFlow.collectAsState()
    return (baseAlpha + beat * 0.25f).coerceIn(0f, 1f)
}

/**
 * Glow color for borders and backgrounds — pulses with beat.
 * Returns a Color with alpha modulated by energy.
 */
@Composable
fun glowColor(color: Color, energyFlow: StateFlow<Float>, beatPulseFlow: StateFlow<Float>): Color {
    val energy by energyFlow.collectAsState()
    val beat by beatPulseFlow.collectAsState()
    val alpha = (0.15f + energy * 0.35f + beat * 0.20f).coerceIn(0f, 1f)
    return color.copy(alpha = alpha)
}
