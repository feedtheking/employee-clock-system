package com.ttri.clockapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// ✅ Only PendingLogEntity is stored locally
@Database(entities = [PendingLogEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pendingLogDao(): PendingLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "clockapp.db" // ✅ one DB file for the app
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
