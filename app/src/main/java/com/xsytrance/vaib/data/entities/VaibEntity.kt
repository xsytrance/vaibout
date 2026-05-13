package com.xsytrance.vaib.data.entities

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
)
