package com.xsytrance.vaib.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.xsytrance.vaib.MainViewModel
import com.xsytrance.vaib.Screen
import com.xsytrance.vaib.audio.EqPreset
import com.xsytrance.vaib.core.design.TrackPalette

private fun formatMs(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
fun NowPlayingScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val track          by viewModel.currentTrack.collectAsState()
    val isPlaying      by viewModel.isPlaying.collectAsState()
    val isBuffering    by viewModel.isBuffering.collectAsState()
    val fraction       by viewModel.playbackFraction.collectAsState()
    val posMs          by viewModel.currentPositionMs.collectAsState()
    val durMs          by viewModel.durationMs.collectAsState()
    val palette        by viewModel.trackPalette.collectAsState()
    val eqPreset       by viewModel.currentEqPreset.collectAsState()
    val shuffle        by viewModel.shuffleEnabled.collectAsState()
    val repeat         by viewModel.repeatEnabled.collectAsState()
    val beatPulse      by viewModel.audioBeatPulse.collectAsState()
    val energy         by viewModel.audioEnergy.collectAsState()

    val vibrant        by animateColorAsState(palette.vibrant,     tween(600), label = "v")
    val darkVibrant    by animateColorAsState(palette.darkVibrant, tween(800), label = "dv")
    val lightVibrant   by animateColorAsState(palette.lightVibrant,tween(600), label = "lv")
    val animBeat       by animateFloatAsState(beatPulse, tween(80), label = "beat")
    val animEnergy     by animateFloatAsState(energy, tween(120), label = "energy")

    var isSeeking      by remember { mutableStateOf(false) }
    var seekFraction   by remember { mutableFloatStateOf(0f) }

    val displayFraction = if (isSeeking) seekFraction else fraction

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(darkVibrant.copy(0.9f), Color.Black, Color.Black))
            )
            .systemBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Top bar ───────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.08f))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(14.dp)) {
                        val c = Color.White.copy(0.7f)
                        drawLine(c, Offset(size.width*0.72f, 0f),         Offset(size.width*0.14f, size.height*0.5f), 2.5f, StrokeCap.Round)
                        drawLine(c, Offset(size.width*0.72f, size.height), Offset(size.width*0.14f, size.height*0.5f), 2.5f, StrokeCap.Round)
                    }
                }

                Text(
                    "NOW PLAYING",
                    color = vibrant.copy(0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                )

                // vAIb out! button
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(vibrant)
                        .clickable { viewModel.navigateTo(Screen.VISUALIZER) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text("vAIb out!", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Album art ─────────────────────────────────────────────
            val artSize = (240 + animEnergy * 16).dp
            val artGlow = animBeat * 0.5f + animEnergy * 0.3f
            Box(
                Modifier
                    .size(artSize)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(darkVibrant.copy(0.9f), Color(0xFF050505))
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                // Inner glow ring
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(vibrant.copy(artGlow * 0.25f), Color.Transparent),
                            center = Offset(size.width / 2, size.height / 2),
                            radius = size.minDimension * 0.6f,
                        )
                    )
                }
                if (track?.albumArtUrl != null) {
                    AsyncImage(
                        model = track?.albumArtUrl,
                        contentDescription = track?.title,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    // Large letter placeholder
                    Text(
                        track?.title?.take(1)?.uppercase() ?: "♪",
                        color = vibrant.copy(0.35f),
                        fontSize = 96.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                // Beat pulse border overlay
                if (animBeat > 0.05f) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(vibrant.copy(animBeat * 0.08f)),
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Title + artist ────────────────────────────────────────
            val titleColor = lerp(Color.White, lightVibrant, animBeat * 0.7f)
            Text(
                track?.title ?: "Nothing playing",
                color = titleColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = (-0.5).sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                track?.artist ?: "",
                color = vibrant.copy(0.65f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )

            // ── Tag chips ─────────────────────────────────────────────
            if (track?.tags?.isNotEmpty() == true) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    track!!.tags.take(5).forEach { tag ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(vibrant.copy(0.12f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                tag.lowercase(),
                                color = vibrant.copy(0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Seek slider ───────────────────────────────────────────
            Slider(
                value = displayFraction,
                onValueChange = { v ->
                    isSeeking    = true
                    seekFraction = v
                },
                onValueChangeFinished = {
                    viewModel.seekTo(seekFraction)
                    isSeeking = false
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor            = vibrant,
                    activeTrackColor      = vibrant,
                    inactiveTrackColor    = Color.White.copy(0.12f),
                ),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatMs(posMs), color = Color.White.copy(0.45f), fontSize = 11.sp)
                Text(formatMs(durMs), color = Color.White.copy(0.45f), fontSize = 11.sp)
            }

            Spacer(Modifier.height(24.dp))

            // ── Transport controls ────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // Shuffle
                NpIconButton(
                    active  = shuffle,
                    tint    = if (shuffle) vibrant else Color.White.copy(0.35f),
                    onClick = viewModel::toggleShuffle,
                ) {
                    Canvas(Modifier.size(20.dp)) { drawShuffleIcon(this, if (shuffle) palette.vibrant else Color.White.copy(0.35f)) }
                }

                // Skip prev
                NpIconButton(tint = Color.White.copy(0.8f), onClick = viewModel::skipPrev) {
                    Canvas(Modifier.size(24.dp)) {
                        val c = Color.White.copy(0.85f)
                        drawRect(c, Offset(size.width*0.10f, size.height*0.18f), Size(size.width*0.10f, size.height*0.64f))
                        drawPath(Path().apply {
                            moveTo(size.width*0.88f, size.height*0.18f)
                            lineTo(size.width*0.28f, size.height*0.50f)
                            lineTo(size.width*0.88f, size.height*0.82f)
                            close()
                        }, c)
                    }
                }

                // Play/pause — large
                Box(
                    Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(vibrant)
                        .clickable(onClick = viewModel::togglePlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(28.dp)) {
                        if (isPlaying) {
                            val bw = size.width*0.22f; val bh = size.height*0.70f
                            val gap = size.width*0.16f; val sx = (size.width-bw*2f-gap)/2f
                            val sy = (size.height-bh)/2f
                            drawRect(Color.Black, Offset(sx,sy), Size(bw,bh))
                            drawRect(Color.Black, Offset(sx+bw+gap,sy), Size(bw,bh))
                        } else {
                            drawPath(Path().apply {
                                moveTo(size.width*0.26f, size.height*0.14f)
                                lineTo(size.width*0.83f, size.height*0.50f)
                                lineTo(size.width*0.26f, size.height*0.86f)
                                close()
                            }, Color.Black)
                        }
                    }
                }

                // Skip next
                NpIconButton(tint = Color.White.copy(0.8f), onClick = viewModel::skipNext) {
                    Canvas(Modifier.size(24.dp)) {
                        val c = Color.White.copy(0.85f)
                        drawPath(Path().apply {
                            moveTo(size.width*0.12f, size.height*0.18f)
                            lineTo(size.width*0.72f, size.height*0.50f)
                            lineTo(size.width*0.12f, size.height*0.82f)
                            close()
                        }, c)
                        drawRect(c, Offset(size.width*0.80f, size.height*0.18f), Size(size.width*0.10f, size.height*0.64f))
                    }
                }

                // Repeat
                NpIconButton(
                    active  = repeat,
                    tint    = if (repeat) vibrant else Color.White.copy(0.35f),
                    onClick = viewModel::toggleRepeat,
                ) {
                    Canvas(Modifier.size(20.dp)) { drawRepeatIcon(this, if (repeat) palette.vibrant else Color.White.copy(0.35f)) }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── EQ preset row ─────────────────────────────────────────
            Text(
                "EQ",
                color = Color.White.copy(0.35f),
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EqPreset.entries.forEach { preset ->
                    val selected = eqPreset == preset
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) vibrant else Color.White.copy(0.07f))
                            .clickable { viewModel.applyEqPreset(preset) }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text(
                            preset.label,
                            color = if (selected) Color.Black else Color.White.copy(0.65f),
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NpIconButton(
    active: Boolean = false,
    tint: Color,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (active) tint.copy(0.12f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { icon() }
}

private fun drawShuffleIcon(scope: androidx.compose.ui.graphics.drawscope.DrawScope, color: Color) {
    scope.apply {
        val strokeW = 2.2f
        // Two crossing arrows simplified as lines with arrowheads
        drawLine(color, Offset(0f, size.height*0.3f), Offset(size.width*0.55f, size.height*0.3f), strokeW, StrokeCap.Round)
        drawLine(color, Offset(size.width*0.45f, size.height*0.7f), Offset(size.width, size.height*0.7f), strokeW, StrokeCap.Round)
        drawLine(color, Offset(0f, size.height*0.7f), Offset(size.width*0.55f, size.height*0.7f), strokeW, StrokeCap.Round)
        drawLine(color, Offset(size.width*0.45f, size.height*0.3f), Offset(size.width, size.height*0.3f), strokeW, StrokeCap.Round)
        // Arrowheads
        listOf(size.height*0.3f, size.height*0.7f).forEach { y ->
            drawLine(color, Offset(size.width*0.82f, y - size.height*0.12f), Offset(size.width, y), strokeW, StrokeCap.Round)
            drawLine(color, Offset(size.width*0.82f, y + size.height*0.12f), Offset(size.width, y), strokeW, StrokeCap.Round)
        }
    }
}

private fun drawRepeatIcon(scope: androidx.compose.ui.graphics.drawscope.DrawScope, color: Color) {
    scope.apply {
        val strokeW = 2.2f
        val r = size.minDimension * 0.38f
        val cx = size.width / 2; val cy = size.height / 2
        drawArc(color, -30f, 240f, false,
            Offset(cx - r, cy - r), Size(r*2, r*2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(strokeW, cap = StrokeCap.Round))
        // Arrowhead
        drawLine(color, Offset(cx + r*0.7f, cy - r*0.3f), Offset(cx + r, cy + r*0.1f), strokeW, StrokeCap.Round)
        drawLine(color, Offset(cx + r*1.0f, cy - r*0.5f), Offset(cx + r, cy + r*0.1f), strokeW, StrokeCap.Round)
    }
}
