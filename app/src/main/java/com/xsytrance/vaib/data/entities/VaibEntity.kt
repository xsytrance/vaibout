package com.xsytrance.vaib.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vaibs")
data class VaibEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vaibName: String,
    val trackUri: String,
    val trackName: String,
    val mood: String,
    val visualizerStyle: String,
    val themeId: String,
    val createdAt: Long,
    /** "LOCAL" for SAF content:// URIs, "INTERNET_ARCHIVE" for https:// streams. */
    @ColumnInfo(defaultValue = "LOCAL")  val sourceType: String = "LOCAL",
    /** Stored EqPreset enum name, e.g. "FLAT", "DEEP_BASS". */
    @ColumnInfo(defaultValue = "FLAT")   val eqPreset:   String = "FLAT",
)
