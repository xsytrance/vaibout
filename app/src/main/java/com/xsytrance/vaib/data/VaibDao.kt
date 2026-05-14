package com.xsytrance.vaib.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.xsytrance.vaib.data.entities.VaibEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaibDao {
    @Query("SELECT * FROM vaibs ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<VaibEntity>>

    @Insert
    suspend fun insert(vaib: VaibEntity): Long

    @Delete
    suspend fun delete(vaib: VaibEntity)
}
