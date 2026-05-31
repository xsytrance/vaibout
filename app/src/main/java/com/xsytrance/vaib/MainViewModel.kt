package com.xsytrance.vaib

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xsytrance.vaib.audio.AudioPlayer
import com.xsytrance.vaib.audio.AudioVisualizerAnalyzer
import com.xsytrance.vaib.audio.EqController
import com.xsytrance.vaib.audio.EqPreset
import com.xsytrance.vaib.core.design.DefaultTrackPalette
import com.xsytrance.vaib.core.design.TrackPalette
import com.xsytrance.vaib.core.design.toTrackPalette
import com.xsytrance.vaib.data.VaibDatabase
import com.xsytrance.vaib.data.entities.VaibEntity
import com.xsytrance.vaib.music.R2Repository
import com.xsytrance.vaib.music.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { LIBRARY, NOW_PLAYING, VISUALIZER }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val audioPlayer    = AudioPlayer(application)
    private val analyzer       = AudioVisualizerAnalyzer()
    private val eqController   = EqController()
    private val vaibDao        = VaibDatabase.get(application).vaibDao()
    private val r2Repo         = R2Repository(application)

    // ── Audio analysis ──────────────────────────────────────────────────
    val audioEnergy    = analyzer.energy
    val audioBeatPulse = analyzer.beatPulse
    val audioFreqBands = analyzer.freqBands

    // ── Playback ────────────────────────────────────────────────────────
    val isPlaying:   StateFlow<Boolean> = audioPlayer.isPlaying
    val isBuffering: StateFlow<Boolean> = audioPlayer.isBuffering

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _playbackFraction = MutableStateFlow(0f)
    val playbackFraction: StateFlow<Float> = _playbackFraction.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    // ── Library ─────────────────────────────────────────────────────────
    val tracks: StateFlow<List<Track>> = r2Repo.observeTracks()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _isRefreshing = MutableStateFlow(true)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError.asStateFlow()

    // ── Queue ───────────────────────────────────────────────────────────
    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private var queueIndex = -1

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatEnabled = MutableStateFlow(false)
    val repeatEnabled: StateFlow<Boolean> = _repeatEnabled.asStateFlow()

    // ── EQ ──────────────────────────────────────────────────────────────
    private val _currentEqPreset = MutableStateFlow(EqPreset.FLAT)
    val currentEqPreset: StateFlow<EqPreset> = _currentEqPreset.asStateFlow()

    // ── Palette / theming ───────────────────────────────────────────────
    private val _trackPalette = MutableStateFlow(DefaultTrackPalette)
    val trackPalette: StateFlow<TrackPalette> = _trackPalette.asStateFlow()

    private val _artworkBytes = MutableStateFlow<ByteArray?>(null)
    val artworkBytes: StateFlow<ByteArray?> = _artworkBytes.asStateFlow()

    // ── vAIbs ───────────────────────────────────────────────────────────
    val savedVaibs: StateFlow<List<VaibEntity>> = vaibDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Navigation ──────────────────────────────────────────────────────
    private val _screen = MutableStateFlow(Screen.LIBRARY)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    init {
        startPositionTicker()
        observePlaybackEnd()
        observeMediaMetadata()
        refreshLibrary()
    }

    // ── Library ─────────────────────────────────────────────────────────

    fun refreshLibrary() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _refreshError.value = null
            r2Repo.refresh()
                .onFailure { _refreshError.value = "Couldn't load library. Check your connection." }
            _isRefreshing.value = false
        }
    }

    fun clearRefreshError() { _refreshError.value = null }

    // ── Playback ─────────────────────────────────────────────────────────

    fun playTrack(track: Track, fromQueue: List<Track> = emptyList()) {
        val q = fromQueue.ifEmpty { _queue.value.ifEmpty { tracks.value } }
        _queue.value = q
        queueIndex   = q.indexOfFirst { it.key == track.key }
        _currentTrack.value        = track
        _playbackFraction.value    = 0f
        _currentPositionMs.value   = 0L
        _durationMs.value          = 0L
        audioPlayer.loadTrack(Uri.parse(track.url))
    }

    fun skipNext() {
        val q = _queue.value
        if (q.isEmpty()) return
        val next = if (_shuffleEnabled.value) (0 until q.size).random()
                   else queueIndex + 1
        when {
            next < q.size      -> playTrack(q[next], q)
            _repeatEnabled.value -> playTrack(q[0], q)
        }
    }

    fun skipPrev() {
        if (audioPlayer.currentPositionMs > 3_000L) {
            audioPlayer.player.seekTo(0); return
        }
        val q    = _queue.value
        val prev = (queueIndex - 1).coerceAtLeast(0)
        if (q.isNotEmpty()) playTrack(q[prev], q)
    }

    fun togglePlayPause() =
        audioPlayer.togglePlayPause(fallbackUri = _currentTrack.value?.url?.let { Uri.parse(it) })

    fun seekTo(fraction: Float) {
        val dur = audioPlayer.durationMs
        if (dur > 0) audioPlayer.player.seekTo((dur * fraction).toLong())
    }

    fun toggleShuffle() { _shuffleEnabled.value = !_shuffleEnabled.value }
    fun toggleRepeat()  { _repeatEnabled.value  = !_repeatEnabled.value  }

    // ── EQ ───────────────────────────────────────────────────────────────

    fun applyEqPreset(preset: EqPreset) {
        _currentEqPreset.value = preset
        eqController.apply(audioPlayer.audioSessionId, preset)
    }

    // ── Analyzer ─────────────────────────────────────────────────────────

    fun startAnalyzer(): Boolean = analyzer.start(audioPlayer.audioSessionId)
    fun stopAnalyzer()           = analyzer.stop()

    // ── vAIbs ────────────────────────────────────────────────────────────

    fun saveVaib(name: String, mood: String, preset: EqPreset = _currentEqPreset.value) {
        val track = _currentTrack.value ?: return
        viewModelScope.launch {
            vaibDao.insert(
                VaibEntity(
                    vaibName        = name.trim().ifEmpty { track.title },
                    trackUri        = track.url,
                    trackName       = track.title,
                    mood            = mood.trim(),
                    visualizerStyle = "AURA",
                    themeId         = "DYNAMIC",
                    createdAt       = System.currentTimeMillis(),
                    sourceType      = "R2",
                    eqPreset        = preset.name,
                )
            )
        }
    }

    fun deleteVaib(vaib: VaibEntity) {
        viewModelScope.launch { vaibDao.delete(vaib) }
    }

    fun loadVaib(vaib: VaibEntity) {
        val track = tracks.value.firstOrNull { it.url == vaib.trackUri }
        if (track != null) {
            playTrack(track)
        } else {
            val synthetic = Track(
                key = vaib.trackUri, url = vaib.trackUri, lrcUrl = null,
                title = vaib.trackName, artist = "xsytrance",
                albumArtUrl = null, tags = emptyList(), lyrics = null, bpm = null,
            )
            playTrack(synthetic)
        }
        val preset = runCatching { EqPreset.valueOf(vaib.eqPreset) }.getOrDefault(EqPreset.FLAT)
        applyEqPreset(preset)
    }

    // ── Navigation ───────────────────────────────────────────────────────

    fun navigateTo(screen: Screen) { _screen.value = screen }

    // ── Internal ─────────────────────────────────────────────────────────

    private fun observePlaybackEnd() {
        viewModelScope.launch {
            audioPlayer.isEnded.collect { ended -> if (ended) skipNext() }
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
                                (audioPlayer.currentPositionMs.toFloat() / dur).coerceIn(0f, 1f)
                            _currentPositionMs.value = audioPlayer.currentPositionMs.coerceAtLeast(0)
                            _durationMs.value        = dur
                        }
                        delay(250)
                    }
                }
            }
        }
    }

    private fun observeMediaMetadata() {
        viewModelScope.launch {
            audioPlayer.mediaMetadata.collect { meta ->
                val bytes = meta.artworkData
                _artworkBytes.value = bytes
                if (bytes != null) {
                    withContext(Dispatchers.Default) {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            ?.toTrackPalette()
                            ?.let { _trackPalette.value = it }
                    }
                } else {
                    _trackPalette.value = DefaultTrackPalette
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        analyzer.stop()
        eqController.release()
        audioPlayer.release()
    }
}
