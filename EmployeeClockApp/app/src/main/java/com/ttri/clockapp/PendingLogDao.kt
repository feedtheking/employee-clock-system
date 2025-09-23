package com.ttri.clockapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PendingLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: PendingLogEntity)

    @Query("SELECT * FROM pending_logs WHERE synced = 0")
    suspend fun getPendingLogs(): List<PendingLogEntity>

    @Update
    suspend fun updateLog(log: PendingLogEntity)

    @Query("DELETE FROM pending_logs WHERE synced = 1")
    suspend fun deleteSyncedLogs()
}
