package com.ttri.clockapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeID: String,
    val action: String,
    val timestamp: Long,
    val photoPath: String? = null,  // local file path of photo
    val synced: Boolean = false     // false = pending sync
)
