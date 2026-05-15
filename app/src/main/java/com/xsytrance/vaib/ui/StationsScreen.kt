package com.xsytrance.vaib.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.xsytrance.vaib.MainViewModel
import com.xsytrance.vaib.core.design.StationTheme
import com.xsytrance.vaib.core.design.VaibAtmosphere
import com.xsytrance.vaib.core.design.VaibColors
import kotlin.math.PI

@Composable
fun StationsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val stations by viewModel.allStations.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var stationToDelete by remember { mutableStateOf<StationUiState?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // ── Ambient background ──────────────────────────────
        StationsAmbientBg(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Header ───────────────────────────────────────
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
                    "Stations",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                )
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "New Station",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Summary bar ──────────────────────────────────
            Text(
                text = "${stations.size} stations",
                color = VaibColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(Modifier.height(16.dp))

            // ── Station grid ─────────────────────────────────
            if (stations.isEmpty()) {
                EmptyStationsState()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(
                        items = stations,
                        key = { it.id },
                    ) { station ->
                        StationCard(
                            station = station,
                            onPlay = { /* TODO: create queue from station tracks */ },
                            onEdit = { /* TODO: edit station */ },
                            onDelete = { stationToDelete = station },
                        )
                    }
                }
            }
        }

        // ── FAB ──────────────────────────────────────────────
        if (stations.isEmpty()) {
            FloatingAddButton(onClick = { showAddDialog = true })
        }

        // ── Delete confirmation ──────────────────────────────
        stationToDelete?.let { uiState ->
            AlertDialog(
                onDismissRequest = { stationToDelete = null },
                containerColor = VaibColors.DeepBackground,
                titleContentColor = Color.White,
                textContentColor = VaibColors.TextSecondary,
                icon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFCC3333),
                    )
                },
                title = {
                    Text("Delete Station?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Text("\"${uiState.name}\" and its track associations will be removed.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteStation(uiState.id)
                            stationToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFCC3333),
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { stationToDelete = null }) {
                        Text("Cancel", color = VaibColors.TextSecondary)
                    }
                },
            )
        }

        // ── Add station dialog ───────────────────────────────
        if (showAddDialog) {
            AddStationDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, desc, icon, themeOrdinal ->
                    viewModel.createStation(name, desc, icon, themeOrdinal)
                    showAddDialog = false
                },
            )
        }
    }
}

// ── Station Card ──────────────────────────────────────────────

@Composable
private fun StationCard(
    station: StationUiState,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val theme = StationTheme.entries.getOrElse(station.themeOrdinal) { StationTheme.NEON_CYAN }
    val atmosphere = theme.toAtmosphere()

    var showContextMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(atmosphere.backgroundAccent)
                .then(
                    if (showContextMenu) Modifier.clickable { showContextMenu = false }
                    else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Gradient background
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = theme.gradientColors,
                    ),
                )
            }

            // Animated wave on card
            StationWaveOverlay(atmosphere = atmosphere)

            // Station icon
            Text(
                text = station.icon,
                fontSize = 40.sp,
            )

            // Play button overlay
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable { onPlay() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }

            // Overflow menu
            IconButton(
                onClick = { showContextMenu = !showContextMenu },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f)),
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }

            // Context menu dropdown
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = { onEdit(); showContextMenu = false },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    },
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { onDelete(); showContextMenu = false },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFCC3333))
                    },
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Station name
        Text(
            text = station.name,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )

        // Description / track count
        if (station.description.isNotEmpty() || station.trackCount > 0) {
            Text(
                text = buildString {
                    if (station.description.isNotEmpty()) append(station.description)
                    if (station.trackCount > 0) {
                        if (isNotEmpty()) append("  ·  ")
                        append("${station.trackCount} tracks")
                    }
                },
                color = VaibColors.TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StationWaveOverlay(atmosphere: VaibAtmosphere) {
    val transition = rememberInfiniteTransition(label = "stationWave")
    val phase by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(4_000, easing = LinearEasing), RepeatMode.Restart),
        label = "swPhase",
    )
    val twoPi = (2.0 * PI).toFloat()

    Canvas(modifier = Modifier.fillMaxSize()) {
        val baseY = size.height * 0.7f
        val amp = size.height * 0.08f
        val path = Path()
        for (i in 0..60) {
            val x = i.toFloat() / 60f * size.width
            val y = baseY + sin(x / size.width * twoPi * 2f + phase * twoPi) * amp
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path,
            atmosphere.primaryColor.copy(alpha = 0.12f),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun EmptyStationsState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "🎵",
            fontSize = 64.sp,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No stations yet",
            color = VaibColors.TextSecondary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Create your first station to organize tracks",
            color = VaibColors.TextTertiary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FloatingAddButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.Black,
            modifier = Modifier
                .padding(20.dp)
                .size(56.dp),
            elevation = FloatingActionButtonDefaults.elevation(8.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "New Station", modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun AddStationDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, desc: String, icon: String, themeOrdinal: Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("🎵") }
    var selectedTheme by remember { mutableStateOf(0) }

    val icons = listOf("🎵", "🎶", "🎸", "🎹", "🥁", "🎷", "🎺", "🎻", "🎤", "🔊", "🎧", "💜", "🌊", "✨", "🔥")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = VaibColors.Surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                Text(
                    "New Station",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(Modifier.height(20.dp))

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Station name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = VaibColors.TextSecondary.copy(alpha = 0.3f),
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                )

                Spacer(Modifier.height(12.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = VaibColors.TextSecondary.copy(alpha = 0.3f),
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                )

                Spacer(Modifier.height(16.dp))

                // Theme picker
                Text(
                    "Theme",
                    color = VaibColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StationTheme.entries.forEachIndexed { index, theme ->
                        val isSelected = selectedTheme == index
                        val borderColor = if (isSelected) theme.primaryColor else VaibColors.TextSecondary.copy(alpha = 0.2f)

                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) theme.primaryColor.copy(alpha = 0.15f) else VaibColors.DeepBackground)
                                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                                .clickable { selectedTheme = index }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("⬤", fontSize = 12.sp, color = theme.primaryColor)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                theme.label,
                                fontSize = 10.sp,
                                color = if (isSelected) Color.White else VaibColors.TextTertiary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Icon picker
                Text(
                    "Icon",
                    color = VaibColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    icons.forEach { icon ->
                        val isSelected = selectedIcon == icon
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) VaibColors.SurfaceElevated else VaibColors.DeepBackground)
                                .border(1.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                                .clickable { selectedIcon = icon },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(icon, fontSize = 20.sp)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = VaibColors.TextSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(name, description, selectedIcon, selectedTheme) },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Create", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Ambient background for station screen ────────────────────

@Composable
private fun StationsAmbientBg(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "stationsBg")
    val phase by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(25_000, easing = LinearEasing), RepeatMode.Restart),
        label = "sbgPhase",
    )
    val twoPi = (2.0 * PI).toFloat()

    Canvas(modifier = modifier) {
        listOf(
            Triple(0.15f, 0.02f, 1.2f),
            Triple(0.45f, 0.03f, 0.8f),
            Triple(0.75f, 0.025f, 1.5f),
        ).forEachIndexed { i, (yFrac, ampFrac, freq) ->
            val phaseOff = phase * twoPi + i * twoPi / 3f
            val baseY = size.height * yFrac
            val amp = size.height * ampFrac
            val path = Path()
            for (step in 0..60) {
                val x = step.toFloat() / 60f * size.width
                val y = baseY + sin(x / size.width * twoPi * freq + phaseOff) * amp
                if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path,
                Color(0xFF00E5FF).copy(alpha = 0.03f + i * 0.01f),
                style = Stroke(width = 1.2f + i * 0.3f, cap = StrokeCap.Round),
            )
        }
    }
}