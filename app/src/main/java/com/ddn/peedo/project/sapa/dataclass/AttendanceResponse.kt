package com.ddn.peedo.project.sapa.model

import com.google.gson.annotations.SerializedName

data class AttendanceResponse(

    @SerializedName("attid")
    val attID: String?,

    @SerializedName("slotID")
    val slotID: String,

    @SerializedName("userID")
    val userID: String,

    @SerializedName("status")
    val status: Int,

    @SerializedName("dateCreated")
    val dateCreated: String?,

    @SerializedName("dateUpdated")
    val dateUpdated: String?
)

// Convert API response → Room entity
//fun AttendanceResponse.toEntity(): AttendanceEntity {
//
//    return AttendanceEntity(
//        attID = ATTID,
//        slotID = SlotID,
//        userID = UserID,
//        status = Status,
//        dateCreated = null,
//        dateUpdated = null
//    )
//}