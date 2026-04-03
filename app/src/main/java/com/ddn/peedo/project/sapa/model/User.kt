package com.ddn.peedo.project.sapa.model

data class User(
    val userID: String,
    val username: String,
    val password: String?,
    val lastname: String,
    val firstname: String,
    val middlename: String?,
    val fullname: String,
    val email: String,
    val roleID: String,
    val rolename: String,
    val schoolID: String?,
    val schoolName: String?,
    val status: String,
    val hospitalName: String?
)