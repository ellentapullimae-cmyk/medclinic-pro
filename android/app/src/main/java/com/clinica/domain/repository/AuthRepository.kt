package com.clinica.domain.repository

import com.clinica.domain.model.LoginRequest
import com.clinica.domain.model.TokenResponse

interface AuthRepository {
    suspend fun login(request: LoginRequest): TokenResponse
    fun logout()
}
