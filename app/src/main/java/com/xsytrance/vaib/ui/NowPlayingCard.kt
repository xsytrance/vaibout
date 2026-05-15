package com.xsytrance.vaib.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xsytrance.vaib.audio.EqPreset
import com.xsytrance.vaib.core.design.VaibAtmosphere
import com.xsytrance.vaib.core.design.VaibColors
import com.xsytrance.vaib.data.entities.VISUALIZER_STYLES
import com.xsytrance.vaib.visualizer.VisualizerStyle
import com.xsytrance.vaib.visualizer.VisualizerSurface

// ── Now Playing hero card ─────────────────────────────────────────────

@Composable
fun NowPlayingCard(
    trackName: String?,
    trackUri: Uri?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    playbackFraction: Float,
    currentPositionMs: Long,
    durationMs: Long,
    hasTrack: Boolean,
    currentEqPreset: EqPreset,
    currentMood: String,
    atmosphere: VaibAtmosphere,
    selectedVisualizerStyle: VisualizerStyle = VisualizerStyle.NEBULA,
    onVisualizerStyleChange: (VisualizerStyle) -> Unit = {},
) {
    val subtitle = when {
        currentMood.isNotEmpty() -> currentMood
        trackUri?.scheme == "https" || trackUri?.scheme == "http" -> "Internet Archive"
        hasTrack -> "Local file"
        else -> ""
    }
    val borderColor = if (isPlaying)
        atmosphere.primaryColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(androidx.compose.foundation.BorderStroke(0.6.dp, borderColor), RoundedCornerShape(22.dp))
            .background(VaibColors.DeepBackground),
    ) {
        // ── Live visualizer with style picker ──────────────
        if (hasTrack) {
            Column {
                VisualizerSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    style = selectedVisualizerStyle,
                    energy = 0.5f,
                    beat = 0.5f,
                    primaryColor = atmosphere.primaryColor,
                    secondaryColor = atmosphere.secondaryColor,
                )

                // Style picker chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    VISUALIZER_STYLES.forEach { styleInfo ->
                        val isSelected = selectedVisualizerStyle == styleInfo.style
                        VisualizerChip(
                            icon = styleInfo.icon,
                            label = styleInfo.label,
                            isSelected = isSelected,
                            onClick = { onVisualizerStyleChange(styleInfo.style) },
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Pick a track to visualize",
                    color = VaibColors.TextSoft.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                )
            }
        }

        // Content below
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                text = trackName ?: "Nothing playing",
                color = if (hasTrack) Color.White else VaibColors.TextSoft.copy(0.45f),
                fontSize = if (hasTrack) 18.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = (-0.3).sp,
            )

            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = atmosphere.primaryColor.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            val showEqChip = currentEqPreset != EqPreset.FLAT
            val showMoodChip = currentMood.isNotEmpty()
            if (showEqChip || showMoodChip) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (showEqChip) {
                        AtmosphereChip(
                            label = "${currentEqPreset.label} EQ",
                            color = atmosphere.secondaryColor,
                        )
                    }
                    if (showMoodChip) {
                        AtmosphereChip(
                            label = currentMood,
                            color = atmosphere.primaryColor,
                        )
                    }
                }
            }

            if (hasTrack) {
                Spacer(Modifier.height(10.dp))
                if (isBuffering) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = atmosphere.primaryColor.copy(0.55f),
                        trackColor = Color.White.copy(0.07f),
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { playbackFraction },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = atmosphere.primaryColor,
                        trackColor = Color.White.copy(0.07f),
                        strokeCap = StrokeCap.Round,
                    )
                }
                if (durationMs > 0) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(formatMs(currentPositionMs), color = VaibColors.TextSoft.copy(0.55f), fontSize = 10.sp)
                        Text(formatMs(durationMs), color = VaibColors.TextSoft.copy(0.55f), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────

private fun formatMs(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
private fun AtmosphereChip(label: String, color: Color) {
    Text(
        text = label.uppercase(),
        color = color,
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.40f)), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun VisualizerChip(
    icon: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(width = 72.dp, height = 32.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected)
            Color.White.copy(alpha = 0.18f)
        else
            Color.White.copy(alpha = 0.05f),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
        else
            null,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Text(icon, fontSize = 14.sp)
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}
