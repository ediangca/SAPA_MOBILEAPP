package com.ddn.peedo.project.sapa.services

import com.ddn.peedo.project.sapa.dataclass.AttendanceRequest
import com.ddn.peedo.project.sapa.dataclass.AttendanceResponse
import com.ddn.peedo.project.sapa.dataclass.AttendanceValidationResponse
import com.ddn.peedo.project.sapa.model.AuthRequest
import com.ddn.peedo.project.sapa.model.AuthResponse
import com.ddn.peedo.project.sapa.model.Information
import com.ddn.peedo.project.sapa.model.School
import com.ddn.peedo.project.sapa.model.VwAppointedStudent
import com.ddn.peedo.project.sapa.model.VwSlot
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

    //-----------------Auth-----------------//
    @POST("Auth")
    suspend fun authenticate(
        @Body request: AuthRequest
    ): Response<AuthResponse>


    //-----------------Schools-----------------//
    @GET("Schools")
    suspend fun getSchools(): Response<List<School>>


    //-----------------Slot-----------------//
    @GET("Slots")
    suspend fun getSchedule(): Response<List<VwSlot>>

    //-----------------Appointed-----------------//
    @GET("AppointedStudents/slot/{id}")
    suspend fun getAppointedStudentsBySlotID(
        @Path("id") slotId: String
    ): Response<List<VwAppointedStudent>>



    //-----------------Attendance-----------------//

    @GET("Attendance/user/{userId}/slot/{slotId}")
    suspend fun getAttendanceBySlotID(
        @Path("slotId") slotId: String
    ): Response<AttendanceResponse>

    @GET("Attendance/user/{userId}/slot/{slotId}")
    suspend fun validateAttendance(
        @Path("userId") userId: String,
        @Path("slotId") slotId: String
    ): Response<AttendanceValidationResponse>

    @POST("Attendance")
    suspend fun postAttendance(
        @Body request: AttendanceRequest
    ): Response<AttendanceResponse>

}