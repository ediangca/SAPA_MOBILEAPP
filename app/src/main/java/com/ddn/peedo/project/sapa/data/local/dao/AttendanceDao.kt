package com.ddn.peedo.project.sapa.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ddn.peedo.project.sapa.data.local.entity.AttendanceEntity

@Dao
interface AttendanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(
        attendance: List<AttendanceEntity>
    )

    @Query("""
        SELECT EXISTS(
            SELECT 1
            FROM attendance
            WHERE slotID = :slotId
            AND userID = :userId
        )
    """)
    suspend fun hasAttendance(
        slotId: String,
        userId: String
    ): Boolean

    @Query("""
        SELECT *
        FROM attendance
        WHERE slotID = :slotId
        AND userID = :userId
        LIMIT 1
    """)
    suspend fun getBySlotAndUser(
        slotId: String,
        userId: String
    ): AttendanceEntity?

    @Query("""
        SELECT *
        FROM attendance
        WHERE slotID = :slotId
    """)
    suspend fun getBySlot(
        slotId: String
    ): List<AttendanceEntity>

    @Query("DELETE FROM attendance")
    suspend fun clear()

    @Query("""
        DELETE FROM attendance
        WHERE slotID NOT IN (:slotIds)
    """)
    suspend fun deleteNotInSlots(
        slotIds: List<String>
    )
}