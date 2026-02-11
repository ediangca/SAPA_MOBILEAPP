package com.ddn.peedo.project.sapa.model

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SimpleDateFormat")
data class VwSlot(
    val slotID: String,
    val bookID: String,
    val dateSlot: String?,   // yyyy-MM-dd
    val shiftID: String,
    val shiftName: String,
    val startTime: String?, // HH:mm:ss
    val endTime: String?,
    val slotStatus: Int?,
    val hospitalID: String,
    val hospitalName: String,
    val sectionID: String,
    val sectionName: String,
    val allocationID: String,
    val allocation: Int?,
    val allocationStatus: Boolean?,
    val userID: String,
    val fullname: String,
    val schoolID: String?,
    val schoolName: String?
) : Parcelable {

    constructor(parcel: Parcel) : this(
        slotID = parcel.readString() ?: "",
        bookID = parcel.readString() ?: "",
        dateSlot = parcel.readString(),
        shiftID = parcel.readString() ?: "",
        shiftName = parcel.readString() ?: "",
        startTime = parcel.readString(),
        endTime = parcel.readString(),
        slotStatus = parcel.readValue(Int::class.java.classLoader) as? Int,
        hospitalID = parcel.readString() ?: "",
        hospitalName = parcel.readString() ?: "",
        sectionID = parcel.readString() ?: "",
        sectionName = parcel.readString() ?: "",
        allocationID = parcel.readString() ?: "",
        allocation = parcel.readValue(Int::class.java.classLoader) as? Int,
        allocationStatus = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        userID = parcel.readString() ?: "",
        fullname = parcel.readString() ?: "",
        schoolID = parcel.readString(),
        schoolName = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(slotID)
        parcel.writeString(bookID)
        parcel.writeString(dateSlot)
        parcel.writeString(shiftID)
        parcel.writeString(shiftName)
        parcel.writeString(startTime)
        parcel.writeString(endTime)
        parcel.writeValue(slotStatus)
        parcel.writeString(hospitalID)
        parcel.writeString(hospitalName)
        parcel.writeString(sectionID)
        parcel.writeString(sectionName)
        parcel.writeString(allocationID)
        parcel.writeValue(allocation)
        parcel.writeValue(allocationStatus)
        parcel.writeString(userID)
        parcel.writeString(fullname)
        parcel.writeString(schoolID)
        parcel.writeString(schoolName)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<VwSlot> {
        override fun createFromParcel(parcel: Parcel): VwSlot {
            return VwSlot(parcel)
        }

        override fun newArray(size: Int): Array<VwSlot?> {
            return arrayOfNulls(size)
        }
    }
}

