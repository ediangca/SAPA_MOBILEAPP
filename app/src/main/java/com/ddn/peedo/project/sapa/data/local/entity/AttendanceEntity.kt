package com.ddn.peedo.project.sapa.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance"
)
data class AttendanceEntity(
    @PrimaryKey
    val attID: String,
    val slotID: String,
    val userID: String,
    val status: Int,
    val dateCreated: String?,
    val dateUpdated: String?
)