package com.xsytrance.vaib.core.design

import androidx.compose.ui.graphics.Color

/**
 * TrackPaint — deterministic visual paint for a song/card.
 *
 * Every track gets its own color family, glyphs, vibe label, and energy
 * derived from its title + artist/source. Same track = same paint every time.
 *
 * This is the visual grammar that future AI passes can enhance.
 */
data class TrackPaint(
    val primaryColor:   Color,
    val secondaryColor: Color,
    val glowColor:      Color,
    val borderColor:    Color,
    val glyphs:         List<String>,
    val vibeLabel:      String,
    val connectionLabel: String,
    val energy:         Float,
) {
    companion object {

        // ── Color families ──────────────────────────────────────────

        private val CHILL = PaintFamily(
            primary   = Color(0xFF4DD0E1),
            secondary = Color(0xFF90CAF9),
            glow      = Color(0xFF4DD0E1).copy(alpha = 0.24f),
            keywords  = listOf("chill", "ambient", "relax", "calm", "soft", "breeze",
                               "lofi", "lo-fi", "sleep", "dream", "gentle", "peace"),
            vibeLabels = listOf("chill wave", "soft drift", "calm signal", "breeze tone"),
            glyphs    = listOf("♪", "♫", "~", "◦"),
        )

        private val COSMIC = PaintFamily(
            primary   = Color(0xFFFFB74D),
            secondary = Color(0xFFCE93D8),
            glow      = Color(0xFFFFB74D).copy(alpha = 0.26f),
            keywords  = listOf("cosmic", "space", "star", "galaxy", "nebula", "orbit",
                               "astro", "celestial", "universe", "solar", "lunar"),
            vibeLabels = listOf("cosmic drift", "star signal", "orbit tone", "nebula wave"),
            glyphs    = listOf("✦", "☆", "✧", "◌"),
        )

        private val DEEP = PaintFamily(
            primary   = Color(0xFF7C4DFF),
            secondary = Color(0xFF651FFF),
            glow      = Color(0xFF7C4DFF).copy(alpha = 0.22f),
            keywords  = listOf("deep", "dub", "dark", "low", "sub", "bass",
                               "underground", "shadow", "night", "void", "drown"),
            vibeLabels = listOf("deep cut", "sub signal", "dark drift", "void tone"),
            glyphs    = listOf("♪", "·", "◆", "◇"),
        )

        private val FOCUS = PaintFamily(
            primary   = Color(0xFF80CBC4),
            secondary = Color(0xFFE0E0E0),
            glow      = Color(0xFF80CBC4).copy(alpha = 0.18f),
            keywords  = listOf("focus", "minimal", "instrumental", "study", "work",
                               "concentrate", "meditation", "quiet", "still"),
            vibeLabels = listOf("focus signal", "minimal wave", "quiet tone", "still drift"),
            glyphs    = listOf("·", "◦", "─", "│"),
        )

        private val ENERGETIC = PaintFamily(
            primary   = Color(0xFF00E5FF),
            secondary = Color(0xFFFF00AA),
            glow      = Color(0xFF00E5FF).copy(alpha = 0.30f),
            keywords  = listOf("energetic", "upbeat", "electronic", "dance", "fast",
                               "high", "power", "pulse", "drive", "beat", "tempo"),
            vibeLabels = listOf("pulse signal", "drive wave", "fast tone", "beat drift"),
            glyphs    = listOf("♪", "♫", "♬", "✦"),
        )

        private val WARM = PaintFamily(
            primary   = Color(0xFFFF8A65),
            secondary = Color(0xFFFFB74D),
            glow      = Color(0xFFFF8A65).copy(alpha = 0.24f),
            keywords  = listOf("warm", "acoustic", "folk", "guitar", "piano", "jazz",
                               "soul", "funk", "groove", "vibe", "organic"),
            vibeLabels = listOf("warm signal", "organic wave", "soul tone", "groove drift"),
            glyphs    = listOf("♪", "♫", "~", "✦"),
        )

        private val ALL_FAMILIES = listOf(CHILL, COSMIC, DEEP, FOCUS, ENERGETIC, WARM)

        private val CONNECTION_LABELS = listOf(
            "open archive signal",
            "connected by mood",
            "nearby vibe",
            "deep cut",
            "cosmic branch",
            "chill link",
            "orbit signal",
            "branch out",
        )

        // ── Mood → family shortcut ──────────────────────────────────

        private fun familyFromMood(mood: String): PaintFamily? {
            return when (mood.trim().lowercase()) {
                "chill"     -> CHILL
                "cosmic"    -> COSMIC
                "deep"      -> DEEP
                "focus"     -> FOCUS
                "energetic" -> ENERGETIC
                else        -> null
            }
        }

        // ── Public paint factory ────────────────────────────────────

        /**
         * Creates a deterministic [TrackPaint] from track metadata.
         * Same inputs produce the same paint every time.
         *
         * @param title   Track title
         * @param creator Artist or source name
         * @param sourceType "LOCAL" | "INTERNET_ARCHIVE" | etc.
         * @param moodHint Saved mood tag if available
         */
        fun fromTrack(
            title: String,
            creator: String = "",
            sourceType: String = "",
            moodHint: String = "",
        ): TrackPaint {
            // 1. Try mood hint first (most reliable)
            val family = familyFromMood(moodHint)
                // 2. Fall back to keyword matching in title + creator
                ?: matchFamily(title + " " + creator)
                // 3. Final fallback: hash-based selection
                ?: hashFamily(title + creator)

            val seed = safeHash(title + creator)

            return TrackPaint(
                primaryColor   = family.primary,
                secondaryColor = family.secondary,
                glowColor      = family.glow,
                borderColor    = family.primary.copy(alpha = 0.22f),
                glyphs         = family.glyphs,
                vibeLabel      = family.vibeLabels[seed % family.vibeLabels.size],
                connectionLabel = CONNECTION_LABELS[seed % CONNECTION_LABELS.size],
                energy         = 0.3f + (seed % 7).toFloat() * 0.1f, // 0.3–0.9
            )
        }

        /** Convenience: paint from an Internet Archive item. */
        fun fromArchiveItem(item: com.xsytrance.vaib.discover.ArchiveItem): TrackPaint {
            return fromTrack(title = item.title, creator = item.creator, sourceType = "INTERNET_ARCHIVE")
        }

        /** Convenience: paint from a saved vAIb entity. */
        fun fromVaibEntity(vaib: com.xsytrance.vaib.data.entities.VaibEntity): TrackPaint {
            return fromTrack(
                title      = vaib.trackName,
                creator    = "",
                sourceType = vaib.sourceType,
                moodHint   = vaib.mood,
            )
        }

        // ── Internal helpers ────────────────────────────────────────

        private fun matchFamily(text: String): PaintFamily? {
            val lower = text.lowercase()
            return ALL_FAMILIES.firstOrNull { family ->
                family.keywords.any { kw -> lower.contains(kw) }
            }
        }

        private fun hashFamily(seed: String): PaintFamily {
            val idx = safeHash(seed) % ALL_FAMILIES.size
            return ALL_FAMILIES[idx]
        }

        private fun safeHash(value: String): Int {
            val h = value.hashCode()
            return if (h == Int.MIN_VALUE) 0 else Math.abs(h)
        }
    }
}

// ── PaintFamily (internal) ──────────────────────────────────────────

private data class PaintFamily(
    val primary:      Color,
    val secondary:    Color,
    val glow:         Color,
    val keywords:     List<String>,
    val vibeLabels:   List<String>,
    val glyphs:       List<String>,
)
