package com.example.screentimetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Single Room database for the app. `fallbackToDestructiveMigration`
 * is deliberately NOT used here -- session history must never be
 * silently wiped by a future schema change without an explicit,
 * reviewed migration.
 */
@Database(entities = [SessionEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "screen_time_sessions.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
