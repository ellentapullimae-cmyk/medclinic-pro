package com.clinica.data.remote.api

import com.clinica.domain.model.Page
import com.clinica.domain.model.Patient
import com.clinica.domain.model.PatientInput
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PatientApi {

    @GET("api/v1/patients")
    suspend fun list(
        @Query("q") q: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
    ): Page<Patient>

    @POST("api/v1/patients")
    suspend fun create(@Body body: PatientInput): Patient

    @GET("api/v1/patients/{id}")
    suspend fun get(@Path("id") id: Int): Patient

    @PATCH("api/v1/patients/{id}")
    suspend fun update(@Path("id") id: Int, @Body body: PatientInput): Patient

    @DELETE("api/v1/patients/{id}")
    suspend fun delete(@Path("id") id: Int): Patient
}
