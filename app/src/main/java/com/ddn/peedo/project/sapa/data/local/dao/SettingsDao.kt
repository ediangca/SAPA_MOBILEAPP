package com.ddn.peedo.project.sapa.data.local.dao

import androidx.room.*
import com.ddn.peedo.project.sapa.data.local.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings")
    fun observeAll(): Flow<List<SettingsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(settings: List<SettingsEntity>)

    @Query("DELETE FROM settings")
    suspend fun clear()
}