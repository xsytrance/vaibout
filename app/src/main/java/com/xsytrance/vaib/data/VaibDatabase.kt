package com.xsytrance.vaib.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.xsytrance.vaib.data.entities.TrackEntity
import com.xsytrance.vaib.data.entities.VaibEntity

@Database(
    entities = [TrackEntity::class, VaibEntity::class],
    version  = 5,
    exportSchema = false,
)
abstract class VaibDatabase : RoomDatabase() {

    abstract fun vaibDao(): VaibDao
    abstract fun trackDao(): TrackDao

    companion object {
        @Volatile private var instance: VaibDatabase? = null

        fun get(context: Context): VaibDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VaibDatabase::class.java,
                    "vaib.db",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
