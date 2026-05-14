package com.xsytrance.vaib.ui

import androidx.activity.compose.BackHandler
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
import com.xsytrance.vaib.core.design.VaibColors
import com.xsytrance.vaib.data.entities.VaibEntity
import com.xsytrance.vaib.discover.ArchiveItem
import com.xsytrance.vaib.discover.DiscoverUiState
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

// ── Mood chip definitions ─────────────────────────────────────────────

private data class MoodChip(val label: String, val query: String? = null)

private val MOOD_CHIPS = listOf(
    MoodChip("All"),
    MoodChip("Chill", "chill"),
    MoodChip("Cosmic", "cosmic"),
    MoodChip("Deep", "deep"),
    MoodChip("Focus", "focus"),
    MoodChip("Energetic", "energetic"),
    MoodChip("Local"),     // special — opens SAF picker
    MoodChip("Open Archive"),
)

private data class MoodWorld(val label: String, val subtitle: String, val accent: Color)

private val MOOD_WORLDS = listOf(
    MoodWorld("Chill",      "calm ambient waves",          Color(0xFF4DD0E1)),
    MoodWorld("Cosmic",     "space experimental signals",  Color(0xFFFFB74D)),
    MoodWorld("Deep",       "dub atmospheric low",         Color(0xFF7C4DFF)),
    MoodWorld("Focus",      "instrumental minimal",        Color(0xFF80CBC4)),
    MoodWorld("Energetic",  "upbeat electronic",           Color(0xFF00E5FF)),
)

// ── Orbit Deck ────────────────────────────────────────────────────────

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

    var searchQuery by remember { mutableStateOf("") }
    var activeMood  by remember { mutableStateOf("All") }

    // Debounced search
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) delay(600L)
        viewModel.fetchDiscoverItems(searchQuery)
    }

    // Initial load
    LaunchedEffect(Unit) {
        viewModel.fetchDiscoverItems("")
    }

    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding(),
    ) {
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
                }
            }

            // ── Hero header ───────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        "Orbit",
                        color         = Color.White,
                        fontSize      = 42.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = (-1.2).sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Open music worlds  \u00b7  Creative Commons",
                        color         = VaibColors.CyanPulse,
                        fontSize      = 12.sp,
                        fontWeight    = FontWeight.Medium,
                        letterSpacing = 0.3.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "choose a vibe, then branch out",
                        color      = VaibColors.TextSoft.copy(alpha = 0.38f),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Normal,
                    )
                    Spacer(Modifier.height(16.dp))

                    // ── Search capsule ──────────────────────────────
                    OrbitSearchCapsule(
                        value    = searchQuery,
                        onValueChange = { searchQuery = it },
                        onSearch = { viewModel.fetchDiscoverItems(searchQuery) },
                    )
                    Spacer(Modifier.height(14.dp))
                }
            }

            // ── Mood / world chips ────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MOOD_CHIPS.forEach { chip ->
                        val selected = activeMood == chip.label
                        OrbitMoodChip(
                            label    = chip.label,
                            selected = selected,
                            onClick  = {
                                activeMood = chip.label
                                when (chip.label) {
                                    "Local"       -> onPickTrack()
                                    "All",
                                    "Open Archive" -> viewModel.fetchDiscoverItems("")
                                    else          -> viewModel.fetchMoodItems(chip.label)
                                }
                            },
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // ── Glow divider ──────────────────────────────────────────
            item {
                GlowDivider(modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(20.dp))
            }

            // ── Content: grouped rails ────────────────────────────────
            when (val s = state) {
                is DiscoverUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color     = VaibColors.CyanPulse,
                                modifier  = Modifier.size(36.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }

                is DiscoverUiState.Error -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                s.message,
                                color      = VaibColors.TextSoft,
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.Normal,
                            )
                            Spacer(Modifier.height(16.dp))
                            VaibOutlinedButton(
                                label   = "Retry",
                                onClick = { viewModel.fetchDiscoverItems(searchQuery) },
                            )
                        }
                    }
                }

                is DiscoverUiState.Success -> {
                    // ── Your Orbit ────────────────────────────────
                    item {
                        SectionHeader(
                            title    = "Your Orbit",
                            subtitle = "personal entry points",
                        )
                    }
                    item {
                        YourOrbitRail(
                            savedVaibs  = savedVaibs,
                            onLocalPick = onPickTrack,
                            onVaibClick = { viewModel.loadVaib(it) },
                            modifier    = Modifier.padding(start = 24.dp),
                        )
                        Spacer(Modifier.height(24.dp))
                    }

                    // ── Open Worlds ───────────────────────────────
                    item {
                        SectionHeader(
                            title    = "Open Worlds",
                            subtitle = "Internet Archive discovery",
                        )
                    }
                    item {
                        OpenWorldsRail(
                            items       = s.items,
                            loadingId   = loadingItemId,
                            onItemClick = { viewModel.loadOnlineTrack(it) },
                            modifier    = Modifier.padding(start = 24.dp),
                        )
                        Spacer(Modifier.height(24.dp))
                    }

                    // ── Mood Constellations ───────────────────────
                    item {
                        SectionHeader(
                            title    = "Mood Constellations",
                            subtitle = "vibe-first browsing",
                        )
                    }
                    item {
                        MoodConstellationsRail(
                            onMoodClick = { mood ->
                                activeMood = mood
                                viewModel.fetchMoodItems(mood)
                            },
                            modifier = Modifier.padding(start = 24.dp),
                        )
                        Spacer(Modifier.height(24.dp))
                    }

                    // ── Branch Out ────────────────────────────────
                    if (s.items.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title    = "Branch Out",
                                subtitle = "connected exploration",
                            )
                        }
                        item {
                            BranchOutRail(
                                items       = s.items.shuffled(),
                                loadingId   = loadingItemId,
                                onItemClick = { viewModel.loadOnlineTrack(it) },
                                modifier    = Modifier.padding(start = 24.dp),
                            )
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }

            // ── Bottom legal ──────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Open audio from public sources such as Internet Archive.\nAvailability may change.",
                    color      = VaibColors.TextSoft.copy(alpha = 0.30f),
                    fontSize   = 9.sp,
                    lineHeight = 14.sp,
                    modifier   = Modifier.padding(horizontal = 24.dp),
                )
            }
        }

        // ── Stream error banner (floating) ──────────────────────────
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
                color      = VaibColors.TextSoft.copy(alpha = 0.40f),
                fontSize   = 14.sp,
            )
        },
        singleLine = true,
        modifier   = Modifier
            .fillMaxWidth()
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

// ── Mood chip ─────────────────────────────────────────────────────────

@Composable
private fun OrbitMoodChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (selected) VaibColors.CyanPulse else Color(0xFF0A0A0A)
    val textColor = if (selected) Color.Black else Color.White.copy(alpha = 0.70f)
    val borderColor = if (selected) Color.Transparent else Color.White.copy(alpha = 0.10f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color      = textColor,
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ── Section header ────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ConnectorDot()
            Text(
                title.uppercase(),
                color         = Color.White.copy(alpha = 0.80f),
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 1.4.sp,
            )
        }
        Text(
            subtitle,
            color         = VaibColors.TextSoft.copy(alpha = 0.35f),
            fontSize      = 10.sp,
            fontWeight    = FontWeight.Medium,
            letterSpacing = 0.3.sp,
            modifier      = Modifier.padding(start = 14.dp, top = 2.dp),
        )
    }
}

// ── Glow divider ──────────────────────────────────────────────────────

@Composable
private fun GlowDivider(modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(1.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        VaibColors.CyanPulse.copy(alpha = 0.0f),
                        VaibColors.CyanPulse.copy(alpha = 0.30f),
                        VaibColors.VioletGlow.copy(alpha = 0.20f),
                        VaibColors.CyanPulse.copy(alpha = 0.0f),
                    ),
                ),
                start  = Offset(0f, size.height / 2),
                end    = Offset(size.width, size.height / 2),
                strokeWidth = 1f,
            )
        }
    }
}

// ── Connector dot ─────────────────────────────────────────────────────

@Composable
private fun ConnectorDot() {
    Box(
        modifier = Modifier
            .size(5.dp)
            .clip(RoundedCornerShape(50))
            .background(VaibColors.CyanPulse.copy(alpha = 0.60f)),
    )
}

// ── Your Orbit rail ───────────────────────────────────────────────────

@Composable
private fun YourOrbitRail(
    savedVaibs: List<VaibEntity>,
    onLocalPick: () -> Unit,
    onVaibClick: (VaibEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier               = modifier.fillMaxWidth(),
        horizontalArrangement  = Arrangement.spacedBy(12.dp),
        contentPadding         = PaddingValues(end = 24.dp),
    ) {
        // Local Files card
        item {
            LocalFilesCard(onClick = onLocalPick)
        }

        // Saved vAIb cards
        items(savedVaibs, key = { it.id }) { vaib ->
            SavedVaibOrbitCard(
                vaib    = vaib,
                onClick = { onVaibClick(vaib) },
            )
        }
    }
}

// ── Local Files card ──────────────────────────────────────────────────

@Composable
private fun LocalFilesCard(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(148.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0A0A0A))
            .border(
                BorderStroke(0.8.dp, Color.White.copy(alpha = 0.08f)),
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                "Local Files",
                color      = Color.White.copy(alpha = 0.88f),
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "pick a track from your device",
                color      = VaibColors.TextSoft.copy(alpha = 0.40f),
                fontSize   = 9.sp,
                lineHeight = 13.sp,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment   = Alignment.CenterVertically,
        ) {
            Text(
                "\u2192  Open picker",
                color         = VaibColors.CyanPulse.copy(alpha = 0.65f),
                fontSize      = 9.sp,
                fontWeight    = FontWeight.Medium,
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
    val accent = when (vaib.mood.lowercase()) {
        "chill"     -> Color(0xFF4DD0E1)
        "cosmic"    -> Color(0xFFFFB74D)
        "deep"      -> Color(0xFF7C4DFF)
        "focus"     -> Color(0xFF80CBC4)
        "energetic" -> Color(0xFF00E5FF)
        else        -> VaibColors.CyanPulse
    }

    Column(
        modifier = Modifier
            .width(148.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0A0A0A))
            .border(
                BorderStroke(0.8.dp, accent.copy(alpha = 0.25f)),
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                vaib.vaibName,
                color      = Color.White.copy(alpha = 0.88f),
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
                lineHeight = 16.sp,
            )
            if (vaib.mood.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    vaib.mood,
                    color      = accent.copy(alpha = 0.70f),
                    fontSize   = 9.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        MiniWaveformBar(color = accent.copy(alpha = 0.35f))
    }
}

// ── Open Worlds rail ──────────────────────────────────────────────────

@Composable
private fun OpenWorldsRail(
    items: List<ArchiveItem>,
    loadingId: String?,
    onItemClick: (ArchiveItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding        = PaddingValues(end = 24.dp),
    ) {
        items(items.take(15), key = { it.id }) { item ->
            OrbitTrackCard(
                item       = item,
                isLoading  = loadingId == item.id,
                anyLoading = loadingId != null,
                branchLabel = "Open Archive",
                onClick    = { onItemClick(item) },
            )
        }
    }
}

// ── Mood Constellations rail ──────────────────────────────────────────

@Composable
private fun MoodConstellationsRail(
    onMoodClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding        = PaddingValues(end = 24.dp),
    ) {
        items(MOOD_WORLDS) { world ->
            MoodWorldCard(
                world   = world,
                onClick = { onMoodClick(world.label) },
            )
        }
    }
}

// ── Mood world card ───────────────────────────────────────────────────

@Composable
private fun MoodWorldCard(
    world: MoodWorld,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(148.dp)
            .height(110.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0A0A0A))
            .border(
                BorderStroke(0.8.dp, world.accent.copy(alpha = 0.30f)),
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                world.label,
                color      = Color.White.copy(alpha = 0.90f),
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                world.subtitle,
                color      = VaibColors.TextSoft.copy(alpha = 0.40f),
                fontSize   = 9.sp,
                lineHeight = 13.sp,
            )
        }
        MiniWaveformBar(color = world.accent.copy(alpha = 0.35f))
    }
}

// ── Branch Out rail ───────────────────────────────────────────────────

@Composable
private fun BranchOutRail(
    items: List<ArchiveItem>,
    loadingId: String?,
    onItemClick: (ArchiveItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val branchLabels = listOf(
        "connected by mood",
        "open archive signal",
        "nearby vibe",
        "deep cut",
        "cosmic branch",
    )

    LazyRow(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding        = PaddingValues(end = 24.dp),
    ) {
        items(items.take(10), key = { it.id }) { item ->
            val label = branchLabels[abs(item.id.hashCode()) % branchLabels.size]
            OrbitTrackCard(
                item        = item,
                isLoading   = loadingId == item.id,
                anyLoading  = loadingId != null,
                branchLabel = label,
                onClick     = { onItemClick(item) },
            )
        }
    }
}

// ── Orbit track card ──────────────────────────────────────────────────

@Composable
private fun OrbitTrackCard(
    item: ArchiveItem,
    isLoading: Boolean,
    anyLoading: Boolean,
    branchLabel: String,
    onClick: () -> Unit,
) {
    val enabled = !anyLoading || isLoading
    val dimAlpha = if (anyLoading && !isLoading) 0.30f else 1.0f

    Column(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0A0A0A))
            .border(
                BorderStroke(0.6.dp, VaibColors.CyanPulse.copy(alpha = 0.12f * dimAlpha)),
                RoundedCornerShape(14.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Title
        Text(
            item.title,
            color      = Color.White.copy(alpha = 0.88f * dimAlpha),
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines   = 2,
            overflow   = TextOverflow.Ellipsis,
            lineHeight = 16.sp,
        )

        // Creator
        if (item.creator.isNotEmpty()) {
            Text(
                item.creator,
                color    = VaibColors.TextSoft.copy(alpha = 0.50f * dimAlpha),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Source + branch
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment   = Alignment.CenterVertically,
        ) {
            Text(
                "Internet Archive",
                color         = VaibColors.CyanPulse.copy(alpha = 0.45f * dimAlpha),
                fontSize      = 8.sp,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 0.3.sp,
            )
            Text(
                "\u00b7",
                color  = VaibColors.TextSoft.copy(alpha = 0.20f),
                fontSize = 8.sp,
            )
            Text(
                branchLabel,
                color         = VaibColors.VioletGlow.copy(alpha = 0.40f * dimAlpha),
                fontSize      = 8.sp,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 0.2.sp,
            )
        }

        // Mini waveform + loading
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment   = Alignment.CenterVertically,
        ) {
            MiniWaveformBar(
                color = VaibColors.CyanPulse.copy(alpha = 0.25f * dimAlpha),
                modifier = Modifier.weight(1f),
            )
            if (isLoading) {
                Spacer(Modifier.width(8.dp))
                CircularProgressIndicator(
                    modifier    = Modifier.size(14.dp),
                    color       = VaibColors.CyanPulse,
                    strokeWidth = 1.5.dp,
                )
            }
        }
    }
}

// ── Mini waveform bar ─────────────────────────────────────────────────

@Composable
private fun MiniWaveformBar(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "miniWave")
    val phase by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2_400, easing = LinearEasing), RepeatMode.Restart),
        label = "miniPhase",
    )
    val barCount = 12
    val twoPi = (2.0 * PI).toFloat()

    Canvas(modifier = modifier.height(10.dp)) {
        val barW = size.width / (barCount * 2 - 1)
        val maxH = size.height
        for (i in 0 until barCount) {
            val speed  = 0.6f + (i % 4) * 0.25f
            val offset = i.toFloat() / barCount
            val raw    = abs(sin((phase + offset) * speed * twoPi))
            val h      = (maxH * (0.15f + raw * 0.85f)).coerceAtLeast(1.5f)
            drawRect(
                color   = color,
                topLeft = Offset(i * barW * 2f, maxH - h),
                size    = Size(barW.coerceAtLeast(1f), h),
            )
        }
    }
}
