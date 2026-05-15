package com.xsytrance.vaib.core.design

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI

/**
 * Motion specification tokens for the entire app.
 * All animations reference these — no ad-hoc durations/easings.
 */
object MotionTokens {
    // ── Durations ─────────────────────────────────────────
    val Instant      = 0
    val Quick        = 150
    val Standard     = 250
    val Express      = 380
    val Leisurely    = 500
    val AmbientMin   = 8_000L
    val AmbientMax   = 30_000L

    // ── Easings ───────────────────────────────────────────
    val Linear            = LinearEasing
    val FastOutSlowIn     = FastOutSlowInEasing
    val FastOutLinearIn   = FastOutLinearInEasing
    val Overshoot         = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
    val Bounce            = CubicBezierEasing(0.34f, 1.6f, 0.6f, 1f)

    // ── Standard transitions ──────────────────────────────
    @Composable
    fun standardTransition(): FiniteAnimationSpec<Float> =
        tween<Float>(durationMillis = Standard, easing = FastOutSlowIn)

    @Composable
    fun quickTransition(): FiniteAnimationSpec<Float> =
        tween<Float>(durationMillis = Quick, easing = FastOutLinearIn)

    @Composable
    fun expressTransition(): FiniteAnimationSpec<Float> =
        tween<Float>(durationMillis = Express, easing = FastOutSlowIn)

    @Composable
    fun springTransition(damping: Float = 0.7f): FiniteAnimationSpec<Float> =
        spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = 0.01f)

    // ── Ambient infinite loops ────────────────────────────
    @Composable
    fun ambientLoop(durationMs: Long = AmbientMin): InfiniteTransition {
        return rememberInfiniteTransition(label = "ambient")
    }

    @Composable
    fun ambientPhase(
        from: Float = 0f,
        to: Float = 1f,
        durationMs: Long = AmbientMin,
        label: String = "ambientPhase"
    ): State<Float> {
        val transition = rememberInfiniteTransition(label = "ambient_${label}")
        return transition.animateFloat(
            initialValue = from,
            targetValue  = to,
            animationSpec = infiniteRepeatable(
                tween(durationMillis = durationMs.toInt(), easing = LinearEasing),
                RepeatMode.Restart
            ),
            label = label
        )
    }
}

// ── Haptic feedback constants ─────────────────────────────
object HapticTokens {
    const val LIGHT_TAP  = "CLICK"
    const val MEDIUM_TAP = "TICK"
    const val HEAVY_TAP  = "THUD"
}

// ── Corner / shape tokens ─────────────────────────────────
object ShapeTokens {
    val Small      = 8.dp
    val Medium     = 12.dp
    val Large      = 16.dp
    val XLarge     = 22.dp
    val Circle     = 50
}

// ── Spacing tokens ────────────────────────────────────────
object SpacingTokens {
    val XSmall   = 2.dp
    val Small    = 4.dp
    val Medium   = 8.dp
    val Large    = 12.dp
    val XLarge   = 16.dp
    val XXLarge  = 20.dp
    val XXXLarge = 24.dp
    val XXXXLarge = 32.dp
}
