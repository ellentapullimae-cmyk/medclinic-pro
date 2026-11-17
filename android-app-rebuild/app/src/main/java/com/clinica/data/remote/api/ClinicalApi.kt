package com.clinica.data.remote.api

import com.clinica.domain.model.ClinicalRecord
import com.clinica.domain.model.ClinicalRecordInput
import com.clinica.domain.model.Page
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ClinicalApi {

    @GET("api/v1/patients/{patientId}/records")
    suspend fun listByPatient(
        @Path("patientId") patientId: Int,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
    ): Page<ClinicalRecord>

    @POST("api/v1/patients/{patientId}/records")
    suspend fun create(
        @Path("patientId") patientId: Int,
        @Body body: ClinicalRecordInput,
    ): ClinicalRecord

    @GET("api/v1/clinical-records/{id}")
    suspend fun get(@Path("id") id: Int): ClinicalRecord
}
