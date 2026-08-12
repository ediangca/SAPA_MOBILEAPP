package com.ddn.peedo.project.sapa.data.repository

import android.content.Context
import com.ddn.peedo.project.sapa.data.local.SapaDatabase
import com.ddn.peedo.project.sapa.data.local.entity.SlotEntity
import com.ddn.peedo.project.sapa.data.local.entity.SyncMetaEntity
import com.ddn.peedo.project.sapa.model.VwSlot
import com.ddn.peedo.project.sapa.retrofit.RetrofitClient
import com.ddn.peedo.project.sapa.utils.ConnectivityUtils

sealed class SlotResult {
    data class FromServer(val slots: List<VwSlot>) : SlotResult()
    data class FromCache(val slots: List<VwSlot>, val lastSyncedAt: Long?) : SlotResult()
    object EmptyNoConnection : SlotResult()
}

class SlotRepository(private val context: Context) {

    companion object {
        const val MODULE_NAME = "SCHEDULES"
    }

    private val db = SapaDatabase.getInstance(context)
    private val slotDao = db.slotDao()
    private val syncMetaDao = db.syncMetaDao()

    suspend fun getSlots(
        roleID: String,
        userID: String,
        hospitalID: String?,
        year: Int
    ): SlotResult {

        if (ConnectivityUtils.isNetworkAvailable(context)) {
            try {
                val api = RetrofitClient.api(context)
                val response = when (roleID) {
                    "UGR0001", "UGR0002" -> api.getSlots(year)
                    "UGR0003" -> api.getSlotsByUserID(userID, year)
                    "UGR0006" -> api.getSlotsByCI(userID, year)
                    "UGR0004" -> api.getSlotsByAppointUserID(userID, year)
                    "UGR0005" -> api.getSlotsByHospitalID(hospitalID ?: "", year)
                    else -> return SlotResult.EmptyNoConnection
                }

                if (response.isSuccessful) {
                    val slots = response.body().orEmpty()
                    cacheSlots(slots)
                    return SlotResult.FromServer(slots)
                }
                // Server responded with an error — fall through to cache below
            } catch (e: Exception) {
                // Network reported available but request failed (server down,
                // captive portal, etc.) — fall through to cache below
            }
        }

        // Offline, or the online attempt failed — read from cache
        val cached = slotDao.getAllOnce().map { it.toModel() }
        val lastSynced = syncMetaDao.get(MODULE_NAME)?.lastSyncedAt

        return if (cached.isEmpty()) {
            SlotResult.EmptyNoConnection
        } else {
            SlotResult.FromCache(cached, lastSynced)
        }
    }

    private suspend fun cacheSlots(slots: List<VwSlot>) {
        slotDao.clear()
        slotDao.upsertAll(slots.map { it.toEntity() })
        syncMetaDao.upsert(SyncMetaEntity(MODULE_NAME, System.currentTimeMillis()))
    }
}

// --- Mappers between API model and Room entity ---

private fun VwSlot.toEntity() = SlotEntity(
    slotID = slotID, bookID = bookID, dateSlot = dateSlot, shiftID = shiftID,
    shiftName = shiftName, startTime = startTime, endTime = endTime,
    slotStatus = slotStatus, hospitalID = hospitalID, hospitalName = hospitalName,
    sectionID = sectionID, sectionName = sectionName, allocationID = allocationID,
    allocation = allocation, allocationStatus = allocationStatus,
    isTimeRestricted = isTimeRestricted, userID = userID, fullname = fullname,
    schoolID = schoolID, schoolName = schoolName, CIID = CIID,
    ci_fullname = ci_fullname, isCIPresent = isCIPresent,
    date_Created = date_Created, date_Updated = date_Updated
)

private fun SlotEntity.toModel() = VwSlot(
    slotID = slotID, bookID = bookID, dateSlot = dateSlot, shiftID = shiftID,
    shiftName = shiftName, startTime = startTime, endTime = endTime,
    slotStatus = slotStatus, hospitalID = hospitalID, hospitalName = hospitalName,
    sectionID = sectionID, sectionName = sectionName, allocationID = allocationID,
    allocation = allocation, allocationStatus = allocationStatus,
    isTimeRestricted = isTimeRestricted, userID = userID, fullname = fullname,
    schoolID = schoolID, schoolName = schoolName, CIID = CIID,
    ci_fullname = ci_fullname, isCIPresent = isCIPresent,
    date_Created = date_Created, date_Updated = date_Updated
)