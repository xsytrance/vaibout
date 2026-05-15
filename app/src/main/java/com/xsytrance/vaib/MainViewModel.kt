package com.xsytrance.vaib

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xsytrance.vaib.audio.AudioPlayer
import com.xsytrance.vaib.audio.AudioVisualizerAnalyzer
import com.xsytrance.vaib.audio.EqController
import com.xsytrance.vaib.audio.EqPreset
import com.xsytrance.vaib.data.TrackPrefs
import com.xsytrance.vaib.data.VaibDatabase
import com.xsytrance.vaib.data.entities.QueueItem
import com.xsytrance.vaib.data.entities.VaibEntity
import com.xsytrance.vaib.discover.ArchiveItem
import com.xsytrance.vaib.discover.DiscoverUiState
import com.xsytrance.vaib.discover.InternetArchiveApi
import com.xsytrance.vaib.repository.MusicRepository
import com.xsytrance.vaib.service.AudioFocusManager
import com.xsytrance.vaib.service.PlayerService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen { HOME, SOLO_DREAMSCAPE, DISCOVER, STATIONS, NOW_PLAYING }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository   = MusicRepository(application)
    private val audioPlayer  = AudioPlayer(application)
    private val trackPrefs   = TrackPrefs(application)
    private val analyzer     = AudioVisualizerAnalyzer()
    private val eqController = EqController()
    private val vaibDao      = VaibDatabase.get(application).vaibDao()
    private val focusManager = AudioFocusManager(application)

    init {
        focusManager.bindPlayer(audioPlayer)
    }

    // ── Reactive state ───────────────────────────────────────

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

    // ── Queue ─────────────────────────────────────────────────

    val queue = repository.allTracks  // Initially just the full library
    private var queueUrns: List<String> = emptyList()
    private var currentQueueIndex = -1

    // ── Discover ──────────────────────────────────────────────

    private val _discoverState = MutableStateFlow<DiscoverUiState>(DiscoverUiState.Loading)
    val discoverState: StateFlow<DiscoverUiState> = _discoverState.asStateFlow()

    private val _loadingItemId = MutableStateFlow<String?>(null)
    val loadingItemId: StateFlow<String?> = _loadingItemId.asStateFlow()

    private val _streamError = MutableStateFlow<String?>(null)
    val streamError: StateFlow<String?> = _streamError.asStateFlow()

    // ── Saved vAIbs ───────────────────────────────────────────

    val savedVaibs = vaibDao.observeAll()
        .stateIn(viewModelScope, com.xsytrance.vaib.data.SharingStarted.Eagerly, emptyList())

    init {
        restorePersistedTrack(application)
        startPositionTicker()
        observePlaybackEnd()
    }

    // ── Track restore ─────────────────────────────────────────

    private fun restorePersistedTrack(application: Application) {
        val savedUri  = trackPrefs.loadUri()  ?: return
        val savedName = trackPrefs.loadName() ?: return

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
        if (!isRemote) {
            audioPlayer.prepareTrack(savedUri)
        }
    }

    // ── Position ticker ───────────────────────────────────────

    private fun observePlaybackEnd() {
        viewModelScope.launch {
            audioPlayer.isEnded.collect { ended ->
                if (ended) {
                    _playbackFraction.value = 0f
                    // Auto-advance to next in queue
                    if (currentQueueIndex < queueUrns.size - 1) {
                        loadQueueItem(currentQueueIndex + 1)
                    }
                }
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

    // ── Local track ───────────────────────────────────────────

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
        trackPrefs.save(uri, cleanName)
        audioPlayer.loadTrack(uri)

        // Record play stat
        viewModelScope.launch {
            val trackId = repository.findOrCreateTrack(uri.toString(), cleanName)
            repository.recordPlay(trackId)
        }
    }

    // ── Queue management ─────────────────────────────────────

    /** Build a queue from a list of URIs and start playing from startIndex. */
    fun loadQueue(uris: List<Uri>, startFrom: Int = 0) {
        queueUrns = uris.map { it.toString() }
        currentQueueIndex = startFrom
        audioPlayer.loadQueue(uris, startFrom)
    }

    /** Advance to next item in queue with crossfade. */
    private fun loadQueueItem(index: Int) {
        if (index in queueUrns.indices) {
            currentQueueIndex = index
            _trackUri.value = Uri.parse(queueUrns[index])
            // TODO: resolve track name from DB or metadata
            audioPlayer.togglePlayPause()  // Will handle STATE_IDLE → play
        }
    }

    fun skipNext() {
        if (currentQueueIndex < queueUrns.size - 1) {
            loadQueueItem(currentQueueIndex + 1)
        }
    }

    fun skipPrevious() {
        if (currentQueueIndex > 0) {
            loadQueueItem(currentQueueIndex - 1)
        }
    }

    /** Update queue order after drag-and-drop reorder. */
    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val mutable = queueUrns.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        queueUrns = mutable
        // Adjust current index if it was affected
        currentQueueIndex = when {
            currentQueueIndex == fromIndex -> toIndex
            currentQueueIndex in minOf(fromIndex, toIndex) + 1 until maxOf(fromIndex, toIndex) ->
                if (fromIndex < toIndex) currentQueueIndex - 1 else currentQueueIndex + 1
            else -> currentQueueIndex
        }
    }

    // ── Discover / online track ───────────────────────────────

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
                    trackPrefs.save(uri, item.title)
                    audioPlayer.prepareTrack(uri)
                    // Record play stat
                    val trackId = repository.findOrCreateTrack(uri.toString(), item.title)
                    repository.recordPlay(trackId)
                    // Start foreground service
                    PlayerService.start(getApplication(), item.title)
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

    // ── Audio focus ───────────────────────────────────────────

    fun requestAudioFocus(): Boolean = focusManager.requestFocus()
    fun abandonAudioFocus()        = focusManager.abandonFocus()

    // ── vAIb save / recall ────────────────────────────────────

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
                    themeId         = "NEON_CYAN",
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
        val preset = runCatching { EqPreset.valueOf(vaib.eqPreset) }.getOrDefault(EqPreset.FLAT)
        applyEqPreset(preset)
        PlayerService.start(getApplication(), vaib.trackName)
    }

    fun deleteVaib(vaib: VaibEntity) {
        viewModelScope.launch { vaibDao.delete(vaib) }
    }

    // ── Playback / navigation ─────────────────────────────────

    fun togglePlayPause() {
        if (!audioPlayer.player.isPlaying && !focusManager.requestFocus()) return
        audioPlayer.togglePlayPause(fallbackUri = _trackUri.value)
        if (audioPlayer.player.isPlaying) {
            PlayerService.start(getApplication(), _trackName.value ?: "vAIb out!")
        } else {
            PlayerService.update(getApplication(), _trackName.value ?: "vAIb out!", false)
        }
    }

    fun startAnalyzer(): Boolean {
        val sessionId = audioPlayer.audioSessionId
        Log.d("VaibDreamscape", "startAnalyzer() audioSessionId=$sessionId")
        val ok = analyzer.start(sessionId)
        Log.d("VaibDreamscape", "startAnalyzer() result=$ok")
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
        focusManager.abandonFocus()
        PlayerService.stop(getApplication())
    }
}