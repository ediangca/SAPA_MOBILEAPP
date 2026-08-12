package com.ddn.peedo.project.sapa.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "slots")
data class SlotEntity(
    @PrimaryKey val slotID: String,
    val bookID: String,
    val dateSlot: String?,
    val shiftID: String,
    val shiftName: String,
    val startTime: String?,
    val endTime: String?,
    val slotStatus: Int?,
    val hospitalID: String,
    val hospitalName: String,
    val sectionID: String,
    val sectionName: String,
    val allocationID: String,
    val allocation: Int?,
    val allocationStatus: Boolean?,
    val isTimeRestricted: Boolean?,
    val userID: String,
    val fullname: String,
    val schoolID: String?,
    val schoolName: String?,
    val CIID: String?,
    val ci_fullname: String?,
    val isCIPresent: Int?,
    val date_Created: String,
    val date_Updated: String
)