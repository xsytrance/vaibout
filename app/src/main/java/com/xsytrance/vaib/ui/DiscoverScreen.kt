package com.xsytrance.vaib.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xsytrance.vaib.MainViewModel
import com.xsytrance.vaib.core.design.VaibColors
import com.xsytrance.vaib.discover.ArchiveItem
import com.xsytrance.vaib.discover.DiscoverUiState
import kotlinx.coroutines.delay

private val StreamErrorBg   = Color(0xFF160808)
private val StreamErrorText = Color(0xFFFF7070)

@Composable
fun DiscoverScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val state         by viewModel.discoverState.collectAsState()
    val loadingItemId by viewModel.loadingItemId.collectAsState()
    val streamError   by viewModel.streamError.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    // Debounced search: fires immediately for empty string (default list),
    // waits 600 ms after the user stops typing for keyword queries.
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) delay(600L)
        viewModel.fetchDiscoverItems(searchQuery)
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding(),
    ) {
        // ── Top bar ───────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 8.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text(
                    "← Back",
                    color = VaibColors.TextSoft,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // ── Header ────────────────────────────────────────────────────
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

            // ── Search bar ────────────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search open music…",
                        color = VaibColors.TextSoft.copy(alpha = 0.45f),
                        fontSize = 14.sp,
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor      = Color.White,
                    unfocusedTextColor    = Color.White,
                    focusedBorderColor    = VaibColors.CyanPulse,
                    unfocusedBorderColor  = VaibColors.TextSoft.copy(alpha = 0.25f),
                    cursorColor           = VaibColors.CyanPulse,
                    focusedLabelColor     = VaibColors.CyanPulse,
                    unfocusedLabelColor   = VaibColors.TextSoft,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { viewModel.fetchDiscoverItems(searchQuery) }
                ),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Stream error banner ───────────────────────────────────────
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
                    Text("✕", color = VaibColors.TextSoft, fontSize = 13.sp)
                }
            }
        }

        // ── Content ───────────────────────────────────────────────────
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
                            color = VaibColors.TextSoft,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        VaibOutlinedButton(
                            label = "Retry",
                            onClick = { viewModel.fetchDiscoverItems(searchQuery) },
                        )
                    }
                }

                is DiscoverUiState.Success -> {
                    LazyColumn {
                        items(s.items, key = { it.id }) { item ->
                            DiscoverItemRow(
                                item       = item,
                                isLoading  = loadingItemId == item.id,
                                anyLoading = loadingItemId != null,
                                onClick    = { viewModel.loadOnlineTrack(item) },
                            )
                            HorizontalDivider(
                                color     = Color.White.copy(alpha = 0.04f),
                                thickness = 1.dp,
                            )
                        }
                    }
                }
            }
        }

        // ── Legal notice ──────────────────────────────────────────────
        Text(
            text = "Open audio from public sources such as Internet Archive. Availability may change.",
            color = VaibColors.TextSoft.copy(alpha = 0.45f),
            fontSize = 10.sp,
            lineHeight = 15.sp,
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .padding(top = 12.dp, bottom = 20.dp),
        )
    }
}

@Composable
private fun DiscoverItemRow(
    item: ArchiveItem,
    isLoading: Boolean,
    anyLoading: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !anyLoading, onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(end = 36.dp)) {
            Text(
                text = item.title,
                color = if (anyLoading && !isLoading) Color.White.copy(alpha = 0.35f)
                        else Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                lineHeight = 20.sp,
            )
            if (item.creator.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.creator,
                    color = if (anyLoading && !isLoading) VaibColors.TextSoft.copy(alpha = 0.35f)
                            else VaibColors.TextSoft,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
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

        if (isLoading) {
            CircularProgressIndicator(
                modifier  = Modifier.align(Alignment.CenterEnd).size(18.dp),
                color     = VaibColors.CyanPulse,
                strokeWidth = 2.dp,
            )
        }
    }
}
