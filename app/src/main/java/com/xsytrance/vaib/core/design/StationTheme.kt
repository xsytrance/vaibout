package com.xsytrance.vaib.core.design

import androidx.compose.ui.graphics.Color

/**
 * Themed palettes that each [Station] can use to paint the UI.
 * Used by [VaibAtmosphere], [VaibTheme], and every visual component.
 */
enum class StationTheme(
    val primary: Color,
    val secondary: Color,
    val glow: Color,
    val backgroundAccent: Color,
    val particleGlyphs: List<String>,
    val label: String,           // human-readable name
    val gradientColors: List<Color>,  // for station thumbnail cards
) {
    // ── Default / Neon Cyan ──────────────────────────────────
    NEON_CYAN(
        primary             = Color(0xFF00E5FF),
        secondary           = Color(0xFF8B5CF6),
        glow                = Color(0xFF00E5FF).copy(alpha = 0.28f),
        backgroundAccent    = Color(0xFF040E10),
        particleGlyphs      = listOf("♪", "♫", "♬"),
        label               = "Neon Cyan",
        gradientColors      = VaibColors.GradientDeep,
    ),

    // ── Deep Violet ──────────────────────────────────────────
    DEEP_VIOLET(
        primary             = Color(0xFFA78BFA),
        secondary           = Color(0xFF6366F1),
        glow                = Color(0xFFA78BFA).copy(alpha = 0.25f),
        backgroundAccent    = Color(0xFF0A081E),
        particleGlyphs      = listOf("✦", "♦", "◆"),
        label               = "Deep Violet",
        gradientColors      = VaibColors.GradientCrimson,
    ),

    // ── Teal Aurora ──────────────────────────────────────────
    TEAL_AURORA(
        primary             = Color(0xFF2DD4BF),
        secondary           = Color(0xFF06D6A0),
        glow                = Color(0xFF2DD4BF).copy(alpha = 0.25f),
        backgroundAccent    = Color(0xFF041A16),
        particleGlyphs      = listOf("🌊", "◌", "○"),
        label               = "Teal Aurora",
        gradientColors      = VaibColors.GradientOlive,
    ),

    // ── Amber Sunset ────────────────────────────────────────
    AMBER_SUNSET(
        primary             = Color(0xFFF59E0B),
        secondary           = Color(0xFFEF4444),
        glow                = Color(0xFFF59E0B).copy(alpha = 0.22f),
        backgroundAccent    = Color(0xFF1A0A04),
        particleGlyphs      = listOf("☀", "✦", "⚡"),
        label               = "Amber Sunset",
        gradientColors      = VaibColors.GradientSunset,
    ),

    // ── Emerald Forest ───────────────────────────────────────
    EMERALD_FOREST(
        primary             = Color(0xFF10B981),
        secondary           = Color(0xFF34D399),
        glow                = Color(0xFF10B981).copy(alpha = 0.24f),
        backgroundAccent    = Color(0xFF0A1A12),
        particleGlyphs      = listOf("🍃", "♣", "✿"),
        label               = "Emerald Forest",
        gradientColors      = VaibColors.GradientForest,
    ),

    // ── Cosmic Indigo ────────────────────────────────────────
    COSMIC_INDIGO(
        primary             = Color(0xFF6366F1),
        secondary           = Color(0xFFEC4899),
        glow                = Color(0xFF6366F1).copy(alpha = 0.26f),
        backgroundAccent    = Color(0xFF0C0828),
        particleGlyphs      = listOf("✨", "⭐", "🌟"),
        label               = "Cosmic Indigo",
        gradientColors      = VaibColors.GradientOcean,
    ),

    // ── Rose Dusk ────────────────────────────────────────────
    ROSE_DUSK(
        primary             = Color(0xFFF43F5E),
        secondary           = Color(0xFFA855F7),
        glow                = Color(0xFFF43F5E).copy(alpha = 0.24f),
        backgroundAccent    = Color(0xFF1A0508),
        particleGlyphs      = listOf("♥", "♡", "💫"),
        label               = "Rose Dusk",
        gradientColors      = VaibColors.GradientEmber,
    );

    fun toAtmosphere(): VaibAtmosphere = VaibAtmosphere(
        primaryColor     = primary,
        secondaryColor   = secondary,
        glowColor        = glow,
        backgroundAccent = backgroundAccent,
        particleGlyphs   = particleGlyphs,
    )

    companion object {
        val DEFAULT = NEON_CYAN

        val STATION_THEMES = entries.toTypedArray()

        fun fromOrdinal(ordinal: Int): StationTheme =
            entries.getOrElse(ordinal) { DEFAULT }
    }
}