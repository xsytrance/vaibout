package com.xsytrance.vaib

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.xsytrance.vaib.audio.AudioPlayer
import com.xsytrance.vaib.data.TrackPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Screen { HOME, SOLO_DREAMSCAPE }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val audioPlayer = AudioPlayer(application)
    private val trackPrefs = TrackPrefs(application)

    val isPlaying: StateFlow<Boolean> = audioPlayer.isPlaying

    private val _trackUri = MutableStateFlow<Uri?>(null)
    val trackUri: StateFlow<Uri?> = _trackUri.asStateFlow()

    private val _trackName = MutableStateFlow<String?>(null)
    val trackName: StateFlow<String?> = _trackName.asStateFlow()

    private val _screen = MutableStateFlow(Screen.HOME)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    init {
        restorePersistedTrack(application)
    }

    private fun restorePersistedTrack(application: Application) {
        val savedUri = trackPrefs.loadUri() ?: return
        val savedName = trackPrefs.loadName() ?: return
        val stillGranted = application.contentResolver.persistedUriPermissions
            .any { it.uri == savedUri && it.isReadPermission }
        if (stillGranted) {
            _trackUri.value = savedUri
            _trackName.value = savedName
            // Deliberately no auto-play on restore
        } else {
            trackPrefs.clear()
        }
    }

    fun loadTrack(uri: Uri, displayName: String?) {
        val cleanName = displayName
            ?.let { name ->
                listOf(".mp3", ".flac", ".wav", ".m4a", ".aac", ".ogg", ".opus")
                    .fold(name) { acc, ext -> acc.removeSuffix(ext) }
            }
            ?: "Unknown Track"
        _trackUri.value = uri
        _trackName.value = cleanName
        trackPrefs.save(uri, cleanName)
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
