package com.xsytrance.vaib.core.design

import androidx.compose.ui.graphics.Color

/**
 * Full chromatic color system for vAIb.
 *
 * Each Station gets its own [StationPalette] derived from a theme.
 * These are the foundation swatches — actual runtime colors are
 * resolved through [StationTheme] / [VaibAtmosphere].
 */
object VaibColors {
    // ── Neutrals ──────────────────────────────────────────
    val Black          = Color(0xFF000000)
    val DeepBackground = Color(0xFF05070A)
    val Surface        = Color(0xFF0C1016)
    val SurfaceVariant = Color(0xFF141B24)
    val SurfaceElevated = Color(0xFF1A2230)

    // ── Primary channel (cyan family) ─────────────────────
    val CyanPrimary    = Color(0xFF00E5FF)
    val CyanLight      = Color(0xFF66F0FF)
    val CyanDark       = Color(0xFF009DAA)
    val CyanPulse      = CyanPrimary  // legacy alias

    // ── Secondary channel (violet family) ─────────────────
    val VioletGlow     = Color(0xFF8B5CF6)
    val VioletLight    = Color(0xFFA78BFA)
    val VioletDark     = Color(0xFF6D28D9)

    // ── Accent palette (per-station rotation) ─────────────
    val TealAccent     = Color(0xFF2DD4BF)
    val IndigoAccent   = Color(0xFF6366F1)
    val AmberAccent    = Color(0xFFF59E0B)
    val RoseAccent     = Color(0xFFF43F5E)
    val EmeraldAccent  = Color(0xFF10B981)
    val SkyAccent      = Color(0xFF38BDF8)

    // ── Mood-specific ─────────────────────────────────────
    val DeepMood       = Color(0xFF1E3A5F)
    val ChillMood      = Color(0xFF1A3D3D)
    val EnergyMood     = Color(0xFF8B2C4A)
    val CosmicMood     = Color(0xFF2D1B69)
    val FocusMood      = Color(0xFF1B3A2D)

    // ── Text ──────────────────────────────────────────────
    val TextPrimary    = Color(0xFFE8ECF2)
    val TextSecondary  = Color(0xFF9AA4B2)
    val TextTertiary   = Color(0xFF6B7280)
    val TextDisabled   = Color(0xFF4B5563)

    // ── Semantic ──────────────────────────────────────────
    val Success        = Color(0xFF34D399)
    val Warning        = Color(0xFFFBBF24)
    val Error          = Color(0xFFF87171)
    val Info           = Color(0xFF60A5FA)

    // ── Gradients (station thumbnails) ────────────────────
    val GradientDeep     = listOf(Color(0xFF041420), Color(0xFF0E0524))
    val GradientForest   = listOf(Color(0xFF051A0E), Color(0xFF050F20))
    val GradientCrimson  = listOf(Color(0xFF18040E), Color(0xFF070420))
    val GradientOlive    = listOf(Color(0xFF0E1204), Color(0xFF04121A))
    val GradientEmber    = listOf(Color(0xFF160A04), Color(0xFF06041A))
    val GradientOcean    = listOf(Color(0xFF041A28), Color(0xFF081438))
    val GradientSunset   = listOf(Color(0xFF2A0A0E), Color(0xFF1A0828))
    val GradientAurora   = listOf(Color(0xFF0A1628), Color(0xFF0E2A18))
}