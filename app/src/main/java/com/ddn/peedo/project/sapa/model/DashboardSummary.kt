package com.ddn.peedo.project.sapa.model

data class DashboardSummary(

    val totalSlots: Int = 0,
    val pendingSchedule: Int = 0,
    val confirmedSchedule: Int = 0,
    val declinedSchedule: Int = 0,
    val cancellationRequest: Int = 0,
    val cancelledSchedule: Int = 0,

    val totalAppointedStudents: Int = 0,
    val totalAttendances: Int = 0,

    val actualRevenue: Double = 0.0,
    val potentialRevenue: Double = 0.0,

    val attendanceRatePercent: Double = 0.0,
    val slotUtilizationPercent: Double = 0.0,

    val futureSlots: Int = 0,
    val pastSlots: Int = 0
)