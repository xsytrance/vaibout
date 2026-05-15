package com.xsytrance.vaib.core.design

import androidx.compose.ui.geometry.Offset

/**
 * Dreamdeck touch interaction models — lightweight data classes
 * for tap ripples, drag trails, and long-press glow.
 *
 * All mutable state lives in the screen composable (remember).
 * These are immutable snapshots for rendering.
 */

data class TouchRipple(
    val id: Long,
    val x: Float,
    val y: Float,
    val startedAt: Long,
    val colorIdx: Int = 0,
)

data class TouchTrailPoint(
    val x: Float,
    val y: Float,
    val age: Float,        // 0f = fresh, 1f = faded
    val pressure: Float,   // 0f..1f for thickness
)

data class BeatRing(
    val id: Long,
    val startedAt: Long,
    val x: Float = 0.5f,  // 0..1 normalized center
    val y: Float = 0.5f,
)
