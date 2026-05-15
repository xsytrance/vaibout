package com.xsytrance.vaib.repository

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.xsytrance.vaib.data.StationDao
import com.xsytrance.vaib.data.TrackDao
import com.xsytrance.vaib.data.VaibDatabase
import com.xsytrance.vaib.data.entities.StationEntity
import com.xsytrance.vaib.data.entities.StationTrackCrossRef
import com.xsytrance.vaib.data.entities.TrackEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Central repository — single source of truth for all data access.
 *
 * Mediates between local Room storage and remote sources (Internet Archive).
 * ViewModels call this, never the DAOs directly.
 */
class MusicRepository(context: Context) {

    private val db = VaibDatabase.get(context)
    private val trackDao: TrackDao = db.trackDao()
    private val stationDao: StationDao = db.stationDao()

    // ═══════════════════════════════════════════════════════
    //  Tracks
    // ═══════════════════════════════════════════════════════

    val allTracks: Flow<List<TrackEntity>> = trackDao.observeAllTracks()
    val favoriteTracks: Flow<List<TrackEntity>> = trackDao.observeFavorites()

    suspend fun getTrackById(id: Long): TrackEntity? = trackDao.getTrackById(id)

    suspend fun findOrCreateTrack(uri: String, title: String, artist: String? = null): Long {
        val existing = trackDao.findByUri(uri)
        if (existing != null) return existing.id

        val track = TrackEntity(
            title = title,
            uri = uri,
            artist = artist,
            sourceType = if (uri.startsWith("http")) "INTERNET_ARCHIVE" else "LOCAL",
            lastPlayedAt = System.currentTimeMillis(),
        )
        return trackDao.insertTrack(track)
    }

    suspend fun recordPlay(trackId: Long) {
        trackDao.incrementPlayCount(trackId)
    }

    suspend fun toggleFavorite(trackId: Long) = trackDao.toggleFavorite(trackId)

    suspend fun deleteTrack(trackId: Long) {
        val track = trackDao.getTrackById(trackId) ?: return
        trackDao.deleteTrack(track)
    }

    // ═══════════════════════════════════════════════════════
    //  Stations
    // ═══════════════════════════════════════════════════════

    val allStations: Flow<List<StationEntity>> = stationDao.observeAllStations()

    suspend fun getStationById(id: Long): StationEntity? = stationDao.getStationById(id)

    suspend fun createStation(
        name: String,
        description: String = "",
        icon: String = "🎵",
        themeOrdinal: Int = 0,
        sourceType: String = "MIXED",
    ): Long {
        val existing = stationDao.observeAllStations().first()
        val sortOrder = existing.size
        val entity = StationEntity(
            name = name,
            description = description,
            icon = icon,
            themeOrdinal = themeOrdinal,
            sourceType = sourceType,
            sortOrder = sortOrder,
        )
        return stationDao.insertStation(entity)
    }

    suspend fun updateStation(station: StationEntity) = stationDao.updateStation(station)

    suspend fun deleteStation(stationId: Long) {
        val station = stationDao.getStationById(stationId) ?: return
        // Remove all cross-references first
        stationDao.observeTracksForStation(stationId).first().forEach { track ->
            stationDao.removeTrackFromStation(stationId, track.id)
        }
        stationDao.deleteStation(station)
    }

    suspend fun reorderStation(stationId: Long, newOrder: Int) {
        val station = stationDao.getStationById(stationId) ?: return
        stationDao.updateStation(station.copy(sortOrder = newOrder))
    }

    // ═══════════════════════════════════════════════════════
    //  Station Membership
    // ═══════════════════════════════════════════════════════

    fun tracksForStation(stationId: Long) = stationDao.observeTracksForStation(stationId)

    fun isTrackInStation(stationId: Long, trackId: Long): Flow<Boolean> {
        // Wrap Int result in Flow for reactive UI
        return kotlinx.coroutines.flow.flow {
            val count = stationDao.isTrackInStation(stationId, trackId)
            emit(count > 0)
        }
    }

    suspend fun addTrackToStation(stationId: Long, trackId: Long): Boolean {
        if (stationDao.isTrackInStation(stationId, trackId) > 0) return false
        stationDao.addTrackToStation(StationTrackCrossRef(
            stationId = stationId,
            trackId = trackId,
        ))
        return true
    }

    suspend fun removeTrackFromStation(stationId: Long, trackId: Long) {
        stationDao.removeTrackFromStation(stationId, trackId)
    }

    suspend fun trackCountForStation(stationId: Long): Int =
        stationDao.getTrackCountForStation(stationId)
}