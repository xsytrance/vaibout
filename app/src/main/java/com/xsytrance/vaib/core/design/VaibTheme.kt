package com.xsytrance.vaib.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VaibDarkScheme = darkColorScheme(
    primary = VaibColors.CyanPulse,
    secondary = VaibColors.VioletGlow,
    background = VaibColors.Black,
    surface = VaibColors.DeepBackground,
    onPrimary = VaibColors.Black,
    onBackground = androidx.compose.ui.graphics.Color(0xFFE6EDF7),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE6EDF7)
)

@Composable
fun VaibTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VaibDarkScheme,
        typography = VaibTypography,
        content = content
    )
}
