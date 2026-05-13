package com.xsytrance.vaib.audio

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer

class AudioPlayer(context: Context) {
    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    fun release() {
        player.release()
    }
}
