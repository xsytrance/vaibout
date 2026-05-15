package com.xsytrance.vaib.visualizer

/**
 * Available visualizer rendering styles.
 *
 * Each style maps to a distinct GPU shader / rendering strategy.
 */
enum class VisualizerStyle {
    /**
     * Nebula — concentric glow rings + beat ripple + particle drift.
     * The classic vAIb look, now enhanced with bloom and color shifting.
     */
    NEBULA,

    /**
     * Waveform — time-domain waveform with 3D perspective projection.
     * Renders the raw audio waveform as a rolling landscape.
     */
    WAVEFORM,

    /**
     * Particles — GPU particle system with beat-reactive spawning.
     * Dots that flow, explode on beats, and emit light trails.
     */
    PARTICLES;

    companion object {
        val DEFAULT = NEBULA

        fun fromOrdinalSafe(ordinal: Int): VisualizerStyle =
            entries.getOrElse(ordinal) { DEFAULT }
    }
}