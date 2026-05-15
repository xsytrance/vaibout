package com.xsytrance.vaib.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xsytrance.vaib.MainViewModel
import com.xsytrance.vaib.audio.EqPreset
import com.xsytrance.vaib.core.design.StationTheme
import com.xsytrance.vaib.core.design.VaibAtmosphere
import com.xsytrance.vaib.core.design.VaibColors
import com.xsytrance.vaib.visualizer.VisualizerStyle
import com.xsytrance.vaib.visualizer.VisualizerSurface
import com.xsytrance.vaib.data.entities.VisualizerStyleInfo
import com.xsytrance.vaib.data.entities.VISUALIZER_STYLES

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

    // ── Visualizer state ──────────────────────────────────
    var selectedVisualizerStyle by remember { mutableStateOf(VisualizerStyle.NEBULA) }
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

    BackHandler { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // ── Full-screen live visualizer background ──────────
        VisualizerSurface(
            modifier = Modifier.fillMaxSize(),
            style = selectedVisualizerStyle,
            energy = audioEnergy,
            beat = 0.5f,  // placeholder — real beat from analyzer
            primaryColor = atmosphere.primaryColor,
            secondaryColor = atmosphere.secondaryColor,
        )

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
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f)),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Text(
                    "vAIb out!",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Visualizer style chips
                    VisualizerStyleChip(
                        styleInfo = VISUALIZER_STYLES[0],
                        isSelected = selectedVisualizerStyle == VISUALIZER_STYLES[0].style,
                        onClick = { selectedVisualizerStyle = VISUALIZER_STYLES[0].style },
                    )
                    VisualizerStyleChip(
                        styleInfo = VISUALIZER_STYLES[1],
                        isSelected = selectedVisualizerStyle == VISUALIZER_STYLES[1].style,
                        onClick = { selectedVisualizerStyle = VISUALIZER_STYLES[1].style },
                    )
                    VisualizerStyleChip(
                        styleInfo = VISUALIZER_STYLES[2],
                        isSelected = selectedVisualizerStyle == VISUALIZER_STYLES[2].style,
                        onClick = { selectedVisualizerStyle = VISUALIZER_STYLES[2].style },
                    )

                    IconButton(onClick = { showEqPanel = !showEqPanel }) {
                        Text(
                            "EQ",
                            color = if (currentEqPreset != EqPreset.FLAT)
                                atmosphere.primaryColor
                            else Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Album art placeholder ───────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Pulsing album art placeholder
                val infiniteTransition = rememberInfiniteTransition(label = "artwork")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "artworkScale",
                )
                Box(
                    modifier = Modifier
                        .size(200.dp * scale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    atmosphere.primaryColor.copy(alpha = 0.15f),
                                    atmosphere.secondaryColor.copy(alpha = 0.05f),
                                    Color.Transparent,
                                ),
                            ),
                        )
                        .border(
                            width = 2.dp,
                            color = atmosphere.primaryColor.copy(alpha = 0.3f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "♪",
                        fontSize = 48.sp,
                        color = atmosphere.primaryColor.copy(alpha = 0.6f),
                    )
                }
            }

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
                        Text("⇄", color = Color.White.copy(alpha = 0.4f), fontSize = 18.sp)
                    }
                }

                Spacer(Modifier.width(24.dp))

                // Previous
                IconButton(onClick = { viewModel.skipPrevious() }) {
                        Text("⏮", color = Color.White, fontSize = 20.sp)
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
                    Text(
                        if (isPlaying) "⏸" else "▶",
                        color = Color.Black,
                        fontSize = 28.sp,
                    )

                Spacer(Modifier.width(24.dp))

                // Next
                IconButton(onClick = { viewModel.skipNext() }) {
                        Text("⏭", color = Color.White, fontSize = 20.sp)
                    }

                Spacer(Modifier.width(24.dp))

                // Speed button
                IconButton(onClick = { showSpeedPicker = !showSpeedPicker }) {
                    Text(
                        text = "${"%.0f".format(viewModel.playbackSpeed)}x",
                        color = atmosphere.primaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(Modifier.width(16.dp))

                // Repeat
                IconButton(onClick = { /* TODO: repeat mode */ }) {
                        Text("🔁", color = Color.White.copy(alpha = 0.4f), fontSize = 18.sp)
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

// ── Mini visualizer style chip ──────────────────────────────────────

@Composable
private fun VisualizerStyleChip(
    styleInfo: VisualizerStyleInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp)),
        color = if (isSelected)
            Color.White.copy(alpha = 0.15f)
        else
            Color.White.copy(alpha = 0.04f),
        border = if (isSelected)
            BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
        else null,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = styleInfo.icon,
                fontSize = 16.sp,
            )
        }
    }
}

// ── EQ Panel ────────────────────────────────────────────────────────

@Composable
private fun EqPanel(
    currentPreset: EqPreset,
    onPresetSelected: (EqPreset) -> Unit,
    onDismiss: () -> Unit,
    atmosphere: VaibAtmosphere,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VaibColors.DeepBackground)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "EQ Preset",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = onDismiss) {
                Text("Done", color = atmosphere.primaryColor, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EqPreset.entries.forEach { preset ->
                FilterChip(
                    selected = currentPreset == preset,
                    onClick = { onPresetSelected(preset) },
                    label = { Text(preset.label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = atmosphere.primaryColor,
                        selectedLabelColor = Color.Black,
                        containerColor = Color.White.copy(0.08f),
                        labelColor = Color.White.copy(0.75f),
                    ),
                )
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────

private fun formatMs(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}