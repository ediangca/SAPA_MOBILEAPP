package com.ddn.peedo.project.sapa.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val sid: Int,
    val settingKey: String?,
    val description: String?,
    val settingValue: String?
)