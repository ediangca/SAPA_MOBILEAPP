package com.ddn.peedo.project.sapa.data.repository

import android.content.Context
import android.util.Log

import com.ddn.peedo.project.sapa.data.local.SapaDatabase
import com.ddn.peedo.project.sapa.data.local.entity.AppointedStudentEntity
import com.ddn.peedo.project.sapa.data.local.entity.SlotEntity
import com.ddn.peedo.project.sapa.data.local.entity.SyncMetaEntity
import com.ddn.peedo.project.sapa.model.VwAppointedStudent
import com.ddn.peedo.project.sapa.model.VwSlot
import com.ddn.peedo.project.sapa.retrofit.RetrofitClient
import com.ddn.peedo.project.sapa.utils.ConnectivityUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope



sealed class SlotResult {

    data class FromServer(
        val slots: List<VwSlot>
    ) : SlotResult()

    data class FromCache(
        val slots: List<VwSlot>,
        val lastSyncedAt: Long?
    ) : SlotResult()

    object EmptyNoConnection : SlotResult()
}class SlotRepository(
    private val context: Context
) {

    companion object {
        const val MODULE_NAME = "SCHEDULES"
    }

    private val db =
        SapaDatabase.getInstance(context)

    private val slotDao =
        db.slotDao()

    private val appointedStudentDao =
        db.appointedStudentDao()

    private val syncMetaDao =
        db.syncMetaDao()

    /**
     * Set when the download-sync is running in “replication” mode
     * (includeStudents = true): reports how many slots' appointed students
     * have been fetched so far so the UI can show a live %.
     */
    var onSlotStudentsProgress: ((done: Int, total: Int) -> Unit)? = null


    suspend fun getSlots(
        roleID: String,
        userID: String,
        hospitalID: String?,
        year: Int,
        includeStudents: Boolean = false
    ): SlotResult {

        if (ConnectivityUtils.isNetworkAvailable(context)) {

            try {

                val api =
                    RetrofitClient.api(context)

                val response =
                    when (roleID) {

                        "UGR0001",
                        "UGR0002" ->
                            api.getSlots(year)

                        "UGR0003" ->
                            api.getSlotsByUserID(
                                userID,
                                year
                            )

                        "UGR0006" ->
                            api.getSlotsByCI(
                                userID,
                                year
                            )

                        "UGR0004" ->
                            api.getSlotsByAppointUserID(
                                userID,
                                year
                            )

                        "UGR0005" ->
                            api.getSlotsByHospitalID(
                                hospitalID ?: "",
                                year
                            )

                        else ->
                            return SlotResult.EmptyNoConnection
                    }


                if (response.isSuccessful) {

                    val slots =
                        response.body().orEmpty()

                    // ==========================================
                    // CACHE SCHEDULES
                    // ==========================================

                    cacheSlots(slots)


                    // ==========================================
                    // CACHE APPOINTED STUDENTS
                    // ==========================================

                    if (includeStudents) {

                        cacheAppointedStudents(
                            slots,
                            year
                        )
                    }


                    return SlotResult.FromServer(
                        slots
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    "SlotRepository",
                    "Error loading schedules",
                    e
                )
            }
        }


        // ==============================================
        // OFFLINE / SERVER FAILURE
        // ==============================================

        val cached =
            slotDao
                .getAllOnce()
                .map { it.toModel() }

        val lastSynced =
            syncMetaDao
                .get(MODULE_NAME)
                ?.lastSyncedAt


        return if (cached.isEmpty()) {

            SlotResult.EmptyNoConnection

        } else {

            SlotResult.FromCache(
                cached,
                lastSynced
            )
        }
    }


    // =========================================================
    // CACHE SLOTS
    // =========================================================

    private suspend fun cacheSlots(
        slots: List<VwSlot>
    ) {

        slotDao.clear()

        slotDao.upsertAll(
            slots.map {
                it.toEntity()
            }
        )

        syncMetaDao.upsert(
            SyncMetaEntity(
                MODULE_NAME,
                System.currentTimeMillis()
            )
        )
    }


    // =========================================================
    // CACHE ALL APPOINTED STUDENTS
    //
    // internal so OfflineSyncManager can re-run this phase alone when
    // count validation says only the appointed-students module changed.
    // =========================================================

    internal suspend fun cacheAppointedStudents(
        slots: List<VwSlot>,
        year: Int
    ) {

        val slotIds =
            slots
                .map { it.slotID }
                .distinct()

        if (slotIds.isEmpty()) {

            appointedStudentDao.clear()

            onSlotStudentsProgress?.invoke(0, 0)

            return
        }


        val api =
            RetrofitClient.api(context)


        // -----------------------------------------------------
        // BULK FIRST: one request for the whole year. This removes
        // the per-slot “request storm” (N requests) — the biggest
        // sync-speed lever on slow connections. Falls back to
        // per-slot requests only if the bulk endpoint is unavailable
        // (e.g. an older server build without it).
        // -----------------------------------------------------

        var allStudents: List<VwAppointedStudent> = emptyList()
        var usedBulk = false

        try {

            val bulkResponse =
                api.getAppointedStudentsByYear(year)

            if (bulkResponse.isSuccessful) {

                val slotIdSet = slotIds.toHashSet()

                allStudents =
                    bulkResponse.body()
                        .orEmpty()
                        .filter { it.slotID in slotIdSet }

                usedBulk = true

                onSlotStudentsProgress?.invoke(1, 1)

                Log.d(
                    "SlotRepository",
                    "Bulk download: ${allStudents.size} appointed students for $year"
                )

            } else {

                Log.w(
                    "SlotRepository",
                    "Bulk student API failed: ${bulkResponse.code()} — falling back to per-slot"
                )
            }

        } catch (e: Exception) {

            Log.e(
                "SlotRepository",
                "Bulk student API error — falling back to per-slot",
                e
            )
        }

        if (!usedBulk) {

            // -------------------------------------------------
            // FALLBACK: request students per slot concurrently,
            // reporting per-slot progress so the UI can show a
            // live download %.
            // -------------------------------------------------

            var settled = 0

            val results =
                coroutineScope {

                    slotIds.map { slotId ->

                        async(Dispatchers.IO) {

                            try {

                                val response =
                                    api.getAppointedStudentsBySlotID(
                                        slotId
                                    )

                                if (response.isSuccessful) {

                                    response.body()
                                        .orEmpty()

                                } else {

                                    Log.w(
                                        "SlotRepository",
                                        "Student API failed for slot $slotId: ${response.code()}"
                                    )

                                    emptyList()
                                }

                            } catch (e: Exception) {

                                Log.e(
                                    "SlotRepository",
                                    "Error loading students for slot $slotId",
                                    e
                                )

                                emptyList()
                            } finally {
                                synchronized(this@SlotRepository) {
                                    settled++
                                    onSlotStudentsProgress?.invoke(settled, slotIds.size)
                                }
                            }
                        }
                    }.awaitAll()
                }


            allStudents =
                results.flatten()
        }


        Log.d(
            "SlotRepository",
            "Downloaded ${allStudents.size} appointed students from ${slotIds.size} slots"
        )


        // -----------------------------------------------------
        // Convert API → Room
        // -----------------------------------------------------

        val entities =
            allStudents.map { student ->

                AppointedStudentEntity(

                    asid =
                        student.asid,

                    slotID =
                        student.slotID,

                    userID =
                        student.userID,

                    appointedDateCreated =
                        student.appointedDateCreated,

                    appointedDateUpdated =
                        student.appointedDateUpdated,

                    bookID =
                        student.bookID,

                    dateSlot =
                        student.dateSlot,

                    shiftID =
                        student.shiftID,

                    shiftName =
                        student.shiftName,

                    startTime =
                        student.startTime,

                    endTime =
                        student.endTime,

                    slotStatus =
                        student.slotStatus,

                    allocationID =
                        student.allocationID,

                    allocation =
                        student.allocation,

                    allocationStatus =
                        student.allocationStatus,

                    hospitalID =
                        student.hospitalID,

                    hospitalName =
                        student.hospitalName,

                    sectionID =
                        student.sectionID,

                    sectionName =
                        student.sectionName,

                    username =
                        student.username,

                    fullname =
                        student.fullname,

                    email =
                        student.email,

                    roleID =
                        student.roleID,

                    userStatus =
                        student.userStatus,

                    schoolID =
                        student.schoolID
                )
            }


        // -----------------------------------------------------
        // Replace local cache
        // -----------------------------------------------------

        appointedStudentDao.clear()

        if (entities.isNotEmpty()) {

            appointedStudentDao.upsertAll(
                entities
            )
        }


        Log.d(
            "SlotRepository",
            "Room now contains ${entities.size} appointed students"
        )
    }
}

// =========================================================
// VwSlot → Room Entity
// =========================================================

private fun VwSlot.toEntity() = SlotEntity(
    slotID = slotID,
    bookID = bookID,
    dateSlot = dateSlot,
    shiftID = shiftID,
    shiftName = shiftName,
    startTime = startTime,
    endTime = endTime,
    slotStatus = slotStatus,

    hospitalID = hospitalID,
    hospitalName = hospitalName,

    sectionID = sectionID,
    sectionName = sectionName,

    allocationID = allocationID,
    allocation = allocation,
    allocationStatus = allocationStatus,

    isTimeRestricted = isTimeRestricted,

    userID = userID,
    fullname = fullname,

    schoolID = schoolID,
    schoolName = schoolName,

    CIID = CIID,
    ci_fullname = ci_fullname,

    isCIPresent = isCIPresent,

    date_Created = date_Created,
    date_Updated = date_Updated
)


// =========================================================
// Room Entity → VwSlot (internal so OfflineSyncManager reuses it)
// =========================================================

internal fun SlotEntity.toModel() = VwSlot(
    slotID = slotID,
    bookID = bookID,
    dateSlot = dateSlot,
    shiftID = shiftID,
    shiftName = shiftName,
    startTime = startTime,
    endTime = endTime,
    slotStatus = slotStatus,

    hospitalID = hospitalID,
    hospitalName = hospitalName,

    sectionID = sectionID,
    sectionName = sectionName,

    allocationID = allocationID,
    allocation = allocation,
    allocationStatus = allocationStatus,

    isTimeRestricted = isTimeRestricted,

    userID = userID,
    fullname = fullname,

    schoolID = schoolID,
    schoolName = schoolName,

    CIID = CIID,
    ci_fullname = ci_fullname,

    isCIPresent = isCIPresent,

    date_Created = date_Created,
    date_Updated = date_Updated
)