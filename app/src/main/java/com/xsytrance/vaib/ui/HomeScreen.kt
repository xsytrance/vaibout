package com.xsytrance.vaib.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xsytrance.vaib.MainViewModel
import com.xsytrance.vaib.core.design.VaibColors

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onPickTrack: () -> Unit,
    onEnterDreamscape: () -> Unit,
) {
    val trackName by viewModel.trackName.collectAsState()
    val trackUri by viewModel.trackUri.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val hasTrack = trackUri != null

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

        Spacer(modifier = Modifier.weight(1f))

        // ── Track info ────────────────────────────────────────────────
        if (hasTrack) {
            Text(
                text = "LOADED",
                color = VaibColors.TextSoft,
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
            Spacer(modifier = Modifier.height(28.dp))
        }

        // ── Controls ──────────────────────────────────────────────────
        VaibOutlinedButton(
            label = if (hasTrack) "Choose Different Track" else "Choose Track",
            onClick = onPickTrack,
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
            VaibGlowButton(
                label = "vAIb out",
                onClick = onEnterDreamscape,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
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
