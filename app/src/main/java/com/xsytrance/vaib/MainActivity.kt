package com.xsytrance.vaib

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.xsytrance.vaib.core.design.VaibTheme
import com.xsytrance.vaib.ui.AuraSpectrumVisualizer
import com.xsytrance.vaib.ui.LibraryScreen
import com.xsytrance.vaib.ui.NowPlayingScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var pendingArtTrackKey by remember { mutableStateOf<String?>(null) }
            val artPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                val key = pendingArtTrackKey
                if (uri != null && key != null) {
                    viewModel.setTrackArtwork(key, uri.toString())
                }
                pendingArtTrackKey = null
            }
            val palette by viewModel.trackPalette.collectAsState()
            VaibTheme(palette = palette) {
                val screen by viewModel.screen.collectAsState()
                when (screen) {
                    Screen.LIBRARY -> LibraryScreen(
                        viewModel    = viewModel,
                        onTrackClick = { track ->
                            viewModel.playTrack(track)
                            viewModel.navigateTo(Screen.NOW_PLAYING)
                        },
                        onUploadArtwork = { track ->
                            pendingArtTrackKey = track.key
                            artPicker.launch("image/*")
                        },
                    )
                    Screen.NOW_PLAYING -> NowPlayingScreen(
                        viewModel = viewModel,
                        onBack    = { viewModel.navigateTo(Screen.LIBRARY) },
                    )
                    Screen.VISUALIZER -> AuraSpectrumVisualizer(
                        viewModel = viewModel,
                        onBack    = { viewModel.navigateTo(Screen.NOW_PLAYING) },
                    )
                }
            }
        }
    }
}
