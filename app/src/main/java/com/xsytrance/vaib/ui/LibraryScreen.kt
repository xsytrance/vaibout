package com.xsytrance.vaib.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xsytrance.vaib.MainViewModel
import com.xsytrance.vaib.core.design.VaibColors
import com.xsytrance.vaib.data.entities.TrackEntity
import com.xsytrance.vaib.data.entities.VaibEntity

/**
 * Unified library screen — browse stations, tracks, and saved vAIbs.
 * Tab-based navigation: Stations | Tracks | Favorites | vAIbs
 */
@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onPlayTrack: (TrackEntity) -> Unit = {},
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Stations", "Tracks", "Favorites", "vAIbs")

    val stations by viewModel.allStations.collectAsState()
    val tracks by viewModel.allTracks.collectAsState()
    val favorites by viewModel.favoriteTracks.collectAsState()
    val vaibs by viewModel.savedVaibs.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Header ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    "Library",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                )
                // Search button
                IconButton(onClick = { /* TODO: search */ }) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Tab row ─────────────────────────────────────────
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = VaibColors.CyanPulse,
                        )
                    }
                },
                divider = {},
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == index) Color.White else Color.White.copy(alpha = 0.5f),
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Content ─────────────────────────────────────────
            when (selectedTab) {
                0 -> StationsTab(stations = stations, onPlayStation = { /* TODO */ })
                1 -> TracksTab(tracks = tracks, onPlayTrack = onPlayTrack)
                2 -> FavoritesTab(tracks = favorites, onPlayTrack = onPlayTrack)
                3 -> VaibsTab(vaibs = vaibs, onPlayVaib = { viewModel.loadVaib(it) })
            }
        }
    }
}

// ── Stations Tab ─────────────────────────────────────────────────────

@Composable
private fun StationsTab(
    stations: List<StationUiState>,
    onPlayStation: (StationUiState) -> Unit,
) {
    if (stations.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🎵", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "No stations yet",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Create stations from the Stations screen",
                color = VaibColors.TextTertiary,
                fontSize = 12.sp,
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(stations, key = { it.id }) { station ->
                StationListItem(
                    station = station,
                    onClick = { onPlayStation(station) },
                )
            }
        }
    }
}

@Composable
private fun StationListItem(
    station: StationUiState,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VaibColors.DeepBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Station icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(station.icon, fontSize = 22.sp)
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                station.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (station.description.isNotEmpty()) {
                Text(
                    station.description,
                    color = VaibColors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Track count badge
        if (station.trackCount > 0) {
            Text(
                "${station.trackCount}",
                color = VaibColors.CyanPulse.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.width(8.dp))

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.size(20.dp),
        )
    }
}

// ── Tracks Tab ───────────────────────────────────────────────────────

@Composable
private fun TracksTab(
    tracks: List<TrackEntity>,
    onPlayTrack: (TrackEntity) -> Unit,
) {
    if (tracks.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🎶", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "No tracks yet",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Pick a track or discover music to get started",
                color = VaibColors.TextTertiary,
                fontSize = 12.sp,
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(tracks, key = { it.id }) { track ->
                TrackListItem(
                    track = track,
                    onClick = { onPlayTrack(track) },
                )
            }
        }
    }
}

@Composable
private fun TrackListItem(
    track: TrackEntity,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Source indicator
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (track.sourceType == "INTERNET_ARCHIVE") Color(0xFF1A3A5C)
                    else Color(0xFF1A2A1C)
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (track.sourceType == "INTERNET_ARCHIVE") "🌐" else "📁",
                fontSize = 16.sp,
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                track.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                track.artist?.let {
                    Text(
                        it,
                        color = VaibColors.TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (track.playCount > 0) {
                    Text(
                        "${track.playCount} plays",
                        color = VaibColors.TextTertiary,
                        fontSize = 10.sp,
                    )
                }
            }
        }

        // Favorite indicator
        if (track.isFavorite) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = "Favorite",
                tint = Color(0xFFF43F5E),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ── Favorites Tab ────────────────────────────────────────────────────

@Composable
private fun FavoritesTab(
    tracks: List<TrackEntity>,
    onPlayTrack: (TrackEntity) -> Unit,
) {
    if (tracks.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("💜", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "No favorites yet",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap the heart icon on any track to favorite it",
                color = VaibColors.TextTertiary,
                fontSize = 12.sp,
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(tracks, key = { it.id }) { track ->
                TrackListItem(
                    track = track,
                    onClick = { onPlayTrack(track) },
                )
            }
        }
    }
}

// ── vAIbs Tab ────────────────────────────────────────────────────────

@Composable
private fun VaibsTab(
    vaibs: List<VaibEntity>,
    onPlayVaib: (VaibEntity) -> Unit,
) {
    if (vaibs.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("✨", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "No vAIbs saved",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Save your current setup as a vAIb from the home screen",
                color = VaibColors.TextTertiary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(vaibs, key = { it.id }) { vaib ->
                VaibListItem(
                    vaib = vaib,
                    onClick = { onPlayVaib(vaib) },
                )
            }
        }
    }
}

@Composable
private fun VaibListItem(
    vaib: VaibEntity,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VaibColors.DeepBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // vAIb icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A1040)),
            contentAlignment = Alignment.Center,
        ) {
            Text("✨", fontSize = 20.sp)
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                vaib.vaibName,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    vaib.trackName,
                    color = VaibColors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (vaib.mood.isNotEmpty()) {
                    Text(
                        vaib.mood,
                        color = VaibColors.CyanPulse.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                    )
                }
            }
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.size(20.dp),
        )
    }
}
