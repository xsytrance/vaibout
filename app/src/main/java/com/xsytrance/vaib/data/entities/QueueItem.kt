package com.xsytrance.vaib.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an item in the playback queue.
 *
 * The queue is persisted so it survives app restarts and process death.
 */
@Entity(tableName = "queue")
data class QueueItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val trackName: String,
    val artist: String? = null,
    val trackUri: String,
    val sourceType: String = "LOCAL",

    // Playback order — lower = plays sooner
    @ColumnInfo(index = true)
    val position: Int,

    val addedAt: Long = System.currentTimeMillis(),
)