package com.xsytrance.vaib.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xsytrance.vaib.MainViewModel
import com.xsytrance.vaib.audio.EqPreset
import com.xsytrance.vaib.core.design.StationTheme
import com.xsytrance.vaib.core.design.VaibColors

/**
 * Settings screen — EQ defaults, crossfade, theme picker, sleep timer.
 */
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding(),
    ) {
        // ── Header ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
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
                "Settings",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
        ) {
            // ── EQ Defaults ──────────────────────────────────────
            SectionHeader("EQUALIZER")
            SettingsCard {
                SettingsRow(
                    title = "Default EQ Preset",
                    subtitle = "Applied when playing new tracks",
                    onClick = { /* TODO: EQ preset picker */ },
                ) {
                    Text(
                        EqPreset.FLAT.label,
                        color = VaibColors.CyanPulse,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Crossfade ────────────────────────────────────────
            SectionHeader("PLAYBACK")
            SettingsCard {
                SettingsRow(
                    title = "Crossfade",
                    subtitle = "Smooth transition between tracks",
                    onClick = { /* TODO: crossfade duration picker */ },
                ) {
                    Text(
                        "Off",
                        color = VaibColors.CyanPulse,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                HorizontalDivider(
                    color = VaibColors.TextSecondary.copy(alpha = 0.08f),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                SettingsRow(
                    title = "Fade In",
                    subtitle = "Gradual volume increase on track start",
                    onClick = { /* TODO */ },
                ) {
                    Text(
                        "0.5s",
                        color = VaibColors.CyanPulse,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                HorizontalDivider(
                    color = VaibColors.TextSecondary.copy(alpha = 0.08f),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                SettingsRow(
                    title = "Fade Out",
                    subtitle = "Gradual volume decrease on track end",
                    onClick = { /* TODO */ },
                ) {
                    Text(
                        "1.0s",
                        color = VaibColors.CyanPulse,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Continuous Play ──────────────────────────────────
            SectionHeader("CONTINUOUS PLAY")
            SettingsCard {
                val continuousPlay by viewModel.continuousPlay.collectAsState()
                SettingsRow(
                    title = "Auto-Play",
                    subtitle = "Always keep music playing from your library",
                    onClick = { viewModel.toggleContinuousPlay() },
                ) {
                    Switch(
                        checked = continuousPlay,
                        onCheckedChange = { viewModel.toggleContinuousPlay() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = VaibColors.CyanPulse,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = VaibColors.TextSecondary.copy(alpha = 0.3f),
                        ),
                    )
                }
                if (continuousPlay) {
                    Text(
                        "Music auto-plays from your shuffled library on startup and keeps going forever. Pick a track or station to override.",
                        color = VaibColors.TextSecondary.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Sleep Timer ──────────────────────────────────────
            SectionHeader("SLEEP TIMER")
            SettingsCard {
                SettingsRow(
                    title = "Auto-stop playback",
                    subtitle = "Fade out and stop after a set time",
                    onClick = { /* TODO: sleep timer picker */ },
                ) {
                    Text(
                        "Off",
                        color = VaibColors.CyanPulse,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Theme ────────────────────────────────────────────
            SectionHeader("APPEARANCE")
            SettingsCard {
                SettingsRow(
                    title = "Station Theme",
                    subtitle = "Default theme for new stations",
                    onClick = { /* TODO: theme picker */ },
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val theme = StationTheme.NEON_CYAN
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(theme.primary),
                        )
                        Text(
                            theme.label,
                            color = VaibColors.CyanPulse,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Data ─────────────────────────────────────────────
            SectionHeader("DATA")
            SettingsCard {
                SettingsRow(
                    title = "Export vAIbs",
                    subtitle = "Save your vAIbs as a JSON file",
                    onClick = { /* TODO: export */ },
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = VaibColors.TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp),
                    )
                }
                HorizontalDivider(
                    color = VaibColors.TextSecondary.copy(alpha = 0.08f),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                SettingsRow(
                    title = "Import vAIbs",
                    subtitle = "Restore vAIbs from a JSON file",
                    onClick = { /* TODO: import */ },
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = VaibColors.TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── About ────────────────────────────────────────────
            SectionHeader("ABOUT")
            SettingsCard {
                SettingsRow(
                    title = "Version",
                    subtitle = "vAIb out! 1.0.0",
                    onClick = {},
                ) {}
                HorizontalDivider(
                    color = VaibColors.TextSecondary.copy(alpha = 0.08f),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                SettingsRow(
                    title = "Open Source",
                    subtitle = "Made with 💜 by xsytrance",
                    onClick = {},
                ) {}
            }
        }
    }
}

// ── Shared components ─────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        color = VaibColors.TextSecondary.copy(alpha = 0.5f),
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VaibColors.Surface),
        content = content,
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                color = VaibColors.TextSecondary,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}
