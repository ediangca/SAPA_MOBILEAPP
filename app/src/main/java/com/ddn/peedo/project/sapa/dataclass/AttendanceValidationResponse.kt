package com.ddn.peedo.project.sapa.dataclass

data class AttendanceValidationResponse(
    val hasAttendance: Boolean,
    val attendance: AttendanceResponse? // nullable
)
