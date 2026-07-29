package com.ddn.peedo.project.sapa.dataclass

import com.ddn.peedo.project.sapa.model.VwSlot

data class HospitalScheduleUi(
    val schoolName: String?,
    val CIName: String?,
    val hospitalName: String?,
    val date: String?,
    val slots: List<VwSlot>
)
