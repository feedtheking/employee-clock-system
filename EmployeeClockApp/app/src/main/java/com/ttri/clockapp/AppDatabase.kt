package com.ttri.clockapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// ✅ Only keep pending_logs table
@Database(
    entities = [PendingLogEntity::class],
    version = 2,               // bump version since schema changed
    exportSchema = false
)
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
                    "employee_clock_db"
                )
                    .fallbackToDestructiveMigration() // ✅ auto-drop old logs table
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
