package com.xsytrance.vaib.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val key: String,
    val url: String,
    val lrcUrl: String?,
    val title: String,
    val artist: String,
    val albumArtUrl: String?,
    val tags: String,       // comma-separated
    val lyrics: String?,
    val bpm: Int?,
)
