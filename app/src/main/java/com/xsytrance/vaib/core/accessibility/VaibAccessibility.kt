package com.xsytrance.vaib.core.accessibility

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.graphics.Color

/**
 * Accessibility helpers for vAIb — content descriptions, roles, and state.
 */
object VaibAccessibility {

    // ── Content descriptions ──────────────────────────────────

    fun Modifier.playButton() = this.semantics {
        contentDescription = "Play"
        role = Role.Button
    }

    fun Modifier.pauseButton() = this.semantics {
        contentDescription = "Pause"
        role = Role.Button
    }

    fun Modifier.skipNextButton() = this.semantics {
        contentDescription = "Skip to next track"
        role = Role.Button
    }

    fun Modifier.skipPreviousButton() = this.semantics {
        contentDescription = "Skip to previous track"
        role = Role.Button
    }

    fun Modifier.favoriteButton(isFavorite: Boolean) = this.semantics {
        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites"
        role = Role.Button
    }

    fun Modifier.searchButton() = this.semantics {
        contentDescription = "Search"
        role = Role.Button
    }

    fun Modifier.backButton() = this.semantics {
        contentDescription = "Go back"
        role = Role.Button
    }

    fun Modifier.closeButton() = this.semantics {
        contentDescription = "Close"
        role = Role.Button
    }

    fun Modifier.settingsButton() = this.semantics {
        contentDescription = "Settings"
        role = Role.Button
    }

    fun Modifier.libraryButton() = this.semantics {
        contentDescription = "Library"
        role = Role.Button
    }

    fun Modifier.discoverButton() = this.semantics {
        contentDescription = "Discover music"
        role = Role.Button
    }

    fun Modifier.visualizerStyleButton(style: String) = this.semantics {
        contentDescription = "Visualizer style: $style"
        role = Role.Button
    }

    fun Modifier.stationCard(name: String, trackCount: Int) = this.semantics {
        contentDescription = "Station: $name, $trackCount tracks"
        role = Role.Button
    }

    fun Modifier.trackItem(title: String, artist: String?) = this.semantics {
        contentDescription = "Track: $title${if (!artist.isNullOrBlank()) " by $artist" else ""}"
        role = Role.Button
    }

    fun Modifier.moodChip(mood: String, isSelected: Boolean) = this.semantics {
        contentDescription = "Mood: $mood${if (isSelected) ", selected" else ""}"
        role = Role.Button
    }

    fun Modifier.seekBar(position: Long, duration: Long) = this.semantics {
        contentDescription = "Seek bar, ${position / 1000} seconds of ${duration / 1000} seconds"
        role = Role.Slider
    }

    fun Modifier.energyIndicator(level: Float) = this.semantics {
        contentDescription = "Audio energy level: ${(level * 100).toInt()} percent"
    }

    // ── High contrast support ─────────────────────────────────

    /** Returns a color with guaranteed minimum contrast ratio. */
    fun ensureContrast(foreground: Color, background: Color, minRatio: Float = 4.5f): Color {
        val bgLuminance = background.luminance()
        val fgLuminance = foreground.luminance()
        val ratio = if (bgLuminance > fgLuminance) {
            (bgLuminance + 0.05f) / (fgLuminance + 0.05f)
        } else {
            (fgLuminance + 0.05f) / (bgLuminance + 0.05f)
        }
        return if (ratio >= minRatio) foreground else {
            // Adjust towards white or black to meet contrast
            if (bgLuminance < 0.5f) Color.White else Color.Black
        }
    }

    private fun Color.luminance(): Float {
        val r = red.linearize()
        val g = green.linearize()
        val b = blue.linearize()
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    private fun Float.linearize(): Float {
        return if (this <= 0.03928f) this / 12.92f
        else Math.pow(((this + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    }
}
