package com.xsytrance.vaib.music

data class Track(
    val key: String,
    val url: String,
    val lrcUrl: String?,
    val title: String,
    val artist: String,
    val albumArtUrl: String?,
    val tags: List<String>,
    val lyrics: String?,
    val bpm: Int?,
)
