package com.xsytrance.vaib.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.xsytrance.vaib.data.entities.QueueItem
import com.xsytrance.vaib.data.entities.StationEntity
import com.xsytrance.vaib.data.entities.StationTrackCrossRef
import com.xsytrance.vaib.data.entities.TrackEntity
import com.xsytrance.vaib.data.entities.VaibEntity

/**
 * Central Room database for vAIb.
 *
 * Version history:
 *   1 — Initial (VaibEntity only)
 *   2 — Added TrackEntity
 *   3 — Expanded TrackEntity fields
 *   4 — Hardened schema
 *   5 — Added StationEntity, StationTrackCrossRef, expanded TrackEntity
 *   6 — Added QueueItem for playback queue
 */
@Database(
    entities = [
        VaibEntity::class,
        TrackEntity::class,
        StationEntity::class,
        StationTrackCrossRef::class,
        QueueItem::class,
    ],
    version = 6,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
    ],
)
abstract class VaibDatabase : RoomDatabase() {

    abstract fun vaibDao(): VaibDao
    abstract fun trackDao(): TrackDao
    abstract fun stationDao(): StationDao
    abstract fun queueDao(): QueueDao

    companion object {
        @Volatile private var instance: VaibDatabase? = null

        fun get(context: Context): VaibDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VaibDatabase::class.java,
                    "vaib.db",
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}