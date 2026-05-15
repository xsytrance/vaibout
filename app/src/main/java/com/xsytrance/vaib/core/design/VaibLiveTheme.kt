package com.xsytrance.vaib.core.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

/**
 * VaibLiveTheme — animated color morphing system for vAIb out!.
 *
 * Creates smoothly animated Color values that transition whenever
 * the track/mood/atmosphere changes. Uses animateColorAsState with
 * ~800ms tween for buttery transitions.
 *
 * Apply the returned LiveColors to backgrounds, borders, visualizers,
 * and buttons for a theme that morphs smoothly between songs.
 */
object VaibLiveTheme {

    data class LiveColors(
        val primary: Color,
        val secondary: Color,
        val glow: Color,
        val border: Color,
        val backgroundAccent: Color,
    )

    /** Duration for color transitions in milliseconds. */
    const val MORPH_DURATION_MS = 800

    /**
     * Create animated live colors from a VaibAtmosphere.
     * Colors smoothly transition whenever the atmosphere changes.
     */
    @Composable
    fun animateFrom(atmosphere: VaibAtmosphere): LiveColors {
        val primary: Color by animateColorAsState(
            targetValue    = atmosphere.primaryColor,
            animationSpec  = tween(MORPH_DURATION_MS),
            label          = "livePrimary",
        )
        val secondary: Color by animateColorAsState(
            targetValue    = atmosphere.secondaryColor,
            animationSpec  = tween(MORPH_DURATION_MS),
            label          = "liveSecondary",
        )
        val glow: Color by animateColorAsState(
            targetValue    = atmosphere.glowColor,
            animationSpec  = tween(MORPH_DURATION_MS),
            label          = "liveGlow",
        )
        val border: Color by animateColorAsState(
            targetValue    = atmosphere.primaryColor.copy(alpha = 0.22f),
            animationSpec  = tween(MORPH_DURATION_MS),
            label          = "liveBorder",
        )
        val bgAccent: Color by animateColorAsState(
            targetValue    = atmosphere.backgroundAccent,
            animationSpec  = tween(MORPH_DURATION_MS),
            label          = "liveBgAccent",
        )
        return LiveColors(
            primary          = primary,
            secondary        = secondary,
            glow             = glow,
            border           = border,
            backgroundAccent = bgAccent,
        )
    }
}
