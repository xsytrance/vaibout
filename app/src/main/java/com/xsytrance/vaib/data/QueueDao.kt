package com.xsytrance.vaib.data

import androidx.room.*
import com.xsytrance.vaib.data.entities.QueueItem
import kotlinx.coroutines.flow.Flow

/**
 * Data access for the playback queue.
 *
 * The queue is persisted so it survives app restarts and background kills.
 */
@Dao
interface QueueDao {

    @Query("SELECT * FROM queue ORDER BY position ASC")
    fun observeQueue(): Flow<List<QueueItem>>

    @Query("SELECT COUNT(*) FROM queue")
    suspend fun getQueueSize(): Int

    @Query("SELECT * FROM queue WHERE position = :position")
    suspend fun getByPosition(position: Int): QueueItem?

    @Query("SELECT * FROM queue WHERE id = :id")
    suspend fun getById(id: Long): QueueItem?

    @Query("SELECT MAX(position) FROM queue")
    suspend fun getMaxPosition(): Int?

    @Insert
    suspend fun insert(item: QueueItem): Long

    @Insert
    suspend fun insertAll(items: List<QueueItem>)

    @Update
    suspend fun update(item: QueueItem)

    @Delete
    suspend fun delete(item: QueueItem)

    @Query("DELETE FROM queue")
    suspend fun clearAll()

    @Query("DELETE FROM queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    // ── Batch position update (for reordering) ─────────────────

    @Query("UPDATE queue SET position = :newPosition WHERE id = :id")
    suspend fun updatePosition(id: Long, newPosition: Int)

    /** Shifts all items after [afterPosition] down by one to make room. */
    @Query("""
        UPDATE queue SET position = position + 1
        WHERE position > :afterPosition
    """)
    suspend fun shiftUp(afterPosition: Int)

    /** Shifts all items between [from] and [to] to close the gap. */
    @Query("""
        UPDATE queue SET position = position - 1
        WHERE position > :from AND position <= :to
    """)
    suspend fun shiftDown(from: Int, to: Int)
}