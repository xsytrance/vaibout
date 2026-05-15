package com.xsytrance.vaib.data

import androidx.room.*
import com.xsytrance.vaib.data.entities.TrackEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access for tracks — separate from the legacy VaibDao.
 */
@Dao
interface TrackDao {

    @Query("SELECT * FROM tracks ORDER BY CASE WHEN lastPlayedAt IS NULL THEN 0 ELSE 1 END DESC, lastPlayedAt DESC")
    fun observeAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isFavorite = 1 ORDER BY CASE WHEN lastPlayedAt IS NULL THEN 0 ELSE 1 END DESC, lastPlayedAt DESC")
    fun observeFavorites(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :trackId")
    suspend fun getTrackById(trackId: Long): TrackEntity?

    @Query("SELECT * FROM tracks WHERE uri = :uri LIMIT 1")
    suspend fun findByUri(uri: String): TrackEntity?

    @Insert
    suspend fun insertTrack(track: TrackEntity): Long

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Delete
    suspend fun deleteTrack(track: TrackEntity)

    // ── Stats updates ───────────────────────────────────────

    @Query("UPDATE tracks SET playCount = playCount + 1, lastPlayedAt = :playedAt WHERE id = :trackId")
    suspend fun incrementPlayCount(trackId: Long, playedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tracks SET isFavorite = CASE WHEN isFavorite = 0 THEN 1 ELSE 0 END WHERE id = :trackId")
    suspend fun toggleFavorite(trackId: Long)
}