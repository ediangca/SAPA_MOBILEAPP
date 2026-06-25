package com.ddn.peedo.project.sapa.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class Settings(
    @SerializedName("sid") var sid: Int? = 0,
    @SerializedName("name") var settingKey: String? = null,
    @SerializedName("description") var description: String? = null,
    @SerializedName("value") var settingValue: String? = null
) : Parcelable {

    constructor(parcel: Parcel) : this(
        sid = parcel.readValue(Int::class.java.classLoader) as? Int,
        settingKey = parcel.readString(),
        description = parcel.readString(),
        settingValue = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(sid)
        parcel.writeString(settingKey)
        parcel.writeString(description)
        parcel.writeString(settingValue)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Settings> {
        override fun createFromParcel(parcel: Parcel): Settings {
            return Settings(parcel)
        }

        override fun newArray(size: Int): Array<Settings?> {
            return arrayOfNulls(size)
        }
    }
}