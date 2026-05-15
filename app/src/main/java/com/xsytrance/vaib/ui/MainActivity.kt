package com.xsytrance.vaib

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.xsytrance.vaib.core.design.VaibTheme
import com.xsytrance.vaib.service.PlayerService
import com.xsytrance.vaib.ui.DiscoverScreen
import com.xsytrance.vaib.ui.HomeScreen
import com.xsytrance.vaib.ui.NowPlayingScreen
import com.xsytrance.vaib.ui.SoloDreamscapeScreen
import com.xsytrance.vaib.ui.StationsScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val pickAudio = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            val displayName = contentResolver
                .query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            viewModel.loadTrack(uri, displayName)
            PlayerService.start(this, displayName ?: "Unknown Track")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VaibTheme {
                val screen by viewModel.screen.collectAsState()
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        viewModel        = viewModel,
                        onPickTrack      = { pickAudio.launch(arrayOf("audio/*")) },
                        onEnterDreamscape = { viewModel.navigateTo(Screen.SOLO_DREAMSCAPE) },
                        onDiscoverMusic  = { viewModel.navigateTo(Screen.DISCOVER) },
                        onStations       = { viewModel.navigateTo(Screen.STATIONS) },
                    )
                    Screen.SOLO_DREAMSCAPE -> SoloDreamscapeScreen(
                        viewModel = viewModel,
                        onBack    = {
                            viewModel.stopAnalyzer()
                            viewModel.navigateTo(Screen.HOME)
                        },
                    )
                    Screen.DISCOVER -> DiscoverScreen(
                        viewModel = viewModel,
                        onBack    = { viewModel.navigateTo(Screen.HOME) },
                    )
                    Screen.STATIONS -> StationsScreen(
                        viewModel = viewModel,
                        onBack    = { viewModel.navigateTo(Screen.HOME) },
                    )
                    Screen.NOW_PLAYING -> NowPlayingScreen(
                        viewModel = viewModel,
                        onBack    = { viewModel.navigateTo(Screen.HOME) },
                    )
                }
            }
        }
    }
}