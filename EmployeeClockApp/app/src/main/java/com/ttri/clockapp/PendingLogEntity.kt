package com.ttri.clockapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_logs")
data class PendingLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeID: String,
    val action: String,
    val timestamp: Long,           // store local timestamp (ms since epoch)
    val localPhotoPath: String?,   // path to local JPEG file
    val synced: Boolean = false    // mark if already uploaded to Firebase
)
