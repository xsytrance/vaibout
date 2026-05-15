package com.xsytrance.vaib.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a user-curated music station.
 *
 * A station collects tracks around a mood, genre, or vibe.
 * Each station has a [StationTheme] that controls its visual identity.
 */
@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val description: String = "",

    // Icon emoji or drawable reference name
    val icon: String = "🎵",

    // Maps to StationTheme ordinal for runtime resolution
    val themeOrdinal: Int = 0,

    // EQ preset name — applied when playing from this station
    val eqPreset: String = "FLAT",

    // Visualizer style — applied when entering dreamscape from this station
    val visualizerStyle: String = "PULSE",

    // Source scope: "LOCAL", "INTERNET_ARCHIVE", or "MIXED"
    val sourceType: String = "MIXED",

    // Manual ordering (lower = higher in the grid)
    val sortOrder: Int = 0,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)