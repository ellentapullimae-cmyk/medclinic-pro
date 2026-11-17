package com.clinica.domain.repository

import com.clinica.domain.model.LoginRequest
import com.clinica.domain.model.TokenResponse
import com.clinica.domain.model.UserBrief

interface AuthRepository {
    val role: String
    val fullName: String
    val isLoggedIn: Boolean

    suspend fun login(request: LoginRequest): TokenResponse
    suspend fun doctors(): List<UserBrief>
    fun logout()
}
