package com.xsytrance.vaib

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xsytrance.vaib.audio.AudioPlayer
import com.xsytrance.vaib.audio.AudioVisualizerAnalyzer
import com.xsytrance.vaib.data.TrackPrefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class Screen { HOME, SOLO_DREAMSCAPE }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val audioPlayer = AudioPlayer(application)
    private val trackPrefs = TrackPrefs(application)
    private val analyzer = AudioVisualizerAnalyzer()

    val audioEnergy    = analyzer.energy
    val audioBeatPulse = analyzer.beatPulse

    val isPlaying: StateFlow<Boolean> = audioPlayer.isPlaying

    private val _trackUri = MutableStateFlow<Uri?>(null)
    val trackUri: StateFlow<Uri?> = _trackUri.asStateFlow()

    private val _trackName = MutableStateFlow<String?>(null)
    val trackName: StateFlow<String?> = _trackName.asStateFlow()

    private val _screen = MutableStateFlow(Screen.HOME)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    // 0f..1f fraction; -1f when duration is unknown
    private val _playbackFraction = MutableStateFlow(0f)
    val playbackFraction: StateFlow<Float> = _playbackFraction.asStateFlow()

    init {
        restorePersistedTrack(application)
        startPositionTicker()
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

    private fun startPositionTicker() {
        viewModelScope.launch {
            audioPlayer.isPlaying.collectLatest { playing ->
                if (playing) {
                    while (true) {
                        val dur = audioPlayer.durationMs
                        if (dur > 0) {
                            _playbackFraction.value =
                                (audioPlayer.currentPositionMs.toFloat() / dur.toFloat())
                                    .coerceIn(0f, 1f)
                        }
                        delay(250)
                    }
                }
            }
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
        _playbackFraction.value = 0f
        trackPrefs.save(uri, cleanName)
        audioPlayer.loadTrack(uri)
    }

    fun togglePlayPause() = audioPlayer.togglePlayPause()

    /** Returns true if the Visualizer attached successfully. */
    fun startAnalyzer(): Boolean = analyzer.start(audioPlayer.audioSessionId)

    fun stopAnalyzer() = analyzer.stop()

    fun navigateTo(screen: Screen) {
        _screen.value = screen
    }

    override fun onCleared() {
        super.onCleared()
        analyzer.stop()
        audioPlayer.release()
    }
}
