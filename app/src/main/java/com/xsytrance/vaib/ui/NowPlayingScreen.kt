package com.xsytrance.vaib.ui

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xsytrance.vaib.MainViewModel
import com.xsytrance.vaib.audio.EqPreset
import com.xsytrance.vaib.core.design.MotionTokens
import com.xsytrance.vaib.core.design.StationTheme
import com.xsytrance.vaib.core.design.VaibAtmosphere
import com.xsytrance.vaib.core.design.VaibColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun NowPlayingScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val trackName by viewModel.trackName.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val playbackFraction by viewModel.playbackFraction.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val currentEqPreset by viewModel.currentEqPreset.collectAsState()
    val audioEnergy by viewModel.audioEnergy.collectAsState()

    val atmosphere = StationTheme.NEON_CYAN.toAtmosphere()

    var showEqPanel by remember { mutableStateOf(false) }
    var showSpeedPicker by remember { mutableStateOf(false) }

    // Immersive fullscreen
    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(Unit) {
        val activity = view.context as android.app.Activity
        val ctrl = androidx.core.view.WindowInsetsControllerCompat(activity.window, view)
        ctrl.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        ctrl.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose { ctrl.show(androidx.core.view.WindowInsetsCompat.Type.systemBars()) }
    }

    androidx.compose.ui.BackHandler { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Animated background ──────────────────────────────
        NowPlayingBackground(atmosphere = atmosphere, energy = audioEnergy)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Top bar ───────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onBack() }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    "vAIb out!",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = { showEqPanel = !showEqPanel }) {
                    Icon(
                        Icons.Default.Equalizer,
                        contentDescription = "EQ",
                        tint = if (currentEqPreset != EqPreset.FLAT)
                            atmosphere.primaryColor
                        else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Album art / visualizer area ───────────────────
            NowPlayingArtwork(
                isPlaying = isPlaying,
                atmosphere = atmosphere,
                energy = audioEnergy,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 24.dp),
            )

            Spacer(Modifier.height(8.dp))

            // ── Track info ────────────────────────────────────
            Text(
                text = trackName ?: "Unknown Track",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))

            // Progress bar
            if (durationMs > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        formatMs(currentPositionMs),
                        color = VaibColors.TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.width(40.dp),
                    )
                    // Custom seek bar
                    Spacer(Modifier.weight(1f))
                    Text(
                        formatMs(durationMs),
                        color = VaibColors.TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.width(40.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
            }

            // ── Transport controls ────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Shuffle
                IconButton(onClick = { /* TODO: shuffle mode */ }) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(26.dp),
                    )
                }

                Spacer(Modifier.width(24.dp))

                // Previous
                IconButton(onClick = { viewModel.skipPrevious() }) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }

                Spacer(Modifier.width(24.dp))

                // Play/Pause — large circle
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    atmosphere.primaryColor.copy(alpha = 0.3f),
                                    atmosphere.primaryColor,
                                ),
                            )
                        )
                        .clickable { viewModel.togglePlayPause() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp),
                    )
                }

                Spacer(Modifier.width(24.dp))

                // Next
                IconButton(onClick = { viewModel.skipNext() }) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }

                Spacer(Modifier.width(24.dp))

                // Speed button
                IconButton(onClick = { showSpeedPicker = !showSpeedPicker }) {
                    Text(
                        text = "${"%.0f".format(audioPlayer.playbackSpeed)}x",
                        color = atmosphere.primaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(Modifier.width(16.dp))

                // Repeat
                IconButton(onClick = { /* TODO: repeat mode */ }) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(26.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── EQ Panel ──────────────────────────────────────
            if (showEqPanel) {
                EqPanel(
                    currentPreset = currentEqPreset,
                    onPresetSelected = { viewModel.applyEqPreset(it) },
                    onDismiss = { showEqPanel = false },
                    atmosphere = atmosphere,
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NowPlayingBackground(atmosphere: VaibAtmosphere, energy: Float) {
    val transition = rememberInfiniteTransition(label = "npBg")
    val phase by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(20_000, easing = LinearEasing), RepeatMode.Restart),
        label = "npBgPhase",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Radial gradient from center
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    atmosphere.primaryColor.copy(alpha = 0.12f + energy * 0.08f),
                    atmosphere.secondaryColor.copy(alpha = 0.06f + energy * 0.04f),
                    Color.Transparent,
                ),
                center = Offset(size.width / 2, size.height / 2),
                radius = size.width * 0.5f,
            ),
        )

        // Flowing wave lines
        val twoPi = (2.0 * PI).toFloat()
        val baseY = size.height * 0.6f
        val amp = size.height * (0.04f + energy * 0.03f)
        val path = Path()
        for (i in 0..80) {
            val x = i.toFloat() / 80f * size.width
            val y = baseY + sin(x / size.width * twoPi * 1.5f + phase * twoPi) * amp
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path,
            atmosphere.primaryColor.copy(alpha = 0.08f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun NowPlayingArtwork(
    isPlaying: Boolean,
    atmosphere: VaibAtmosphere,
    energy: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center,
    ) {
        // Animated gradient background
        val transition = rememberInfiniteTransition(label = "artwork")
        val phase by transition.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(12_000, easing = LinearEasing), RepeatMode.Restart),
            label = "artPhase",
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Three color blobs that slowly morph
            val colors = listOf(
                atmosphere.primaryColor.copy(alpha = 0.15f + energy * 0.1f),
                atmosphere.secondaryColor.copy(alpha = 0.10f + energy * 0.06f),
                Color.Transparent,
            )
            val cx = size.width * (0.3f + 0.1f * sin(phase * twoPi()))
            val cy = size.height * (0.4f + 0.1f * sin(phase * twoPi() + 2f))
            val radius = size.width * (0.35f + energy * 0.1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors[0], Color.Transparent),
                    center = Offset(cx, cy),
                    radius = radius,
                ),
            )
        }

        // Vinyl-style rotating ring when playing
        if (isPlaying) {
            val spin by transition.animateFloat(
                0f, 360f,
                infiniteRepeatable(tween(8_000, easing = LinearEasing)),
                label = "vinylSpin",
            )
            Canvas(modifier = Modifier.size(180.dp).align(Alignment.Center)) {
                drawArc(
                    color = Color.White.copy(alpha = 0.06f),
                    startAngle = 0f,
                    sweepAngle = 300f,
                    useCenter = false,
                    topLeft = Offset(10f, 10f),
                    size = androidx.compose.ui.geometry.Size(160f, 160f),
                    style = Stroke(width = 1.5f),
                )
            }
        }

        // Pause/play icon overlay
        val iconAlpha by animateFloatAsState(
            targetValue = if (isPlaying) 0f else 0.7f,
            animationSpec = tween(300),
            label = "playIconAlpha",
        )
        if (!isPlaying) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White.copy(alpha = iconAlpha),
                modifier = Modifier.size(48.dp),
            )
        }
    }
}

private fun twoPi() = (2.0 * PI).toFloat()

private fun formatMs(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
private fun EqPanel(
    currentPreset: EqPreset,
    onPresetSelected: (EqPreset) -> Unit,
    onDismiss: () -> Unit,
    atmosphere: VaibAtmosphere,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        color = VaibColors.SurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            0.5f, Color.White.copy(alpha = 0.08f)
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "EQUALIZER",
                    color = VaibColors.TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close EQ",
                        tint = VaibColors.TextTertiary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Preset chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                EqPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = currentPreset == preset,
                        onClick = { onPresetSelected(preset) },
                        label = {
                            Text(
                                preset.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = atmosphere.primaryColor,
                            selectedLabelColor = Color.Black,
                            containerColor = VaibColors.DeepBackground,
                            labelColor = VaibColors.TextSecondary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = currentPreset == preset,
                            borderColor = Color.Transparent,
                            selectedBorderColor = Color.Transparent,
                        ),
                    )
                }
            }
        }
    }
}