package com.xsytrance.vaib

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.xsytrance.vaib.audio.AudioPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Screen { HOME, SOLO_DREAMSCAPE }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val audioPlayer = AudioPlayer(application)

    val isPlaying: StateFlow<Boolean> = audioPlayer.isPlaying

    private val _trackUri = MutableStateFlow<Uri?>(null)
    val trackUri: StateFlow<Uri?> = _trackUri.asStateFlow()

    private val _trackName = MutableStateFlow<String?>(null)
    val trackName: StateFlow<String?> = _trackName.asStateFlow()

    private val _screen = MutableStateFlow(Screen.HOME)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    fun loadTrack(uri: Uri, displayName: String?) {
        _trackUri.value = uri
        _trackName.value = displayName
            ?.let { name ->
                listOf(".mp3", ".flac", ".wav", ".m4a", ".aac", ".ogg", ".opus")
                    .fold(name) { acc, ext -> acc.removeSuffix(ext) }
            }
            ?: "Unknown Track"
        audioPlayer.loadTrack(uri)
    }

    fun togglePlayPause() = audioPlayer.togglePlayPause()

    fun navigateTo(screen: Screen) {
        _screen.value = screen
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
