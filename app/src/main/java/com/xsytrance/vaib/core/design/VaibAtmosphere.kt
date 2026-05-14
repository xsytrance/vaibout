package com.xsytrance.vaib.core.design

import androidx.compose.ui.graphics.Color

/**
 * Describes the visual atmosphere for a vAIb moment — colors, glow, and particle glyphs.
 * Future vAIb cards can supply a custom atmosphere to "paint" the UI without layout changes.
 */
data class VaibAtmosphere(
    val primaryColor:     Color,
    val secondaryColor:   Color,
    val glowColor:        Color,
    val backgroundAccent: Color,
    val particleGlyphs:   List<String>,
) {
    companion object {
        /** Default cyan/violet OLED atmosphere matching the current design language. */
        val Default = VaibAtmosphere(
            primaryColor     = VaibColors.CyanPulse,
            secondaryColor   = VaibColors.VioletGlow,
            glowColor        = VaibColors.CyanPulse.copy(alpha = 0.28f),
            backgroundAccent = Color(0xFF040E10),
            particleGlyphs   = listOf("♪", "♫", "♬"),
        )
    }
}
