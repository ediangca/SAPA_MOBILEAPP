package com.ddn.peedo.project.sapa.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appointed_students")
data class AppointedStudentEntity(
    @PrimaryKey val asid: String,
    val slotID: String,
    val userID: String,
    val appointedDateCreated: String?,
    val appointedDateUpdated: String?,
    val bookID: String?,
    val dateSlot: String?,
    val shiftID: String?,
    val shiftName: String,
    val startTime: String?,
    val endTime: String?,
    val slotStatus: Int?,
    val allocationID: String?,
    val allocation: Int?,
    val allocationStatus: Boolean?,
    val hospitalID: String?,
    val hospitalName: String,
    val sectionID: String?,
    val sectionName: String,
    val username: String,
    val fullname: String,
    val email: String,
    val roleID: String,
    val userStatus: Char,
    val schoolID: String?
)