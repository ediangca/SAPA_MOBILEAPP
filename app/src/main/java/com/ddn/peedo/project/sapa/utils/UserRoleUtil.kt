package com.ddn.peedo.project.sapa.util

object UserRoleUtil {
    const val SYSTEM_ADMIN = "UGR0000"
    const val ADMIN = "UGR0001"
    const val SCHOOL_COORDINATOR = "UGR0003" // guessed from your BackgroundService memory notes
    const val STUDENT = "UGR0004" // guessed from your BackgroundService memory notes

    const val HOSPITAL_SUPERVISOR = "UGR0001"
    const val CLINICAL_INSTRUCTOR = "UGR0006" // guessed from your BackgroundService memory notes

    val adminTierRoles = setOf(SYSTEM_ADMIN, ADMIN)
    val schoolScopedRoles = setOf(SCHOOL_COORDINATOR, CLINICAL_INSTRUCTOR, STUDENT)


    val analyticsVisibleRoles = setOf(
        SYSTEM_ADMIN, ADMIN, SCHOOL_COORDINATOR, CLINICAL_INSTRUCTOR, HOSPITAL_SUPERVISOR
    )

    fun canViewAnalytics(roleId: String?): Boolean =
        roleId != null && roleId in analyticsVisibleRoles
}