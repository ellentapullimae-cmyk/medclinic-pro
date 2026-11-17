package com.clinica.data.repository

import com.clinica.core.session.SessionManager
import com.clinica.data.remote.api.AuthApi
import com.clinica.domain.model.LoginRequest
import com.clinica.domain.model.TokenResponse
import com.clinica.domain.model.UserBrief
import com.clinica.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val session: SessionManager
) : AuthRepository {

    override val role: String get() = session.role
    override val fullName: String get() = session.fullName
    override val isLoggedIn: Boolean get() = session.isLoggedIn

    override suspend fun login(request: LoginRequest): TokenResponse {
        val token = api.login(request)
        session.save(token.accessToken, token.role, token.fullName)
        return token
    }

    override suspend fun doctors(): List<UserBrief> = api.doctors()

    override fun logout() {
        session.clear()
    }
}
