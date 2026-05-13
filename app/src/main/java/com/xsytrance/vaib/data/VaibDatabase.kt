package com.xsytrance.vaib.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.xsytrance.vaib.data.entities.TrackEntity
import com.xsytrance.vaib.data.entities.VaibEntity

@Database(
    entities = [TrackEntity::class, VaibEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VaibDatabase : RoomDatabase()
