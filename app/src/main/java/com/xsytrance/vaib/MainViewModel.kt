package com.xsytrance.vaib

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xsytrance.vaib.audio.AudioPlayer
import com.xsytrance.vaib.audio.AudioVisualizerAnalyzer
import com.xsytrance.vaib.data.TrackPrefs
import com.xsytrance.vaib.data.VaibDatabase
import com.xsytrance.vaib.data.entities.VaibEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen { HOME, SOLO_DREAMSCAPE }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val audioPlayer = AudioPlayer(application)
    private val trackPrefs  = TrackPrefs(application)
    private val analyzer    = AudioVisualizerAnalyzer()
    private val vaibDao     = VaibDatabase.get(application).vaibDao()

    val audioEnergy    = analyzer.energy
    val audioBeatPulse = analyzer.beatPulse

    val isPlaying: StateFlow<Boolean> = audioPlayer.isPlaying

    private val _trackUri = MutableStateFlow<Uri?>(null)
    val trackUri: StateFlow<Uri?> = _trackUri.asStateFlow()

    private val _trackName = MutableStateFlow<String?>(null)
    val trackName: StateFlow<String?> = _trackName.asStateFlow()

    private val _screen = MutableStateFlow(Screen.HOME)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _playbackFraction = MutableStateFlow(0f)
    val playbackFraction: StateFlow<Float> = _playbackFraction.asStateFlow()

    val savedVaibs: StateFlow<List<VaibEntity>> = vaibDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        restorePersistedTrack(application)
        startPositionTicker()
    }

    private fun restorePersistedTrack(application: Application) {
        val savedUri  = trackPrefs.loadUri()  ?: return
        val savedName = trackPrefs.loadName() ?: return
        val stillGranted = application.contentResolver.persistedUriPermissions
            .any { it.uri == savedUri && it.isReadPermission }
        if (stillGranted) {
            _trackUri.value  = savedUri
            _trackName.value = savedName
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
        _trackUri.value  = uri
        _trackName.value = cleanName
        _playbackFraction.value = 0f
        trackPrefs.save(uri, cleanName)
        audioPlayer.loadTrack(uri)
    }

    fun saveVaib(vaibName: String, mood: String) {
        val uri = _trackUri.value ?: return
        viewModelScope.launch {
            vaibDao.insert(
                VaibEntity(
                    vaibName         = vaibName.trim().ifEmpty { _trackName.value ?: "Untitled vAIb" },
                    trackUri         = uri.toString(),
                    trackName        = _trackName.value ?: "Unknown Track",
                    mood             = mood.trim(),
                    visualizerStyle  = "PULSE",
                    themeId          = "OLED_CYAN",
                    createdAt        = System.currentTimeMillis(),
                )
            )
        }
    }

    /** Restores a saved vAIb: loads track state and prepares player without auto-play. */
    fun loadVaib(vaib: VaibEntity) {
        val uri = Uri.parse(vaib.trackUri)
        _trackUri.value  = uri
        _trackName.value = vaib.trackName
        _playbackFraction.value = 0f
        trackPrefs.save(uri, vaib.trackName)
        audioPlayer.prepareTrack(uri)
    }

    fun togglePlayPause() = audioPlayer.togglePlayPause()

    fun startAnalyzer(): Boolean = analyzer.start(audioPlayer.audioSessionId)
    fun stopAnalyzer()           = analyzer.stop()

    fun navigateTo(screen: Screen) {
        _screen.value = screen
    }

    override fun onCleared() {
        super.onCleared()
        analyzer.stop()
        audioPlayer.release()
    }
}
