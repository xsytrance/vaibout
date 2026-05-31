package com.xsytrance.vaib.core.design

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette

data class TrackPalette(
    val vibrant: Color,
    val darkVibrant: Color,
    val muted: Color,
    val darkMuted: Color,
    val lightVibrant: Color,
)

val DefaultTrackPalette = TrackPalette(
    vibrant      = Color(0xFF00E5FF),
    darkVibrant  = Color(0xFF003340),
    muted        = Color(0xFF8B5CF6),
    darkMuted    = Color(0xFF0C0520),
    lightVibrant = Color(0xFF80F3FF),
)

fun Bitmap.toTrackPalette(): TrackPalette {
    val p = Palette.from(this).generate()
    return TrackPalette(
        vibrant      = Color(p.getVibrantColor(Color(0xFF00E5FF).toArgb())),
        darkVibrant  = Color(p.getDarkVibrantColor(Color(0xFF003340).toArgb())),
        muted        = Color(p.getMutedColor(Color(0xFF8B5CF6).toArgb())),
        darkMuted    = Color(p.getDarkMutedColor(Color(0xFF0C0520).toArgb())),
        lightVibrant = Color(p.getLightVibrantColor(Color(0xFF80F3FF).toArgb())),
    )
}
