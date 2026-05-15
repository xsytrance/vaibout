package com.xsytrance.vaib.core.design

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts dominant colors from album art for dynamic theming.
 * Uses Android's Palette API for color extraction.
 */
object DynamicColors {

    data class ExtractedColors(
        val primary: Color,
        val secondary: Color,
        val background: Color,
        val surface: Color,
        val onPrimary: Color,
    )

    /**
     * Extract dominant colors from a bitmap URI.
     * Returns null if extraction fails.
     */
    suspend fun fromUri(context: Context, uri: Uri): ExtractedColors? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            fromBitmap(bitmap)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extract dominant colors from a bitmap.
     */
    fun fromBitmap(bitmap: Bitmap): ExtractedColors? {
        return try {
            val palette = Palette.from(bitmap).generate()
            val dominant = palette.dominantSwatch
            val vibrant = palette.vibrantSwatch
            val muted = palette.mutedSwatch
            val darkVibrant = palette.darkVibrantSwatch

            val primary = vibrant?.rgb?.let { Color(it) }
                ?: dominant?.rgb?.let { Color(it) }
                ?: VaibColors.CyanPulse

            val secondary = muted?.rgb?.let { Color(it) }
                ?: darkVibrant?.rgb?.let { Color(it) }
                ?: VaibColors.VioletGlow

            val bg = darkVibrant?.rgb?.let { Color(it) }
                ?: dominant?.rgb?.let { darken(it, 0.7f) }
                ?: VaibColors.DeepBackground

            val surfaceColor = dominant?.rgb?.let { lighten(it, 0.15f) }
                ?: VaibColors.Surface

            val onPrimary = if (dominant?.bodyTextColor != 0) Color(dominant.bodyTextColor)
            else Color.White

            ExtractedColors(
                primary = primary,
                secondary = secondary,
                background = bg,
                surface = surfaceColor,
                onPrimary = onPrimary,
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generate a VaibAtmosphere from extracted colors.
     */
    fun ExtractedColors.toAtmosphere(): VaibAtmosphere = VaibAtmosphere(
        primaryColor = primary,
        secondaryColor = secondary,
        glowColor = primary.copy(alpha = 0.3f),
        backgroundAccent = background,
        particleGlyphs = listOf("♫", "♪", "✦", "◈"),
    )

    // ── Color utilities ────────────────────────────────────────

    private fun darken(color: Int, factor: Float): Int {
        val a = AndroidColor.alpha(color)
        val r = (AndroidColor.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (AndroidColor.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (AndroidColor.blue(color) * factor).toInt().coerceIn(0, 255)
        return AndroidColor.argb(a, r, g, b)
    }

    private fun lighten(color: Int, factor: Float): Int {
        val a = AndroidColor.alpha(color)
        val r = (AndroidColor.red(color) + (255 - AndroidColor.red(color)) * factor).toInt().coerceIn(0, 255)
        val g = (AndroidColor.green(color) + (255 - AndroidColor.green(color)) * factor).toInt().coerceIn(0, 255)
        val b = (AndroidColor.blue(color) + (255 - AndroidColor.blue(color)) * factor).toInt().coerceIn(0, 255)
        return AndroidColor.argb(a, r, g, b)
    }
}
