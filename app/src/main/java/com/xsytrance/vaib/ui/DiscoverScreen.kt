package com.xsytrance.vaib.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xsytrance.vaib.MainViewModel
import com.xsytrance.vaib.core.design.VaibColors
import com.xsytrance.vaib.discover.ArchiveItem
import com.xsytrance.vaib.discover.DiscoverUiState
import kotlinx.coroutines.delay

private val StreamErrorBg = Color(0xFF160808)
private val StreamErrorText = Color(0xFFFF7070)

// ── Mood categories for discovery ──────────────────────────────────

private data class MoodCategory(
    val name: String,
    val emoji: String,
    val query: String,
    val gradient: List<Color>,
)

private val MOOD_CATEGORIES = listOf(
    MoodCategory("Deep", "🌊", "ambient electronic", listOf(Color(0xFF0A1628), Color(0xFF081438))),
    MoodCategory("Chill", "🍃", "chillout lounge", listOf(Color(0xFF0A1A14), Color(0xFF081820))),
    MoodCategory("Energetic", "⚡", "electronic dance", listOf(Color(0xFF1A0A14), Color(0xFF180820))),
    MoodCategory("Cosmic", "✨", "space ambient", listOf(Color(0xFF0E0A28), Color(0xFF100830))),
    MoodCategory("Focus", "🎯", "instrumental focus", listOf(Color(0xFF0A1420), Color(0xFF081028))),
)

// ── Main Discover Screen ───────────────────────────────────────────

@Composable
fun DiscoverScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.discoverState.collectAsState()
    val loadingItemId by viewModel.loadingItemId.collectAsState()
    val streamError by viewModel.streamError.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf<String?>(null) }

    // Debounced search
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) delay(600L)
        viewModel.fetchDiscoverItems(searchQuery)
    }

    // Fetch default items on first load
    LaunchedEffect(Unit) {
        if (state is DiscoverUiState.Loading || state is DiscoverUiState.Success && (state as? DiscoverUiState.Success)?.items?.isEmpty() == true) {
            viewModel.fetchDiscoverItems("")
        }
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding(),
    ) {
        // ── Top bar ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 8.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text(
                    "← Back",
                    color = VaibColors.TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // ── Header ─────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 28.dp)) {
            Text(
                text = "Open Music",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.2).sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Internet Archive  ·  Creative Commons",
                color = VaibColors.CyanPulse,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp,
            )
            Spacer(modifier = Modifier.height(16.dp))

            // ── Search bar ──────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    selectedMood = null
                },
                placeholder = {
                    Text(
                        "Search open music…",
                        color = VaibColors.TextSecondary.copy(alpha = 0.45f),
                        fontSize = 14.sp,
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = VaibColors.CyanPulse,
                    unfocusedBorderColor = VaibColors.TextSecondary.copy(alpha = 0.25f),
                    cursorColor = VaibColors.CyanPulse,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { viewModel.fetchDiscoverItems(searchQuery) }
                ),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Mood categories (horizontal scroll) ────────────────
        if (searchQuery.isBlank()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                items(MOOD_CATEGORIES) { mood ->
                    MoodChip(
                        mood = mood,
                        isSelected = selectedMood == mood.name,
                        onClick = {
                            selectedMood = if (selectedMood == mood.name) null else mood.name
                            searchQuery = if (selectedMood == mood.name) mood.query else ""
                        },
                    )
                }
            }
        }

        // ── Section label ──────────────────────────────────────
        val sectionLabel = when {
            selectedMood != null -> "${selectedMood!!.uppercase()} PICKS"
            searchQuery.isNotBlank() -> "SEARCH RESULTS"
            else -> "TOP OPEN PICKS"
        }
        Text(
            text = sectionLabel,
            color = VaibColors.TextSecondary.copy(alpha = 0.40f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.8.sp,
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .padding(bottom = 10.dp),
        )

        // ── Stream error banner ────────────────────────────────
        if (streamError != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StreamErrorBg)
                    .padding(horizontal = 28.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = streamError!!,
                    color = StreamErrorText,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { viewModel.clearStreamError() }) {
                    Text("✕", color = VaibColors.TextSecondary, fontSize = 13.sp)
                }
            }
        }

        // ── Content ────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when (val s = state) {
                is DiscoverUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = VaibColors.CyanPulse,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }

                is DiscoverUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 28.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = s.message,
                            color = VaibColors.TextSecondary,
                            fontSize = 14.sp,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        VaibOutlinedButton(
                            label = "Retry",
                            onClick = {
                                viewModel.fetchDiscoverItems(
                                    if (selectedMood != null) {
                                        MOOD_CATEGORIES.find { it.name == selectedMood }?.query ?: ""
                                    } else {
                                        searchQuery
                                    }
                                )
                            },
                        )
                    }
                }

                is DiscoverUiState.Success -> {
                    if (s.items.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 28.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("🔍", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No results found",
                                color = VaibColors.TextSecondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Try a different search or mood",
                                color = VaibColors.TextTertiary,
                                fontSize = 12.sp,
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp, bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(s.items, key = { it.id }) { item ->
                                DiscoverCard(
                                    item = item,
                                    isLoading = loadingItemId == item.id,
                                    anyLoading = loadingItemId != null,
                                    onClick = { viewModel.loadOnlineTrack(item) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Legal notice ───────────────────────────────────────
        Text(
            text = "Open audio from public sources such as Internet Archive. Availability may change.",
            color = VaibColors.TextSecondary.copy(alpha = 0.45f),
            fontSize = 10.sp,
            lineHeight = 15.sp,
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .padding(top = 12.dp, bottom = 20.dp),
        )
    }
}

// ── Mood Chip ──────────────────────────────────────────────────────

@Composable
private fun MoodChip(
    mood: MoodCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor by animateColorAsState(
        if (isSelected) VaibColors.CyanPulse.copy(alpha = 0.2f)
        else VaibColors.Surface,
        label = "moodBg",
    )
    val borderColor by animateColorAsState(
        if (isSelected) VaibColors.CyanPulse
        else VaibColors.TextSecondary.copy(alpha = 0.15f),
        label = "moodBorder",
    )
    val textColor by animateColorAsState(
        if (isSelected) Color.White
        else VaibColors.TextSecondary,
        label = "moodText",
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(mood.emoji, fontSize = 14.sp)
        Text(
            mood.name,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

// ── Discover Card (redesigned) ─────────────────────────────────────

@Composable
private fun DiscoverCard(
    item: ArchiveItem,
    isLoading: Boolean,
    anyLoading: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !anyLoading, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = VaibColors.Surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Artwork placeholder with gradient
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                VaibColors.CyanPulse.copy(alpha = 0.3f),
                                VaibColors.VioletGlow.copy(alpha = 0.2f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("🎵", fontSize = 24.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = if (anyLoading && !isLoading) Color.White.copy(alpha = 0.35f)
                    else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.creator.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.creator,
                        color = if (anyLoading && !isLoading) VaibColors.TextSecondary.copy(alpha = 0.35f)
                        else VaibColors.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Internet Archive",
                    color = VaibColors.CyanPulse.copy(
                        alpha = if (anyLoading && !isLoading) 0.25f else 0.50f
                    ),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                )
            }

            Spacer(Modifier.width(8.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = VaibColors.CyanPulse,
                    strokeWidth = 2.dp,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(VaibColors.CyanPulse.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("▶", color = VaibColors.CyanPulse, fontSize = 14.sp)
                }
            }
        }
    }
}
