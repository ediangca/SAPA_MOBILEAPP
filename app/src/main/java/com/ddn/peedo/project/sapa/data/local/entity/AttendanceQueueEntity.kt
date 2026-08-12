package com.ddn.peedo.project.sapa.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_queue")
data class AttendanceQueueEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val slotID: String,
    val userID: String,
    val status: Int = 1,
    val scannedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val syncAttempts: Int = 0,
    val lastError: String? = null
)