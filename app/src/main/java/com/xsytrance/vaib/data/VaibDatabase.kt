package com.xsytrance.vaib.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
 */
@Database(
    entities = [
        VaibEntity::class,
        TrackEntity::class,
        StationEntity::class,
        StationTrackCrossRef::class,
    ],
    version = 5,
    exportSchema = true,  // Enable schema exports for migration safety
    autoMigrations = [
        // v4 → v5: Add stations and enrich tracks
        AutoMigration(from = 4, to = 5)
    ],
)
abstract class VaibDatabase : RoomDatabase() {

    abstract fun vaibDao(): VaibDao
    abstract fun trackDao(): TrackDao
    abstract fun stationDao(): StationDao

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
                            // Pre-populate default stations on first run
                            populateDefaults(context)
                        }
                    })
                    .fallbackToDestructiveMigration()  // Dev phase — replace with migrations before release
                    .build()
                    .also { instance = it }
            }

        /**
         * Seed the app with default preset stations on first launch.
         */
        private fun populateDefaults(context: Context) {
            // Done async via coroutine — triggered from Application or ViewModel
        }
    }
}