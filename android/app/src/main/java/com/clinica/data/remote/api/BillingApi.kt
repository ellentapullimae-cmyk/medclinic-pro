package com.clinica.data.remote.api

import com.clinica.domain.model.Invoice
import com.clinica.domain.model.InvoiceInput
import com.clinica.domain.model.Page
import com.clinica.domain.model.PaymentInput
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BillingApi {

    @GET("api/v1/invoices")
    suspend fun list(
        @Query("status") status: String? = null,
        @Query("patient_id") patientId: Int? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
    ): Page<Invoice>

    @POST("api/v1/invoices")
    suspend fun create(@Body body: InvoiceInput): Invoice

    @GET("api/v1/invoices/{id}")
    suspend fun get(@Path("id") id: Int): Invoice

    @POST("api/v1/invoices/{id}/payments")
    suspend fun pay(@Path("id") id: Int, @Body body: PaymentInput): Invoice

    @POST("api/v1/invoices/{id}/cancel")
    suspend fun cancel(@Path("id") id: Int): Invoice
}
