package com.xsytrance.vaib.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.xsytrance.vaib.MainViewModel
import com.xsytrance.vaib.core.design.TrackPalette
import kotlinx.coroutines.delay

@Composable
fun AuraSpectrumVisualizer(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val view = LocalView.current

    // RECORD_AUDIO permission + analyzer lifecycle
    val context = view.context
    var analyzerActive by remember { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) analyzerActive = viewModel.startAnalyzer()
    }
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            analyzerActive = viewModel.startAnalyzer()
            if (!analyzerActive) { delay(600L); analyzerActive = viewModel.startAnalyzer() }
        } else {
            permLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    DisposableEffect(Unit) { onDispose { viewModel.stopAnalyzer() } }

    // Immersive fullscreen
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val ctrl = WindowInsetsControllerCompat(window, view)
        ctrl.hide(WindowInsetsCompat.Type.systemBars())
        ctrl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose { ctrl.show(WindowInsetsCompat.Type.systemBars()) }
    }

    val targetBands  by viewModel.audioFreqBands.collectAsState()
    val energy       by viewModel.audioEnergy.collectAsState()
    val beatPulse    by viewModel.audioBeatPulse.collectAsState()
    val palette      by viewModel.trackPalette.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val fraction     by viewModel.playbackFraction.collectAsState()
    val isPlaying    by viewModel.isPlaying.collectAsState()

    // Animated palette
    val vibrant      by animateColorAsState(palette.vibrant,      tween(700), label = "v")
    val darkVibrant  by animateColorAsState(palette.darkVibrant,  tween(900), label = "dv")
    val lightVibrant by animateColorAsState(palette.lightVibrant, tween(700), label = "lv")
    val animBeat     by animateFloatAsState(beatPulse,  tween(80),  label = "beat")
    val animEnergy   by animateFloatAsState(energy,     tween(120), label = "energy")

    // 60fps display bands with peak caps
    val displayBands    = remember { FloatArray(32) }
    val peakCaps        = remember { FloatArray(32) }
    val latestBands     by rememberUpdatedState(targetBands)
    var frameCount      by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            for (i in displayBands.indices) {
                val t = latestBands.getOrElse(i) { 0f }
                displayBands[i] = displayBands[i] * 0.72f + t * 0.28f
                if (displayBands[i] > peakCaps[i]) peakCaps[i] = displayBands[i]
                peakCaps[i] = (peakCaps[i] * 0.993f).coerceAtLeast(displayBands[i])
            }
            frameCount++
        }
    }

    // Reading frameCount in composable body causes recomposition every 16ms
    val f = frameCount

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // ── Main visualizer canvas ─────────────────────────────────
        Canvas(Modifier.fillMaxSize()) {
            @Suppress("UNUSED_EXPRESSION") f

            // Background radial gradient
            drawRect(
                Brush.radialGradient(
                    colors = listOf(darkVibrant.copy(0.85f), Color(0xFF050505)),
                    center = Offset(size.width / 2, size.height * 0.4f),
                    radius = size.width * 0.95f,
                )
            )

            // Beat flash overlay
            if (animBeat > 0.08f) {
                drawRect(vibrant.copy(alpha = animBeat * 0.07f))
            }

            // EQ bars — bottom 48% of screen
            drawEqBars(
                bands       = displayBands,
                peaks       = peakCaps,
                darkVibrant = darkVibrant,
                vibrant     = vibrant,
                lightVibrant = lightVibrant,
                energy      = animEnergy,
                beat        = animBeat,
            )

            // Center radial glow (behind the art placeholder)
            drawCircle(
                Brush.radialGradient(
                    colors = listOf(
                        vibrant.copy(0.12f + animBeat * 0.08f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width / 2, size.height * 0.32f),
                    radius = size.width * (0.28f + animEnergy * 0.06f),
                ),
                center = Offset(size.width / 2, size.height * 0.32f),
                radius = size.width * (0.28f + animEnergy * 0.06f),
            )
        }

        // ── Album art placeholder (center top area) ────────────────
        val artSize = (130 + animEnergy * 14).dp
        Box(
            Modifier
                .size(artSize)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(darkVibrant.copy(0.95f), Color(0xFF080808))
                    )
                )
                .align(Alignment.TopCenter)
                .offset(y = (80 + animEnergy * 6).dp),
        ) {
            // Glow border via canvas
            Canvas(Modifier.fillMaxSize()) {
                val borderAlpha = 0.5f + animBeat * 0.45f
                val borderWidth = 2.5f + animBeat * 3.5f
                drawCircle(
                    vibrant.copy(borderAlpha),
                    radius = size.minDimension / 2f - borderWidth / 2f,
                    style  = androidx.compose.ui.graphics.drawscope.Stroke(borderWidth),
                )
            }
            Text(
                currentTrack?.title?.take(1)?.uppercase() ?: "♪",
                color    = vibrant.copy(0.45f),
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // ── Track info ─────────────────────────────────────────────
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = (230 + animEnergy * 6).dp)
                .padding(horizontal = 28.dp),
        ) {
            val titleColor = lerp(Color.White, lightVibrant, animBeat * 0.75f)
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    currentTrack?.title ?: "",
                    color      = titleColor,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    letterSpacing = (-0.4).sp,
                )
                if (currentTrack?.artist?.isNotBlank() == true) {
                    Text(
                        currentTrack!!.artist,
                        color    = vibrant.copy(0.60f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // ── Progress bar (very bottom) ─────────────────────────────
        LinearProgressIndicator(
            progress   = { fraction },
            modifier   = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.BottomCenter),
            color      = vibrant,
            trackColor = Color.White.copy(0.08f),
            strokeCap  = StrokeCap.Butt,
        )

        // ── Close button ───────────────────────────────────────────
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(0.08f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Text("✕", color = Color.White.copy(0.55f), fontSize = 14.sp)
        }

        // ── Play/pause tap zone ────────────────────────────────────
        Box(
            Modifier
                .align(Alignment.Center)
                .offset(y = 60.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(0.06f))
                .clickable { viewModel.togglePlayPause() },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(20.dp)) {
                if (isPlaying) {
                    val bw = size.width*0.22f; val bh = size.height*0.70f
                    val gap = size.width*0.16f; val sx = (size.width-bw*2f-gap)/2f
                    val sy = (size.height-bh)/2f
                    drawRect(Color.White.copy(0.8f), Offset(sx,sy), Size(bw,bh))
                    drawRect(Color.White.copy(0.8f), Offset(sx+bw+gap,sy), Size(bw,bh))
                } else {
                    drawPath(androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width*0.26f, size.height*0.14f)
                        lineTo(size.width*0.83f, size.height*0.50f)
                        lineTo(size.width*0.26f, size.height*0.86f)
                        close()
                    }, Color.White.copy(0.8f))
                }
            }
        }

        // Fallback label when analyzer unavailable
        if (!analyzerActive) {
            Text(
                "tap play to enable visualizer",
                color    = Color.White.copy(0.25f),
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp),
            )
        }
    }
}

// ── EQ bar drawing ─────────────────────────────────────────────────────

private fun DrawScope.drawEqBars(
    bands: FloatArray,
    peaks: FloatArray,
    darkVibrant: Color,
    vibrant: Color,
    lightVibrant: Color,
    energy: Float,
    beat: Float,
) {
    val numBars    = bands.size
    val areaH      = size.height * 0.48f
    val areaBottom = size.height
    val margin     = size.width * 0.025f
    val totalW     = size.width - margin * 2
    val spacing    = 2.8f
    val barW       = (totalW - spacing * (numBars - 1)) / numBars

    for (i in 0 until numBars) {
        val x    = margin + i * (barW + spacing)
        val barH = (areaH * bands.getOrElse(i) { 0f }).coerceAtLeast(4f)
        val top  = areaBottom - barH

        // Outer glow pass (2 layers — wider + more transparent)
        for (spread in 2 downTo 1) {
            val s = spread.toFloat()
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lightVibrant.copy(0.08f * s),
                        vibrant.copy(0.05f * s),
                        Color.Transparent,
                    ),
                    startY = top - s * 6f,
                    endY   = areaBottom,
                ),
                topLeft      = Offset(x - barW * s * 0.4f, top - s * 6f),
                size         = Size(barW * (1f + s * 0.8f), barH + s * 12f),
                cornerRadius = CornerRadius(barW / 2 + s * 3f),
            )
        }

        // Main bar body — gradient: light top → vibrant mid → dark bottom
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(lightVibrant, vibrant, darkVibrant),
                startY = top,
                endY   = areaBottom,
            ),
            topLeft      = Offset(x, top),
            size         = Size(barW, barH),
            cornerRadius = CornerRadius(barW / 2),
        )

        // Peak cap — bright white-tinted sliver
        val peakH    = (areaH * peaks.getOrElse(i) { 0f }).coerceAtLeast(4f)
        val capTop   = areaBottom - peakH - 5f
        val capAlpha = (0.85f + beat * 0.15f).coerceIn(0f, 1f)
        drawRoundRect(
            color        = lerp(lightVibrant, Color.White, 0.6f).copy(capAlpha),
            topLeft      = Offset(x, capTop),
            size         = Size(barW, 3.5f),
            cornerRadius = CornerRadius(2f),
        )
    }
}
