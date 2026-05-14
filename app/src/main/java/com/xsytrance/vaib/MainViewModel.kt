package com.xsytrance.vaib

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xsytrance.vaib.audio.AudioOutputDetector
import com.xsytrance.vaib.audio.AudioPlayer
import com.xsytrance.vaib.audio.AudioVisualizerAnalyzer
import com.xsytrance.vaib.audio.EqController
import com.xsytrance.vaib.audio.EqPreset
import com.xsytrance.vaib.core.design.VaibAtmosphere
import com.xsytrance.vaib.data.TrackPrefs
import com.xsytrance.vaib.data.VaibDatabase
import com.xsytrance.vaib.data.entities.VaibEntity
import com.xsytrance.vaib.discover.ArchiveItem
import com.xsytrance.vaib.discover.DiscoverUiState
import com.xsytrance.vaib.discover.InternetArchiveApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen { HOME, SOLO_DREAMSCAPE, DISCOVER }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val audioPlayer  = AudioPlayer(application)
    private val trackPrefs   = TrackPrefs(application)
    private val analyzer     = AudioVisualizerAnalyzer()
    private val eqController = EqController()
    private val vaibDao      = VaibDatabase.get(application).vaibDao()

    val audioEnergy    = analyzer.energy
    val audioBeatPulse = analyzer.beatPulse

    val isPlaying:   StateFlow<Boolean> = audioPlayer.isPlaying
    val isBuffering: StateFlow<Boolean> = audioPlayer.isBuffering

    private val _trackUri = MutableStateFlow<Uri?>(null)
    val trackUri: StateFlow<Uri?> = _trackUri.asStateFlow()

    private val _trackName = MutableStateFlow<String?>(null)
    val trackName: StateFlow<String?> = _trackName.asStateFlow()

    private val _screen = MutableStateFlow(Screen.HOME)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _playbackFraction = MutableStateFlow(0f)
    val playbackFraction: StateFlow<Float> = _playbackFraction.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _currentEqPreset = MutableStateFlow(EqPreset.FLAT)
    val currentEqPreset: StateFlow<EqPreset> = _currentEqPreset.asStateFlow()

    private val _currentMood = MutableStateFlow("")
    val currentMood: StateFlow<String> = _currentMood.asStateFlow()

    private val _currentAtmosphere = MutableStateFlow(VaibAtmosphere.Default)
    val currentAtmosphere: StateFlow<VaibAtmosphere> = _currentAtmosphere.asStateFlow()

    val savedVaibs: StateFlow<List<VaibEntity>> = vaibDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Discover ──────────────────────────────────────────────────────

    private val _discoverState = MutableStateFlow<DiscoverUiState>(DiscoverUiState.Loading)
    val discoverState: StateFlow<DiscoverUiState> = _discoverState.asStateFlow()

    private val _loadingItemId = MutableStateFlow<String?>(null)
    val loadingItemId: StateFlow<String?> = _loadingItemId.asStateFlow()

    private val _streamError = MutableStateFlow<String?>(null)
    val streamError: StateFlow<String?> = _streamError.asStateFlow()

    init {
        restorePersistedTrack(application)
        prepareStartupTrack(application)
        startPositionTicker()
        observePlaybackEnd()
    }

    // ── Track restore ─────────────────────────────────────────────────

    private fun restorePersistedTrack(application: Application) {
        val savedUri  = trackPrefs.loadUri()  ?: return
        val savedName = trackPrefs.loadName() ?: return

        // Remote https:// URIs don't use SAF persistable permissions — skip the check
        val isRemote = savedUri.scheme == "https" || savedUri.scheme == "http"
        if (!isRemote) {
            val stillGranted = application.contentResolver.persistedUriPermissions
                .any { it.uri == savedUri && it.isReadPermission }
            if (!stillGranted) {
                trackPrefs.clear()
                return
            }
        }
        _trackUri.value  = savedUri
        _trackName.value = savedName
        // Prepare local files so the Play button works immediately on restore.
        // Remote streams are intentionally not prepared here to avoid background network traffic.
        if (!isRemote) {
            audioPlayer.prepareTrack(savedUri)
        }
    }

    // ── Headphone-safe startup playback ───────────────────────────────

    private fun prepareStartupTrack(application: Application) {
        val uri = _trackUri.value ?: return
        val headphones = AudioOutputDetector.hasPersonalAudioOutput(application)
        val isRemote = uri.scheme == "https" || uri.scheme == "http"

        // Always prepare the track so UI shows loaded state
        if (isRemote) {
            audioPlayer.prepareTrack(uri)
        }

        // Only autoplay if safe personal audio is detected
        if (headphones) {
            audioPlayer.player.play()
        }
    }

    // ── Position ticker ───────────────────────────────────────────────

    private fun observePlaybackEnd() {
        viewModelScope.launch {
            audioPlayer.isEnded.collect { ended ->
                if (ended) _playbackFraction.value = 0f
            }
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
                            _currentPositionMs.value = audioPlayer.currentPositionMs.coerceAtLeast(0)
                            _durationMs.value = dur
                        }
                        delay(250)
                    }
                }
            }
        }
    }

    // ── Local track ───────────────────────────────────────────────────

    fun loadTrack(uri: Uri, displayName: String?) {
        val cleanName = displayName
            ?.let { name ->
                val decoded = try { java.net.URLDecoder.decode(name, "UTF-8") } catch (_: Exception) { name }
                listOf(".mp3", ".flac", ".wav", ".m4a", ".aac", ".ogg", ".opus")
                    .fold(decoded) { acc, ext -> acc.removeSuffix(ext) }
                    .trim()
            }
            ?: "Unknown Track"
        _trackUri.value  = uri
        _trackName.value = cleanName
        _playbackFraction.value = 0f
        _currentPositionMs.value = 0L
        _durationMs.value = 0L
        _currentMood.value = ""
        _currentAtmosphere.value = VaibAtmosphere.Default
        trackPrefs.save(uri, cleanName)
        audioPlayer.loadTrack(uri)
    }

    // ── Discover / online track ───────────────────────────────────────

    fun fetchDiscoverItems(query: String = "") {
        viewModelScope.launch {
            _discoverState.value = DiscoverUiState.Loading
            _streamError.value   = null
            try {
                val items = InternetArchiveApi.fetchItems(query)
                _discoverState.value = if (items.isEmpty()) {
                    DiscoverUiState.Error(
                        if (query.isBlank()) "No results found. Check your connection."
                        else "No results for \"$query\"."
                    )
                } else {
                    DiscoverUiState.Success(items)
                }
            } catch (_: Exception) {
                _discoverState.value = DiscoverUiState.Error(
                    "Couldn't load music. Check your connection."
                )
            }
        }
    }

    /** Mood-first discovery: maps mood labels to Internet Archive search queries. */
    fun fetchMoodItems(mood: String) {
        val query = when (mood.trim().lowercase()) {
            "chill"     -> "chill ambient relaxing calm"
            "cosmic"    -> "cosmic space ambient experimental"
            "deep"      -> "deep dub atmospheric low"
            "focus"     -> "instrumental minimal focus"
            "energetic" -> "upbeat electronic energetic"
            else        -> ""
        }
        fetchDiscoverItems(query)
    }

    fun loadOnlineTrack(item: ArchiveItem) {
        viewModelScope.launch {
            _loadingItemId.value = item.id
            _streamError.value   = null
            try {
                val url = InternetArchiveApi.resolveStreamUrl(item.id)
                if (url != null) {
                    val uri = Uri.parse(url)
                    _trackUri.value  = uri
                    _trackName.value = item.title
                    _playbackFraction.value = 0f
                    _currentMood.value = ""
                    _currentAtmosphere.value = VaibAtmosphere.Default
                    trackPrefs.save(uri, item.title)
                    audioPlayer.prepareTrack(uri)
                    navigateTo(Screen.HOME)
                } else {
                    _streamError.value = "Couldn't load this stream. Try another track."
                }
            } catch (_: Exception) {
                _streamError.value = "Couldn't load this stream. Try another track."
            } finally {
                _loadingItemId.value = null
            }
        }
    }

    fun clearStreamError() { _streamError.value = null }

    // ── vAIb save / recall ────────────────────────────────────────────

    fun applyEqPreset(preset: EqPreset) {
        _currentEqPreset.value = preset
        eqController.apply(audioPlayer.audioSessionId, preset)
    }

    fun saveVaib(vaibName: String, mood: String, eqPreset: EqPreset = _currentEqPreset.value) {
        val uri = _trackUri.value ?: return
        val sourceType = if (uri.scheme == "https" || uri.scheme == "http")
            "INTERNET_ARCHIVE" else "LOCAL"
        viewModelScope.launch {
            vaibDao.insert(
                VaibEntity(
                    vaibName        = vaibName.trim().ifEmpty { _trackName.value ?: "Untitled vAIb" },
                    trackUri        = uri.toString(),
                    trackName       = _trackName.value ?: "Unknown Track",
                    mood            = mood.trim(),
                    visualizerStyle = "PULSE",
                    themeId         = "OLED_CYAN",
                    createdAt       = System.currentTimeMillis(),
                    sourceType      = sourceType,
                    eqPreset        = eqPreset.name,
                )
            )
        }
    }

    fun loadVaib(vaib: VaibEntity) {
        val uri = Uri.parse(vaib.trackUri)
        _trackUri.value  = uri
        _trackName.value = vaib.trackName
        _playbackFraction.value = 0f
        trackPrefs.save(uri, vaib.trackName)
        audioPlayer.prepareTrack(uri)
        _currentMood.value = vaib.mood
        _currentAtmosphere.value = VaibAtmosphere.fromMood(vaib.mood)
        val preset = runCatching { EqPreset.valueOf(vaib.eqPreset) }.getOrDefault(EqPreset.FLAT)
        applyEqPreset(preset)
    }

    fun deleteVaib(vaib: VaibEntity) {
        viewModelScope.launch { vaibDao.delete(vaib) }
    }

    // ── Playback / navigation ─────────────────────────────────────────

    fun togglePlayPause() = audioPlayer.togglePlayPause(fallbackUri = _trackUri.value)

    fun startAnalyzer(): Boolean {
        val sessionId = audioPlayer.audioSessionId
        android.util.Log.d("VaibDreamscape", "startAnalyzer() audioSessionId=$sessionId")
        val ok = analyzer.start(sessionId)
        android.util.Log.d("VaibDreamscape", "startAnalyzer() result=$ok")
        return ok
    }

    fun stopAnalyzer() = analyzer.stop()

    fun navigateTo(screen: Screen) {
        _screen.value = screen
    }

    override fun onCleared() {
        super.onCleared()
        analyzer.stop()
        eqController.release()
        audioPlayer.release()
    }
}
