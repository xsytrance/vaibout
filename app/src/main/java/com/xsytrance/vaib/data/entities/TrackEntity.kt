package com.xsytrance.vaib.data.entities

import androidx.room.*

/**
 * Enhanced track entity with richer metadata for organization.
 */
@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val durationMs: Long = 0L,

    // URI — either content:// (local SAF) or https:// (Internet Archive)
    val uri: String,

    // Track source: "LOCAL" or "INTERNET_ARCHIVE"
    val sourceType: String = "LOCAL",

    // Stats
    val playCount: Int = 0,
    val lastPlayedAt: Long? = null,

    // User favorites flag
    val isFavorite: Boolean = false,
)

/**
 * Convenience data class used across the app when track + station context
 * is needed together (e.g., library browsing, station detail).
 */
data class TrackWithStation(
    val trackId: Long,
    val trackTitle: String,
    val artist: String?,
    val album: String?,
    val stationId: Long?,
    val stationName: String?,
)