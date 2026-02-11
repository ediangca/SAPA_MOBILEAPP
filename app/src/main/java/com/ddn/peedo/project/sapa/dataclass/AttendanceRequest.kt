package com.ddn.peedo.project.sapa.dataclass

data class AttendanceRequest(
    val SlotID: String,
    val UserID: String,
    val Status: Int = 1
)
