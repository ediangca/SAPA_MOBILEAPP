package com.ddn.peedo.project.sapa.data.local.dao

import androidx.room.*
import com.ddn.peedo.project.sapa.data.local.entity.AppointedStudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointedStudentDao {
    @Query("SELECT * FROM appointed_students WHERE slotID = :slotId")
    fun observeBySlot(slotId: String): Flow<List<AppointedStudentEntity>>

    @Query("SELECT * FROM appointed_students WHERE slotID = :slotId AND userID = :userId LIMIT 1")
    suspend fun getBySlotAndUser(slotId: String, userId: String): AppointedStudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(students: List<AppointedStudentEntity>)

    @Query("DELETE FROM appointed_students")
    suspend fun clear()
}