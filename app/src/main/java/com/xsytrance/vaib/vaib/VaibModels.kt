package com.xsytrance.vaib.vaib

import com.xsytrance.vaib.visualizer.VisualizerStyle

data class Vaib(
    val id: Long,
    val name: String,
    val trackId: Long,
    val visualizerStyle: VisualizerStyle,
    val mood: String
)
