package com.ddn.peedo.project.sapa.data.local.dao

import androidx.room.*
import com.ddn.peedo.project.sapa.data.local.entity.AttendanceQueueEntity

@Dao
interface AttendanceQueueDao {
    @Insert
    suspend fun enqueue(entry: AttendanceQueueEntity): Long

    @Query("SELECT * FROM attendance_queue WHERE isSynced = 0 ORDER BY scannedAt ASC")
    suspend fun getPending(): List<AttendanceQueueEntity>

    @Query("SELECT COUNT(*) FROM attendance_queue WHERE isSynced = 0")
    suspend fun pendingCount(): Int

    @Query("SELECT * FROM attendance_queue WHERE slotID = :slotId AND userID = :userId LIMIT 1")
    suspend fun findExisting(slotId: String, userId: String): AttendanceQueueEntity?

    @Query(""" SELECT EXISTS(
        SELECT 1
        FROM attendance_queue
        WHERE slotID = :slotId
        AND userID = :userId
        AND status = 1
    )
""")
    suspend fun hasAttendance(
        slotId: String,
        userId: String
    ): Boolean
    @Update
    suspend fun update(entry: AttendanceQueueEntity)

    @Query("DELETE FROM attendance_queue WHERE isSynced = 1")
    suspend fun clearSynced()
}