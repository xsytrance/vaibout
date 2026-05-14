package com.xsytrance.vaib.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xsytrance.vaib.audio.EqPreset
import com.xsytrance.vaib.core.design.TrackPaint
import com.xsytrance.vaib.core.design.VaibColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private val STUDIO_MOODS = listOf("Chill", "Deep", "Cosmic", "Focus", "Energetic")

// ── vAIb Card Studio Bottom Sheet ─────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaibCardStudioSheet(
    showSheet: Boolean,
    trackName: String?,
    trackSource: String,
    currentMood: String,
    currentEqPreset: EqPreset,
    onDismiss: () -> Unit,
    onSave: (name: String, mood: String, eqPreset: EqPreset) -> Unit,
) {
    if (!showSheet) return

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
    )

    ModalBottomSheet(
        onDismissRequest  = onDismiss,
        sheetState        = sheetState,
        containerColor    = Color(0xFF050505),
        contentColor      = Color.White,
        dragHandle        = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.15f)),
            )
        },
    ) {
        VaibCardStudioContent(
            trackName       = trackName,
            trackSource     = trackSource,
            currentMood     = currentMood,
            currentEqPreset = currentEqPreset,
            onDismiss       = onDismiss,
            onSave          = onSave,
        )
    }
}

// ── Content (stateful) ────────────────────────────────────────────────

@Composable
private fun VaibCardStudioContent(
    trackName: String?,
    trackSource: String,
    currentMood: String,
    currentEqPreset: EqPreset,
    onDismiss: () -> Unit,
    onSave: (name: String, mood: String, eqPreset: EqPreset) -> Unit,
) {
    val defaultName = remember(trackName, currentMood) {
        suggestDefaultVaibName(trackName, currentMood)
    }

    // Local state for the studio
    val nameState    = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    val moodState    = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(currentMood) }
    val eqState      = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(currentEqPreset) }

    val name    = nameState.value
    val mood    = moodState.value
    val eq      = eqState.value
    val effectiveName = name.ifEmpty { defaultName }

    // Paint for the preview card
    val paint = remember(effectiveName, mood, trackSource) {
        TrackPaint.fromTrack(title = effectiveName, creator = "", sourceType = trackSource, moodHint = mood)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        // ── Header ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment   = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "vAIb Card Studio",
                    color      = Color.White,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.4).sp,
                )
                Text(
                    "capture this atmosphere",
                    color      = VaibColors.TextSoft.copy(alpha = 0.45f),
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            TextButton(onClick = onDismiss) {
                Text("Close", color = VaibColors.TextSoft.copy(alpha = 0.55f), fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(16.dp))

        // ── Live card preview ───────────────────────────────────────
        VaibCardPreview(
            name       = effectiveName,
            mood       = mood,
            eqPreset   = eq,
            trackSource = trackSource,
            paint      = paint,
        )
        Spacer(Modifier.height(16.dp))

        // ── Track info line ─────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                trackSource,
                color      = paint.primaryColor.copy(alpha = 0.55f),
                fontSize   = 9.sp,
                fontWeight = FontWeight.Medium,
            )
            if (mood.isNotEmpty()) {
                Text(
                    "\u00b7",
                    color  = VaibColors.TextSoft.copy(alpha = 0.25f),
                    fontSize = 9.sp,
                )
                Text(
                    mood,
                    color      = paint.secondaryColor.copy(alpha = 0.50f),
                    fontSize   = 9.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(
                "\u00b7",
                color  = VaibColors.TextSoft.copy(alpha = 0.25f),
                fontSize = 9.sp,
            )
            Text(
                eq.label,
                color      = VaibColors.TextSoft.copy(alpha = 0.45f),
                fontSize   = 9.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(16.dp))

        // ── Name field ──────────────────────────────────────────────
        OutlinedTextField(
            value         = name,
            onValueChange = { nameState.value = it },
            placeholder   = {
                Text(defaultName, color = VaibColors.TextSoft.copy(alpha = 0.30f), fontSize = 14.sp)
            },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor      = Color.White,
                unfocusedTextColor    = Color.White,
                focusedBorderColor    = paint.primaryColor.copy(alpha = 0.70f),
                unfocusedBorderColor  = Color.White.copy(alpha = 0.08f),
                cursorColor           = paint.primaryColor,
                focusedLabelColor     = paint.primaryColor.copy(alpha = 0.70f),
                unfocusedLabelColor   = VaibColors.TextSoft.copy(alpha = 0.40f),
            ),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
        Spacer(Modifier.height(14.dp))

        // ── Mood chips ──────────────────────────────────────────────
        Text(
            "MOOD",
            color      = VaibColors.TextSoft.copy(alpha = 0.50f),
            fontSize   = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            STUDIO_MOODS.forEach { moodOption ->
                StudioFilterChip(
                    label    = moodOption,
                    selected = mood == moodOption,
                    onClick  = { moodState.value = if (mood == moodOption) "" else moodOption },
                    accentColor = paint.primaryColor,
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        // ── EQ preset chips ─────────────────────────────────────────
        Text(
            "EQ",
            color      = VaibColors.TextSoft.copy(alpha = 0.50f),
            fontSize   = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EqPreset.entries.forEach { preset ->
                StudioFilterChip(
                    label    = preset.label,
                    selected = eq == preset,
                    onClick  = { eqState.value = preset },
                    accentColor = VaibColors.VioletGlow,
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        // ── Actions ─────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Cancel
            VaibOutlinedButton(
                label   = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
            // Save
            Button(
                onClick = {
                    onSave(effectiveName, mood, eq)
                    onDismiss()
                },
                modifier  = Modifier.weight(1f).height(48.dp),
                shape     = RoundedCornerShape(12.dp),
                colors    = ButtonDefaults.buttonColors(
                    containerColor = paint.primaryColor.copy(alpha = 0.85f),
                    contentColor   = Color.Black,
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp),
            ) {
                Text(
                    "Save vAIb",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ── Live card preview ─────────────────────────────────────────────────

@Composable
private fun VaibCardPreview(
    name: String,
    mood: String,
    eqPreset: EqPreset,
    trackSource: String,
    paint: TrackPaint,
) {
    val transition = rememberInfiniteTransition(label = "cardPreviewWave")
    val phase by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2_800, easing = LinearEasing), RepeatMode.Restart),
        label = "cardPreviewPhase",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0A0A0A))
            .border(
                BorderStroke(1.2.dp, paint.borderColor.copy(alpha = 0.55f)),
                RoundedCornerShape(16.dp),
            ),
    ) {
        // Gradient background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            paint.primaryColor.copy(alpha = 0.14f),
                            paint.secondaryColor.copy(alpha = 0.07f),
                        ),
                    ),
                ),
        ) {
            // Mini waveform
            CardPreviewWaveform(
                paint = paint,
                phase = phase,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .align(Alignment.BottomCenter),
            )
        }

        // Info at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                name,
                color      = Color.White.copy(alpha = 0.90f),
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (mood.isNotEmpty()) {
                    PreviewBadge(mood, paint.primaryColor)
                }
                PreviewBadge(eqPreset.label, paint.secondaryColor.copy(alpha = 0.65f))
                Text(
                    paint.glyphs.first(),
                    color  = paint.primaryColor.copy(alpha = 0.25f),
                    fontSize = 10.sp,
                )
            }
        }

        // Source badge top-right
        Text(
            trackSource.uppercase(),
            color         = VaibColors.TextSoft.copy(alpha = 0.30f),
            fontSize      = 7.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            modifier      = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
        )
    }
}

// ── Mini waveform for card preview ────────────────────────────────────

@Composable
private fun CardPreviewWaveform(
    paint: TrackPaint,
    phase: Float,
    modifier: Modifier = Modifier,
) {
    val barCount = 20
    val twoPi = (2.0 * PI).toFloat()
    Canvas(modifier = modifier) {
        val barW = size.width / barCount
        val maxH = size.height
        for (i in 0 until barCount) {
            val speed  = 0.5f + (i % 4) * 0.2f
            val offset = i.toFloat() / barCount
            val raw    = abs(sin(((phase + offset) * speed * twoPi).toDouble())).toFloat()
            val h      = (maxH * (0.12f + raw * 0.88f)).coerceAtLeast(1.5f)
            drawRect(
                color   = paint.primaryColor.copy(alpha = 0.18f + raw * 0.22f),
                topLeft = Offset(i * barW + 1f, maxH - h),
                size    = Size((barW - 2f).coerceAtLeast(1f), h),
            )
        }
    }
}

// ── Studio filter chip ────────────────────────────────────────────────

@Composable
private fun StudioFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
) {
    FilterChip(
        selected = selected,
        onClick  = onClick,
        label    = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
        colors   = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accentColor.copy(alpha = 0.70f),
            selectedLabelColor     = Color.Black,
            containerColor         = Color.White.copy(alpha = 0.05f),
            labelColor             = Color.White.copy(alpha = 0.70f),
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = if (selected) Color.Transparent else Color.White.copy(alpha = 0.10f),
            selectedBorderColor = Color.Transparent,
        ),
    )
}

// ── Preview badge ─────────────────────────────────────────────────────

@Composable
private fun PreviewBadge(label: String, color: Color) {
    Text(
        text          = label.uppercase(),
        color         = color.copy(alpha = 0.75f),
        fontSize      = 7.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        modifier      = Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(BorderStroke(0.8.dp, color.copy(alpha = 0.30f)), RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

// ── Default name suggestion ───────────────────────────────────────────

private fun suggestDefaultVaibName(trackName: String?, mood: String): String {
    val base = trackName ?: "Untitled"
    return if (mood.isNotEmpty()) "$base $mood" else "$base Capture"
}
