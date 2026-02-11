package com.ddn.peedo.project.sapa.model

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SimpleDateFormat")
data class VwAppointedStudent(

    // =====================
    // Appointed Student
    // =====================
    val asid: String,
    val slotID: String,
    val userID: String,

    val appointedDateCreated: String?, // yyyy-MM-dd HH:mm:ss
    val appointedDateUpdated: String?,

    // =====================
    // Slot
    // =====================
    val bookID: String?,
    val dateSlot: String?, // yyyy-MM-dd
    val shiftID: String?,
    val shiftName: String,

    val startTime: String?, // HH:mm:ss
    val endTime: String?,

    val slotStatus: Int?,
    val slotDateCreated: String?,
    val slotDateUpdated: String?,

    // =====================
    // Allocation
    // =====================
    val allocationID: String?,
    val allocation: Int?,
    val allocationStatus: Boolean?,

    // =====================
    // Hospital
    // =====================
    val hospitalID: String?,
    val hospitalName: String,

    // =====================
    // Section
    // =====================
    val sectionID: String?,
    val sectionName: String,

    // =====================
    // User (Student)
    // =====================
    val username: String,
    val fullname: String,
    val email: String,

    val roleID: String,
    val userStatus: Char,
    val schoolID: String?,

    val userDateCreated: String?,
    val userDateUpdated: String?

) : Parcelable {

    constructor(parcel: Parcel) : this(
        asid = parcel.readString() ?: "",
        slotID = parcel.readString() ?: "",
        userID = parcel.readString() ?: "",

        appointedDateCreated = parcel.readString(),
        appointedDateUpdated = parcel.readString(),

        bookID = parcel.readString(),
        dateSlot = parcel.readString(),
        shiftID = parcel.readString(),
        shiftName = parcel.readString() ?: "N/A",

        startTime = parcel.readString(),
        endTime = parcel.readString(),

        slotStatus = parcel.readValue(Int::class.java.classLoader) as? Int,
        slotDateCreated = parcel.readString(),
        slotDateUpdated = parcel.readString(),

        allocationID = parcel.readString(),
        allocation = parcel.readValue(Int::class.java.classLoader) as? Int,
        allocationStatus = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,

        hospitalID = parcel.readString(),
        hospitalName = parcel.readString() ?: "N/A",

        sectionID = parcel.readString(),
        sectionName = parcel.readString() ?: "N/A",

        username = parcel.readString() ?: "",
        fullname = parcel.readString() ?: "",
        email = parcel.readString() ?: "",

        roleID = parcel.readString() ?: "",
        userStatus = parcel.readString()?.firstOrNull() ?: 'P',
        schoolID = parcel.readString(),

        userDateCreated = parcel.readString(),
        userDateUpdated = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(asid)
        parcel.writeString(slotID)
        parcel.writeString(userID)

        parcel.writeString(appointedDateCreated)
        parcel.writeString(appointedDateUpdated)

        parcel.writeString(bookID)
        parcel.writeString(dateSlot)
        parcel.writeString(shiftID)
        parcel.writeString(shiftName)

        parcel.writeString(startTime)
        parcel.writeString(endTime)

        parcel.writeValue(slotStatus)
        parcel.writeString(slotDateCreated)
        parcel.writeString(slotDateUpdated)

        parcel.writeString(allocationID)
        parcel.writeValue(allocation)
        parcel.writeValue(allocationStatus)

        parcel.writeString(hospitalID)
        parcel.writeString(hospitalName)

        parcel.writeString(sectionID)
        parcel.writeString(sectionName)

        parcel.writeString(username)
        parcel.writeString(fullname)
        parcel.writeString(email)

        parcel.writeString(roleID)
        parcel.writeString(userStatus.toString())
        parcel.writeString(schoolID)

        parcel.writeString(userDateCreated)
        parcel.writeString(userDateUpdated)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<VwAppointedStudent> {
        override fun createFromParcel(parcel: Parcel): VwAppointedStudent {
            return VwAppointedStudent(parcel)
        }

        override fun newArray(size: Int): Array<VwAppointedStudent?> {
            return arrayOfNulls(size)
        }
    }
}
