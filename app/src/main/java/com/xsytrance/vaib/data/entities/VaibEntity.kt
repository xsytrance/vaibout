package com.xsytrance.vaib.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vaibs")
data class VaibEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val trackId: Long,
    val visualizerStyle: String,
    val eqPreset: String?,
    val effectsPreset: String?,
    val theme: String?,
    val mood: String
)
