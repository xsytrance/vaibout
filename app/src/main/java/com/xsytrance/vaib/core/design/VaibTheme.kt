package com.xsytrance.vaib.core.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

@Composable
fun VaibTheme(
    palette: TrackPalette = DefaultTrackPalette,
    content: @Composable () -> Unit,
) {
    val primary     by animateColorAsState(palette.vibrant,     tween(700), label = "p")
    val secondary   by animateColorAsState(palette.muted,       tween(700), label = "s")
    val bg          by animateColorAsState(palette.darkMuted,   tween(900), label = "bg")
    val surface     by animateColorAsState(
        palette.darkMuted.copy(alpha = 0.85f).let {
            Color(
                (it.red   + Color.Black.red)   / 2f,
                (it.green + Color.Black.green) / 2f,
                (it.blue  + Color.Black.blue)  / 2f,
            )
        },
        tween(900), label = "surf",
    )

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary      = primary,
            secondary    = secondary,
            background   = bg,
            surface      = surface,
            onPrimary    = Color.Black,
            onBackground = Color(0xFFE6EDF7),
            onSurface    = Color(0xFFE6EDF7),
        ),
        typography = VaibTypography,
        content    = content,
    )
}
