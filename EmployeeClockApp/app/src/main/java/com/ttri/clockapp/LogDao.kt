package com.ttri.clockapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LogDao {
    @Insert
    suspend fun insert(log: LogEntity)

    @Query("SELECT * FROM logs")
    suspend fun getAll(): List<LogEntity>

    @Query("SELECT * FROM logs WHERE synced = 0")
    suspend fun getUnsynced(): List<LogEntity>

    @Query("UPDATE logs SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Int)
}
