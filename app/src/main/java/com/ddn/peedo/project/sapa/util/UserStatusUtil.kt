package com.ddn.peedo.project.sapa.util

object UserStatusUtil {
    const val UNVERIFIED = 'U'
    const val PENDING = 'P'
    const val APPROVED = 'A'
    const val SUSPENDED = 'S'
    const val INACTIVE = 'I'

    fun label(status: Char?): String = when (status) {
        UNVERIFIED -> "Unverified"
        PENDING -> "Pending"
        APPROVED -> "Approved"
        SUSPENDED -> "Suspended"
        INACTIVE -> "Inactive"
        else -> "Unknown"
    }
}