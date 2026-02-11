package com.ddn.peedo.project.sapa.model


import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SimpleDateFormat")
data class Slot(
    var allocationID: String? = "",
    var sectionID: String? = null,
    var sectionName: String? = null,
    var hospitalID: String? = null,
    var hospitalName: String? = null,
    var allocation: Int? = 0,
    var status: Boolean? = null,
    var userID: String? = null,
    var dateCreated: String? = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date()),
    var dateUpdated: String? = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
) : Parcelable {

    constructor(parcel: Parcel) : this(
        allocationID = parcel.readString(),
        sectionID = parcel.readString(),
        sectionName = parcel.readString(),
        hospitalID = parcel.readString(),
        hospitalName = parcel.readString(),
        allocation = parcel.readValue(Int::class.java.classLoader) as? Int,
        status = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        userID = parcel.readString(),
        dateCreated = parcel.readString(),
        dateUpdated = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(allocationID)
        parcel.writeString(sectionID)
        parcel.writeString(sectionName)
        parcel.writeString(hospitalID)
        parcel.writeString(hospitalName)
        parcel.writeValue(allocation)
        parcel.writeValue(status)
        parcel.writeString(userID)
        parcel.writeString(dateCreated)
        parcel.writeString(dateUpdated)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Slot> {
        override fun createFromParcel(parcel: Parcel): Slot {
            return Slot(parcel)
        }

        override fun newArray(size: Int): Array<Slot?> {
            return arrayOfNulls(size)
        }
    }
}

