package com.xsytrance.vaib.core.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * OrbitAtmosphereLayer — floating musical note particles for the Discover/Orbit screen.
 *
 * Soft glowing notes drift upward like carbonation in soda.
 * Periodic beat pulse causes notes to briefly brighten and pop.
 * Mood-tinted colors shift based on the active atmosphere.
 *
 * Performance: Canvas-based, ~24 particles, single animation loop.
 * Designed for smooth 60fps on S24 Ultra.
 */
@Composable
fun OrbitAtmosphereLayer(
    moodColor: Color = VaibColors.CyanPulse,
    secondaryMoodColor: Color = VaibColors.VioletGlow,
    energy: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val particleCount = (12 + (energy * 18f).toInt()).coerceIn(12, 30)
    val particles = remember(particleCount) { createParticles(particleCount) }

    // Primary drift animation (continuous rising)
    val driftTransition = rememberInfiniteTransition(label = "atmosphereDrift")
    val driftPhase by driftTransition.animateFloat(
        initialValue    = 0f,
        targetValue     = 1f,
        animationSpec   = infiniteRepeatable(
            tween(20_000, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "drift",
    )

    // Beat pulse (periodic pop every ~960ms)
    val pulseTransition = rememberInfiniteTransition(label = "atmospherePulse")
    val pulsePhase by pulseTransition.animateFloat(
        initialValue    = 0f,
        targetValue     = 1f,
        animationSpec   = infiniteRepeatable(
            tween(960, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "pulse",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        for (p in particles) {
            drawParticle(
                particle      = p,
                driftPhase    = driftPhase,
                pulsePhase    = pulsePhase,
                canvasW       = w,
                canvasH       = h,
                moodColor     = moodColor,
                secondaryColor = secondaryMoodColor,
                energy        = energy,
                textMeasurer  = textMeasurer,
            )
        }
    }
}

// ── Particle data ─────────────────────────────────────────────────────

private data class NoteParticle(
    val glyph: String,
    val baseX: Float,      // 0..1 normalized start position
    val baseY: Float,      // 0..1 normalized start position
    val size: Float,       // 8..18 sp
    val driftSpeed: Float, // 0.3..1.0
    val swayAmp: Float,    // horizontal sway amplitude
    val swayFreq: Float,   // sway frequency
    val baseAlpha: Float,  // 0.04..0.14
    val pulseReactive: Boolean, // brightens on beat
    val colorMix: Float,   // 0=moodColor, 1=secondaryColor
)

private val GLYPHS = listOf("♪", "♫", "♬", "♩", "·", "◦", "◌")

private fun createParticles(count: Int): List<NoteParticle> {
    val rng = Random(42) // deterministic seed
    return List(count) { i ->
        NoteParticle(
            glyph         = GLYPHS[rng.nextInt(GLYPHS.size)],
            baseX         = rng.nextFloat(),
            baseY         = 0.8f + rng.nextFloat() * 0.3f, // start near bottom, some off-screen
            size          = 8f + rng.nextFloat() * 10f,
            driftSpeed    = 0.3f + rng.nextFloat() * 0.7f,
            swayAmp       = 12f + rng.nextFloat() * 28f,
            swayFreq      = 0.5f + rng.nextFloat() * 1.5f,
            baseAlpha     = 0.04f + rng.nextFloat() * 0.10f,
            pulseReactive = rng.nextFloat() > 0.4f, // 60% react to pulse
            colorMix      = rng.nextFloat(),
        )
    }
}

// ── Single particle draw ──────────────────────────────────────────────

private fun DrawScope.drawParticle(
    particle: NoteParticle,
    driftPhase: Float,
    pulsePhase: Float,
    canvasW: Float,
    canvasH: Float,
    moodColor: Color,
    secondaryColor: Color,
    energy: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    // Vertical drift: rise upward, wrap around
    val cycleOffset = (driftPhase * particle.driftSpeed) % 1f
    val yNorm = (particle.baseY - cycleOffset + 1f) % 1f
    val y = yNorm * canvasH

    // Horizontal sway
    val sway = sin(yNorm * PI * 2f * particle.swayFreq).toFloat() * particle.swayAmp
    val x = (particle.baseX * canvasW) + sway

    // Skip if off-screen
    if (x < -30f || x > canvasW + 30f || y < -20f || y > canvasH + 20f) return

    // Beat pulse: brief brightening
    val pulseIntensity = if (particle.pulseReactive) {
        val pulseWave = abs(sin(pulsePhase * PI * 2f)).toFloat()
        1f + pulseWave * 0.6f // 1.0x to 1.6x alpha
    } else 1f

    // Fade at top and bottom edges
    val edgeFade = when {
        yNorm > 0.85f -> (1f - yNorm) / 0.15f
        yNorm < 0.08f -> yNorm / 0.08f
        else -> 1f
    }

    val alpha = (particle.baseAlpha * pulseIntensity * edgeFade * (0.4f + energy * 1.5f)).coerceIn(0.01f, 0.45f)

    // Color: mix mood + secondary
    val color = if (particle.colorMix < 0.5f) {
        moodColor.copy(alpha = alpha)
    } else {
        secondaryColor.copy(alpha = alpha)
    }

    // Draw glyph as text
    val style = TextStyle(
        color      = color,
        fontSize   = particle.size.sp,
        fontWeight = FontWeight.Light,
    )
    val textLayout = textMeasurer.measure(particle.glyph, style)
    val textSize = textLayout.size.toSize()

    withTransform({
        translate(left = x - textSize.width / 2f, top = y - textSize.height / 2f)
    }) {
        drawText(textLayout)
    }
}
