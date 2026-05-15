package com.xsytrance.vaib.ui

import com.xsytrance.vaib.data.entities.StationEntity

/**
 * UI-facing station model — wraps [StationEntity] with resolved display fields.
 */
data class StationUiState(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val icon: String = "🎵",
    val themeOrdinal: Int = 0,
    val eqPreset: String = "FLAT",
    val visualizerStyle: String = "NEBULA",
    val sourceType: String = "MIXED",
    val sortOrder: Int = 0,
    val trackCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Convert Room entity → UI state. */
fun StationEntity.toUiState(trackCount: Int = 0) = StationUiState(
    id = id,
    name = name,
    description = description,
    icon = icon,
    themeOrdinal = themeOrdinal,
    eqPreset = eqPreset,
    visualizerStyle = visualizerStyle,
    sourceType = sourceType,
    sortOrder = sortOrder,
    trackCount = trackCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
