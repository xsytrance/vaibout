package com.xsytrance.vaib.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xsytrance.vaib.MainViewModel
import com.xsytrance.vaib.audio.EqPreset
import com.xsytrance.vaib.core.design.VaibColors
import com.xsytrance.vaib.data.entities.VaibEntity
import com.xsytrance.vaib.vaib.VaibCard

private val MOOD_OPTIONS = listOf("Deep", "Chill", "Energetic", "Cosmic", "Focus")

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
    val hasTrack = trackUri != null

    var showSaveDialog     by remember { mutableStateOf(false) }
    var nameInput          by remember { mutableStateOf("") }
    var selectedMood       by remember { mutableStateOf("") }
    var selectedEqPreset   by remember { mutableStateOf(EqPreset.FLAT) }
    var pendingDeleteVaib  by remember { mutableStateOf<VaibEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 48.dp),
    ) {
        // ── Header ────────────────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = "vAIb out!",
                color = Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1.2).sp,
            )
            Text(
                    text = "let's chill",
                color = VaibColors.CyanPulse,
                fontSize = 12.sp,
                letterSpacing = 0.3.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(28.dp))
        }

        // ── Now Playing card ──────────────────────────────────────────
        item {
            NowPlayingCard(
                trackName        = trackName,
                trackUri         = trackUri,
                isPlaying        = isPlaying,
                isBuffering      = isBuffering,
                playbackFraction = playbackFraction,
                hasTrack         = hasTrack,
            )
            Spacer(modifier = Modifier.height(28.dp))
        }

        // ── Transport controls ────────────────────────────────────────
        item {
            TransportControls(
                isPlaying = isPlaying,
                hasTrack  = hasTrack,
                onPlayPause = viewModel::togglePlayPause,
            )
            Spacer(modifier = Modifier.height(28.dp))
        }

        // ── vAIb out! hero button ─────────────────────────────────────
        item {
            Button(
                onClick  = onEnterDreamscape,
                enabled  = hasTrack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape  = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor         = VaibColors.CyanPulse,
                    contentColor           = Color.Black,
                    disabledContainerColor = VaibColors.CyanPulse.copy(alpha = 0.10f),
                    disabledContentColor   = VaibColors.TextSoft.copy(alpha = 0.28f),
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    text       = "vAIb out!",
                    fontSize   = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.3.sp,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Secondary action pills ────────────────────────────────────
        item {
            Row(
                modifier            = Modifier.fillMaxWidth(),
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
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick  = {
                            selectedEqPreset = currentEqPreset
                            showSaveDialog = true
                        },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text       = "+ Save as vAIb",
                        color      = VaibColors.TextSoft.copy(alpha = 0.50f),
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Normal,
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // ── Your vAIbs ────────────────────────────────────────────────
        if (savedVaibs.isNotEmpty()) {
            item {
                Text(
                    text          = "YOUR VAIBS",
                    color         = VaibColors.TextSoft.copy(alpha = 0.50f),
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

    // ── Delete vAIb confirmation dialog ──────────────────────────────
    pendingDeleteVaib?.let { vaib ->
        AlertDialog(
            onDismissRequest  = { pendingDeleteVaib = null },
            containerColor    = VaibColors.DeepBackground,
            titleContentColor = Color.White,
            textContentColor  = VaibColors.TextSoft,
            title = {
                Text("Delete vAIb?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text("\"${vaib.vaibName}\" will be removed from your collection.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVaib(vaib)
                        pendingDeleteVaib = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFCC3333),
                        contentColor   = Color.White,
                    ),
                    shape     = RoundedCornerShape(10.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
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
        val dismiss = {
            showSaveDialog = false
            nameInput = ""
            selectedMood = ""
            selectedEqPreset = EqPreset.FLAT
        }
        AlertDialog(
            onDismissRequest  = dismiss,
            containerColor    = VaibColors.DeepBackground,
            titleContentColor = Color.White,
            textContentColor  = VaibColors.TextSoft,
            title = {
                Text("Save vAIb", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column {
                    OutlinedTextField(
                        value         = nameInput,
                        onValueChange = { nameInput = it },
                        label         = { Text("Name") },
                        placeholder   = { Text(trackName ?: "My vAIb") },
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
                                onClick  = {
                                    selectedMood = if (selectedMood == mood) "" else mood
                                },
                                label = {
                                    Text(mood, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                },
                                colors = FilterChipDefaults.filterChipColors(
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
                                    // Live-preview: apply immediately if audio is active.
                                    viewModel.applyEqPreset(preset)
                                },
                                label = {
                                    Text(preset.label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                },
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
                            nameInput.ifEmpty { trackName ?: "Untitled vAIb" },
                            selectedMood,
                            selectedEqPreset,
                        )
                        dismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VaibColors.CyanPulse,
                        contentColor   = Color.Black,
                    ),
                    shape     = RoundedCornerShape(10.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = dismiss) {
                    Text("Cancel", color = VaibColors.TextSoft)
                }
            },
        )
    }
}

// ── Now Playing card ─────────────────────────────────────────────────

@Composable
private fun NowPlayingCard(
    trackName: String?,
    trackUri: android.net.Uri?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    playbackFraction: Float,
    hasTrack: Boolean,
) {
    val statusText = when {
        isBuffering           -> "BUFFERING…"
        isPlaying             -> "PLAYING"
        playbackFraction > 0f -> "PAUSED"
        hasTrack              -> "READY"
        else                  -> "NO TRACK"
    }
    val statusColor = if (isPlaying) VaibColors.CyanPulse else VaibColors.TextSoft.copy(alpha = 0.55f)

    val sourceLabel = when (trackUri?.scheme) {
        "https", "http" -> "Internet Archive stream"
        null            -> null
        else            -> "Local file"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(VaibColors.DeepBackground)
            .padding(horizontal = 18.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MiniOrb(isPlaying = isPlaying, hasTrack = hasTrack)
            Spacer(modifier = Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text          = statusText,
                    color         = statusColor,
                    fontSize      = 9.sp,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text       = trackName ?: "Choose a track to vAIb out",
                    color      = if (hasTrack) Color.White else VaibColors.TextSoft.copy(alpha = 0.40f),
                    fontSize   = if (hasTrack) 17.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 2,
                    lineHeight = 22.sp,
                    overflow   = TextOverflow.Ellipsis,
                )
                if (sourceLabel != null) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text          = sourceLabel,
                        color         = VaibColors.TextSoft.copy(alpha = 0.38f),
                        fontSize      = 10.sp,
                        letterSpacing = 0.4.sp,
                    )
                }
            }
        }

        if (hasTrack) {
            Spacer(modifier = Modifier.height(16.dp))
            if (isBuffering) {
                LinearProgressIndicator(
                    modifier   = Modifier.fillMaxWidth().height(2.dp),
                    color      = VaibColors.CyanPulse.copy(alpha = 0.55f),
                    trackColor = Color.White.copy(alpha = 0.07f),
                )
            } else {
                LinearProgressIndicator(
                    progress   = { playbackFraction },
                    modifier   = Modifier.fillMaxWidth().height(2.dp),
                    color      = VaibColors.CyanPulse,
                    trackColor = Color.White.copy(alpha = 0.07f),
                    strokeCap  = StrokeCap.Round,
                )
            }
        }
    }
}

// ── Mini animated orb ─────────────────────────────────────────────────

@Composable
private fun MiniOrb(isPlaying: Boolean, hasTrack: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue  = 1.00f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )
    val orbScale = if (isPlaying && hasTrack) breathe else 0.72f
    val coreAlpha = when {
        isPlaying && hasTrack -> 0.90f
        hasTrack              -> 0.32f
        else                  -> 0.08f
    }

    Canvas(modifier = Modifier.size(56.dp)) {
        val r = size.minDimension / 2f
        // Outer glow halo
        drawCircle(
            color  = VaibColors.CyanPulse.copy(alpha = coreAlpha * 0.18f),
            radius = r * orbScale * 1.45f,
        )
        // Core orb
        drawCircle(
            color  = VaibColors.CyanPulse.copy(alpha = coreAlpha),
            radius = r * orbScale,
        )
    }
}

// ── Transport controls ────────────────────────────────────────────────

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
        // ⏮ Previous (not yet implemented — shown disabled)
        TransportCircle(
            symbol  = "⏮",
            size    = 56,
            enabled = false,
        )
        Spacer(modifier = Modifier.size(28.dp))

        // ▶/⏸ Play/Pause
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    if (hasTrack) Color.White.copy(alpha = 0.92f)
                    else          Color.White.copy(alpha = 0.06f)
                )
                .clickable(enabled = hasTrack, onClick = onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text     = if (isPlaying) "⏸" else "▶",
                fontSize = if (isPlaying) 24.sp else 26.sp,
                color    = if (hasTrack) Color.Black else VaibColors.TextSoft.copy(alpha = 0.22f),
            )
        }

        Spacer(modifier = Modifier.size(28.dp))
        // ⏭ Next (not yet implemented — shown disabled)
        TransportCircle(
            symbol  = "⏭",
            size    = 56,
            enabled = false,
        )
    }
}

@Composable
private fun TransportCircle(symbol: String, size: Int, enabled: Boolean) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.04f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text     = symbol,
            fontSize = (size * 0.36f).sp,
            color    = VaibColors.TextSoft.copy(alpha = if (enabled) 0.75f else 0.20f),
        )
    }
}

// ── Shared button styles (also used by DiscoverScreen) ───────────────

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
        Text(
            text          = label,
            fontSize      = 14.sp,
            fontWeight    = FontWeight.Medium,
            letterSpacing = 0.3.sp,
        )
    }
}

@Composable
internal fun VaibSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick  = onClick,
        modifier = modifier.height(54.dp),
        shape    = RoundedCornerShape(12.dp),
        colors   = ButtonDefaults.buttonColors(
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
        onClick  = onClick,
        modifier = modifier.height(58.dp),
        shape    = RoundedCornerShape(14.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor = VaibColors.CyanPulse,
            contentColor   = Color.Black,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}
