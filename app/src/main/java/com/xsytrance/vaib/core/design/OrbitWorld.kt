package com.xsytrance.vaib.core.design

import androidx.compose.ui.graphics.Color

/**
 * OrbitWorld — defines the worlds/stations in Orbit.
 *
 * Each world has unique colors, a search query, and a fallback query
 * for when the primary query returns no results.
 */
enum class OrbitWorld(
    val label: String,
    val subtitle: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val query: String,
    val fallbackQuery: String,
) {
    ALL(
        label          = "All",
        subtitle       = "open music",
        primaryColor   = Color(0xFF00E5FF),
        secondaryColor = Color(0xFFB388FF),
        query          = "",
        fallbackQuery  = "",
    ),
    CHILL(
        label          = "Chill",
        subtitle       = "calm ambient waves",
        primaryColor   = Color(0xFF4DD0E1),
        secondaryColor = Color(0xFF90CAF9),
        query          = "chill ambient relaxing calm",
        fallbackQuery  = "ambient",
    ),
    COSMIC(
        label          = "Cosmic",
        subtitle       = "space experimental signals",
        primaryColor   = Color(0xFFFFB74D),
        secondaryColor = Color(0xFFCE93D8),
        query          = "cosmic space ambient experimental",
        fallbackQuery  = "electronic",
    ),
    DEEP(
        label          = "Deep",
        subtitle       = "dub atmospheric low",
        primaryColor   = Color(0xFF7C4DFF),
        secondaryColor = Color(0xFF651FFF),
        query          = "deep dub atmospheric low",
        fallbackQuery  = "bass",
    ),
    FOCUS(
        label          = "Focus",
        subtitle       = "instrumental minimal",
        primaryColor   = Color(0xFF80CBC4),
        secondaryColor = Color(0xFFE0E0E0),
        query          = "instrumental minimal focus",
        fallbackQuery  = "instrumental",
    ),
    ENERGETIC(
        label          = "Energetic",
        subtitle       = "upbeat electronic",
        primaryColor   = Color(0xFF00E5FF),
        secondaryColor = Color(0xFFFF00AA),
        query          = "upbeat electronic energetic",
        fallbackQuery  = "dance",
    ),
    OPEN_ARCHIVE(
        label          = "Open Archive",
        subtitle       = "curated open music",
        primaryColor   = Color(0xFF69F0AE),
        secondaryColor = Color(0xFF00E5FF),
        query          = "",
        fallbackQuery  = "",
    ),
    LOCAL_FILES(
        label          = "Local Files",
        subtitle       = "your music",
        primaryColor   = Color(0xFF80DEEA),
        secondaryColor = Color(0xFF4DD0E1),
        query          = "",
        fallbackQuery  = "",
    );

    companion object {
        /** Ordered list of worlds as shown in the carousel. */
        val DISPLAY_ORDER = listOf(ALL, CHILL, COSMIC, DEEP, FOCUS, ENERGETIC, OPEN_ARCHIVE, LOCAL_FILES)

        fun fromLabel(label: String): OrbitWorld {
            return entries.firstOrNull { it.label == label } ?: ALL
        }
    }
}
