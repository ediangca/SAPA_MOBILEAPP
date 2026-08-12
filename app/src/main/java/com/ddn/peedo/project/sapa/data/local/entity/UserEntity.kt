package com.ddn.peedo.project.sapa.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userID: String,
    val username: String,
    val lastname: String?,
    val firstname: String?,
    val middlename: String?,
    val fullname: String,
    val email: String,
    val roleID: String,
    val rolename: String,
    val schoolID: String?,
    val schoolName: String?,
    val status: Char?,
    val coorSchoolID: String?,
    val coorSchoolCode: String?,
    val coorSchoolName: String?,
    val hospitalID: String?,
    val hospitalName: String?,
    val dateCreated: String,
    val dateUpdated: String
)