package com.ddn.peedo.project.sapa.services

import com.ddn.peedo.project.sapa.dataclass.AttendanceRequest
import com.ddn.peedo.project.sapa.dataclass.AttendanceValidationResponse
import com.ddn.peedo.project.sapa.model.AttendanceResponse
import com.ddn.peedo.project.sapa.model.AuthRequest
import com.ddn.peedo.project.sapa.model.AuthResponse
import com.ddn.peedo.project.sapa.model.DashboardSummary
import com.ddn.peedo.project.sapa.model.Information
import com.ddn.peedo.project.sapa.model.School
import com.ddn.peedo.project.sapa.model.Settings
import com.ddn.peedo.project.sapa.model.VwAppointedStudent
import com.ddn.peedo.project.sapa.model.VwPrivilege
import com.ddn.peedo.project.sapa.model.VwSlot
import com.ddn.peedo.project.sapa.model.VwUser
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Body


interface ApiService {

    //-----------------Information-----------------//
    @GET("Information/{id}")
    suspend fun getSAPAInformation(
        @Path("id") id: Int
    ): Response<Information>

    //-----------------Dashboard-----------------//
    @GET("dashboard/summary")
    suspend fun getDashboardSummary(
        @Query("userId") userId: String,
        @Query("roleId") roleId: String
    ): Response<DashboardSummary>

    //-----------------Auth-----------------//
    @POST("Auth")
    suspend fun authenticate(
        @Body request: AuthRequest
    ): Response<AuthResponse>

    //-----------------Users-----------------//
    @GET("Users")
    suspend fun getUsers(): Response<List<VwUser>>

    @GET("Users/GetUserbyUsername/{username}")
    suspend fun getUserByUsername(
        @Path("username") username: String
    ): Response<VwUser>

    @GET("Users/GetUserBySchoolID/{schoolID}")
    suspend fun getStudentsBySchoolID(
        @Path("schoolID") schoolID: String
    ): Response<List<VwUser>>
    @POST("Users/resend-verification")
    suspend fun resendVerification(
        @Body email: String
    ): Response<ResponseBody>

    @POST("Users/Approve/{userId}")
    suspend fun approveUser(
        @Path("userId") userId: String
    ): Response<ResponseBody>


    //-----------------Privileges-----------------//
    @GET("Privileges/Role/{roleId}")
    suspend fun getPrivilegeByRole(
        @Path("roleId") roleId: String
    ): Response<List<VwPrivilege>>


    //-----------------Schools-----------------//
    @GET("Schools")
    suspend fun getSchools(): Response<List<School>>


    //-----------------Slot-----------------//
    @GET("Slots")
    suspend fun getSchedule(): Response<List<VwSlot>>

    @GET("Slots")
    suspend fun getSlots(
        @Query("year") year: Int?
    ): Response<List<VwSlot>>

    @GET("Slots/user/{userId}")
    suspend fun getSlotsByUserID(
        @Path("userId") userId: String,
        @Query("year") year: Int?
    ): Response<List<VwSlot>>


    @GET("Slots/ci/{userId}")
    suspend fun getSlotsByCI(
        @Path("userId") userId: String,
        @Query("year") year: Int?
    ): Response<List<VwSlot>>

    @GET("Slots/hospital/{hospitalId}")
    suspend fun getSlotsByHospitalID(
        @Path("hospitalId") hospitalId: String,
        @Query("year") year: Int?
    ): Response<List<VwSlot>>

    @GET("Slots/user/appointed/{userId}")
    suspend fun getSlotsByAppointUserID(
        @Path("userId") userId: String,
        @Query("year") year: Int?
    ): Response<List<VwSlot>>

    //-----------------Appointed-----------------//
    @GET("AppointedStudents/slot/{id}")
    suspend fun getAppointedStudentsBySlotID(
        @Path("id") slotId: String
    ): Response<List<VwAppointedStudent>>

    /**
     * BULK replication endpoint: all appointed students whose slot falls in
     * [year] — one request instead of one-per-slot. Used by the offline sync;
     * per-slot remains the fallback for older server builds.
     */
    @GET("AppointedStudents/year/{year}")
    suspend fun getAppointedStudentsByYear(
        @Path("year") year: Int
    ): Response<List<VwAppointedStudent>>

    //-----------------Sync manifest (count validation)-----------------//

    /**
     * Cheap role-scoped record counts mirroring exactly what the offline
     * replication caches. OfflineSyncManager compares them with the local
     * Room counts and re-downloads only the modules that actually differ.
     */
    @GET("SyncManifest")
    suspend fun getSyncManifest(
        @Query("year") year: Int,
        @Query("userID") userId: String,
        @Query("roleID") roleId: String,
        @Query("hospitalID") hospitalId: String?,
        @Query("schoolIds") schoolIds: String?
    ): Response<SyncManifest>



    //-----------------Attendance-----------------//

    @POST("Attendance/by-slots")
    suspend fun getAttendanceBySlots(
        @Body slotIDs: List<String>
    ): Response<List<AttendanceResponse>>

    @GET("Attendance/user/{userId}/slot/{slotId}")
    suspend fun validateAttendance(
        @Path("userId") userId: String,
        @Path("slotId") slotId: String
    ): Response<AttendanceValidationResponse>

    @GET("Attendance/sync")
    suspend fun syncAttendance(
        @Query("since") since: String?
    ): Response<List<AttendanceResponse>>

    @POST("Attendance")
    suspend fun postAttendance(
        @Body request: AttendanceRequest
    ): Response<AttendanceResponse>

    //-----------------Offline queue upload-----------------//

    /**
     * Batch upload for the offline attendance queue. Always returns HTTP 200
     * with per-item results (see BatchAttendanceResponse) so the worker can
     * mark exactly which queued rows uploaded and which failed.
     */
    @POST("Attendance/batch")
    suspend fun postAttendanceBatch(
        @Body request: BatchAttendanceRequest
    ): Response<BatchAttendanceResponse>


    //-----------------Settinggs-----------------//

    @GET("Settings/")
    suspend fun getSettings(): Response<List<Settings>>

}

data class GenericResponse(
    val message: String? = null
)

/**
 * One item in a batch upload. ScannedAt preserves the true scan time when the
 * record was captured offline (server stores it as DateCreated/DateUpdated).
 */
data class BatchAttendanceItem(
    val SlotID: String,
    val UserID: String,
    val Status: Int = 1,
    val ScannedAt: String? = null
)

data class BatchAttendanceRequest(
    val items: List<BatchAttendanceItem>
)

data class BatchAttendanceItemResult(
    val SlotID: String,
    val UserID: String,
    val Succeeded: Boolean,
    val Code: String? = null,
    val Message: String? = null,
    val ATTID: String? = null
)

data class BatchAttendanceResponse(
    val succeeded: List<BatchAttendanceItemResult>,
    val failed: List<BatchAttendanceItemResult>
)

/**
 * Server-side record counts for the modules replicated by OfflineSyncManager
 * (GET SyncManifest). Field names map to the API's camelCase JSON.
 */
data class SyncManifest(
    val slots: Int = 0,
    val appointedStudents: Int = 0,
    val attendance: Int = 0,
    val schools: Int = 0,
    val users: Int = 0
)