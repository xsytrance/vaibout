package com.xsytrance.vaib.audio

data class Track(
    val id: Long,
    val title: String,
    val artist: String? = null,
    val durationMs: Long = 0L,
    val uri: String
)
