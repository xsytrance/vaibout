package com.xsytrance.vaib.core.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

/**
 * Atmospheric model that paints the UI — background, glow, particles.
 * Now driven by [StationTheme] (or custom, as in the original [Default]).
 */
data class VaibAtmosphere(
    val primaryColor:     Color,
    val secondaryColor:   Color,
    val glowColor:        Color,
    val backgroundAccent: Color,
    val particleGlyphs:   List<String>,
) {
    companion object {
        /** Original default atmosphere — preserved for backward compatibility. */
        val Default = StationTheme.NEON_CYAN.toAtmosphere()

        /** Generates a gradient from [primaryColor] → [secondaryColor] for card headers. */
        fun VaibAtmosphere.cardGradient(): Brush = Brush.verticalGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.35f),
                secondaryColor.copy(alpha = 0.15f),
                backgroundAccent,
            ),
        )

        /** Generates a sweeping gradient for the Now Playing hero area. */
        fun VaibAtmosphere.heroGradient(): Brush = Brush.radialGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.18f),
                secondaryColor.copy(alpha = 0.08f),
                Color.Transparent,
            ),
            center = androidx.compose.ui.geometry.Offset.Unspecified,
            radius = 0.8f,
        )
    }
}