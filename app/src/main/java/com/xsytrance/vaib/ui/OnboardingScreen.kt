package com.xsytrance.vaib.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xsytrance.vaib.core.design.VaibColors
import com.xsytrance.vaib.core.design.VaibAtmosphere
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * 3-screen onboarding flow:
 * 1. Brand — "vAIb out!" logo with ambient animation
 * 2. Features — swipeable cards showing Visualizer, Stations, EQ, Discover
 * 3. Permission — RECORD_AUDIO explanation + CTA
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onRequestPermission: () -> Unit = {},
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val atmosphere = VaibAtmosphere.Default

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Ambient background
        OnboardingAmbientBg(atmosphere = atmosphere)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (page) {
                    0 -> BrandPage(atmosphere = atmosphere)
                    1 -> FeaturesPage(atmosphere = atmosphere)
                    2 -> PermissionPage(
                        atmosphere = atmosphere,
                        onRequestPermission = onRequestPermission,
                        onComplete = onComplete,
                    )
                }
            }

            // Page indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(3) { index ->
                    val isActive = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isActive) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) VaibColors.CyanPulse
                                else VaibColors.TextSecondary.copy(alpha = 0.3f)
                            ),
                    )
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        },
                    ) {
                        Text("Back", color = VaibColors.TextSecondary, fontSize = 14.sp)
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < 2) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onComplete()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VaibColors.CyanPulse,
                        contentColor = Color.Black,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                ) {
                    Text(
                        if (pagerState.currentPage < 2) "Next" else "Get Started",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}

// ── Page 1: Brand ─────────────────────────────────────────────────

@Composable
private fun BrandPage(atmosphere: VaibAtmosphere) {
    val transition = rememberInfiniteTransition(label = "brand")
    val pulse by transition.animateFloat(
        0.8f, 1.2f,
        infiniteRepeatable(tween(2_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "brandPulse",
    )
    val glow by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(4_000, easing = LinearEasing), RepeatMode.Restart),
        label = "brandGlow",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Animated logo area
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            atmosphere.primaryColor.copy(alpha = 0.3f * pulse),
                            atmosphere.secondaryColor.copy(alpha = 0.1f),
                            Color.Transparent,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "🎵",
                fontSize = 48.sp,
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "vAIb out!",
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1.5).sp,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Your music. Your vibe.\nYour visual experience.",
            color = VaibColors.TextSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "LET'S CHILL",
            color = atmosphere.primaryColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 3.sp,
        )
    }
}

// ── Page 2: Features ──────────────────────────────────────────────

private data class FeatureInfo(
    val icon: String,
    val title: String,
    val description: String,
    val color: Color,
)

private val FEATURES = listOf(
    FeatureInfo("🌌", "Visualizer", "Stunning real-time visuals that react to your music", VaibColors.CyanPulse),
    FeatureInfo("📻", "Stations", "Organize your music into themed stations", VaibColors.VioletGlow),
    FeatureInfo("🎛️", "Equalizer", "Fine-tune your sound with a 5-band EQ", VaibColors.TealAccent),
    FeatureInfo("🔍", "Discover", "Explore free music from Internet Archive", VaibColors.AmberAccent),
)

@Composable
private fun FeaturesPage(atmosphere: VaibAtmosphere) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "What's inside",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            "Everything you need for the perfect vibe",
            color = VaibColors.TextSecondary,
            fontSize = 14.sp,
        )

        Spacer(Modifier.height(28.dp))

        FEATURES.forEach { feature ->
            FeatureCard(feature = feature)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FeatureCard(feature: FeatureInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VaibColors.Surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(feature.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(feature.icon, fontSize = 24.sp)
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                feature.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                feature.description,
                color = VaibColors.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

// ── Page 3: Permission ────────────────────────────────────────────

@Composable
private fun PermissionPage(
    atmosphere: VaibAtmosphere,
    onRequestPermission: () -> Unit,
    onComplete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(VaibColors.Surface),
            contentAlignment = Alignment.Center,
        ) {
            Text("🎤", fontSize = 36.sp)
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "One last thing",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "vAIb needs access to your microphone to visualize audio.\n\nYour audio is processed locally and never leaves your device.",
            color = VaibColors.TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                onRequestPermission()
                onComplete()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = VaibColors.CyanPulse,
                contentColor = Color.Black,
            ),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Allow & Continue", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onComplete) {
            Text("Skip for now", color = VaibColors.TextTertiary, fontSize = 13.sp)
        }
    }
}

// ── Ambient background ────────────────────────────────────────────

@Composable
private fun OnboardingAmbientBg(atmosphere: VaibAtmosphere) {
    val transition = rememberInfiniteTransition(label = "onboardBg")
    val phase by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(20_000, easing = LinearEasing), RepeatMode.Restart),
        label = "obPhase",
    )
    val twoPi = (2.0 * PI).toFloat()

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        listOf(
            Triple(0.2f, 0.03f, 1.0f),
            Triple(0.5f, 0.02f, 1.5f),
            Triple(0.8f, 0.025f, 0.8f),
        ).forEachIndexed { i, (yFrac, ampFrac, freq) ->
            val phaseOff = phase * twoPi + i * twoPi / 3f
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
                atmosphere.primaryColor.copy(alpha = 0.04f + i * 0.01f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.5f + i * 0.5f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                ),
            )
        }
    }
}
