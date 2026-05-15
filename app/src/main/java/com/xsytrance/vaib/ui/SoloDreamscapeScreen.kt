package com.xsytrance.vaib.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.xsytrance.vaib.MainViewModel
import com.xsytrance.vaib.audio.EqPreset
import com.xsytrance.vaib.core.design.VaibAtmosphere
import com.xsytrance.vaib.core.design.VaibColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

private const val TAG_DS = "VaibDreamscape"

// ── Fullscreen Dreamdeck bar counts ──────────────────────────────────
private const val DS_BAR_COUNT = 36

@Composable
fun SoloDreamscapeScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val view           = LocalView.current
    val scope          = rememberCoroutineScope()
    val audioEnergy    by viewModel.audioEnergy.collectAsState()
    val audioBeat      by viewModel.audioBeatPulse.collectAsState()
    val trackName      by viewModel.trackName.collectAsState()
    val currentMood    by viewModel.currentMood.collectAsState()
    val currentEqPreset by viewModel.currentEqPreset.collectAsState()
    var reactiveAvailable by remember { mutableStateOf(false) }

    // ── Permission + analyzer start (one-shot retry for stream warm-up) ──
    val context = view.context
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                reactiveAvailable = viewModel.startAnalyzer()
                if (!reactiveAvailable) {
                    delay(500L)
                    reactiveAvailable = viewModel.startAnalyzer()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val already = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG_DS, "RECORD_AUDIO granted=$already")
        if (already) {
            reactiveAvailable = viewModel.startAnalyzer()
            if (!reactiveAvailable) {
                delay(500L)
                reactiveAvailable = viewModel.startAnalyzer()
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) { onDispose { viewModel.stopAnalyzer() } }

    // ── Immersive fullscreen ──────────────────────────────────────────
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val ctrl   = WindowInsetsControllerCompat(window, view)
        ctrl.hide(WindowInsetsCompat.Type.systemBars())
        ctrl.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose { ctrl.show(WindowInsetsCompat.Type.systemBars()) }
    }

    BackHandler(onBack = onBack)

    // ── Touch state ───────────────────────────────────────────────────
    // tapPulse: 0→1 on tap then decays (driven by LaunchedEffect)
    var tapPulseTarget  by remember { mutableFloatStateOf(0f) }
    var tapPulse        by remember { mutableFloatStateOf(0f) }
    var tapPosition     by remember { mutableStateOf(Offset.Zero) }
    // dragOffset: accumulated drag for wave phase bending (-1..1)
    var dragOffsetX     by remember { mutableFloatStateOf(0f) }
    // longPressGlow: 0→1 while held
    var longPressGlow   by remember { mutableFloatStateOf(0f) }

    // Decay tap pulse each frame via a coroutine
    LaunchedEffect(tapPulseTarget) {
        if (tapPulseTarget > 0f) {
            tapPulse = 1f
            while (tapPulse > 0.01f) {
                delay(16L)
                tapPulse = (tapPulse * 0.88f).coerceAtLeast(0f)
            }
            tapPulse = 0f
        }
    }

    val atmosphere = VaibAtmosphere.Default

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Tap: fire radial pulse at touch point
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        tapPosition    = offset
                        tapPulseTarget = tapPulseTarget + 1f  // re-trigger
                    },
                    onLongPress = {
                        longPressGlow  = 0f
                        scope.launch {
                            // Gently ramp up while hold is detected, then decay
                            var g = 0f
                            repeat(30) { delay(30L); g = (g + 0.05f).coerceAtMost(1f); longPressGlow = g }
                            delay(600L)
                            while (longPressGlow > 0.01f) { delay(16L); longPressGlow = (longPressGlow * 0.93f) }
                            longPressGlow = 0f
                        }
                    },
                )
            }
            // Drag: bend wave phase
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd   = { scope.launch { while (abs(dragOffsetX) > 0.002f) { delay(16L); dragOffsetX *= 0.90f }; dragOffsetX = 0f } },
                    onDrag      = { _, delta -> dragOffsetX = (dragOffsetX + delta.x / 400f).coerceIn(-1f, 1f) },
                )
            },
    ) {
        DreamdeckFullscreenViz(
            energy       = audioEnergy,
            beat         = audioBeat,
            tapPulse     = tapPulse,
            tapPosition  = tapPosition,
            dragOffsetX  = dragOffsetX,
            longPressGlow = longPressGlow,
            atmosphere   = atmosphere,
            modifier     = Modifier.fillMaxSize(),
        )

        // ── Close button ─────────────────────────────────────────────
        IconButton(
            onClick  = onBack,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp)
                .size(44.dp),
        ) {
            Icon(
                imageVector     = Icons.Default.Close,
                contentDescription = "Exit",
                tint            = Color.White.copy(alpha = 0.40f),
                modifier        = Modifier.size(22.dp),
            )
        }

        // ── Mini-player label ─────────────────────────────────────────
        if (!trackName.isNullOrBlank()) {
            MiniPlayerLabel(
                trackName   = trackName ?: "",
                mood        = currentMood,
                eqPreset    = currentEqPreset,
                modifier    = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
            )
        }

        // Subtle fallback note
        if (!reactiveAvailable) {
            Text(
                text       = "Reactive audio unavailable",
                color      = VaibColors.TextSecondary.copy(alpha = 0.30f),
                fontSize   = 10.sp,
                modifier   = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp),
            )
        }
    }
}

// ── Fullscreen Dreamdeck Visualizer ──────────────────────────────────

@Composable
private fun DreamdeckFullscreenViz(
    energy:        Float,
    beat:          Float,
    tapPulse:      Float,
    tapPosition:   Offset,
    dragOffsetX:   Float,
    longPressGlow: Float,
    atmosphere:    VaibAtmosphere,
    modifier:      Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "dsViz")

    // Slow drift phase for wave background
    val wavePhase by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(16_000, easing = LinearEasing), RepeatMode.Restart),
        label = "dsWavePhase",
    )
    // Note drift phase (slower)
    val notePhase by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(26_000, easing = LinearEasing), RepeatMode.Restart),
        label = "dsNotePhase",
    )
    // Bar shimmer phase
    val barPhase by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(3_800, easing = LinearEasing), RepeatMode.Restart),
        label = "dsBarPhase",
    )

    // Remap raw energy (observed 0.05–0.45) to 0–1
    val e    = ((energy - 0.05f) / 0.40f).coerceIn(0f, 1f)
    val b    = beat.coerceIn(0f, 1f)
    val glow = (e * 0.6f + b * 0.3f + longPressGlow * 0.3f).coerceIn(0f, 1f)
    val twoPi = (2.0 * PI).toFloat()

    Canvas(modifier = modifier) {
        // ── 1. Background radial vignette ─────────────────────────────
        drawRect(
            Brush.radialGradient(
                colors = listOf(
                    atmosphere.secondaryColor.copy(alpha = 0.08f + glow * 0.06f),
                    Color.Black,
                ),
                center = Offset(size.width * 0.5f, size.height * 0.55f),
                radius = size.width * (0.75f + glow * 0.10f),
            ),
        )

        // ── 2. Flowing background wave lines ─────────────────────────
        drawWaveLines(
            phase      = wavePhase + dragOffsetX * 0.4f,
            energy     = e,
            atmosphere = atmosphere,
            twoPi      = twoPi,
        )

        // ── 3. Floating note glyphs (drawn as tiny bars / dots) ───────
        drawFloatingNotes(
            phase      = notePhase,
            energy     = e,
            beat       = b,
            atmosphere = atmosphere,
            twoPi      = twoPi,
        )

        // ── 4. Reactive bar field ─────────────────────────────────────
        drawReactiveBars(
            barPhase   = barPhase,
            energy     = e,
            beat       = b,
            atmosphere = atmosphere,
            twoPi      = twoPi,
        )

        // ── 5. Tap ripple pulse ───────────────────────────────────────
        if (tapPulse > 0.02f) {
            drawTapRipple(
                center     = tapPosition,
                pulse      = tapPulse,
                atmosphere = atmosphere,
            )
        }
    }
}

// ── Wave lines layer ─────────────────────────────────────────────────

private fun DrawScope.drawWaveLines(
    phase: Float, energy: Float, atmosphere: VaibAtmosphere, twoPi: Float,
) {
    // 4 subtle sine waves spread vertically
    val configs = listOf(
        Triple(0.28f, 0.08f + energy * 0.06f, 1.3f),
        Triple(0.45f, 0.11f + energy * 0.09f, 0.9f),
        Triple(0.60f, 0.07f + energy * 0.05f, 1.7f),
        Triple(0.75f, 0.09f + energy * 0.07f, 1.1f),
    )
    val colors = listOf(
        atmosphere.primaryColor,
        atmosphere.secondaryColor,
        atmosphere.primaryColor,
        atmosphere.secondaryColor,
    )
    val baseAlphas = listOf(0.08f, 0.10f, 0.07f, 0.09f)

    configs.forEachIndexed { i, (yFrac, ampFrac, freq) ->
        val wavePhaseOff = phase * twoPi + i * twoPi / 4f
        val baseY        = size.height * yFrac
        val amp          = size.height * ampFrac
        val path         = Path()
        var first        = true
        for (step in 0..100) {
            val x = step.toFloat() / 100f * size.width
            val y = baseY + sin(x / size.width * twoPi * freq + wavePhaseOff) * amp
            if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
        }
        drawPath(
            path,
            colors[i].copy(alpha = baseAlphas[i] + energy * 0.06f),
            style = Stroke(width = 1.4f + energy * 0.8f, cap = StrokeCap.Round),
        )
    }
}

// ── Floating note dots layer ──────────────────────────────────────────

private data class NoteConfig(
    val xFrac: Float, val yBase: Float, val speed: Float,
    val sway: Float, val swayFreq: Float, val phase: Float,
)

private val NOTE_CONFIGS = listOf(
    NoteConfig(0.10f, 0.85f, 0.14f, 0.04f, 1.2f, 0.00f),
    NoteConfig(0.82f, 0.30f, 0.10f, 0.03f, 0.8f, 1.80f),
    NoteConfig(0.25f, 0.60f, 0.16f, 0.05f, 1.5f, 3.50f),
    NoteConfig(0.68f, 0.18f, 0.12f, 0.04f, 1.0f, 0.90f),
    NoteConfig(0.46f, 0.75f, 0.09f, 0.03f, 0.7f, 2.40f),
    NoteConfig(0.15f, 0.44f, 0.13f, 0.06f, 1.3f, 4.20f),
    NoteConfig(0.90f, 0.68f, 0.11f, 0.04f, 0.9f, 1.20f),
    NoteConfig(0.56f, 0.08f, 0.15f, 0.05f, 1.1f, 5.10f),
)

private fun DrawScope.drawFloatingNotes(
    phase: Float, energy: Float, beat: Float,
    atmosphere: VaibAtmosphere, twoPi: Float,
) {
    NOTE_CONFIGS.forEachIndexed { i, cfg ->
        val t   = (phase * cfg.speed + cfg.phase / twoPi).rem(1f)
        val y   = size.height * (cfg.yBase - t).rem(1f).let { if (it < 0) it + 1f else it }
        val x   = size.width  * (cfg.xFrac + sin(t * twoPi * cfg.swayFreq) * cfg.sway)
        val baseAlpha = 0.12f + energy * 0.08f + beat * 0.10f
        val color     = if (i % 2 == 0) atmosphere.primaryColor else atmosphere.secondaryColor

        // Draw as a tiny 3-bar waveform icon (simplified note glyph)
        val w = 6.dp.toPx()
        val bh = 3.dp.toPx() + energy * 4.dp.toPx()
        listOf(0.4f, 1.0f, 0.7f).forEachIndexed { j, hFrac ->
            drawRect(
                color   = color.copy(alpha = baseAlpha),
                topLeft = Offset(x + j * (w * 0.45f), y - bh * hFrac),
                size    = Size(w * 0.30f, bh * hFrac),
            )
        }
    }
}

// ── Reactive bar field ────────────────────────────────────────────────

private fun DrawScope.drawReactiveBars(
    barPhase: Float, energy: Float, beat: Float,
    atmosphere: VaibAtmosphere, twoPi: Float,
) {
    val barW   = size.width / DS_BAR_COUNT
    val maxH   = size.height * 0.58f           // bars occupy bottom ~60%
    val baseY  = size.height                   // bars grow upward from bottom

    for (i in 0 until DS_BAR_COUNT) {
        val t        = i.toFloat() / (DS_BAR_COUNT - 1)
        // Mountain envelope: taller in middle
        val mountain = sin(t * PI.toFloat())
        val speed    = 0.50f + (i % 5) * 0.22f + (i % 3) * 0.11f
        val rawPhase = i.toFloat() / DS_BAR_COUNT
        val raw      = abs(sin((barPhase + rawPhase) * speed * twoPi))

        // Energy drives overall height; beat adds a short jump
        val energyH  = energy * 0.75f + 0.10f
        val beatBoost = beat * 0.18f
        val h = (maxH * (mountain * 0.60f + raw * 0.40f) * (energyH + beatBoost))
            .coerceAtLeast(3f)

        val color = lerp(atmosphere.primaryColor, atmosphere.secondaryColor, t)

        // Bar body
        val alpha = 0.50f + raw * 0.28f + energy * 0.15f + beat * 0.08f
        drawRect(
            color   = color.copy(alpha = alpha.coerceIn(0f, 1f)),
            topLeft = Offset(i * barW + 1.5f, baseY - h),
            size    = Size((barW - 3f).coerceAtLeast(1f), h),
        )

        // Glow cap at top of each bar
        val glowAlpha = (energy * 0.20f + beat * 0.25f).coerceIn(0f, 0.60f)
        if (glowAlpha > 0.02f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors  = listOf(color.copy(glowAlpha), color.copy(0f)),
                    startY  = baseY - h - 12f,
                    endY    = baseY - h + 4f,
                ),
                topLeft = Offset(i * barW, baseY - h - 12f),
                size    = Size(barW, 16f),
            )
        }
    }
}

// ── Tap ripple ───────────────────────────────────────────────────────

private fun DrawScope.drawTapRipple(
    center: Offset, pulse: Float, atmosphere: VaibAtmosphere,
) {
    val maxRadius = size.width * 0.45f
    // Two expanding rings that fade as they grow
    listOf(pulse to 1.0f, (pulse * 0.7f) to 0.6f).forEach { (p, scale) ->
        val radius = maxRadius * (1f - p) * scale + 10f
        val alpha  = p * 0.40f
        drawCircle(
            color  = atmosphere.primaryColor.copy(alpha = alpha),
            radius = radius,
            center = center,
            style  = Stroke(width = 1.5f + p * 1.5f),
        )
    }
    // Small center flash dot
    drawCircle(
        color  = Color.White.copy(alpha = pulse * 0.55f),
        radius = 6f + pulse * 8f,
        center = center,
    )
}

// ── Mini-player label ─────────────────────────────────────────────────

@Composable
private fun MiniPlayerLabel(
    trackName: String,
    mood:      String,
    eqPreset:  EqPreset,
    modifier:  Modifier = Modifier,
) {
    val labelParts = buildList {
        if (mood.isNotBlank()) add(mood)
        if (eqPreset != EqPreset.FLAT) add(eqPreset.label)
    }.joinToString(" · ")

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.52f), CircleShape)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text          = trackName,
                color         = Color.White.copy(alpha = 0.85f),
                fontSize      = 13.sp,
                fontWeight    = FontWeight.SemiBold,
                maxLines      = 1,
                overflow      = TextOverflow.Ellipsis,
            )
            if (labelParts.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text      = labelParts,
                    color     = VaibColors.CyanPulse.copy(alpha = 0.70f),
                    fontSize  = 10.sp,
                    maxLines  = 1,
                )
            }
        }
    }
}
