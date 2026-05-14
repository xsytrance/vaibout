package com.xsytrance.vaib.ui

import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xsytrance.vaib.MainViewModel
import com.xsytrance.vaib.audio.EqPreset
import com.xsytrance.vaib.core.design.VaibAtmosphere
import com.xsytrance.vaib.core.design.VaibColors
import com.xsytrance.vaib.data.entities.VaibEntity
import com.xsytrance.vaib.vaib.VaibCard
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private val MOOD_OPTIONS = listOf("Deep", "Chill", "Energetic", "Cosmic", "Focus")

// ── Ambient background — floating note data ───────────────────────────

private data class AmbientNote(
    val glyphIndex: Int,     // index into atmosphere.particleGlyphs
    val baseX: Float,        // 0..1 of screen width
    val baseY: Float,        // 0..1 starting position
    val speed: Float,        // upward drift speed (screen fractions per loop)
    val alpha: Float,
    val swayAmp: Float,      // horizontal sway amplitude (screen fraction)
    val swayFreq: Float,
    val phase: Float,
    val isPrimary: Boolean = true,  // primaryColor vs secondaryColor
)

private val AMBIENT_NOTE_DATA = listOf(
    AmbientNote(0, 0.08f, 0.10f, 0.12f, 0.085f, 0.028f, 1.20f, 0.00f),
    AmbientNote(1, 0.86f, 0.38f, 0.09f, 0.068f, 0.022f, 0.80f, 1.80f, false),
    AmbientNote(2, 0.22f, 0.68f, 0.14f, 0.078f, 0.035f, 1.50f, 3.50f),
    AmbientNote(0, 0.72f, 0.22f, 0.11f, 0.062f, 0.028f, 1.00f, 0.90f, false),
    AmbientNote(1, 0.44f, 0.82f, 0.08f, 0.072f, 0.022f, 0.70f, 2.40f),
    AmbientNote(2, 0.14f, 0.52f, 0.13f, 0.065f, 0.038f, 1.30f, 4.20f, false),
    AmbientNote(0, 0.91f, 0.72f, 0.10f, 0.060f, 0.026f, 0.90f, 1.20f),
    AmbientNote(1, 0.58f, 0.06f, 0.15f, 0.070f, 0.038f, 1.10f, 5.10f, false),
)

// ── HomeScreen ────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onPickTrack: () -> Unit,
    onEnterDreamscape: () -> Unit,
    onDiscoverMusic: () -> Unit,
) {
    val trackName        by viewModel.trackName.collectAsState()
    val trackUri         by viewModel.trackUri.collectAsState()
    val isPlaying        by viewModel.isPlaying.collectAsState()
    val isBuffering      by viewModel.isBuffering.collectAsState()
    val playbackFraction by viewModel.playbackFraction.collectAsState()
    val savedVaibs       by viewModel.savedVaibs.collectAsState()
    val currentEqPreset  by viewModel.currentEqPreset.collectAsState()
    val currentMood      by viewModel.currentMood.collectAsState()
    val hasTrack = trackUri != null

    // Atmosphere drives ambient colors and glyphs — defaults to cyan/violet
    val atmosphere = VaibAtmosphere.Default

    var showSaveDialog       by remember { mutableStateOf(false) }
    var nameInput            by remember { mutableStateOf("") }
    var selectedMood         by remember { mutableStateOf("") }
    var selectedEqPreset     by remember { mutableStateOf(EqPreset.FLAT) }
    // Capture the EQ preset active BEFORE the dialog opens so we can revert on cancel
    var eqPresetBeforeDialog by remember { mutableStateOf(EqPreset.FLAT) }
    var pendingDeleteVaib    by remember { mutableStateOf<VaibEntity?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AmbientBackground(atmosphere = atmosphere, modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier       = Modifier.fillMaxSize().systemBarsPadding(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 48.dp),
        ) {

            // ── Header ────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(36.dp))
                Text(
                    text          = "vAIb out!",
                    color         = Color.White,
                    fontSize      = 38.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = (-1.2).sp,
                )
                Text(
                    text          = "let's chill",
                    color         = atmosphere.primaryColor,
                    fontSize      = 12.sp,
                    letterSpacing = 0.3.sp,
                    fontWeight    = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(22.dp))
            }

            // ── Player panel ──────────────────────────────────────────
            item {
                PlayerPanel(
                    trackName        = trackName,
                    trackUri         = trackUri,
                    isPlaying        = isPlaying,
                    isBuffering      = isBuffering,
                    playbackFraction = playbackFraction,
                    hasTrack         = hasTrack,
                    currentEqPreset  = currentEqPreset,
                    currentMood      = currentMood,
                    atmosphere       = atmosphere,
                )
                Spacer(modifier = Modifier.height(22.dp))
            }

            // ── Transport ─────────────────────────────────────────────
            item {
                TransportControls(
                    isPlaying   = isPlaying,
                    hasTrack    = hasTrack,
                    onPlayPause = viewModel::togglePlayPause,
                )
                Spacer(modifier = Modifier.height(22.dp))
            }

            // ── vAIb out! hero ────────────────────────────────────────
            item {
                Button(
                    onClick   = onEnterDreamscape,
                    enabled   = hasTrack,
                    modifier  = Modifier.fillMaxWidth().height(64.dp),
                    shape     = RoundedCornerShape(18.dp),
                    colors    = ButtonDefaults.buttonColors(
                        containerColor         = atmosphere.primaryColor,
                        contentColor           = Color.Black,
                        disabledContainerColor = atmosphere.primaryColor.copy(alpha = 0.10f),
                        disabledContentColor   = VaibColors.TextSoft.copy(alpha = 0.28f),
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) {
                    Text(
                        text          = "vAIb out!",
                        fontSize      = 19.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = 0.3.sp,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Secondary actions ─────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    VaibOutlinedButton(
                        label    = if (hasTrack) "Change Track" else "Choose Track",
                        onClick  = onPickTrack,
                        modifier = Modifier.weight(1f),
                    )
                    VaibOutlinedButton(
                        label    = "Discover",
                        onClick  = onDiscoverMusic,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (hasTrack) {
                    Spacer(modifier = Modifier.height(2.dp))
                    TextButton(
                        onClick  = {
                            eqPresetBeforeDialog = currentEqPreset
                            selectedEqPreset = currentEqPreset
                            showSaveDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text       = "+ Save as vAIb",
                            color      = VaibColors.TextSoft.copy(alpha = 0.48f),
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Normal,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── Your vAIbs ────────────────────────────────────────────
            if (savedVaibs.isNotEmpty()) {
                item {
                    Text(
                        text          = "YOUR VAIBS",
                        color         = VaibColors.TextSoft.copy(alpha = 0.48f),
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.SemiBold,
                        letterSpacing = 1.8.sp,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                items(savedVaibs, key = { it.id }) { vaib ->
                    VaibCard(
                        vaib            = vaib,
                        onClick         = { viewModel.loadVaib(vaib) },
                        onDeleteRequest = { pendingDeleteVaib = vaib },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }

    // ── Delete confirmation ───────────────────────────────────────────
    pendingDeleteVaib?.let { vaib ->
        AlertDialog(
            onDismissRequest  = { pendingDeleteVaib = null },
            containerColor    = VaibColors.DeepBackground,
            titleContentColor = Color.White,
            textContentColor  = VaibColors.TextSoft,
            title = { Text("Delete vAIb?", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text  = { Text("\"${vaib.vaibName}\" will be removed.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteVaib(vaib); pendingDeleteVaib = null },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFCC3333),
                        contentColor   = Color.White,
                    ),
                    shape     = RoundedCornerShape(10.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) { Text("Delete", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteVaib = null }) {
                    Text("Cancel", color = VaibColors.TextSoft)
                }
            },
        )
    }

    // ── Save vAIb dialog ──────────────────────────────────────────────
    if (showSaveDialog) {
        val trackName2 by viewModel.trackName.collectAsState()

        // Cancel: revert EQ to what it was before the dialog opened
        val dismissWithCancel = {
            showSaveDialog = false
            nameInput = ""
            selectedMood = ""
            selectedEqPreset = EqPreset.FLAT
            viewModel.applyEqPreset(eqPresetBeforeDialog)
        }
        // Confirm: keep the selected EQ (already applied via live preview)
        val dismissWithSave = {
            showSaveDialog = false
            nameInput = ""
            selectedMood = ""
            selectedEqPreset = EqPreset.FLAT
        }

        AlertDialog(
            onDismissRequest  = dismissWithCancel,
            containerColor    = VaibColors.DeepBackground,
            titleContentColor = Color.White,
            textContentColor  = VaibColors.TextSoft,
            title = { Text("Save vAIb", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column {
                    OutlinedTextField(
                        value         = nameInput,
                        onValueChange = { nameInput = it },
                        label         = { Text("Name") },
                        placeholder   = { Text(trackName2 ?: "My vAIb") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor     = Color.White,
                            unfocusedTextColor   = Color.White,
                            focusedBorderColor   = VaibColors.CyanPulse,
                            unfocusedBorderColor = VaibColors.TextSoft.copy(alpha = 0.3f),
                            focusedLabelColor    = VaibColors.CyanPulse,
                            unfocusedLabelColor  = VaibColors.TextSoft,
                            cursorColor          = VaibColors.CyanPulse,
                        ),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "MOOD",
                        color         = VaibColors.TextSoft.copy(alpha = 0.6f),
                        fontSize      = 10.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight    = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MOOD_OPTIONS.forEach { mood ->
                            FilterChip(
                                selected = selectedMood == mood,
                                onClick  = { selectedMood = if (selectedMood == mood) "" else mood },
                                label    = { Text(mood, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VaibColors.CyanPulse,
                                    selectedLabelColor     = Color.Black,
                                    containerColor         = Color.White.copy(alpha = 0.08f),
                                    labelColor             = Color.White.copy(alpha = 0.75f),
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled             = true,
                                    selected            = selectedMood == mood,
                                    borderColor         = Color.White.copy(alpha = 0.12f),
                                    selectedBorderColor = Color.Transparent,
                                ),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "EQ",
                        color         = VaibColors.TextSoft.copy(alpha = 0.6f),
                        fontSize      = 10.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight    = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        EqPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = selectedEqPreset == preset,
                                onClick  = {
                                    selectedEqPreset = preset
                                    viewModel.applyEqPreset(preset)
                                },
                                label  = { Text(preset.label, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VaibColors.VioletGlow,
                                    selectedLabelColor     = Color.White,
                                    containerColor         = Color.White.copy(alpha = 0.08f),
                                    labelColor             = Color.White.copy(alpha = 0.75f),
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled             = true,
                                    selected            = selectedEqPreset == preset,
                                    borderColor         = Color.White.copy(alpha = 0.12f),
                                    selectedBorderColor = Color.Transparent,
                                ),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveVaib(
                            nameInput.ifEmpty { trackName2 ?: "Untitled vAIb" },
                            selectedMood,
                            selectedEqPreset,
                        )
                        dismissWithSave()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VaibColors.CyanPulse,
                        contentColor   = Color.Black,
                    ),
                    shape     = RoundedCornerShape(10.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) { Text("Save", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = dismissWithCancel) {
                    Text("Cancel", color = VaibColors.TextSoft)
                }
            },
        )
    }
}

// ── Ambient background — atmosphere-driven floating notes ─────────────

@Composable
private fun AmbientBackground(
    atmosphere: VaibAtmosphere,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "ambient")
    val phase by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(30_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ambientPhase",
    )
    val twoPi  = (2.0 * PI).toFloat()
    val glyphs = atmosphere.particleGlyphs

    BoxWithConstraints(modifier = modifier) {
        val w = maxWidth
        val h = maxHeight
        AMBIENT_NOTE_DATA.forEach { note ->
            val glyph = glyphs.getOrElse(note.glyphIndex % glyphs.size) { "♪" }
            val swayX = sin(phase * note.swayFreq * twoPi + note.phase) * note.swayAmp
            val xDp   = w * (note.baseX + swayX).coerceIn(0.02f, 0.96f)
            val yDp   = h * ((note.baseY + phase * note.speed) % 1.05f)
            val color = (if (note.isPrimary) atmosphere.primaryColor else atmosphere.secondaryColor)
                .copy(alpha = note.alpha)
            Text(
                text     = glyph,
                color    = color,
                fontSize = 16.sp,
                modifier = Modifier.absoluteOffset(x = xDp, y = yDp),
            )
        }
    }
}

// ── Player panel ──────────────────────────────────────────────────────

@Composable
private fun PlayerPanel(
    trackName: String?,
    trackUri: Uri?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    playbackFraction: Float,
    hasTrack: Boolean,
    currentEqPreset: EqPreset,
    currentMood: String,
    atmosphere: VaibAtmosphere,
) {
    val statusText = when {
        isBuffering           -> "BUFFERING…"
        isPlaying             -> "PLAYING"
        playbackFraction > 0f -> "PAUSED"
        hasTrack              -> "READY"
        else                  -> "NO TRACK"
    }
    val statusColor = when {
        isPlaying   -> atmosphere.primaryColor
        isBuffering -> atmosphere.primaryColor.copy(alpha = 0.55f)
        else        -> VaibColors.TextSoft.copy(alpha = 0.45f)
    }
    val sourceLabel = when (trackUri?.scheme) {
        "https", "http" -> "Internet Archive"
        null            -> null
        else            -> "Local file"
    }
    val borderColor = if (isPlaying)
        atmosphere.primaryColor.copy(alpha = 0.22f)
    else
        Color.White.copy(alpha = 0.05f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(BorderStroke(0.5.dp, borderColor), RoundedCornerShape(20.dp))
            .background(VaibColors.DeepBackground)
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MiniOrb(isPlaying = isPlaying, hasTrack = hasTrack, atmosphere = atmosphere)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Status dot + label
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(statusColor),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text          = statusText,
                        color         = statusColor,
                        fontSize      = 9.sp,
                        fontWeight    = FontWeight.SemiBold,
                        letterSpacing = 2.sp,
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                // Track name
                Text(
                    text       = trackName ?: "Choose a track to vAIb out",
                    color      = if (hasTrack) Color.White else VaibColors.TextSoft.copy(alpha = 0.36f),
                    fontSize   = if (hasTrack) 16.sp else 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 2,
                    lineHeight = 21.sp,
                    overflow   = TextOverflow.Ellipsis,
                )
                // Inline metadata: source · EQ · mood
                val hasMetadata = sourceLabel != null || currentEqPreset != EqPreset.FLAT || currentMood.isNotEmpty()
                if (hasMetadata) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        var needDot = false
                        if (sourceLabel != null) {
                            Text(sourceLabel, color = VaibColors.TextSoft.copy(alpha = 0.36f), fontSize = 10.sp)
                            needDot = true
                        }
                        if (currentEqPreset != EqPreset.FLAT) {
                            if (needDot) Text("  ·  ", color = VaibColors.TextSoft.copy(alpha = 0.25f), fontSize = 10.sp)
                            Text(
                                text  = "EQ: ${currentEqPreset.label}",
                                color = atmosphere.secondaryColor.copy(alpha = 0.65f),
                                fontSize = 10.sp,
                            )
                            needDot = true
                        }
                        if (currentMood.isNotEmpty()) {
                            if (needDot) Text("  ·  ", color = VaibColors.TextSoft.copy(alpha = 0.25f), fontSize = 10.sp)
                            Text(
                                text  = currentMood,
                                color = atmosphere.primaryColor.copy(alpha = 0.52f),
                                fontSize = 10.sp,
                            )
                        }
                    }
                }
            }
        }

        // Bar visualizer preview
        Spacer(modifier = Modifier.height(14.dp))
        BarVisualizerPreview(
            isPlaying  = isPlaying,
            atmosphere = atmosphere,
            modifier   = Modifier.fillMaxWidth().height(42.dp),
        )

        // Progress bar
        if (hasTrack) {
            Spacer(modifier = Modifier.height(10.dp))
            if (isBuffering) {
                LinearProgressIndicator(
                    modifier   = Modifier.fillMaxWidth().height(2.dp),
                    color      = atmosphere.primaryColor.copy(alpha = 0.55f),
                    trackColor = Color.White.copy(alpha = 0.07f),
                )
            } else {
                LinearProgressIndicator(
                    progress   = { playbackFraction },
                    modifier   = Modifier.fillMaxWidth().height(2.dp),
                    color      = atmosphere.primaryColor,
                    trackColor = Color.White.copy(alpha = 0.07f),
                    strokeCap  = StrokeCap.Round,
                )
            }
        }
    }
}

// ── Bar visualizer preview ────────────────────────────────────────────

private const val BAR_COUNT = 20

@Composable
private fun BarVisualizerPreview(
    isPlaying: Boolean,
    atmosphere: VaibAtmosphere,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "barViz")
    val phase by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(3_400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "barPhase",
    )
    val twoPi      = (2.0 * PI).toFloat()
    val multiplier = if (isPlaying) 1f else 0.15f

    Canvas(modifier = modifier) {
        val barW = size.width / BAR_COUNT
        val maxH = size.height
        for (i in 0 until BAR_COUNT) {
            val speed  = 0.55f + (i % 5) * 0.20f + (i % 3) * 0.12f
            val offset = i.toFloat() / BAR_COUNT
            val raw    = abs(sin((phase + offset) * speed * twoPi))
            val h      = (maxH * multiplier * (0.10f + raw * 0.90f)).coerceAtLeast(2f)
            val t      = i.toFloat() / (BAR_COUNT - 1).coerceAtLeast(1)
            val color  = lerp(atmosphere.primaryColor, atmosphere.secondaryColor, t)
                .copy(alpha = 0.42f + raw * 0.28f)
            drawRect(
                color    = color,
                topLeft  = Offset(i * barW + 1.5f, maxH - h),
                size     = Size((barW - 3f).coerceAtLeast(1f), h),
            )
        }
    }
}

// ── Mini animated orb ─────────────────────────────────────────────────

@Composable
private fun MiniOrb(
    isPlaying: Boolean,
    hasTrack: Boolean,
    atmosphere: VaibAtmosphere,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val breathe by infiniteTransition.animateFloat(
        initialValue  = 0.82f,
        targetValue   = 1.00f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )
    val orbScale  = if (isPlaying && hasTrack) breathe else 0.72f
    val coreAlpha = when {
        isPlaying && hasTrack -> 0.90f
        hasTrack              -> 0.32f
        else                  -> 0.08f
    }
    Canvas(modifier = Modifier.size(52.dp)) {
        val r = size.minDimension / 2f
        drawCircle(color = atmosphere.primaryColor.copy(alpha = coreAlpha * 0.18f), radius = r * orbScale * 1.45f)
        drawCircle(color = atmosphere.primaryColor.copy(alpha = coreAlpha),          radius = r * orbScale)
    }
}

// ── Transport controls — Canvas-drawn icons (no emoji) ────────────────

@Composable
private fun TransportControls(
    isPlaying: Boolean,
    hasTrack: Boolean,
    onPlayPause: () -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        // Previous (disabled)
        TransportSideButton { SkipPrevIcon(Modifier.size(24.dp)) }

        Spacer(modifier = Modifier.width(28.dp))

        // Play / Pause — main button
        val bgAlpha   = if (hasTrack) 0.92f else 0.06f
        val iconColor = if (hasTrack) Color.Black else Color.White.copy(alpha = 0.15f)
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = bgAlpha))
                .clickable(enabled = hasTrack, onClick = onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            if (isPlaying) {
                PauseIcon(modifier = Modifier.size(28.dp), color = iconColor)
            } else {
                PlayIcon(modifier = Modifier.size(28.dp), color = iconColor)
            }
        }

        Spacer(modifier = Modifier.width(28.dp))

        // Next (disabled)
        TransportSideButton { SkipNextIcon(Modifier.size(24.dp)) }
    }
}

@Composable
private fun TransportSideButton(icon: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.04f)),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

// Canvas-drawn transport icons — no emoji, pure geometry

@Composable
private fun PlayIcon(modifier: Modifier = Modifier, color: Color = Color.Black) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.26f, size.height * 0.14f)
            lineTo(size.width * 0.83f, size.height * 0.50f)
            lineTo(size.width * 0.26f, size.height * 0.86f)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
private fun PauseIcon(modifier: Modifier = Modifier, color: Color = Color.Black) {
    Canvas(modifier = modifier) {
        val barW   = size.width  * 0.22f
        val barH   = size.height * 0.70f
        val gap    = size.width  * 0.16f
        val startX = (size.width - barW * 2f - gap) / 2f
        val startY = (size.height - barH) / 2f
        drawRect(color, topLeft = Offset(startX, startY),         size = Size(barW, barH))
        drawRect(color, topLeft = Offset(startX + barW + gap, startY), size = Size(barW, barH))
    }
}

@Composable
private fun SkipPrevIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val c = Color.White.copy(alpha = 0.20f)
        // Vertical bar
        val barW = size.width * 0.10f
        drawRect(c, topLeft = Offset(size.width * 0.10f, size.height * 0.18f),
            size = Size(barW, size.height * 0.64f))
        // Triangle pointing left
        val path = Path().apply {
            moveTo(size.width * 0.88f, size.height * 0.18f)
            lineTo(size.width * 0.28f, size.height * 0.50f)
            lineTo(size.width * 0.88f, size.height * 0.82f)
            close()
        }
        drawPath(path, c)
    }
}

@Composable
private fun SkipNextIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val c = Color.White.copy(alpha = 0.20f)
        // Triangle pointing right
        val path = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.18f)
            lineTo(size.width * 0.72f, size.height * 0.50f)
            lineTo(size.width * 0.12f, size.height * 0.82f)
            close()
        }
        drawPath(path, c)
        // Vertical bar
        val barW = size.width * 0.10f
        drawRect(c, topLeft = Offset(size.width * 0.80f, size.height * 0.18f),
            size = Size(barW, size.height * 0.64f))
    }
}

// ── Shared button styles ──────────────────────────────────────────────

@Composable
internal fun VaibOutlinedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick  = onClick,
        modifier = modifier.height(50.dp),
        shape    = RoundedCornerShape(12.dp),
        colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        border   = BorderStroke(1.dp, VaibColors.TextSoft.copy(alpha = 0.28f)),
    ) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
    }
}

@Composable
internal fun VaibSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick   = onClick,
        modifier  = modifier.height(54.dp),
        shape     = RoundedCornerShape(12.dp),
        colors    = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.07f),
            contentColor   = Color.White,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp)
    }
}

@Composable
internal fun VaibGlowButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick   = onClick,
        modifier  = modifier.height(58.dp),
        shape     = RoundedCornerShape(14.dp),
        colors    = ButtonDefaults.buttonColors(
            containerColor = VaibColors.CyanPulse,
            contentColor   = Color.Black,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}
