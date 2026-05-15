package com.xsytrance.vaib.data.entities

import com.xsytrance.vaib.visualizer.VisualizerStyle

/**
 * Compact model used in the VisualizerStyle picker and saved-vAIb displays.
 */
data class VisualizerStyleInfo(
    val style: VisualizerStyle,
    val label: String,
    val description: String,
    val icon: String,
)

/**
 * Pre-built style metadata for the picker UI.
 */
val VISUALIZER_STYLES = listOf(
    VisualizerStyleInfo(
        style = VisualizerStyle.NEBULA,
        label = "Nebula",
        description = "Glowing rings + beat ripple",
        icon = "🌌",
    ),
    VisualizerStyleInfo(
        style = VisualizerStyle.WAVEFORM,
        label = "Waveform",
        description = "3D perspective waveform",
        icon = "〰️",
    ),
    VisualizerStyleInfo(
        style = VisualizerStyle.PARTICLES,
        label = "Particles",
        description = "Beat-reactive particle system",
        icon = "✨",
    ),
)
