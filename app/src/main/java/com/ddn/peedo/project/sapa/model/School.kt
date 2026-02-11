package com.ddn.peedo.project.sapa.model

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SimpleDateFormat")
data class School(
    var schoolID: String? = null,
    var schoolName: String? = null,
    var address: String? = null,
    var userID: String? = null,
    var createdBy: String? = null,
    var status: Int? = 0,
    var code: String? = null,
    var dateCreated: String? = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date()),
    var dateUpdated: String? = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
) : Parcelable {

    constructor(parcel: Parcel) : this(
        schoolID = parcel.readString(),
        schoolName = parcel.readString(),
        address = parcel.readString(),
        userID = parcel.readString(),
        createdBy = parcel.readString(),
        status = parcel.readValue(Int::class.java.classLoader) as? Int,
        code = parcel.readString(),
        dateCreated = parcel.readString(),
        dateUpdated = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(schoolID)
        parcel.writeString(schoolName)
        parcel.writeString(address)
        parcel.writeString(userID)
        parcel.writeString(createdBy)
        parcel.writeValue(status)
        parcel.writeString(code)
        parcel.writeString(dateCreated)
        parcel.writeString(dateUpdated)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<School> {
        override fun createFromParcel(parcel: Parcel): School {
            return School(parcel)
        }

        override fun newArray(size: Int): Array<School?> {
            return arrayOfNulls(size)
        }
    }
}
