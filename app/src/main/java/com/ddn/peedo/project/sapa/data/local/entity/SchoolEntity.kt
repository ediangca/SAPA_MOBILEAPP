package com.ddn.peedo.project.sapa.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schools")
data class SchoolEntity(
    @PrimaryKey val schoolID: String,
    val schoolName: String?,
    val address: String?,
    val userID: String?,
    val createdBy: String?,
    val status: Int?,
    val code: String?,
    val dateCreated: String?,
    val dateUpdated: String?
)