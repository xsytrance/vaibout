package com.xsytrance.vaib.core.design

import androidx.compose.ui.graphics.Color

/**
 * Describes the visual atmosphere for a vAIb moment — colors, glow, and particle glyphs.
 * vAIb cards supply a custom atmosphere to "paint" the UI without layout changes.
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

        /** Chill — cyan / soft blue / calm notes */
        val Chill = VaibAtmosphere(
            primaryColor     = Color(0xFF4DD0E1),
            secondaryColor   = Color(0xFF90CAF9),
            glowColor        = Color(0xFF4DD0E1).copy(alpha = 0.24f),
            backgroundAccent = Color(0xFF040E14),
            particleGlyphs   = listOf("♪", "♫", "~"),
        )

        /** Deep — indigo / violet / sparse particles */
        val Deep = VaibAtmosphere(
            primaryColor     = Color(0xFF7C4DFF),
            secondaryColor   = Color(0xFF651FFF),
            glowColor        = Color(0xFF7C4DFF).copy(alpha = 0.22f),
            backgroundAccent = Color(0xFF080414),
            particleGlyphs   = listOf("♪", "·"),
        )

        /** Energetic — cyan / magenta / faster feel */
        val Energetic = VaibAtmosphere(
            primaryColor     = Color(0xFF00E5FF),
            secondaryColor   = Color(0xFFFF00AA),
            glowColor        = Color(0xFF00E5FF).copy(alpha = 0.30f),
            backgroundAccent = Color(0xFF0E0408),
            particleGlyphs   = listOf("♪", "♫", "♬", "✦"),
        )

        /** Cosmic — violet / amber / star-like particles */
        val Cosmic = VaibAtmosphere(
            primaryColor     = Color(0xFFFFB74D),
            secondaryColor   = Color(0xFFCE93D8),
            glowColor        = Color(0xFFFFB74D).copy(alpha = 0.26f),
            backgroundAccent = Color(0xFF0A0610),
            particleGlyphs   = listOf("✦", "☆", "✧"),
        )

        /** Focus — teal / white / minimal particles */
        val Focus = VaibAtmosphere(
            primaryColor     = Color(0xFF80CBC4),
            secondaryColor   = Color(0xFFE0E0E0),
            glowColor        = Color(0xFF80CBC4).copy(alpha = 0.18f),
            backgroundAccent = Color(0xFF040A08),
            particleGlyphs   = listOf("·"),
        )

        /**
         * Maps a mood label to its atmosphere palette.
         * Falls back to [Default] for unknown or empty moods.
         */
        fun fromMood(mood: String): VaibAtmosphere {
            return when (mood.trim().lowercase()) {
                "chill"      -> Chill
                "deep"       -> Deep
                "energetic"  -> Energetic
                "cosmic"     -> Cosmic
                "focus"      -> Focus
                else         -> Default
            }
        }
    }
}
