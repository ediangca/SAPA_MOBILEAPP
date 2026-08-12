package com.ddn.peedo.project.sapa.data.repository

import android.content.Context
import android.util.Log
import com.ddn.peedo.project.sapa.data.local.SapaDatabase
import com.ddn.peedo.project.sapa.data.local.entity.AttendanceEntity
import com.ddn.peedo.project.sapa.data.local.entity.SyncMetaEntity
import com.ddn.peedo.project.sapa.retrofit.RetrofitClient
import java.time.Instant

class AttendanceRepository(
    private val context: Context
) {

    companion object {
        const val MODULE_NAME = "ATTENDANCE"
    }

    private val db =
        SapaDatabase.getInstance(context)

    private val attendanceDao =
        db.attendanceDao()

    private val syncMetaDao =
        db.syncMetaDao()

    suspend fun sync(): Boolean {

        return try {

            val lastSync =
                syncMetaDao
                    .get(MODULE_NAME)
                    ?.lastSyncedAt

            val since =
                lastSync?.let {
                    Instant
                        .ofEpochMilli(it)
                        .toString()
                }

            Log.d(
                "AttendanceSync",
                "Last sync: $since"
            )

            val response =
                RetrofitClient
                    .api(context)
                    .syncAttendance(since)

            if (!response.isSuccessful) {

                Log.e(
                    "AttendanceSync",
                    "API failed: ${response.code()}"
                )

                return false
            }

            val serverRecords =
                response.body().orEmpty()

            Log.d(
                "AttendanceSync",
                "Received ${serverRecords.size} records"
            )

            val entities =
                serverRecords.mapNotNull { attendance ->

                    val attID =
                        attendance.attID

                    if (attID.isNullOrBlank()) {

                        Log.e(
                            "AttendanceSync",
                            "Skipping attendance with missing ATTID"
                        )

                        null

                    } else {

                        AttendanceEntity(
                            attID = attID,
                            slotID = attendance.slotID,
                            userID = attendance.userID,
                            status = attendance.status,
                            dateCreated = attendance.dateCreated,
                            dateUpdated = attendance.dateUpdated
                        )
                    }
                }

            if (entities.isNotEmpty()) {

                attendanceDao.upsertAll(
                    entities
                )
            }

            /*
             * IMPORTANT:
             *
             * Only update lastSyncedAt after the API
             * request successfully completed.
             */

            syncMetaDao.upsert(
                SyncMetaEntity(
                    moduleName = MODULE_NAME,
                    lastSyncedAt =
                        System.currentTimeMillis()
                )
            )

            Log.d(
                "AttendanceSync",
                "Synchronization successful."
            )

            true

        } catch (e: Exception) {

            Log.e(
                "AttendanceSync",
                "Synchronization failed",
                e
            )

            false
        }
    }
}