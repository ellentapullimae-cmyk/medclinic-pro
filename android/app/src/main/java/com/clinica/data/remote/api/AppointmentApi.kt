package com.clinica.data.remote.api

import com.clinica.domain.model.Appointment
import com.clinica.domain.model.AppointmentInput
import com.clinica.domain.model.AppointmentUpdate
import com.clinica.domain.model.Page
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AppointmentApi {

    @GET("api/v1/appointments")
    suspend fun list(
        @Query("date") date: String? = null,
        @Query("doctor_id") doctorId: Int? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
    ): Page<Appointment>

    @POST("api/v1/appointments")
    suspend fun create(@Body body: AppointmentInput): Appointment

    @PATCH("api/v1/appointments/{id}")
    suspend fun update(@Path("id") id: Int, @Body body: AppointmentUpdate): Appointment

    @DELETE("api/v1/appointments/{id}")
    suspend fun cancel(@Path("id") id: Int): Appointment
}
