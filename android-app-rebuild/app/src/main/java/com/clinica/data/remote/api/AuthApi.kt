package com.clinica.data.remote.api

import com.clinica.domain.model.LoginRequest
import com.clinica.domain.model.TokenResponse
import com.clinica.domain.model.UserBrief
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): TokenResponse

    @GET("api/v1/users/doctors")
    suspend fun doctors(): List<UserBrief>
}
