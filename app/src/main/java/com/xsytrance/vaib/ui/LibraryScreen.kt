package com.xsytrance.vaib.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.xsytrance.vaib.MainViewModel
import com.xsytrance.vaib.Screen
import com.xsytrance.vaib.music.Track
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.PI

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onTrackClick: (Track) -> Unit,
) {
    val tracks        by viewModel.tracks.collectAsState()
    val isRefreshing  by viewModel.isRefreshing.collectAsState()
    val refreshError  by viewModel.refreshError.collectAsState()
    val currentTrack  by viewModel.currentTrack.collectAsState()
    val isPlaying     by viewModel.isPlaying.collectAsState()
    val playbackFrac  by viewModel.playbackFraction.collectAsState()
    val palette       by viewModel.trackPalette.collectAsState()
    val beatPulse     by viewModel.audioBeatPulse.collectAsState()

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        "xsytrance",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.8).sp,
                    )
                    Text(
                        "${tracks.size} TRACKS",
                        color = palette.vibrant.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp,
                    )
                }
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = palette.vibrant,
                        strokeWidth = 2.dp,
                    )
                }
            }

            // ── Error banner ─────────────────────────────────────────
            refreshError?.let { err ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF330A0A))
                        .clickable { viewModel.clearRefreshError(); viewModel.refreshLibrary() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(err, color = Color(0xFFFF6B6B), fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Text("Retry", color = palette.vibrant, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Track grid ───────────────────────────────────────────
            if (tracks.isEmpty() && !isRefreshing) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "No tracks found.\nAdd MP3s to your R2 bucket.",
                        color = Color.White.copy(0.35f),
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 14.dp, end = 14.dp,
                        top = 4.dp,
                        bottom = if (currentTrack != null) 84.dp else 20.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement   = Arrangement.spacedBy(10.dp),
                ) {
                    items(tracks, key = { it.key }) { track ->
                        TrackCard(
                            track = track,
                            isPlaying = isPlaying && currentTrack?.key == track.key,
                            palette = palette,
                            onClick = { onTrackClick(track) },
                        )
                    }
                }
            }
        }

        // ── Mini player ──────────────────────────────────────────────
        currentTrack?.let { track ->
            MiniPlayer(
                track    = track,
                isPlaying = isPlaying,
                fraction = playbackFrac,
                beatPulse = beatPulse,
                palette  = palette,
                modifier = Modifier.align(Alignment.BottomCenter),
                onTap    = { viewModel.navigateTo(Screen.NOW_PLAYING) },
                onPlayPause = viewModel::togglePlayPause,
            )
        }
    }
}

// ── Track card ────────────────────────────────────────────────────────

@Composable
private fun TrackCard(
    track: Track,
    isPlaying: Boolean,
    palette: com.xsytrance.vaib.core.design.TrackPalette,
    onClick: () -> Unit,
) {
    val (grad1, grad2) = remember(track.key) { trackGradient(track.key) }
    val transition = rememberInfiniteTransition(label = "card_${track.key}")
    val phase by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(
            tween(
                durationMillis = 4_000 + (track.key.hashCode() % 2000).coerceAtLeast(0),
                easing = LinearEasing,
            ),
            RepeatMode.Restart,
        ),
        label = "cardPhase",
    )

    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0D0D0D))
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(Brush.linearGradient(listOf(grad1, grad2))),
        ) {
            if (track.albumArtUrl != null) {
                AsyncImage(
                    model = track.albumArtUrl,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                // Animated wave lines on the card art
                Canvas(Modifier.fillMaxSize()) {
                    val twoPi = (2 * PI).toFloat()
                    for (i in 0..2) {
                        val yFrac = 0.35f + i * 0.22f
                        val amp   = size.height * (0.06f + i * 0.02f)
                        val freq  = 1.2f + i * 0.4f
                        val ph    = phase * twoPi + i * twoPi / 3f
                        val path  = androidx.compose.ui.graphics.Path()
                        var first = true
                        for (step in 0..60) {
                            val x = step / 60f * size.width
                            val y = size.height * yFrac + sin(x / size.width * twoPi * freq + ph) * amp
                            if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
                        }
                        drawPath(path, Color.White.copy(0.10f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(1.4f))
                    }
                }
                // First letter watermark
                Text(
                    track.title.take(1).uppercase(),
                    color = Color.White.copy(0.12f),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 10.dp, bottom = 4.dp),
                )
            }

            // Playing indicator — pulsing dot
            if (isPlaying) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(8.dp)
                        .background(palette.vibrant, CircleShape),
                )
            }
        }

        Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Text(
                track.title,
                color = if (isPlaying) palette.vibrant else Color.White.copy(0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
            )
            if (track.tags.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    track.tags.take(2).joinToString(" · "),
                    color = palette.vibrant.copy(0.55f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun trackGradient(key: String): Pair<Color, Color> {
    val h = key.hashCode()
    val hue1 = abs(h % 360).toFloat()
    val hue2 = abs((h / 7 + 120) % 360).toFloat()
    val hsv1 = floatArrayOf(hue1, 0.65f, 0.28f)
    val hsv2 = floatArrayOf(hue2, 0.55f, 0.45f)
    return Color(android.graphics.Color.HSVToColor(hsv1)) to
           Color(android.graphics.Color.HSVToColor(hsv2))
}

// ── Mini player ───────────────────────────────────────────────────────

@Composable
private fun MiniPlayer(
    track: Track,
    isPlaying: Boolean,
    fraction: Float,
    beatPulse: Float,
    palette: com.xsytrance.vaib.core.design.TrackPalette,
    onTap: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(Color(0xFF111111).copy(alpha = 0.97f))
            .clickable(onClick = onTap),
    ) {
        // Thin progress bar at very top
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = palette.vibrant,
            trackColor = Color.White.copy(0.08f),
            strokeCap = StrokeCap.Butt,
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Album art placeholder
            val (g1, g2) = remember(track.key) { trackGradient(track.key) }
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.linearGradient(listOf(g1, g2))),
                contentAlignment = Alignment.Center,
            ) {
                Text(track.title.take(1).uppercase(), color = Color.White.copy(0.5f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                val titleColor = lerp(Color.White, palette.vibrant, beatPulse * 0.6f)
                Text(
                    track.title,
                    color = titleColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    track.artist,
                    color = palette.vibrant.copy(0.55f),
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }

            Spacer(Modifier.width(12.dp))

            // Play/pause button
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(palette.vibrant)
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(16.dp)) {
                    if (isPlaying) {
                        val bw = size.width * 0.22f; val bh = size.height * 0.70f
                        val gap = size.width * 0.16f; val sx = (size.width - bw * 2f - gap) / 2f
                        val sy = (size.height - bh) / 2f
                        drawRect(Color.Black, Offset(sx, sy), Size(bw, bh))
                        drawRect(Color.Black, Offset(sx + bw + gap, sy), Size(bw, bh))
                    } else {
                        drawPath(androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width * 0.26f, size.height * 0.14f)
                            lineTo(size.width * 0.83f, size.height * 0.50f)
                            lineTo(size.width * 0.26f, size.height * 0.86f)
                            close()
                        }, Color.Black)
                    }
                }
            }
        }
    }
}
