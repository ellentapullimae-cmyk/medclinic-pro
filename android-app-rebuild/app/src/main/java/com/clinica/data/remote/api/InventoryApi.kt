package com.clinica.data.remote.api

import com.clinica.domain.model.InventoryItem
import com.clinica.domain.model.InventoryItemInput
import com.clinica.domain.model.Page
import com.clinica.domain.model.StockMovement
import com.clinica.domain.model.StockMovementInput
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface InventoryApi {

    @GET("api/v1/inventory")
    suspend fun list(
        @Query("q") q: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
    ): Page<InventoryItem>

    @POST("api/v1/inventory")
    suspend fun create(@Body body: InventoryItemInput): InventoryItem

    @POST("api/v1/inventory/{id}/movements")
    suspend fun move(@Path("id") id: Int, @Body body: StockMovementInput): StockMovement

    @GET("api/v1/inventory/{id}/movements")
    suspend fun movements(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
    ): Page<StockMovement>
}
