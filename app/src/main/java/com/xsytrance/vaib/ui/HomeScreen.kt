package com.xsytrance.vaib.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xsytrance.vaib.MainViewModel
import com.xsytrance.vaib.core.design.VaibColors
import com.xsytrance.vaib.vaib.VaibCard

private val MOOD_OPTIONS = listOf("Deep", "Chill", "Energetic", "Cosmic", "Focus")

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onPickTrack: () -> Unit,
    onEnterDreamscape: () -> Unit,
    onDiscoverMusic: () -> Unit,
) {
    val trackName        by viewModel.trackName.collectAsState()
    val trackUri         by viewModel.trackUri.collectAsState()
    val isPlaying        by viewModel.isPlaying.collectAsState()
    val playbackFraction by viewModel.playbackFraction.collectAsState()
    val savedVaibs       by viewModel.savedVaibs.collectAsState()
    val hasTrack = trackUri != null

    var showSaveDialog by remember { mutableStateOf(false) }
    var nameInput      by remember { mutableStateOf("") }
    var selectedMood   by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
            .padding(horizontal = 28.dp),
    ) {
        Spacer(modifier = Modifier.height(56.dp))

        // ── Header ────────────────────────────────────────────────────
        Text(
            text = "vAIb out!",
            color = Color.White,
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.5).sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "The visualizer is the product.",
            color = VaibColors.CyanPulse,
            fontSize = 14.sp,
            letterSpacing = 0.3.sp,
            fontWeight = FontWeight.Medium,
        )

        // ── Saved vAIbs list (fills middle space) ─────────────────────
        if (savedVaibs.isEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "YOUR VAIBS",
                color = VaibColors.TextSoft.copy(alpha = 0.55f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.8.sp,
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(savedVaibs, key = { it.id }) { vaib ->
                    VaibCard(vaib = vaib, onClick = { viewModel.loadVaib(vaib) })
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // ── Track info ────────────────────────────────────────────────
        if (hasTrack) {
            Text(
                text = when {
                    isPlaying        -> "PLAYING"
                    playbackFraction > 0f -> "PAUSED"
                    else             -> "READY"
                },
                color = if (isPlaying) VaibColors.CyanPulse else VaibColors.TextSoft,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = trackName ?: "",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { playbackFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = VaibColors.CyanPulse,
                trackColor = Color.White.copy(alpha = 0.07f),
                strokeCap = StrokeCap.Round,
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // ── Controls ──────────────────────────────────────────────────
        VaibOutlinedButton(
            label = if (hasTrack) "Choose Different Track" else "Choose Track",
            onClick = onPickTrack,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        VaibOutlinedButton(
            label = "Discover Open Music",
            onClick = onDiscoverMusic,
            modifier = Modifier.fillMaxWidth(),
        )

        if (hasTrack) {
            Spacer(modifier = Modifier.height(12.dp))
            VaibSecondaryButton(
                label = if (isPlaying) "Pause" else "Play",
                onClick = viewModel::togglePlayPause,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            VaibOutlinedButton(
                label = "Save vAIb",
                onClick = { showSaveDialog = true },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            VaibGlowButton(
                label = "vAIb out",
                onClick = onEnterDreamscape,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
    }

    // ── Save vAIb dialog ──────────────────────────────────────────────
    if (showSaveDialog) {
        val dismiss = {
            showSaveDialog = false
            nameInput = ""
            selectedMood = ""
        }
        AlertDialog(
            onDismissRequest = dismiss,
            containerColor = VaibColors.DeepBackground,
            titleContentColor = Color.White,
            textContentColor = VaibColors.TextSoft,
            title = {
                Text(
                    "Save vAIb",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Name") },
                        placeholder = { Text(trackName ?: "My vAIb") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = VaibColors.CyanPulse,
                            unfocusedBorderColor = VaibColors.TextSoft.copy(alpha = 0.3f),
                            focusedLabelColor = VaibColors.CyanPulse,
                            unfocusedLabelColor = VaibColors.TextSoft,
                            cursorColor = VaibColors.CyanPulse,
                        ),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "MOOD",
                        color = VaibColors.TextSoft.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MOOD_OPTIONS.forEach { mood ->
                            FilterChip(
                                selected = selectedMood == mood,
                                onClick = {
                                    selectedMood = if (selectedMood == mood) "" else mood
                                },
                                label = {
                                    Text(
                                        mood,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VaibColors.CyanPulse,
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color.White.copy(alpha = 0.08f),
                                    labelColor = Color.White.copy(alpha = 0.75f),
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedMood == mood,
                                    borderColor = Color.White.copy(alpha = 0.12f),
                                    selectedBorderColor = Color.Transparent,
                                ),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveVaib(
                            nameInput.ifEmpty { trackName ?: "Untitled vAIb" },
                            selectedMood,
                        )
                        dismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VaibColors.CyanPulse,
                        contentColor = Color.Black,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = dismiss) {
                    Text("Cancel", color = VaibColors.TextSoft)
                }
            },
        )
    }
}

@Composable
internal fun VaibOutlinedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.White,
        ),
        border = BorderStroke(1.dp, VaibColors.TextSoft.copy(alpha = 0.35f)),
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp,
        )
    }
}

@Composable
internal fun VaibSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.07f),
            contentColor = Color.White,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp,
        )
    }
}

@Composable
internal fun VaibGlowButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = VaibColors.CyanPulse,
            contentColor = Color.Black,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
    }
}
