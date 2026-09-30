package com.ddn.peedo.project.sapa.util

object UserRoleUtil {
    const val SYSTEM_ADMIN = "UGR0000"
    const val ADMIN = "UGR0001"
    const val SAP_ADMIN = "UGR0002"
    const val SCHOOL_COORDINATOR = "UGR0003" // guessed from your BackgroundService memory notes
    const val STUDENT = "UGR0004" // guessed from your BackgroundService memory notes

    const val HOSPITAL_SUPERVISOR = "UGR0001"
    const val CLINICAL_INSTRUCTOR = "UGR0006" // guessed from your BackgroundService memory notes

    val adminTierRoles = setOf(SYSTEM_ADMIN, ADMIN)
    val schoolScopedRoles = setOf(SCHOOL_COORDINATOR, CLINICAL_INSTRUCTOR, STUDENT)

    /**
     * Roles allowed to open the Users directory tab.
     */
    val usersDirectoryRoles = setOf(
        SYSTEM_ADMIN, ADMIN, SAP_ADMIN, SCHOOL_COORDINATOR, CLINICAL_INSTRUCTOR
    )

    /**
     * Who each role sees in the Users directory (before the school scoping
     * applied in UsersFragment):
     *  - System Admin / SAP Admin -> everyone (admin tier, no restriction)
     *  - School Coordinator (UGR0003) -> CIs + students of their school,
     *    combined into one list
     *  - Clinical Instructor (UGR0006) -> students of their school only
     */
    val usersDirectoryVisibleRoles = mapOf(
        SCHOOL_COORDINATOR to setOf(CLINICAL_INSTRUCTOR, STUDENT),
        CLINICAL_INSTRUCTOR to setOf(STUDENT)
    )


    val analyticsVisibleRoles = setOf(
        SYSTEM_ADMIN, ADMIN, SCHOOL_COORDINATOR, CLINICAL_INSTRUCTOR, HOSPITAL_SUPERVISOR
    )

    fun canViewAnalytics(roleId: String?): Boolean =
        roleId != null && roleId in analyticsVisibleRoles
}