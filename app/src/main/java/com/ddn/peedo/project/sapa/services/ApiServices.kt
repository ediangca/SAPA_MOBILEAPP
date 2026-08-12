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


    //-----------------Settinggs-----------------//

    @GET("Settings/")
    suspend fun getSettings(): Response<List<Settings>>

}

data class GenericResponse(
    val message: String? = null
)