package com.ddn.peedo.project.sapa.data.repository

import android.content.Context
import android.util.Log
import com.ddn.peedo.project.sapa.data.local.SapaDatabase
import com.ddn.peedo.project.sapa.data.local.entity.AppointedStudentEntity
import com.ddn.peedo.project.sapa.retrofit.RetrofitClient
import com.ddn.peedo.project.sapa.model.VwSlot

class AppointedStudentRepository(
    private val context: Context
) {

    private val database =
        SapaDatabase.getInstance(context)

    private val dao =
        database.appointedStudentDao()

    /**
     * Synchronize all appointed students for the supplied slots.
     *
     * This should be called after SlotRepository successfully
     * retrieves the current schedule from the server.
     */
    suspend fun syncForSlots(
        slots: List<VwSlot>
    ): Boolean {

        if (slots.isEmpty()) {
            Log.d(
                "AppointedStudentRepo",
                "No slots available. Nothing to sync."
            )
            return true
        }

        return try {

            val api =
                RetrofitClient.create(context)

            /*
             * Avoid requesting the same slot multiple times.
             */
            val slotIds =
                slots
                    .map { it.slotID }
                    .distinct()

            Log.d(
                "AppointedStudentRepo",
                "Syncing appointed students for ${slotIds.size} slots"
            )

            /*
             * Get appointed students for every slot.
             */
            val allStudents =
                mutableListOf<AppointedStudentEntity>()

            for (slotId in slotIds) {

                try {

                    val response =
                        api.getAppointedStudentsBySlotID(slotId)

                    if (!response.isSuccessful) {

                        Log.e(
                            "AppointedStudentRepo",
                            "Failed to load students for slot $slotId: ${response.code()}"
                        )

                        continue
                    }

                    val students =
                        response.body()
                            ?: emptyList()

                    /*
                     * Convert API model → Room entity.
                     */
                    val entities =
                        students.map { student ->

                            AppointedStudentEntity(
                                asid = student.asid,
                                slotID = student.slotID,
                                userID = student.userID,

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

                    allStudents.addAll(entities)

                    Log.d(
                        "AppointedStudentRepo",
                        "Slot $slotId → ${entities.size} students"
                    )

                } catch (e: Exception) {

                    /*
                     * Do not destroy existing cache if one
                     * particular slot fails.
                     */
                    Log.e(
                        "AppointedStudentRepo",
                        "Error syncing slot $slotId",
                        e
                    )
                }
            }

            /*
             * Save everything to Room in one transaction.
             */
            if (allStudents.isNotEmpty()) {

                dao.upsertAll(allStudents)

                Log.d(
                    "AppointedStudentRepo",
                    "Saved ${allStudents.size} appointed students to Room"
                )
            }

            true

        } catch (e: Exception) {

            Log.e(
                "AppointedStudentRepo",
                "Unable to synchronize appointed students",
                e
            )

            false
        }
    }
}