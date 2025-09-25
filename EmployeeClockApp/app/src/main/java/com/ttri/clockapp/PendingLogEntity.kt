package com.ttri.clockapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_logs")
data class PendingLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeID: String,
    val action: String,
    val timestamp: Long,           // epoch ms (UTC)
    val localPhotoPath: String?,   // local JPEG path
    val synced: Boolean = false    // false until uploaded
)
