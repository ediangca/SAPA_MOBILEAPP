package com.ddn.peedo.project.sapa.model

import android.os.Parcel
import android.os.Parcelable

data class VwUser(
    val userID: String,
    val username: String,
    val password: String,
    val lastname: String?,
    val firstname: String?,
    val middlename: String?,
    val fullname: String,
    val email: String,
    val emailVerifiedAt: String?, // DateTime -> String (ISO format)
    val roleID: String,
    val rolename: String,
    val schoolID: String?,
    val schoolName: String?,
    val status: Char?,
    val coorSchoolID: String?,
    val coorSchoolCode: String?,
    val coorSchoolName: String?,
    val hospitalID: String?,
    val hospitalName: String?,
    val dateCreated: String, // DateTime -> String
    val dateUpdated: String
) : Parcelable {

    constructor(parcel: Parcel) : this(
        userID = parcel.readString() ?: "",
        username = parcel.readString() ?: "",
        password = parcel.readString() ?: "",
        lastname = parcel.readString(),
        firstname = parcel.readString(),
        middlename = parcel.readString(),
        fullname = parcel.readString() ?: "",
        email = parcel.readString() ?: "",
        emailVerifiedAt = parcel.readString(),
        roleID = parcel.readString() ?: "",
        rolename = parcel.readString() ?: "",
        schoolID = parcel.readString(),
        schoolName = parcel.readString(),
        status = parcel.readValue(Char::class.java.classLoader) as? Char,
        coorSchoolID = parcel.readString(),
        coorSchoolCode = parcel.readString(),
        coorSchoolName = parcel.readString(),
        hospitalID = parcel.readString(),
        hospitalName = parcel.readString(),
        dateCreated = parcel.readString() ?: "",
        dateUpdated = parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(userID)
        parcel.writeString(username)
        parcel.writeString(password)
        parcel.writeString(lastname)
        parcel.writeString(firstname)
        parcel.writeString(middlename)
        parcel.writeString(fullname)
        parcel.writeString(email)
        parcel.writeString(emailVerifiedAt)
        parcel.writeString(roleID)
        parcel.writeString(rolename)
        parcel.writeString(schoolID)
        parcel.writeString(schoolName)
        parcel.writeValue(status)
        parcel.writeString(coorSchoolID)
        parcel.writeString(coorSchoolCode)
        parcel.writeString(coorSchoolName)
        parcel.writeString(hospitalID)
        parcel.writeString(hospitalName)
        parcel.writeString(dateCreated)
        parcel.writeString(dateUpdated)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<VwUser> {
        override fun createFromParcel(parcel: Parcel): VwUser {
            return VwUser(parcel)
        }

        override fun newArray(size: Int): Array<VwUser?> {
            return arrayOfNulls(size)
        }
    }
}