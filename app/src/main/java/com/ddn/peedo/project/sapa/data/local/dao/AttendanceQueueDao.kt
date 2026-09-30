package com.ddn.peedo.project.sapa.data.local.dao

import androidx.room.*
import com.ddn.peedo.project.sapa.data.local.entity.AttendanceQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceQueueDao {
    @Insert
    suspend fun enqueue(entry: AttendanceQueueEntity): Long

    @Query("SELECT * FROM attendance_queue WHERE isSynced = 0 ORDER BY scannedAt ASC")
    suspend fun getPending(): List<AttendanceQueueEntity>

    @Query("SELECT COUNT(*) FROM attendance_queue WHERE isSynced = 0")
    suspend fun pendingCount(): Int

    @Query("SELECT COUNT(*) FROM attendance_queue WHERE isSynced = 0")
    fun observePendingCount(): Flow<Int>

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

    @Query("UPDATE attendance_queue SET isSynced = 1, lastError = NULL WHERE localId = :localId")
    suspend fun markSynced(localId: Long)

    /**
     * Records a failed upload attempt for one queued row.
     * Incrementing syncAttempts lets the UI flag rows that keep failing
     * (e.g. real conflicts like "not appointed") as needing attention.
     */
    @Query(
        "UPDATE attendance_queue SET " +
                "syncAttempts = syncAttempts + 1, " +
                "lastError = :error " +
                "WHERE localId = :localId"
    )
    suspend fun markFailed(localId: Long, error: String?)

    /**
     * Removes permanently-rejected rows (client errors the server will never
     * accept, e.g. "not appointed") so they stop counting as pending.
     */
    @Query("DELETE FROM attendance_queue WHERE localId = :localId")
    suspend fun deleteById(localId: Long)

    @Query("DELETE FROM attendance_queue WHERE isSynced = 1")
    suspend fun clearSynced()
}