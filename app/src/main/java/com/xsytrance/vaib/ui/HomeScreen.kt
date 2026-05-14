package com.xsytrance.vaib.ui

import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xsytrance.vaib.MainViewModel
import com.xsytrance.vaib.audio.EqPreset
import com.xsytrance.vaib.core.design.OrbitAtmosphereLayer
import com.xsytrance.vaib.core.design.VaibAtmosphere
import com.xsytrance.vaib.core.design.VaibColors
import com.xsytrance.vaib.data.entities.VaibEntity
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private val MOOD_OPTIONS = listOf("Deep", "Chill", "Energetic", "Cosmic", "Focus")

private fun formatMs(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

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
    val hasTrack = trackUri != null

    val atmosphere by viewModel.currentAtmosphere.collectAsState()

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
        // Track-painted background gradient
        DreamdeckBackground(
            atmosphere = atmosphere,
            hasTrack   = hasTrack,
            modifier   = Modifier.fillMaxSize(),
        )

        // Floating note particles
        OrbitAtmosphereLayer(
            moodColor          = atmosphere.primaryColor,
            secondaryMoodColor = atmosphere.secondaryColor,
            modifier           = Modifier.fillMaxSize(),
        )

        LazyColumn(
            modifier       = Modifier.fillMaxSize().systemBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 48.dp),
        ) {

            // ── Header ────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(24.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "vAIb out!",
                        color         = Color.White,
                        fontSize      = 28.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = (-0.8).sp,
                    )
                    Text(
                        "let's chill",
                        color         = atmosphere.primaryColor,
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.SemiBold,
                        letterSpacing = 2.2.sp,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Queue hint ────────────────────────────────────────────
            val queueReady by viewModel.queueReady.collectAsState()
            if (queueReady && !hasTrack) {
                item {
                    QueueHintChip(
                        onClick = { viewModel.navigateTo(com.xsytrance.vaib.Screen.DISCOVER) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── Dreamdeck Hero: visualizer + floating controls + track ──
            item {
                DreamdeckHero(
                    trackName        = trackName,
                    trackUri         = trackUri,
                    isPlaying        = isPlaying,
                    isBuffering      = isBuffering,
                    playbackFraction = playbackFraction,
                    currentPositionMs = currentPositionMs,
                    durationMs       = durationMs,
                    hasTrack         = hasTrack,
                    currentEqPreset  = currentEqPreset,
                    currentMood      = currentMood,
                    atmosphere       = atmosphere,
                    onPlayPause      = viewModel::togglePlayPause,
                )
                Spacer(Modifier.height(16.dp))
            }

            // ── Cockpit actions ───────────────────────────────────────
            item {
                CockpitActions(
                    hasTrack         = hasTrack,
                    atmosphere       = atmosphere,
                    onVaibOut        = onEnterDreamscape,
                    onPickTrack      = onPickTrack,
                    onDiscover       = onDiscoverMusic,
                )
                Spacer(Modifier.height(16.dp))
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
                            color      = Color.White.copy(alpha = 0.70f),
                            fontSize   = 13.sp,
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
                                    color         = atmosphere.primaryColor.copy(alpha = 0.70f),
                                    fontSize      = 9.sp,
                                    fontWeight    = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
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
                                onClick         = { viewModel.loadVaib(vaib) },
                                onDeleteRequest = { pendingDeleteVaib = vaib },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            } else if (hasTrack) {
                item {
                    Text(
                        "No vAIbs saved yet. Hit ⊕ SAVE AS VAIB to capture this moment.",
                        color      = VaibColors.TextSoft.copy(alpha = 0.28f),
                        fontSize   = 11.sp,
                        lineHeight = 16.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── Kimi lab stamp (small, bottom) ────────────────────────
            item {
                Spacer(Modifier.height(12.dp))
                KimiLabStamp(atmosphere = atmosphere)
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

    // ── vAIb Card Studio bottom sheet ────────────────────────────────
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

    VaibCardStudioSheet(
        showSheet       = showSaveDialog,
        trackName       = trackName,
        trackSource     = when {
            trackUri?.scheme == "https" || trackUri?.scheme == "http" -> "Open Archive"
            hasTrack -> "Local"
            else -> ""
        },
        currentMood     = selectedMood,
        currentEqPreset = selectedEqPreset,
        onDismiss       = dismissWithCancel,
        onSave          = { name, mood, eq ->
            viewModel.saveVaib(name, mood, eq)
            dismissWithSave()
        },
    )
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
            val glyph = glyphs.getOrElse(note.glyphIndex % glyphs.size) { "♪" }
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

// ── Now Playing hero card ─────────────────────────────────────────────

@Composable
private fun NowPlayingCard(
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
            .border(BorderStroke(0.6.dp, borderColor), RoundedCornerShape(22.dp))
            .background(VaibColors.DeepBackground),
    ) {
        // Artwork area
        ArtworkArea(
            isPlaying  = isPlaying,
            hasTrack   = hasTrack,
            atmosphere = atmosphere,
            modifier   = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)),
        )

        // Content below artwork
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            // Track name
            Text(
                text       = trackName ?: "Nothing playing",
                color      = if (hasTrack) Color.White else VaibColors.TextSoft.copy(0.45f),
                fontSize   = if (hasTrack) 20.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                letterSpacing = (-0.4).sp,
            )

            // Subtitle
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text      = subtitle,
                    color     = atmosphere.primaryColor.copy(alpha = 0.75f),
                    fontSize  = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Attribute chips (EQ + mood shown as chips only when both present or distinct)
            val showEqChip   = currentEqPreset != EqPreset.FLAT
            val showMoodChip = currentMood.isNotEmpty()
            if (showEqChip || showMoodChip) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

            // Progress row
            if (hasTrack) {
                Spacer(Modifier.height(12.dp))
                if (isBuffering) {
                    LinearProgressIndicator(
                        modifier   = Modifier.fillMaxWidth().height(2.dp),
                        color      = atmosphere.primaryColor.copy(0.55f),
                        trackColor = Color.White.copy(0.07f),
                    )
                } else {
                    LinearProgressIndicator(
                        progress   = { playbackFraction },
                        modifier   = Modifier.fillMaxWidth().height(2.dp),
                        color      = atmosphere.primaryColor,
                        trackColor = Color.White.copy(0.07f),
                        strokeCap  = StrokeCap.Round,
                    )
                }
                if (durationMs > 0) {
                    Spacer(Modifier.height(5.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(formatMs(currentPositionMs), color = VaibColors.TextSoft.copy(0.55f), fontSize = 10.sp)
                        Text(formatMs(durationMs),        color = VaibColors.TextSoft.copy(0.55f), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ── Artwork area — animated gradient + wave lines + bar viz overlay ───

@Composable
private fun ArtworkArea(
    isPlaying: Boolean,
    hasTrack: Boolean,
    atmosphere: VaibAtmosphere,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        // Background gradient + animated wave lines
        ArtworkCanvas(atmosphere = atmosphere, modifier = Modifier.fillMaxSize())

        // Bar visualizer overlay at bottom of artwork
        BarVisualizerPreview(
            isPlaying  = isPlaying,
            atmosphere = atmosphere,
            modifier   = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .align(Alignment.BottomCenter),
        )

        // Waveform level indicator top-right
        WaveformIndicator(
            isActive = isPlaying || hasTrack,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
        )
    }
}

@Composable
private fun ArtworkCanvas(atmosphere: VaibAtmosphere, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "artworkWaves")
    val phase by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(12_000, easing = LinearEasing), RepeatMode.Restart),
        label = "artPhase",
    )
    val twoPi = (2.0 * PI).toFloat()
    Canvas(modifier = modifier) {
        // Dark gradient background
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF040E12),
                    Color(0xFF0B0620),
                    Color(0xFF03030A),
                ),
                startY = 0f, endY = size.height,
            ),
        )
        // Three soft flowing wave lines
        val waveColors = listOf(atmosphere.primaryColor, atmosphere.secondaryColor, atmosphere.primaryColor)
        val waveAlphas = listOf(0.07f, 0.09f, 0.06f)
        val baseYs     = listOf(0.35f, 0.55f, 0.70f)
        val amplitudes = listOf(0.08f, 0.10f, 0.06f)
        val freqs      = listOf(1.4f, 1.0f, 1.8f)

        waveColors.forEachIndexed { i, color ->
            val wavePhase = phase * twoPi + i * twoPi / 3f
            val baseY     = size.height * baseYs[i]
            val amp       = size.height * amplitudes[i]
            val path      = Path()
            var first     = true
            for (step in 0..80) {
                val x = step.toFloat() / 80f * size.width
                val y = baseY + sin(x / size.width * twoPi * freqs[i] + wavePhase) * amp
                if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
            }
            drawPath(path, color.copy(alpha = waveAlphas[i]), style = Stroke(width = 1.8f))
        }
    }
}

@Composable
private fun WaveformIndicator(isActive: Boolean, modifier: Modifier = Modifier) {
    val alpha = if (isActive) 0.50f else 0.20f
    Canvas(modifier = modifier.size(20.dp)) {
        val bars = listOf(0.45f, 0.90f, 0.65f, 0.85f, 0.50f)
        val bw   = size.width / (bars.size * 2 - 1).toFloat()
        bars.forEachIndexed { i, h ->
            val barH = size.height * h
            drawRect(
                Color.White.copy(alpha),
                topLeft = Offset(i * bw * 2f, size.height - barH),
                size    = Size(bw, barH),
            )
        }
    }
}

// ── Standalone Visualizer card ────────────────────────────────────────

@Composable
private fun VisualizerCard(isPlaying: Boolean, atmosphere: VaibAtmosphere) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(VaibColors.DeepBackground)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            "VISUALIZER",
            color         = VaibColors.TextSoft.copy(0.45f),
            fontSize      = 9.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(10.dp))
        MountainBarViz(
            isPlaying  = isPlaying,
            atmosphere = atmosphere,
            modifier   = Modifier
                .fillMaxWidth()
                .height(80.dp),
        )
    }
}

// Mountain-envelope bar visualizer for the standalone card
private const val VIZ_BAR_COUNT = 28

@Composable
private fun MountainBarViz(isPlaying: Boolean, atmosphere: VaibAtmosphere, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "mountainViz")
    val phase by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(3_600, easing = LinearEasing), RepeatMode.Restart),
        label = "mvPhase",
    )
    val twoPi      = (2.0 * PI).toFloat()
    val multiplier = if (isPlaying) 1f else 0.18f

    Canvas(modifier = modifier) {
        val barW = size.width / VIZ_BAR_COUNT
        val maxH = size.height
        for (i in 0 until VIZ_BAR_COUNT) {
            val t          = i.toFloat() / (VIZ_BAR_COUNT - 1)
            val mountain   = sin(t * PI.toFloat())              // 0→1→0 envelope
            val speed      = 0.50f + (i % 5) * 0.22f + (i % 3) * 0.11f
            val rawPhase   = i.toFloat() / VIZ_BAR_COUNT
            val raw        = abs(sin((phase + rawPhase) * speed * twoPi))
            val h          = (maxH * multiplier * (mountain * 0.65f + raw * 0.35f)).coerceAtLeast(2f)
            val color      = lerp(atmosphere.primaryColor, atmosphere.secondaryColor, t)
                .copy(alpha = 0.45f + raw * 0.30f)
            drawRect(
                color   = color,
                topLeft = Offset(i * barW + 1.5f, maxH - h),
                size    = Size((barW - 3f).coerceAtLeast(1f), h),
            )
        }
    }
}

// Inline compact bar viz (for artwork overlay)
private const val BAR_COUNT = 20

@Composable
private fun BarVisualizerPreview(isPlaying: Boolean, atmosphere: VaibAtmosphere, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "barViz")
    val phase by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(3_400, easing = LinearEasing), RepeatMode.Restart),
        label = "barPhase",
    )
    val twoPi      = (2.0 * PI).toFloat()
    val multiplier = if (isPlaying) 1f else 0.12f

    Canvas(modifier = modifier) {
        val barW = size.width / BAR_COUNT
        val maxH = size.height
        for (i in 0 until BAR_COUNT) {
            val speed  = 0.55f + (i % 5) * 0.20f + (i % 3) * 0.12f
            val offset = i.toFloat() / BAR_COUNT
            val raw    = abs(sin((phase + offset) * speed * twoPi))
            val h      = (maxH * multiplier * (0.10f + raw * 0.90f)).coerceAtLeast(1.5f)
            val t      = i.toFloat() / (BAR_COUNT - 1).coerceAtLeast(1)
            val color  = lerp(atmosphere.primaryColor, atmosphere.secondaryColor, t)
                .copy(alpha = 0.55f + raw * 0.30f)
            drawRect(color, Offset(i * barW + 1f, maxH - h), Size((barW - 2f).coerceAtLeast(1f), h))
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

        // Center play/pause — prominent cyan circle
        val bgColor   = if (hasTrack) atmosphere.primaryColor else Color.White.copy(0.07f)
        val iconColor = if (hasTrack) Color.Black              else Color.White.copy(0.18f)
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

@Composable
private fun TransportSideButton(icon: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color.White.copy(0.04f)),
        contentAlignment = Alignment.Center,
    ) { icon() }
}

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

// ── vAIb out! hero button ─────────────────────────────────────────────

@Composable
private fun VaibOutButton(hasTrack: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (hasTrack) Color.White
                else Color.White.copy(alpha = 0.06f)
            )
            .clickable(enabled = hasTrack, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "vAIb out!",
            color      = if (hasTrack) Color.Black else Color.White.copy(0.18f),
            fontSize   = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.2.sp,
        )
    }
}

// ── Atmosphere chip (used inside NowPlayingCard) ───────────────────────

@Composable
private fun AtmosphereChip(label: String, color: Color) {
    Text(
        text          = label.uppercase(),
        color         = color,
        fontSize      = 9.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        modifier      = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.40f)), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

// ── Compact vAIb cards (horizontal row) ──────────────────────────────

@Composable
private fun CompactVaibCard(
    vaib: VaibEntity,
    onClick: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    val paint = androidx.compose.runtime.remember(vaib.id) {
        com.xsytrance.vaib.core.design.TrackPaint.fromVaibEntity(vaib)
    }
    val eqLabel = runCatching { EqPreset.valueOf(vaib.eqPreset) }
        .getOrDefault(EqPreset.FLAT)
        .takeIf { it != EqPreset.FLAT }
        ?.label

    val gradientColors = listOf(
        paint.primaryColor.copy(alpha = 0.18f),
        paint.secondaryColor.copy(alpha = 0.10f),
    )

    Box(
        modifier = Modifier
            .size(width = 138.dp, height = 126.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(VaibColors.DeepBackground)
            .border(
                BorderStroke(0.6.dp, paint.borderColor),
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick),
    ) {
        Column {
            // Visual / artwork area — painted by track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .background(
                        Brush.verticalGradient(colors = gradientColors),
                    ),
            ) {
                CompactCardWave(
                    accentColor = paint.primaryColor.copy(alpha = 0.25f),
                    modifier    = Modifier.fillMaxSize(),
                )
            }
            // Label area
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                Text(
                    vaib.vaibName,
                    color      = Color.White.copy(0.88f),
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                if (vaib.mood.isNotEmpty()) {
                    Text(
                        paint.vibeLabel.uppercase(),
                        color         = paint.primaryColor.copy(alpha = 0.55f),
                        fontSize      = 7.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 0.7.sp,
                    )
                } else if (eqLabel != null) {
                    Text(
                        eqLabel,
                        color    = paint.secondaryColor.copy(alpha = 0.50f),
                        fontSize = 9.sp,
                    )
                }
            }
        }

        // Subtle delete button top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp)
                .clickable(onClick = onDeleteRequest),
            contentAlignment = Alignment.Center,
        ) {
            Text("✕", color = Color.White.copy(0.28f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun CompactCardWave(accentColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path()
        path.moveTo(0f, size.height * 0.6f)
        for (step in 0..40) {
            val x = step / 40f * size.width
            val y = size.height * 0.6f + sin(x / size.width * (2 * PI).toFloat() * 1.2f) * size.height * 0.18f
            path.lineTo(x, y)
        }
        path.lineTo(size.width, size.height)
        path.lineTo(0f, size.height)
        path.close()
        drawPath(path, color = Color.White.copy(alpha = 0.06f))
        // Single line arc
        val linePath = Path()
        linePath.moveTo(0f, size.height * 0.55f)
        for (step in 0..40) {
            val x = step / 40f * size.width
            val y = size.height * 0.55f + sin(x / size.width * (2 * PI).toFloat() * 1.0f + 0.8f) * size.height * 0.14f
            linePath.lineTo(x, y)
        }
        drawPath(linePath, accentColor.copy(alpha = 0.0f).let {
            lerp(Color.White.copy(0.15f), accentColor.copy(0.05f), 0.5f)
        }, style = Stroke(width = 1.2f))
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
        modifier = modifier.height(48.dp),
        shape    = RoundedCornerShape(12.dp),
        colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        border   = BorderStroke(1.dp, VaibColors.TextSoft.copy(alpha = 0.28f)),
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
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
            containerColor = Color.White.copy(0.07f),
            contentColor   = Color.White,
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp),
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp)
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
        elevation = ButtonDefaults.buttonElevation(0.dp),
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

// ── Kimi Dreamdeck Lab badge ──────────────────────────────────────────

@Composable
private fun KimiLabBadge(atmosphere: VaibAtmosphere) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0A0A0A))
                .border(
                    BorderStroke(0.5.dp, atmosphere.primaryColor.copy(alpha = 0.35f)),
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                "KIMI DREAMDECK LAB",
                color         = atmosphere.primaryColor.copy(alpha = 0.85f),
                fontSize      = 8.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )
        }
        Text(
            "Paintable Atmosphere Test",
            color         = VaibColors.TextSoft.copy(alpha = 0.38f),
            fontSize      = 8.sp,
            fontWeight    = FontWeight.Medium,
            letterSpacing = 0.4.sp,
        )
    }
}

// ── Kimi experiment debug footer ──────────────────────────────────────

@Composable
private fun KimiDebugFooter(atmosphere: VaibAtmosphere) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.fillMaxWidth(),
    ) {
        Text(
            "branch: kimi-experiment-dreamdeck",
            color         = VaibColors.TextSoft.copy(alpha = 0.22f),
            fontSize      = 8.sp,
            fontWeight    = FontWeight.Medium,
            letterSpacing = 0.3.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Atmosphere branch \u2022 Kimi experiment",
            color         = atmosphere.secondaryColor.copy(alpha = 0.18f),
            fontSize      = 7.sp,
            fontWeight    = FontWeight.Medium,
            letterSpacing = 0.3.sp,
        )
    }
}

// ── Dreamdeck Cockpit: Background ─────────────────────────────────────

@Composable
private fun DreamdeckBackground(
    atmosphere: VaibAtmosphere,
    hasTrack: Boolean,
    modifier: Modifier = Modifier,
) {
    val primary   = atmosphere.primaryColor
    val secondary = atmosphere.secondaryColor
    val bgAccent  = atmosphere.backgroundAccent

    Box(modifier = modifier.background(Color.Black)) {
        // Subtle radial gradient from center — painted by track atmosphere
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primary.copy(alpha   = if (hasTrack) 0.06f else 0.03f),
                        secondary.copy(alpha = if (hasTrack) 0.03f else 0.015f),
                        bgAccent.copy(alpha   = 0.80f),
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.35f),
                    radius = size.width * 0.8f,
                ),
            )
        }
    }
}

// ── Dreamdeck Cockpit: Hero ───────────────────────────────────────────

@Composable
private fun DreamdeckHero(
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
    onPlayPause: () -> Unit,
) {
    val subtitle = when {
        currentMood.isNotEmpty() -> currentMood
        trackUri?.scheme == "https" || trackUri?.scheme == "http" -> "Internet Archive"
        hasTrack -> "Local file"
        else -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(22.dp)),
    ) {
        // Living visualizer fills the hero
        CockpitVisualizer(
            isPlaying  = isPlaying,
            atmosphere = atmosphere,
            modifier   = Modifier.fillMaxSize(),
        )

        // Soft gradient overlay at bottom for text readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                    ),
                ),
        )

        // Track info overlay at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Text(
                text       = trackName ?: "Nothing playing",
                color      = if (hasTrack) Color.White else VaibColors.TextSoft.copy(0.45f),
                fontSize   = if (hasTrack) 18.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                letterSpacing = (-0.3).sp,
            )
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text      = subtitle,
                    color     = atmosphere.primaryColor.copy(alpha = 0.65f),
                    fontSize  = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            // Compact chips row
            val showEq   = currentEqPreset != EqPreset.FLAT
            val showMood = currentMood.isNotEmpty()
            if (showEq || showMood) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (showEq) {
                        AtmosphereChip(
                            label = currentEqPreset.label,
                            color = atmosphere.secondaryColor,
                        )
                    }
                    if (showMood) {
                        AtmosphereChip(
                            label = currentMood,
                            color = atmosphere.primaryColor,
                        )
                    }
                }
            }
        }

        // Floating transport controls centered
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            TransportSideButton { SkipPrevIcon(Modifier.size(20.dp)) }

            Spacer(Modifier.width(24.dp))

            val bgColor   = if (hasTrack) atmosphere.primaryColor else Color.White.copy(0.07f)
            val iconColor = if (hasTrack) Color.Black else Color.White.copy(0.18f)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .clickable(enabled = hasTrack, onClick = onPlayPause),
                contentAlignment = Alignment.Center,
            ) {
                if (isPlaying) PauseIcon(Modifier.size(24.dp), iconColor)
                else           PlayIcon (Modifier.size(24.dp), iconColor)
            }

            Spacer(Modifier.width(24.dp))

            TransportSideButton { SkipNextIcon(Modifier.size(20.dp)) }
        }

        // Progress line at very bottom
        if (hasTrack) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp),
            ) {
                if (isBuffering) {
                    LinearProgressIndicator(
                        modifier   = Modifier.fillMaxWidth().height(2.dp),
                        color      = atmosphere.primaryColor.copy(alpha = 0.55f),
                        trackColor = Color.White.copy(alpha = 0.06f),
                    )
                } else {
                    LinearProgressIndicator(
                        progress   = { playbackFraction },
                        modifier   = Modifier.fillMaxWidth().height(2.dp),
                        color      = atmosphere.primaryColor.copy(alpha = 0.75f),
                        trackColor = Color.White.copy(alpha = 0.06f),
                        strokeCap  = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

// ── Cockpit visualizer (full-bleed waveform field) ─────────────────────

@Composable
private fun CockpitVisualizer(
    isPlaying: Boolean,
    atmosphere: VaibAtmosphere,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "cockpitViz")
    val phase by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(3_600, easing = LinearEasing), RepeatMode.Restart),
        label = "cockpitVizPhase",
    )
    val twoPi      = (2.0 * PI).toFloat()
    val multiplier = if (isPlaying) 1f else 0.20f
    val barCount   = 40

    Canvas(modifier = modifier) {
        val barW = size.width / barCount
        val maxH = size.height * 0.65f
        for (i in 0 until barCount) {
            val t        = i.toFloat() / (barCount - 1).coerceAtLeast(1)
            val mountain = sin(t * PI.toFloat()).toFloat()
            val speed    = 0.45f + (i % 5).toFloat() * 0.18f + (i % 3).toFloat() * 0.10f
            val offset   = i.toFloat() / barCount
            val raw      = kotlin.math.abs(
                kotlin.math.sin(((phase + offset) * speed * twoPi).toDouble())
            ).toFloat()
            val h        = (maxH * multiplier * (mountain * 0.55f + raw * 0.45f)).coerceAtLeast(2f)
            val color    = lerp(atmosphere.primaryColor, atmosphere.secondaryColor, t)
                .copy(alpha = 0.12f + raw * 0.22f)
            drawRect(
                color   = color,
                topLeft = Offset(i * barW + 1f, size.height - h),
                size    = Size((barW - 2f).coerceAtLeast(1f), h),
            )
        }
    }
}

// ── Cockpit Actions ────────────────────────────────────────────────────

@Composable
private fun CockpitActions(
    hasTrack: Boolean,
    atmosphere: VaibAtmosphere,
    onVaibOut: () -> Unit,
    onPickTrack: () -> Unit,
    onDiscover: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // vAIb out! — dark glassy cockpit button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0A0A0A))
                .border(
                    BorderStroke(1.dp, atmosphere.primaryColor.copy(
                        alpha = if (hasTrack) 0.45f else 0.12f
                    )),
                    RoundedCornerShape(14.dp),
                )
                .clickable(enabled = hasTrack, onClick = onVaibOut),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "vAIb out!",
                    color      = if (hasTrack) atmosphere.primaryColor.copy(alpha = 0.90f)
                                else VaibColors.TextSoft.copy(alpha = 0.25f),
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.2.sp,
                )
                if (hasTrack) {
                    Text(
                        "save this atmosphere",
                        color      = VaibColors.TextSoft.copy(alpha = 0.30f),
                        fontSize   = 8.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.2.sp,
                    )
                }
            }
        }

        // Navigation row: Change Track | Orbit
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
                label    = "Orbit",
                onClick  = onDiscover,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ── Kimi Lab Stamp (small, bottom of screen) ──────────────────────────

@Composable
private fun KimiLabStamp(atmosphere: VaibAtmosphere) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically,
        modifier              = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF0A0A0A))
                .border(
                    BorderStroke(0.5.dp, atmosphere.primaryColor.copy(alpha = 0.20f)),
                    RoundedCornerShape(4.dp),
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                "KIMI DREAMDECK LAB",
                color         = atmosphere.primaryColor.copy(alpha = 0.50f),
                fontSize      = 7.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            "kimi-experiment-dreamdeck",
            color         = VaibColors.TextSoft.copy(alpha = 0.18f),
            fontSize      = 7.sp,
            fontWeight    = FontWeight.Medium,
            letterSpacing = 0.2.sp,
        )
    }
}

// ── Queue hint chip ───────────────────────────────────────────────────

@Composable
private fun QueueHintChip(onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0A0A0A))
            .border(
                BorderStroke(0.8.dp, VaibColors.CyanPulse.copy(alpha = 0.22f)),
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(50))
                .background(VaibColors.CyanPulse.copy(alpha = 0.70f)),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Shuffled Orbit ready",
            color         = VaibColors.CyanPulse.copy(alpha = 0.75f),
            fontSize      = 12.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
        )
        Spacer(Modifier.weight(1f))
        Text(
            "\u2192",
            color      = VaibColors.CyanPulse.copy(alpha = 0.55f),
            fontSize   = 14.sp,
        )
    }
}
