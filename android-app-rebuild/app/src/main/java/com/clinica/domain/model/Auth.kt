package com.clinica.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    val role: String,
    @SerialName("full_name") val fullName: String
)

@Serializable
data class Page<T>(
    val items: List<T>,
    val total: Int,
    val page: Int,
    val pages: Int
)
