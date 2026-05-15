package com.xsytrance.vaib.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Junction table linking stations to tracks.
 * A track can belong to multiple stations; a station holds many tracks.
 */
@Entity(
    primaryKeys = ["stationId", "trackId"],
    tableName = "station_tracks"
)
data class StationTrackCrossRef(
    val stationId: Long,
    val trackId: Long,
    val addedAt: Long = System.currentTimeMillis(),
)