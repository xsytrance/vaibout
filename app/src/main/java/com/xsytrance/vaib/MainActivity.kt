package com.xsytrance.vaib

import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.xsytrance.vaib.core.design.VaibTheme
import com.xsytrance.vaib.ui.HomeScreen
import com.xsytrance.vaib.ui.SoloDreamscapeScreen

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
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            viewModel.loadTrack(uri, displayName)
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
                        viewModel = viewModel,
                        onPickTrack = { pickAudio.launch(arrayOf("audio/*")) },
                        onEnterDreamscape = { viewModel.navigateTo(Screen.SOLO_DREAMSCAPE) },
                    )
                    Screen.SOLO_DREAMSCAPE -> SoloDreamscapeScreen(
                        onBack = { viewModel.navigateTo(Screen.HOME) },
                    )
                }
            }
        }
    }
}
