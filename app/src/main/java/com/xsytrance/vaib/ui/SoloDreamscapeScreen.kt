package com.xsytrance.vaib.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.xsytrance.vaib.MainViewModel
import com.xsytrance.vaib.core.design.VaibColors
import com.xsytrance.vaib.visualizer.VisualizerSurface

// Set to false to remove debug overlay before release.
private const val DEBUG_VISUALIZER = true
private const val TAG_DS = "VaibDreamscape"

@Composable
fun SoloDreamscapeScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val view    = LocalView.current
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val audioEnergy by viewModel.audioEnergy.collectAsState()
    val audioBeat   by viewModel.audioBeatPulse.collectAsState()
    var reactiveAvailable by remember { mutableStateOf(false) }

    // ── Permission + analyzer start (with one-shot retry) ─────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                reactiveAvailable = viewModel.startAnalyzer()
                if (!reactiveAvailable) {
                    // audioSessionId may be 0 if the audio renderer isn't ready yet;
                    // one retry after 500 ms covers the typical stream warm-up window.
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
        Log.d(TAG_DS, "LaunchedEffect: RECORD_AUDIO already granted=$already")
        if (already) {
            reactiveAvailable = viewModel.startAnalyzer()
            Log.d(TAG_DS, "startAnalyzer (immediate) reactive=$reactiveAvailable")
            if (!reactiveAvailable) {
                delay(500L)
                reactiveAvailable = viewModel.startAnalyzer()
                Log.d(TAG_DS, "startAnalyzer (retry 500ms) reactive=$reactiveAvailable")
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // ── Cleanup on exit ───────────────────────────────────────────────
    DisposableEffect(Unit) {
        onDispose { viewModel.stopAnalyzer() }
    }

    // ── System bars ───────────────────────────────────────────────────
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val controller = WindowInsetsControllerCompat(window, view)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    BackHandler(onBack = onBack)

    // ── UI ────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VisualizerSurface(
            modifier = Modifier.fillMaxSize(),
            energy = audioEnergy,
            beat   = audioBeat,
        )

        // Exit button (top-right, barely visible)
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp)
                .size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Exit Solo Dreamscape",
                tint = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.size(22.dp),
            )
        }

        // Subtle fallback label if reactive audio is unavailable
        if (!reactiveAvailable) {
            Text(
                text       = "Reactive audio unavailable",
                color      = VaibColors.TextSoft.copy(alpha = 0.4f),
                fontSize   = 11.sp,
                fontWeight = FontWeight.Normal,
                modifier   = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp),
            )
        }

        // DEBUG: energy + beat readout — set DEBUG_VISUALIZER=false before release
        if (DEBUG_VISUALIZER && reactiveAvailable) {
            VisualizerDebugOverlay(
                energy = audioEnergy,
                beat   = audioBeat,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 20.dp),
            )
        }
    }
}

@Composable
private fun VisualizerDebugOverlay(
    energy: Float,
    beat: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier          = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Energy bar
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.08f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(energy.coerceIn(0f, 1f))
                    .height(3.dp)
                    .background(VaibColors.CyanPulse.copy(alpha = 0.70f)),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        // Beat flash dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Color.White.copy(alpha = (beat * 0.9f).coerceIn(0f, 0.9f))
                ),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text     = "e:${"%.2f".format(energy)}",
            color    = VaibColors.TextSoft.copy(alpha = 0.45f),
            fontSize = 9.sp,
        )
    }
}
