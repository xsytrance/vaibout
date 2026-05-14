package com.xsytrance.vaib.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xsytrance.vaib.MainViewModel
import com.xsytrance.vaib.core.design.OrbitAtmosphereLayer
import com.xsytrance.vaib.core.design.OrbitWorld
import com.xsytrance.vaib.core.design.TrackPaint
import com.xsytrance.vaib.core.design.VaibColors
import com.xsytrance.vaib.data.entities.VaibEntity
import com.xsytrance.vaib.discover.ArchiveItem
import com.xsytrance.vaib.discover.DiscoverUiState
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

// ── Orbit Flow ─────────────────────────────────────────────────────────

@Composable
fun DiscoverScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onPickTrack: () -> Unit,
) {
    val state         by viewModel.discoverState.collectAsState()
    val loadingItemId by viewModel.loadingItemId.collectAsState()
    val streamError   by viewModel.streamError.collectAsState()
    val savedVaibs    by viewModel.savedVaibs.collectAsState()
    val sessionQueue  by viewModel.sessionQueue.collectAsState()
    val queueReady    by viewModel.queueReady.collectAsState()
    val beatPulse     by viewModel.visualSignal.beatPulse.collectAsState()
    val energy        by viewModel.visualSignal.energy.collectAsState()
    val breathValue   = com.xsytrance.vaib.core.design.ambientBreathAnimation(durationMs = 4_000)

    var searchQuery by remember { mutableStateOf("") }
    var activeWorld by remember { mutableStateOf<OrbitWorld>(OrbitWorld.ALL) }
    var isSearching by remember { mutableStateOf(false) }

    // Derive items to show: search > world > queue
    val items: List<ArchiveItem> = when {
        isSearching && state is DiscoverUiState.Success -> (state as DiscoverUiState.Success).items
        state is DiscoverUiState.Success -> (state as DiscoverUiState.Success).items
        else -> if (queueReady && activeWorld == OrbitWorld.ALL) sessionQueue else emptyList()
    }

    // World colors for painting
    val world = activeWorld

    // Debounced search
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            delay(600L)
            isSearching = true
            viewModel.fetchDiscoverItems(searchQuery)
        } else {
            isSearching = false
        }
    }

    // Initial world load
    LaunchedEffect(activeWorld) {
        if (!isSearching) {
            viewModel.fetchWorldItems(activeWorld)
        }
    }

    // Queue feed when no search and world=ALL
    LaunchedEffect(queueReady, activeWorld) {
        if (queueReady && activeWorld == OrbitWorld.ALL && !isSearching && state !is DiscoverUiState.Success) {
            // Use queue items directly, no fetch needed
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding(),
    ) {
        // World-painted atmosphere — energy drives particle life
        OrbitAtmosphereLayer(
            moodColor          = world.primaryColor,
            secondaryMoodColor = world.secondaryColor,
            energy             = energy,
            modifier           = Modifier.fillMaxSize(),
        )

        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            // ── Top bar ───────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp, end = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onBack) {
                        Text(
                            "\u2190 Back",
                            color      = VaibColors.TextSoft,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    if (queueReady) {
                        Spacer(Modifier.weight(1f))
                        QueueReadyChip()
                    }
                }
            }

            // ── Hero header ───────────────────────────────────────────
            item {
                WorldHeroHeader(world = world)
                Spacer(Modifier.height(12.dp))
            }

            // ── Search capsule ────────────────────────────────────────
            item {
                OrbitSearchCapsule(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    onSearch      = {
                        isSearching = true
                        viewModel.fetchDiscoverItems(searchQuery)
                    },
                )
                Spacer(Modifier.height(12.dp))
            }

            // ── Horizontal world carousel ─────────────────────────────
            item {
                WorldCarousel(
                    activeWorld = activeWorld,
                    onWorldSelected = { activeWorld = it },
                )
                Spacer(Modifier.height(12.dp))
            }

            // ── Glow divider — pulses with energy ─────────────────────
            item {
                WorldGlowDivider(world = world, energy = energy)
                Spacer(Modifier.height(12.dp))
            }

            // ── Song feed ─────────────────────────────────────────────
            when {
                activeWorld == OrbitWorld.LOCAL_FILES -> {
                    // Local Files portal
                    item {
                        LocalFilesPortal(onClick = onPickTrack)
                        Spacer(Modifier.height(16.dp))
                    }
                }
                state is DiscoverUiState.Loading && items.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color     = world.primaryColor,
                                modifier  = Modifier.size(36.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }
                state is DiscoverUiState.Error && items.isEmpty() -> {
                    item {
                        val errorText = (state as DiscoverUiState.Error).message
                        EmptySignalCard(
                            message = errorText,
                            world   = world,
                            onRetry = { viewModel.fetchWorldItems(activeWorld) },
                        )
                    }
                }
                items.isEmpty() -> {
                    item {
                        EmptySignalCard(
                            message = "No tracks in this world yet.",
                            world   = world,
                            onRetry = { viewModel.fetchWorldItems(activeWorld) },
                        )
                    }
                }
                else -> {
                    // Queue-ready header
                    if (queueReady && activeWorld == OrbitWorld.ALL && !isSearching) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(world.primaryColor.copy(alpha = 0.60f)),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "SHUFFLED ORBIT SESSION",
                                    color         = world.primaryColor.copy(alpha = 0.65f),
                                    fontSize      = 9.sp,
                                    fontWeight    = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }

                    // Vertical song feed
                    items(items, key = { it.id }) { item ->
                        OrbitSongCard(
                            item       = item,
                            isLoading  = loadingItemId == item.id,
                            anyLoading = loadingItemId != null,
                            world      = world,
                            energy     = energy,
                            onClick    = { viewModel.loadOnlineTrack(item) },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }

            // ── Saved vAIbs in-world ──────────────────────────────────
            if (activeWorld == OrbitWorld.ALL && savedVaibs.isNotEmpty() && !isSearching) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    ) {
                        ConnectorDot(color = world.primaryColor)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "YOUR SAVED VAIBS",
                            color         = Color.White.copy(alpha = 0.70f),
                            fontSize      = 10.sp,
                            fontWeight    = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        )
                    }
                }
                items(savedVaibs, key = { it.id }) { vaib ->
                    SavedVaibOrbitCard(
                        vaib    = vaib,
                        onClick = { viewModel.loadVaib(vaib) },
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            // ── Bottom legal ──────────────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Open audio from public sources. Availability may change.",
                    color      = VaibColors.TextSoft.copy(alpha = 0.22f),
                    fontSize   = 9.sp,
                    lineHeight = 14.sp,
                    modifier   = Modifier.padding(horizontal = 20.dp),
                )
            }
        }

        // ── Stream error banner ─────────────────────────────────────
        if (streamError != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF160808))
                    .border(
                        BorderStroke(0.5.dp, Color(0xFFFF7070).copy(alpha = 0.30f)),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    streamError!!,
                    color     = Color(0xFFFF7070),
                    fontSize  = 12.sp,
                    modifier  = Modifier.weight(1f),
                )
                TextButton(onClick = { viewModel.clearStreamError() }) {
                    Text("\u2715", color = VaibColors.TextSoft, fontSize = 13.sp)
                }
            }
        }
    }
}

// ── World hero header ─────────────────────────────────────────────────

@Composable
private fun WorldHeroHeader(world: OrbitWorld) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            world.label,
            color         = Color.White,
            fontSize      = 36.sp,
            fontWeight    = FontWeight.ExtraBold,
            letterSpacing = (-1.0).sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            world.subtitle,
            color         = world.primaryColor.copy(alpha = 0.70f),
            fontSize      = 12.sp,
            fontWeight    = FontWeight.Medium,
            letterSpacing = 0.3.sp,
        )
    }
}

// ── World carousel ────────────────────────────────────────────────────

@Composable
private fun WorldCarousel(
    activeWorld: OrbitWorld,
    onWorldSelected: (OrbitWorld) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OrbitWorld.DISPLAY_ORDER.forEach { world ->
            val isActive = world == activeWorld
            WorldPlanetChip(
                world    = world,
                isActive = isActive,
                onClick  = { onWorldSelected(world) },
            )
        }
    }
}

// ── World planet chip ─────────────────────────────────────────────────

@Composable
private fun WorldPlanetChip(
    world: OrbitWorld,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val bgColor     = if (isActive) world.primaryColor.copy(alpha = 0.15f) else Color(0xFF0A0A0A)
    val borderColor = if (isActive) world.primaryColor.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f)
    val textColor   = if (isActive) world.primaryColor else Color.White.copy(alpha = 0.55f)
    val subColor    = if (isActive) world.secondaryColor.copy(alpha = 0.50f) else VaibColors.TextSoft.copy(alpha = 0.30f)

    Column(
        modifier = Modifier
            .width(78.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Tiny planet orb
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(50))
                .background(if (isActive) world.primaryColor else world.primaryColor.copy(alpha = 0.25f)),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            world.label,
            color      = textColor,
            fontSize   = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
        )
        Text(
            world.subtitle,
            color      = subColor,
            fontSize   = 7.sp,
            fontWeight = FontWeight.Medium,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            letterSpacing = 0.2.sp,
        )
    }
}

// ── Search capsule ────────────────────────────────────────────────────

@Composable
private fun OrbitSearchCapsule(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        placeholder   = {
            Text(
                "search open worlds\u2026",
                color    = VaibColors.TextSoft.copy(alpha = 0.40f),
                fontSize = 14.sp,
            )
        },
        singleLine = true,
        modifier   = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0A0A0A)),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor      = Color.White,
            unfocusedTextColor    = Color.White,
            focusedBorderColor    = VaibColors.CyanPulse.copy(alpha = 0.55f),
            unfocusedBorderColor  = Color.White.copy(alpha = 0.08f),
            cursorColor           = VaibColors.CyanPulse,
            focusedLabelColor     = VaibColors.CyanPulse,
            unfocusedLabelColor   = VaibColors.TextSoft,
        ),
        shape = RoundedCornerShape(14.dp),
        keyboardOptions  = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions  = KeyboardActions(onSearch = { onSearch() }),
    )
}

// ── World glow divider ────────────────────────────────────────────────

@Composable
private fun WorldGlowDivider(world: OrbitWorld, energy: Float) {
    val baseAlpha = 0.30f
    val glowAlpha = (baseAlpha + energy * 0.25f).coerceIn(0.15f, 0.60f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(1.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(
                        world.primaryColor.copy(alpha = 0.0f),
                        world.primaryColor.copy(alpha = glowAlpha),
                        world.secondaryColor.copy(alpha = glowAlpha * 0.7f),
                        world.primaryColor.copy(alpha = 0.0f),
                    ),
                ),
                start  = Offset(0f, size.height / 2),
                end    = Offset(size.width, size.height / 2),
                strokeWidth = 1f,
            )
        }
    }
}

// ── Orbit song card (vertical feed) ───────────────────────────────────

@Composable
private fun OrbitSongCard(
    item: ArchiveItem,
    isLoading: Boolean,
    anyLoading: Boolean,
    world: OrbitWorld,
    energy: Float,
    onClick: () -> Unit,
) {
    val paint    = remember(item.id) { TrackPaint.fromArchiveItem(item) }
    val enabled  = !anyLoading || isLoading
    val dimAlpha = if (anyLoading && !isLoading) 0.30f else 1.0f

    // Blend world color with track paint for world-consistent cards
    val cardPrimary   = lerp(world.primaryColor, paint.primaryColor, 0.35f)
    val cardSecondary = lerp(world.secondaryColor, paint.secondaryColor, 0.35f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0A0A0A))
            .border(
                BorderStroke(0.6.dp, cardPrimary.copy(alpha = 0.18f * dimAlpha)),
                RoundedCornerShape(14.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment   = Alignment.CenterVertically,
    ) {
        // Mini waveform thumbnail — pulses with energy
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(cardPrimary.copy(alpha = 0.10f + energy * 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            SongCardMiniWaveform(
                color    = cardPrimary.copy(alpha = (0.30f + energy * 0.25f).coerceIn(0.15f, 0.60f) * dimAlpha),
                energy   = energy,
            )
        }

        // Track info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.title,
                color      = Color.White.copy(alpha = 0.88f * dimAlpha),
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            if (item.creator.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    item.creator,
                    color    = VaibColors.TextSoft.copy(alpha = 0.45f * dimAlpha),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    paint.vibeLabel.uppercase(),
                    color         = cardPrimary.copy(alpha = 0.50f * dimAlpha),
                    fontSize      = 7.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.7.sp,
                )
                Text(
                    "\u00b7",
                    color  = VaibColors.TextSoft.copy(alpha = 0.20f),
                    fontSize = 7.sp,
                )
                Text(
                    paint.glyphs.first(),
                    color  = cardPrimary.copy(alpha = 0.25f * dimAlpha),
                    fontSize = 8.sp,
                )
            }
        }

        // Loading or source indicator
        if (isLoading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(16.dp),
                color       = cardPrimary,
                strokeWidth = 1.5.dp,
            )
        } else {
            Text(
                "\u2192",
                color      = cardSecondary.copy(alpha = 0.35f * dimAlpha),
                fontSize   = 14.sp,
            )
        }
    }
}

// ── Saved vAIb orbit card ─────────────────────────────────────────────

@Composable
private fun SavedVaibOrbitCard(
    vaib: VaibEntity,
    onClick: () -> Unit,
) {
    val paint = remember(vaib.id) { TrackPaint.fromVaibEntity(vaib) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0A0A0A))
            .border(
                BorderStroke(0.8.dp, paint.borderColor),
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment   = Alignment.CenterVertically,
    ) {
        // Mini wave thumbnail
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(paint.primaryColor.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            SongCardMiniWaveform(color = paint.primaryColor.copy(alpha = 0.30f))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                vaib.vaibName,
                color      = Color.White.copy(alpha = 0.88f),
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            if (vaib.mood.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    paint.vibeLabel.uppercase(),
                    color         = paint.primaryColor.copy(alpha = 0.55f),
                    fontSize      = 7.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.7.sp,
                )
            }
        }

        Text(
            paint.glyphs.first(),
            color  = paint.primaryColor.copy(alpha = 0.25f),
            fontSize = 10.sp,
        )
    }
}

// ── Local Files portal ────────────────────────────────────────────────

@Composable
private fun LocalFilesPortal(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0A0A0A))
            .border(
                BorderStroke(1.dp, Color(0xFF80DEEA).copy(alpha = 0.25f)),
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                "Local Files",
                color      = Color.White.copy(alpha = 0.90f),
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "Pick a track from your device",
                color      = VaibColors.TextSoft.copy(alpha = 0.40f),
                fontSize   = 11.sp,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment   = Alignment.CenterVertically,
        ) {
            Text(
                "\u2192  Open picker",
                color      = Color(0xFF80DEEA).copy(alpha = 0.60f),
                fontSize   = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ── Empty signal card ─────────────────────────────────────────────────

@Composable
private fun EmptySignalCard(
    message: String,
    world: OrbitWorld,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 40.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0A0A0A))
            .border(
                BorderStroke(0.6.dp, world.primaryColor.copy(alpha = 0.15f)),
                RoundedCornerShape(16.dp),
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "\u25CB",
            color  = world.primaryColor.copy(alpha = 0.30f),
            fontSize = 32.sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            message,
            color      = VaibColors.TextSoft.copy(alpha = 0.50f),
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(16.dp))
        VaibOutlinedButton(label = "Retry", onClick = onRetry)
    }
}

// ── Queue ready chip ──────────────────────────────────────────────────

@Composable
private fun QueueReadyChip() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0A0A0A))
            .border(
                BorderStroke(0.5.dp, VaibColors.CyanPulse.copy(alpha = 0.25f)),
                RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(RoundedCornerShape(50))
                .background(VaibColors.CyanPulse.copy(alpha = 0.70f)),
        )
        Text(
            "Shuffled Orbit ready",
            color         = VaibColors.CyanPulse.copy(alpha = 0.65f),
            fontSize      = 9.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
        )
    }
}

// ── Song card mini waveform ───────────────────────────────────────────

@Composable
private fun SongCardMiniWaveform(color: Color, energy: Float = 0f) {
    val transition = rememberInfiniteTransition(label = "songCardWave")
    val phase by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2_000, easing = LinearEasing), RepeatMode.Restart),
        label = "songCardPhase",
    )
    val barCount = 6
    val twoPi = (2.0 * PI).toFloat()

    Canvas(modifier = Modifier.size(28.dp, 16.dp)) {
        val barW = size.width / (barCount * 2 - 1)
        val maxH = size.height
        for (i in 0 until barCount) {
            val speed = 0.5f + (i % 3) * 0.3f
            val offset = i.toFloat() / barCount
            val raw = abs(sin(((phase + offset) * speed * twoPi).toDouble())).toFloat()
            // Energy drives bar height variation
            val h = (maxH * (0.2f + raw * 0.8f) * (0.6f + energy * 0.8f)).coerceAtLeast(1.5f)
            drawRect(
                color   = color,
                topLeft = Offset(i * barW * 2f, maxH - h),
                size    = Size(barW.coerceAtLeast(1f), h),
            )
        }
    }
}

// ── Connector dot ─────────────────────────────────────────────────────

@Composable
private fun ConnectorDot(color: Color) {
    Box(
        modifier = Modifier
            .size(5.dp)
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.60f)),
    )
}

// ── Color lerp (internal helper) ──────────────────────────────────────

private fun lerp(a: Color, b: Color, fraction: Float): Color {
    return Color(
        red   = a.red   + (b.red   - a.red)   * fraction,
        green = a.green + (b.green - a.green) * fraction,
        blue  = a.blue  + (b.blue  - a.blue)  * fraction,
        alpha = a.alpha + (b.alpha - a.alpha) * fraction,
    )
}
