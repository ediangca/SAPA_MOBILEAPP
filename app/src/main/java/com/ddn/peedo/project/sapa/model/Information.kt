package com.ddn.peedo.project.sapa.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime


data class Information(
    @SerializedName("Id") val id: Int,
    @SerializedName("Title") val title: String,
    @SerializedName("TagLine") val tagLine: String,
    @SerializedName("AboutUs") val aboutUs: String,
    @SerializedName("Version") val version: Double,
    @SerializedName("Date_Created") val dateCreated: String,
    @SerializedName("Date_Updated") val dateUpdated: String
)

