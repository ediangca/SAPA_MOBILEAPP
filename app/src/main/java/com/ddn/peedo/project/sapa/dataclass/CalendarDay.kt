package com.ddn.peedo.project.sapa.dataclass

import java.time.LocalDate

data class CalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val schoolIds: List<String>,
    val isToday: Boolean = false,
    val isSelected: Boolean = false
)

