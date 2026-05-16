package com.xsytrance.vaib.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.xsytrance.vaib.visualizer.VisualizerSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

private const val TAG_DS = "VaibDreamscape"
private const val DS_BAR_COUNT = 36

private val NOTE_GLYPHS = listOf("♪", "♫", "♬", "♩", "♭", "♯")

// ── Touch models ──────────────────────────────────────────────────────

private data class TouchRipple(val id: Long, val x: Float, val y: Float, val startedAt: Long)
private data class TouchTrailPoint(val x: Float, val y: Float, val age: Float, val pressure: Float)
private data class BeatRing(val id: Long, val startedAt: Long, val x: Float, val y: Float)

// ── Solo Dreamscape Screen ────────────────────────────────────────────

@Composable
fun SoloDreamscapeScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val view            = LocalView.current
    val scope           = rememberCoroutineScope()
    val audioEnergy     by viewModel.audioEnergy.collectAsState()
    val audioBeat       by viewModel.audioBeatPulse.collectAsState()
    val isPlaying       by viewModel.isPlaying.collectAsState()
    val trackName       by viewModel.trackName.collectAsState()
    val currentMood     by viewModel.currentMood.collectAsState()
    val currentEqPreset by viewModel.currentEqPreset.collectAsState()
    val atmosphere      by viewModel.currentAtmosphere.collectAsState()
    var reactiveAvailable by remember { mutableStateOf(false) }

    // ── Permission + analyzer start ───────────────────────────────────
    val context = view.context
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                reactiveAvailable = viewModel.startAnalyzer()
                if (!reactiveAvailable) { delay(500L); reactiveAvailable = viewModel.startAnalyzer() }
            }
        }
    }
    LaunchedEffect(Unit) {
        val already = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG_DS, "RECORD_AUDIO granted=$already")
        if (already) {
            reactiveAvailable = viewModel.startAnalyzer()
            if (!reactiveAvailable) { delay(500L); reactiveAvailable = viewModel.startAnalyzer() }
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
        ctrl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose { ctrl.show(WindowInsetsCompat.Type.systemBars()) }
    }
    BackHandler(onBack = onBack)

    // ── Touch + beat state ────────────────────────────────────────────
    var tapPulse       by remember { mutableFloatStateOf(0f) }
    var tapPos         by remember { mutableStateOf(Offset.Zero) }
    var dragOffsetX    by remember { mutableFloatStateOf(0f) }
    var longPressGlow  by remember { mutableFloatStateOf(0f) }
    var longPressPos   by remember { mutableStateOf(Offset.Zero) }
    val trailPoints    = remember { mutableListOf<TouchTrailPoint>() }
    val ripples        = remember { mutableListOf<TouchRipple>() }
    val beatRings      = remember { mutableListOf<BeatRing>() }
    var nextId         by remember { mutableStateOf(0L) }

    // Tap pulse decay
    LaunchedEffect(tapPulse) {
        while (tapPulse > 0.01f) { delay(16L); tapPulse = (tapPulse * 0.88f).coerceAtLeast(0f) }
        tapPulse = 0f
    }
    // Long press decay
    LaunchedEffect(longPressGlow) {
        while (longPressGlow > 0.01f) { delay(16L); longPressGlow = (longPressGlow * 0.93f) }
        longPressGlow = 0f
    }
    // Beat rings
    LaunchedEffect(audioBeat) {
        if (audioBeat > 0.25f) {
            beatRings.add(BeatRing(
                id = nextId++, startedAt = System.currentTimeMillis(),
                x = Random.nextFloat() * 0.6f + 0.2f, y = Random.nextFloat() * 0.6f + 0.2f,
            ))
            if (beatRings.size > 4) beatRings.removeAt(0)
        }
    }
    // Periodic cleanup
    LaunchedEffect(Unit) {
        while (true) {
            delay(100L)
            val now = System.currentTimeMillis()
            ripples.removeAll   { now - it.startedAt > 1_200L }
            beatRings.removeAll { now - it.startedAt > 1_500L }
            trailPoints.removeAll { it.age > 1f }
            if (trailPoints.isNotEmpty()) {
                val aged = trailPoints.map { it.copy(age = (it.age + 0.02f).coerceAtMost(1f)) }
                trailPoints.clear(); trailPoints.addAll(aged)
            }
        }
    }

    // ── Animate atmosphere colors ─────────────────────────────────────
    val livePrimary   by animateColorAsState(atmosphere.primaryColor,   tween(800), label = "dsPrim")
    val liveSecondary by animateColorAsState(atmosphere.secondaryColor, tween(800), label = "dsSec")
    val liveGlow      by animateColorAsState(atmosphere.glowColor,      tween(800), label = "dsGlow")

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Layer 1: OpenGL shader background
        VisualizerSurface(energy = audioEnergy, beat = audioBeat, modifier = Modifier.fillMaxSize())

        // Layer 2: Canvas overlay
        DreamdeckCanvasViz(
            energy        = audioEnergy,
            beat          = audioBeat,
            isPlaying     = isPlaying,
            tapPulse      = tapPulse,
            tapPos        = tapPos,
            dragOffsetX   = dragOffsetX,
            longPressGlow = longPressGlow,
            longPressPos  = longPressPos,
            trailPoints   = trailPoints.toList(),
            ripples       = ripples.toList(),
            beatRings     = beatRings.toList(),
            livePrimary   = livePrimary,
            liveSecondary = liveSecondary,
            liveGlow      = liveGlow,
            atmosphere    = atmosphere,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            tapPos = offset; tapPulse = 1f
                            ripples.add(TouchRipple(nextId++, offset.x, offset.y, System.currentTimeMillis()))
                            if (ripples.size > 6) ripples.removeAt(0)
                        },
                        onLongPress = { offset ->
                            longPressPos = offset; longPressGlow = 1f
                            scope.launch {
                                var g = 0f
                                repeat(20) { delay(30L); g = (g + 0.06f).coerceAtMost(1f); longPressGlow = g }
                                delay(400L)
                                while (longPressGlow > 0.01f) { delay(16L); longPressGlow *= 0.92f }
                                longPressGlow = 0f
                            }
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            trailPoints.add(TouchTrailPoint(offset.x, offset.y, 0f, 0.8f))
                        },
                        onDragEnd = {
                            scope.launch { while (abs(dragOffsetX) > 0.002f) { delay(16L); dragOffsetX *= 0.90f }; dragOffsetX = 0f }
                        },
                        onDrag = { _, dragAmount ->
                            dragOffsetX = (dragOffsetX + dragAmount.x / 400f).coerceIn(-1f, 1f)
                            val lastPt = trailPoints.lastOrNull()
                            if (lastPt != null) {
                                if (trailPoints.size > 80) trailPoints.removeAt(0)
                                trailPoints.add(TouchTrailPoint(
                                    lastPt.x + dragAmount.x, lastPt.y + dragAmount.y, 0f, 0.6f + longPressGlow * 0.4f
                                ))
                            }
                        },
                    )
                },
        )

        // Layer 3: Controls overlay
        DreamdeckControls(
            isPlaying    = isPlaying,
            trackName    = trackName,
            mood         = currentMood,
            eqPreset     = currentEqPreset,
            livePrimary  = livePrimary,
            onBack       = onBack,
            onPlayPause  = viewModel::togglePlayPause,
            onNext       = { viewModel.playNextFromQueue() },
            onPrev       = { viewModel.playPreviousFromQueue() },
            modifier     = Modifier.fillMaxSize(),
        )

        // Fallback note
        if (!reactiveAvailable) {
            Text(
                text     = "Reactive audio unavailable — grant microphone permission",
                color    = VaibColors.TextSoft.copy(alpha = 0.30f),
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 56.dp),
            )
        }
    }
}

// ── Canvas Visualizer ─────────────────────────────────────────────────

@Composable
private fun DreamdeckCanvasViz(
    energy:        Float,
    beat:          Float,
    isPlaying:     Boolean,
    tapPulse:      Float,
    tapPos:        Offset,
    dragOffsetX:   Float,
    longPressGlow: Float,
    longPressPos:  Offset,
    trailPoints:   List<TouchTrailPoint>,
    ripples:       List<TouchRipple>,
    beatRings:     List<BeatRing>,
    livePrimary:   Color,
    liveSecondary: Color,
    liveGlow:      Color,
    atmosphere:    VaibAtmosphere,
    modifier:      Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "dsViz")
    val wavePhase  by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(16_000, easing = LinearEasing), RepeatMode.Restart), label = "dsWave")
    val notePhase  by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(20_000, easing = LinearEasing), RepeatMode.Restart), label = "dsNote")
    val barPhase   by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(3_800, easing = LinearEasing), RepeatMode.Restart), label = "dsBar")

    val e = ((energy - 0.05f) / 0.40f).coerceIn(0f, 1f)
    val b = beat.coerceIn(0f, 1f)
    val motionMul = if (isPlaying) 1f else 0.25f
    val glow = (e * 0.5f + b * 0.2f + longPressGlow * 0.25f).coerceIn(0f, 0.8f)
    val twoPi = (2.0 * PI).toFloat()

    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height

        // 1. Background vignette — breathes with glow
        drawRect(Brush.radialGradient(
            colors = listOf(liveSecondary.copy(alpha = (0.06f + glow * 0.08f).coerceIn(0f, 1f)), Color.Black),
            center = Offset(w * 0.5f, h * 0.55f), radius = w * (0.75f + glow * 0.12f),
        ))

        // 2. Flowing wave lines
        drawWaveLines(wavePhase + dragOffsetX * 0.4f, e * motionMul, livePrimary, liveSecondary, twoPi)

        // 3. Floating musical notes as glyphs
        drawNoteGlyphs(notePhase, e * motionMul, b, livePrimary, liveSecondary, twoPi)

        // 4. Reactive bar field
        drawReactiveBars(barPhase, e * motionMul, b, livePrimary, liveSecondary, liveGlow, twoPi)

        // 5. Tap ripples (multiple)
        for (ripple in ripples) {
            val age = ((System.currentTimeMillis() - ripple.startedAt) / 1_200f).coerceIn(0f, 1f)
            val pulse = 1f - age
            if (pulse > 0.02f) drawTapRipple(Offset(ripple.x, ripple.y), pulse, livePrimary, liveSecondary)
        }

        // 6. Drag trail
        if (trailPoints.size >= 2) {
            val path = Path(); val first = trailPoints.first(); path.moveTo(first.x, first.y)
            for (i in 1 until trailPoints.size) { val pt = trailPoints[i]; path.lineTo(pt.x, pt.y) }
            val fade = (1f - trailPoints.first().age).coerceIn(0.05f, 0.8f)
            drawPath(path, livePrimary.copy(alpha = fade * 0.30f),
                style = Stroke(width = (3f + e * 2f).coerceAtLeast(1f), cap = StrokeCap.Round))
        }

        // 7. Long-press glow orb
        if (longPressGlow > 0.02f) {
            val orbR = 20f + longPressGlow * 45f; val orbA = (longPressGlow * 0.45f).coerceIn(0f, 1f)
            drawCircle(livePrimary.copy(alpha = orbA), orbR, longPressPos)
            drawCircle(liveSecondary.copy(alpha = orbA * 0.35f), orbR * 0.55f, longPressPos)
        }

        // 8. Beat pop rings
        for (ring in beatRings) {
            val age = ((System.currentTimeMillis() - ring.startedAt) / 1_500f).coerceIn(0f, 1f)
            val pulse = 1f - age
            if (pulse > 0.02f) {
                val maxR = w * 0.35f; val r = maxR * (1f - pulse * 0.5f) + 8f
                val a = (pulse * 0.35f).coerceIn(0f, 1f)
                drawCircle(livePrimary.copy(alpha = a), r, Offset(ring.x * w, ring.y * h), style = Stroke(width = 2.5f))
                drawCircle(liveSecondary.copy(alpha = a * 0.5f), r * 0.75f, Offset(ring.x * w, ring.y * h), style = Stroke(width = 1.5f))
            }
        }
    }
}

// ── Wave lines ────────────────────────────────────────────────────────

private fun DrawScope.drawWaveLines(
    phase: Float, energy: Float, primary: Color, secondary: Color, twoPi: Float,
) {
    val configs = listOf(
        Triple(0.28f, 0.08f + energy * 0.06f, 1.3f), Triple(0.45f, 0.11f + energy * 0.09f, 0.9f),
        Triple(0.60f, 0.07f + energy * 0.05f, 1.7f), Triple(0.75f, 0.09f + energy * 0.07f, 1.1f),
    )
    val colors = listOf(primary, secondary, primary, secondary)
    val baseAlphas = listOf(0.08f, 0.10f, 0.07f, 0.09f)
    configs.forEachIndexed { i, (yFrac, ampFrac, freq) ->
        val waveOff = phase * twoPi + i * twoPi / 4f
        val baseY = size.height * yFrac; val amp = size.height * ampFrac
        val path = Path(); var first = true
        for (step in 0..100) {
            val x = step.toFloat() / 100f * size.width
            val y = baseY + sin(x / size.width * twoPi * freq + waveOff) * amp
            if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
        }
        drawPath(path, colors[i].copy(alpha = (baseAlphas[i] + energy * 0.06f).coerceIn(0f, 1f)),
            style = Stroke(width = (1.4f + energy * 0.8f).coerceAtLeast(0.5f), cap = StrokeCap.Round))
    }
}

// ── Musical note glyphs ───────────────────────────────────────────────

private data class NoteConfig(val xFrac: Float, val yBase: Float, val speed: Float, val sway: Float, val swayFreq: Float, val phaseOff: Float)
private val NOTE_CONFIGS = listOf(
    NoteConfig(0.10f, 0.85f, 0.14f, 0.04f, 1.2f, 0.00f), NoteConfig(0.82f, 0.30f, 0.10f, 0.03f, 0.8f, 1.80f),
    NoteConfig(0.25f, 0.60f, 0.16f, 0.05f, 1.5f, 3.50f), NoteConfig(0.68f, 0.18f, 0.12f, 0.04f, 1.0f, 0.90f),
    NoteConfig(0.46f, 0.75f, 0.09f, 0.03f, 0.7f, 2.40f), NoteConfig(0.15f, 0.44f, 0.13f, 0.06f, 1.3f, 4.20f),
    NoteConfig(0.90f, 0.68f, 0.11f, 0.04f, 0.9f, 1.20f), NoteConfig(0.56f, 0.08f, 0.15f, 0.05f, 1.1f, 5.10f),
    NoteConfig(0.35f, 0.92f, 0.12f, 0.03f, 0.8f, 0.50f), NoteConfig(0.72f, 0.55f, 0.14f, 0.04f, 1.1f, 3.00f),
    NoteConfig(0.05f, 0.25f, 0.10f, 0.05f, 1.4f, 4.80f), NoteConfig(0.48f, 0.12f, 0.13f, 0.03f, 0.9f, 2.10f),
)

private fun DrawScope.drawNoteGlyphs(
    phase: Float, energy: Float, beat: Float, primary: Color, secondary: Color, twoPi: Float,
) {
    NOTE_CONFIGS.forEachIndexed { i, cfg ->
        val t = (phase * cfg.speed + cfg.phaseOff / twoPi).rem(1f)
        val yNorm = (cfg.yBase - t).rem(1f).let { if (it < 0) it + 1f else it }
        val y = size.height * yNorm
        val x = size.width * (cfg.xFrac + sin(t * twoPi * cfg.swayFreq).toFloat() * cfg.sway)
        val glyph = NOTE_GLYPHS[i % NOTE_GLYPHS.size]
        val baseAlpha = (0.10f + energy * 0.12f + beat * 0.15f).coerceIn(0.05f, 0.55f)
        val color = if (i % 2 == 0) primary else secondary
        val fontSize = (10f + energy * 6f).coerceAtLeast(6f)
        val fadeEdge = when { yNorm > 0.85f -> (1f - yNorm) / 0.15f; yNorm < 0.08f -> yNorm / 0.08f; else -> 1f }
        // Draw as small glow rectangles representing note glyphs
        val w = fontSize; val bh = fontSize * 0.8f
        listOf(0.5f, 1.0f, 0.6f).forEachIndexed { j, hFrac ->
            drawRect(color.copy(alpha = baseAlpha * fadeEdge),
                topLeft = Offset(x + j * (w * 0.4f), y - bh * hFrac), Size(w * 0.30f, bh * hFrac))
        }
    }
}

// ── Reactive bars ─────────────────────────────────────────────────────

private fun DrawScope.drawReactiveBars(
    barPhase: Float, energy: Float, beat: Float, primary: Color, secondary: Color, glow: Color, twoPi: Float,
) {
    val barW = size.width / DS_BAR_COUNT; val maxH = size.height * 0.58f; val baseY = size.height
    for (i in 0 until DS_BAR_COUNT) {
        val t = i.toFloat() / (DS_BAR_COUNT - 1)
        val mountain = sin(t * PI.toFloat()).toFloat()
        val speed = 0.50f + (i % 5) * 0.22f + (i % 3) * 0.11f
        val rawPhase = i.toFloat() / DS_BAR_COUNT
        val raw = abs(sin((barPhase + rawPhase) * speed * twoPi))
        val energyH = energy * 0.75f + 0.10f
        val beatBoost = beat * 0.18f
        val h = (maxH * (mountain * 0.60f + raw * 0.40f) * (energyH + beatBoost)).coerceAtLeast(3f)
        val color = lerp(primary, secondary, t)
        val alpha = (0.50f + raw * 0.28f + energy * 0.15f + beat * 0.08f).coerceIn(0f, 1f)
        drawRect(color.copy(alpha = alpha), Offset(i * barW + 1.5f, baseY - h), Size((barW - 3f).coerceAtLeast(1f), h))
        // Glow cap
        val glowAlpha = (energy * 0.20f + beat * 0.25f).coerceIn(0f, 1f)
        if (glowAlpha > 0.02f) {
            drawRect(Brush.verticalGradient(listOf(color.copy(alpha = glowAlpha), color.copy(alpha = 0f)),
                startY = baseY - h - 12f, endY = baseY - h + 4f),
                Offset(i * barW, baseY - h - 12f), Size(barW, 16f))
        }
    }
}

// ── Tap ripple ────────────────────────────────────────────────────────

private fun DrawScope.drawTapRipple(center: Offset, pulse: Float, primary: Color, secondary: Color) {
    val maxR = size.width * 0.45f
    listOf(pulse to 1.0f, (pulse * 0.7f) to 0.6f).forEach { (p, scale) ->
        val r = maxR * (1f - p) * scale + 10f; val a = (p * 0.40f).coerceIn(0f, 1f)
        drawCircle(primary.copy(alpha = a), r, center, style = Stroke(width = (1.5f + p * 1.5f).coerceAtLeast(0.5f)))
    }
    drawCircle(Color.White.copy(alpha = (pulse * 0.55f).coerceIn(0f, 1f)), 6f + pulse * 8f, center)
}

// ── Fullscreen Controls ───────────────────────────────────────────────

@Composable
private fun DreamdeckControls(
    isPlaying:    Boolean,
    trackName:    String?,
    mood:         String,
    eqPreset:     EqPreset,
    livePrimary:  Color,
    onBack:       () -> Unit,
    onPlayPause:  () -> Unit,
    onNext:       () -> Unit,
    onPrev:       () -> Unit,
    modifier:     Modifier = Modifier,
) {
    Box(modifier = modifier) {
        // Close button
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(44.dp)) {
            Icon(Icons.Default.Close, "Exit", tint = Color.White.copy(alpha = 0.45f), modifier = Modifier.size(22.dp))
        }

        // Bottom controls
        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Play/Pause + Prev/Next
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev, modifier = Modifier.size(44.dp)) {
                    SkipPrevIcon(Modifier.size(18.dp), Color.White.copy(alpha = 0.45f))
                }
                Box(modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(livePrimary.copy(alpha = 0.85f))
                    .clickable(onClick = onPlayPause), contentAlignment = Alignment.Center) {
                    if (isPlaying) PauseIcon(Modifier.size(22.dp), Color.Black)
                    else PlayIcon(Modifier.size(22.dp), Color.Black)
                }
                IconButton(onClick = onNext, modifier = Modifier.size(44.dp)) {
                    SkipNextIcon(Modifier.size(18.dp), Color.White.copy(alpha = 0.45f))
                }
            }
            Spacer(Modifier.height(10.dp))
            // Track label
            if (!trackName.isNullOrBlank()) {
                val labelParts = buildList { if (mood.isNotBlank()) add(mood); if (eqPreset != EqPreset.FLAT) add(eqPreset.label) }.joinToString(" · ")
                Box(modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.50f), CircleShape)
                    .padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(trackName, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (labelParts.isNotBlank()) {
                            Text(labelParts, color = livePrimary.copy(alpha = 0.70f), fontSize = 9.sp, maxLines = 1)
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            // DREAMDECK 2.0 stamp
            Text("DREAMDECK 2.0", color = livePrimary.copy(alpha = 0.40f), fontSize = 7.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)
            Text("touch + audio", color = VaibColors.TextSoft.copy(alpha = 0.22f), fontSize = 6.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp)
        }
    }
}

// ── Icon helpers ──────────────────────────────────────────────────────

@Composable
private fun SkipPrevIcon(modifier: Modifier = Modifier, color: Color = Color.White.copy(0.20f)) {
    androidx.compose.foundation.Canvas(modifier) {
        drawRect(color, Offset(size.width * 0.10f, size.height * 0.18f), Size(size.width * 0.10f, size.height * 0.64f))
        drawPath(Path().apply { moveTo(size.width * 0.88f, size.height * 0.18f); lineTo(size.width * 0.28f, size.height * 0.50f); lineTo(size.width * 0.88f, size.height * 0.82f); close() }, color)
    }
}

@Composable
private fun SkipNextIcon(modifier: Modifier = Modifier, color: Color = Color.White.copy(0.20f)) {
    androidx.compose.foundation.Canvas(modifier) {
        drawPath(Path().apply { moveTo(size.width * 0.12f, size.height * 0.18f); lineTo(size.width * 0.72f, size.height * 0.50f); lineTo(size.width * 0.12f, size.height * 0.82f); close() }, color)
        drawRect(color, Offset(size.width * 0.80f, size.height * 0.18f), Size(size.width * 0.10f, size.height * 0.64f))
    }
}

@Composable
private fun PlayIcon(modifier: Modifier = Modifier, color: Color = Color.White.copy(0.55f)) {
    androidx.compose.foundation.Canvas(modifier) {
        drawPath(Path().apply { moveTo(size.width * 0.28f, size.height * 0.18f); lineTo(size.width * 0.82f, size.height * 0.50f); lineTo(size.width * 0.28f, size.height * 0.82f); close() }, color)
    }
}

@Composable
private fun PauseIcon(modifier: Modifier = Modifier, color: Color = Color.White.copy(0.55f)) {
    androidx.compose.foundation.Canvas(modifier) {
        drawRect(color, Offset(size.width * 0.26f, size.height * 0.18f), Size(size.width * 0.14f, size.height * 0.64f))
        drawRect(color, Offset(size.width * 0.60f, size.height * 0.18f), Size(size.width * 0.14f, size.height * 0.64f))
    }
}
