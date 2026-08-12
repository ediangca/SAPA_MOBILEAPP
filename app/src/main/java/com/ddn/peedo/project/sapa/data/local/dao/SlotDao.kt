package com.ddn.peedo.project.sapa.data.local.dao

import androidx.room.*
import com.ddn.peedo.project.sapa.data.local.entity.SlotEntity
import com.ddn.peedo.project.sapa.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SlotDao {

    @Query("SELECT * FROM slots")
    suspend fun getAllOnce(): List<SlotEntity>

    @Query("SELECT * FROM slots")
    fun observeAll(): Flow<List<SlotEntity>>

    @Query("SELECT * FROM slots WHERE userID = :userId")
    fun observeByUser(userId: String): Flow<List<SlotEntity>>

    @Query("SELECT * FROM slots WHERE slotID = :slotId LIMIT 1")
    suspend fun getById(slotId: String): SlotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(slots: List<SlotEntity>)

    @Query("DELETE FROM slots")
    suspend fun clear()
}