package com.ddn.peedo.project.sapa.dataclass

import com.ddn.peedo.project.sapa.model.AttendanceResponse

data class AttendanceValidationResponse(
    val hasAttendance: Boolean,
    val attendance: AttendanceResponse? // nullable
)
