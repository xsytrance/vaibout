package com.xsytrance.vaib.data

import androidx.room.*
import com.xsytrance.vaib.data.entities.StationEntity
import com.xsytrance.vaib.data.entities.StationTrackCrossRef
import com.xsytrance.vaib.data.entities.TrackEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access for stations, track memberships, and enhanced track queries.
 */
@Dao
interface StationDao {

    // ── Station CRUD ────────────────────────────────────────

    @Query("SELECT * FROM stations ORDER BY sortOrder ASC, createdAt DESC")
    fun observeAllStations(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE id = :stationId")
    suspend fun getStationById(stationId: Long): StationEntity?

    @Insert
    suspend fun insertStation(station: StationEntity): Long

    @Update
    suspend fun updateStation(station: StationEntity)

    @Delete
    suspend fun deleteStation(station: StationEntity)

    // ── Station membership ──────────────────────────────────

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN station_tracks st ON t.id = st.trackId
        WHERE st.stationId = :stationId
        ORDER BY st.addedAt DESC
    """)
    fun observeTracksForStation(stationId: Long): Flow<List<TrackEntity>>

    @Insert
    suspend fun addTrackToStation(crossRef: StationTrackCrossRef)

    @Query("DELETE FROM station_tracks WHERE stationId = :stationId AND trackId = :trackId")
    suspend fun removeTrackFromStation(stationId: Long, trackId: Long)

    @Query("SELECT COUNT(*) FROM station_tracks WHERE stationId = :stationId")
    suspend fun getTrackCountForStation(stationId: Long): Int

    // ── Membership checks ───────────────────────────────────

    @Query("SELECT COUNT(*) FROM station_tracks WHERE stationId = :stationId AND trackId = :trackId")
    suspend fun isTrackInStation(stationId: Long, trackId: Long): Int
}