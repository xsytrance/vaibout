package com.xsytrance.vaib.ui

import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xsytrance.vaib.MainViewModel
import com.xsytrance.vaib.audio.EqPreset
import com.xsytrance.vaib.core.design.VaibAtmosphere
import com.xsytrance.vaib.core.design.VaibColors
import com.xsytrance.vaib.data.entities.VaibEntity
import com.xsytrance.vaib.visualizer.VisualizerStyle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private val MOOD_OPTIONS = listOf("Deep", "Chill", "Energetic", "Cosmic", "Focus")

private fun formatMs(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

// ── Compact card accent gradients (one per saved vAIb slot mod 5) ─────
private val CARD_ACCENTS = listOf(
    listOf(Color(0xFF041420), Color(0xFF0E0524)),
    listOf(Color(0xFF051A0E), Color(0xFF050F20)),
    listOf(Color(0xFF18040E), Color(0xFF070420)),
    listOf(Color(0xFF0E1204), Color(0xFF04121A)),
    listOf(Color(0xFF160A04), Color(0xFF06041A)),
)

// ── Ambient background note data ──────────────────────────────────────
private data class AmbientNote(
    val glyphIndex: Int,
    val baseX: Float,
    val baseY: Float,
    val speed: Float,
    val alpha: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val phase: Float,
    val isPrimary: Boolean = true,
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
    onOpenLibrary: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val trackName          by viewModel.trackName.collectAsState()
    val trackUri           by viewModel.trackUri.collectAsState()
    val isPlaying          by viewModel.isPlaying.collectAsState()
    val isBuffering        by viewModel.isBuffering.collectAsState()
    val playbackFraction   by viewModel.playbackFraction.collectAsState()
    val currentPositionMs  by viewModel.currentPositionMs.collectAsState()
    val durationMs         by viewModel.durationMs.collectAsState()
    val savedVaibs         by viewModel.savedVaibs.collectAsState()
    val currentEqPreset    by viewModel.currentEqPreset.collectAsState()
    val currentMood        by viewModel.currentMood.collectAsState()
    val vizStyle           by viewModel.selectedVisualizerStyle.collectAsState()
    val hasTrack = trackUri != null

    val atmosphere = VaibAtmosphere.Default

    var showSaveDialog       by remember { mutableStateOf(false) }
    var nameInput            by remember { mutableStateOf("") }
    var selectedMood         by remember { mutableStateOf("") }
    var selectedEqPreset     by remember { mutableStateOf(EqPreset.FLAT) }
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 48.dp),
        ) {

            // ── Header ────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "vAIb out!",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1.0).sp,
                        )
                        Text(
                            "LET'S CHILL",
                            color = atmosphere.primaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.2.sp,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onOpenLibrary) {
                            Icon(
                                Icons.Default.LibraryMusic,
                                contentDescription = "Library",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // ── Now Playing hero card (live visualizer) ───────────────
            item {
                NowPlayingCard(
                    trackName               = trackName,
                    trackUri                = trackUri,
                    isPlaying               = isPlaying,
                    isBuffering             = isBuffering,
                    playbackFraction        = playbackFraction,
                    currentPositionMs       = currentPositionMs,
                    durationMs              = durationMs,
                    hasTrack                = hasTrack,
                    currentEqPreset         = currentEqPreset,
                    currentMood             = currentMood,
                    atmosphere              = atmosphere,
                    selectedVisualizerStyle = vizStyle,
                    onVisualizerStyleChange = viewModel::setVisualizerStyle,
                )
                Spacer(Modifier.height(14.dp))
            }

            // ── Transport controls ────────────────────────────────────
            item {
                TransportControls(
                    isPlaying   = isPlaying,
                    hasTrack    = hasTrack,
                    atmosphere  = atmosphere,
                    onPlayPause = viewModel::togglePlayPause,
                )
                Spacer(Modifier.height(18.dp))
            }

            // ── vAIb out! hero button ─────────────────────────────────
            item {
                VaibOutButton(
                    hasTrack  = hasTrack,
                    onClick   = onEnterDreamscape,
                )
                Spacer(Modifier.height(12.dp))
            }

            // ── Secondary actions ─────────────────────────────────────
            item {
                Row(
                    Modifier.fillMaxWidth(),
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
                Spacer(Modifier.height(24.dp))
            }

            // ── Saved vAIbs ───────────────────────────────────────────
            if (savedVaibs.isNotEmpty() || hasTrack) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Saved vAIbs",
                            color      = Color.White.copy(alpha = 0.75f),
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (hasTrack) {
                            TextButton(
                                onClick = {
                                    eqPresetBeforeDialog = currentEqPreset
                                    selectedEqPreset = currentEqPreset
                                    showSaveDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    "⊕  SAVE AS VAIB",
                                    color         = atmosphere.primaryColor.copy(alpha = 0.80f),
                                    fontSize      = 10.sp,
                                    fontWeight    = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }

            if (savedVaibs.isNotEmpty()) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding        = PaddingValues(end = 4.dp),
                    ) {
                        items(savedVaibs, key = { it.id }) { vaib ->
                            CompactVaibCard(
                                vaib            = vaib,
                                accentColors    = CARD_ACCENTS[(vaib.id % CARD_ACCENTS.size).toInt()],
                                onClick         = { viewModel.loadVaib(vaib) },
                                onDeleteRequest = { pendingDeleteVaib = vaib },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            } else if (hasTrack) {
                item {
                    Text(
                        "No vAIbs saved yet. Hit ⊕ SAVE AS VAIB to capture this moment.",
                        color      = VaibColors.TextSoft.copy(alpha = 0.36f),
                        fontSize   = 12.sp,
                        lineHeight = 18.sp,
                    )
                    Spacer(Modifier.height(12.dp))
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
                    elevation = ButtonDefaults.buttonElevation(0.dp),
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
        val dismissWithCancel = {
            showSaveDialog = false
            nameInput = ""
            selectedMood = ""
            selectedEqPreset = EqPreset.FLAT
            viewModel.applyEqPreset(eqPresetBeforeDialog)
        }
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
                    Spacer(Modifier.height(16.dp))
                    Text("MOOD", color = VaibColors.TextSoft.copy(0.6f), fontSize = 10.sp,
                        letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
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
                                    containerColor         = Color.White.copy(0.08f),
                                    labelColor             = Color.White.copy(0.75f),
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true, selected = selectedMood == mood,
                                    borderColor = Color.White.copy(0.12f),
                                    selectedBorderColor = Color.Transparent,
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("EQ", color = VaibColors.TextSoft.copy(0.6f), fontSize = 10.sp,
                        letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        EqPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = selectedEqPreset == preset,
                                onClick  = { selectedEqPreset = preset; viewModel.applyEqPreset(preset) },
                                label    = { Text(preset.label, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VaibColors.VioletGlow,
                                    selectedLabelColor     = Color.White,
                                    containerColor         = Color.White.copy(0.08f),
                                    labelColor             = Color.White.copy(0.75f),
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true, selected = selectedEqPreset == preset,
                                    borderColor = Color.White.copy(0.12f),
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
                    elevation = ButtonDefaults.buttonElevation(0.dp),
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

// ── Ambient background ────────────────────────────────────────────────

@Composable
private fun AmbientBackground(atmosphere: VaibAtmosphere, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ambient")
    val phase by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(30_000, easing = LinearEasing), RepeatMode.Restart),
        label = "ambientPhase",
    )
    val twoPi  = (2.0 * PI).toFloat()
    val glyphs = atmosphere.particleGlyphs
    BoxWithConstraints(modifier = modifier) {
        val w = maxWidth
        val h = maxHeight
        AMBIENT_NOTE_DATA.forEach { note ->
            val glyph = glyphs.getOrElse(note.glyphIndex % glyphs.size) { "♫" }
            val swayX = sin(phase * note.swayFreq * twoPi + note.phase) * note.swayAmp
            val xDp   = w * (note.baseX + swayX).coerceIn(0.02f, 0.96f)
            val yDp   = h * ((note.baseY + phase * note.speed) % 1.05f)
            Text(
                text     = glyph,
                color    = (if (note.isPrimary) atmosphere.primaryColor else atmosphere.secondaryColor)
                    .copy(alpha = note.alpha),
                fontSize = 16.sp,
                modifier = Modifier.absoluteOffset(x = xDp, y = yDp),
            )
        }
    }
}

// ── Transport controls ────────────────────────────────────────────────

@Composable
private fun TransportControls(
    isPlaying: Boolean,
    hasTrack: Boolean,
    atmosphere: VaibAtmosphere,
    onPlayPause: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        TransportSideButton { SkipPrevIcon(Modifier.size(22.dp)) }
        Spacer(Modifier.width(30.dp))
        val bgColor   = if (hasTrack) atmosphere.primaryColor else Color.White.copy(0.07f)
        val iconColor = if (hasTrack) Color.Black else Color.White.copy(0.18f)
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(bgColor)
                .clickable(enabled = hasTrack, onClick = onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            if (isPlaying) PauseIcon(Modifier.size(26.dp), iconColor)
            else           PlayIcon (Modifier.size(26.dp), iconColor)
        }
        Spacer(Modifier.width(30.dp))
        TransportSideButton { SkipNextIcon(Modifier.size(22.dp)) }
    }
}

// ── Icon helpers ──────────────────────────────────────────────────────

@Composable
private fun PlayIcon(modifier: Modifier = Modifier, color: Color = Color.Black) {
    Canvas(modifier = modifier) {
        drawPath(Path().apply {
            moveTo(size.width * 0.26f, size.height * 0.14f)
            lineTo(size.width * 0.83f, size.height * 0.50f)
            lineTo(size.width * 0.26f, size.height * 0.86f)
            close()
        }, color)
    }
}

@Composable
private fun PauseIcon(modifier: Modifier = Modifier, color: Color = Color.Black) {
    Canvas(modifier = modifier) {
        val bw = size.width * 0.22f
        val bh = size.height * 0.70f
        val gap = size.width * 0.16f
        val sx = (size.width - bw * 2f - gap) / 2f
        val sy = (size.height - bh) / 2f
        drawRect(color, Offset(sx, sy),         Size(bw, bh))
        drawRect(color, Offset(sx + bw + gap, sy), Size(bw, bh))
    }
}

@Composable
private fun SkipPrevIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val c = Color.White.copy(0.20f)
        drawRect(c, Offset(size.width * 0.10f, size.height * 0.18f),
            Size(size.width * 0.10f, size.height * 0.64f))
        drawPath(Path().apply {
            moveTo(size.width * 0.88f, size.height * 0.18f)
            lineTo(size.width * 0.28f, size.height * 0.50f)
            lineTo(size.width * 0.88f, size.height * 0.82f)
            close()
        }, c)
    }
}

@Composable
private fun SkipNextIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val c = Color.White.copy(0.20f)
        drawPath(Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.18f)
            lineTo(size.width * 0.72f, size.height * 0.50f)
            lineTo(size.width * 0.12f, size.height * 0.82f)
            close()
        }, c)
        drawRect(c, Offset(size.width * 0.80f, size.height * 0.18f),
            Size(size.width * 0.10f, size.height * 0.64f))
    }
}

// ── Atmosphere chip ───────────────────────────────────────────────────

@Composable
private fun AtmosphereChip(label: String, color: Color) {
    Text(
        text       = label.uppercase(),
        color      = color,
        fontSize   = 9.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        modifier   = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.40f)), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

// ── Compact vAIb cards ───────────────────────────────────────────────

@Composable
private fun CompactVaibCard(
    vaib: VaibEntity,
    accentColors: List<Color>,
    onClick: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    val eqLabel = runCatching { EqPreset.valueOf(vaib.eqPreset) }
        .getOrDefault(EqPreset.FLAT)
        .takeIf { it != EqPreset.FLAT }
        ?.label

    Box(
        modifier = Modifier
            .size(width = 138.dp, height = 126.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(VaibColors.DeepBackground)
            .clickable(onClick = onClick),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .background(
                        Brush.verticalGradient(colors = accentColors),
                    ),
            ) {
                CompactCardWave(
                    accentColor = accentColors.first(),
                    modifier    = Modifier.fillMaxSize(),
                )
            }
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                Text(
                    vaib.vaibName,
                    color      = Color.White.copy(0.88f),
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    eqLabel?.let {
                        Text(
                            it,
                            color     = VaibColors.CyanPulse.copy(alpha = 0.7f),
                            fontSize  = 9.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        vaib.mood.ifEmpty { "—" },
                        color     = VaibColors.TextSoft.copy(0.5f),
                        fontSize  = 9.sp,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.Transparent)
                .clickable { onDeleteRequest() },
            contentAlignment = Alignment.Center,
        ) {
            Text("×", color = Color.White.copy(0.35f), fontSize = 14.sp)
        }
    }
}

// ── Compact card waveform ────────────────────────────────────────────

@Composable
private fun CompactCardWave(accentColor: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "compactWave")
    val phase by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "cwPhase",
    )
    val twoPi = (2.0 * PI).toFloat()
    Canvas(modifier = modifier) {
        for (i in 0..30) {
            val x = i.toFloat() / 30f * size.width
            val h = size.height * (0.15f + 0.35f * abs(sin(x / size.width * twoPi * 1.8f + phase * twoPi)))
            drawRect(
                accentColor.copy(alpha = 0.25f + 0.15f * abs(sin(phase * twoPi + i * 0.3f))),
                topLeft = Offset(x, size.height - h),
                size = Size(size.width / 32f, h),
            )
        }
    }
}

// ── VaibOut hero button ──────────────────────────────────────────────

@Composable
private fun VaibOutButton(hasTrack: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = hasTrack,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = VaibColors.CyanPulse,
            disabledContainerColor = Color.White.copy(alpha = 0.06f),
            contentColor = Color.Black,
            disabledContentColor = Color.White.copy(alpha = 0.2f),
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
        ),
    ) {
        Text(
            "vAIb  out!",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            letterSpacing = 1.2.sp,
        )
    }
}

// ── VaibOutlinedButton ───────────────────────────────────────────────

@Composable
private fun VaibOutlinedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White.copy(alpha = 0.04f),
            contentColor = Color.White.copy(alpha = 0.85f),
        ),
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
        )
    }
}

// ── TransportSideButton ──────────────────────────────────────────────

@Composable
private fun TransportSideButton(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f)),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}