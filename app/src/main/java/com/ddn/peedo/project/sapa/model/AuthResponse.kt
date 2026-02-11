package com.ddn.peedo.project.sapa.model

data class AuthResponse(
    val token: String,
    val userID: String,
    val roleID: String,
    val message: String
){
    override fun toString(): String {
        return "Token: $token, UserID: $userID, roleID: $roleID, message: $message"
    }
}
