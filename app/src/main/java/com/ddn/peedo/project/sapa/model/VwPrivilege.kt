package com.ddn.peedo.project.sapa.model

import android.os.Parcel
import android.os.Parcelable

data class VwPrivilege(
    val privilegeID: Int?,
    val roleID: String,
    val roleName: String,
    val moduleID: String,
    val moduleName: String,
    val isActive: Boolean?,
    val c: Boolean?,
    val r: Boolean?,
    val u: Boolean?,
    val d: Boolean?,
    val s: Boolean?,
    val pa: Boolean?,
    val dateCreated: String?,
    val dateUpdated: String?
) : Parcelable {

    constructor(parcel: Parcel) : this(
        privilegeID = parcel.readValue(Int::class.java.classLoader) as? Int,
        roleID = parcel.readString() ?: "",
        roleName = parcel.readString() ?: "",
        moduleID = parcel.readString() ?: "",
        moduleName = parcel.readString() ?: "",
        isActive = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        c = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        r = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        u = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        d = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        s = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        pa = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        dateCreated = parcel.readString(),
        dateUpdated = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(privilegeID)
        parcel.writeString(roleID)
        parcel.writeString(roleName)
        parcel.writeString(moduleID)
        parcel.writeString(moduleName)
        parcel.writeValue(isActive)
        parcel.writeValue(c)
        parcel.writeValue(r)
        parcel.writeValue(u)
        parcel.writeValue(d)
        parcel.writeValue(s)
        parcel.writeValue(pa)
        parcel.writeString(dateCreated)
        parcel.writeString(dateUpdated)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<VwPrivilege> {
        override fun createFromParcel(parcel: Parcel): VwPrivilege {
            return VwPrivilege(parcel)
        }

        override fun newArray(size: Int): Array<VwPrivilege?> {
            return arrayOfNulls(size)
        }
    }
}