package com.xsytrance.vaib.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xsytrance.vaib.core.design.VaibAtmosphere
import com.xsytrance.vaib.core.design.VaibColors
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/**
 * Animated splash screen — logo with ambient wave, auto-dismisses after delay.
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
) {
    val atmosphere = VaibAtmosphere.Default

    // Animation states
    val transition = rememberInfiniteTransition(label = "splash")
    val pulse by transition.animateFloat(
        0.8f, 1.2f,
        infiniteRepeatable(tween(2_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "splashPulse",
    )
    val glow by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(4_000, easing = LinearEasing), RepeatMode.Restart),
        label = "splashGlow",
    )
    val wavePhase by transition.animateFloat(
        0f, (2.0 * PI).toFloat(),
        infiniteRepeatable(tween(3_000, easing = LinearEasing), RepeatMode.Restart),
        label = "splashWave",
    )

    // Fade in/out
    var started by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        if (started) 1f else 0f,
        animationSpec = tween(800),
        label = "splashAlpha",
    )

    LaunchedEffect(Unit) {
        started = true
        delay(2_500)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .alpha(alpha),
        contentAlignment = Alignment.Center,
    ) {
        // Ambient wave background
        SplashWaveBg(phase = wavePhase, atmosphere = atmosphere)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Logo circle
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                atmosphere.primaryColor.copy(alpha = 0.4f),
                                atmosphere.secondaryColor.copy(alpha = 0.15f),
                                Color.Transparent,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("🎵", fontSize = 40.sp)
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "vAIb out!",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1.5).sp,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "LET'S CHILL",
                color = atmosphere.primaryColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp,
            )
        }
    }
}

@Composable
private fun SplashWaveBg(phase: Float, atmosphere: VaibAtmosphere) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val twoPi = (2.0 * PI).toFloat()
        listOf(
            Triple(0.3f, 0.04f, 1.0f),
            Triple(0.5f, 0.03f, 1.5f),
            Triple(0.7f, 0.035f, 0.8f),
        ).forEachIndexed { i, (yFrac, ampFrac, freq) ->
            val phaseOff = phase + i * twoPi / 3f
            val baseY = size.height * yFrac
            val amp = size.height * ampFrac
            val path = androidx.compose.ui.graphics.Path()
            for (step in 0..80) {
                val x = step.toFloat() / 80f * size.width
                val y = baseY + sin(x / size.width * twoPi * freq + phaseOff) * amp
                if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path,
                atmosphere.primaryColor.copy(alpha = 0.06f + i * 0.015f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2f + i * 0.5f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                ),
            )
        }
    }
}
