package com.clinica.core.session

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("clinica_session", Context.MODE_PRIVATE)

    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    val token: String?
        get() = prefs.getString(KEY_TOKEN, null)

    val role: String
        get() = prefs.getString(KEY_ROLE, "") ?: ""

    val fullName: String
        get() = prefs.getString(KEY_NAME, "") ?: ""

    val isLoggedIn: Boolean
        get() = !token.isNullOrEmpty()

    fun save(token: String, role: String, fullName: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_ROLE, role)
            .putString(KEY_NAME, fullName)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun expire() {
        val hadSession = isLoggedIn
        clear()
        if (hadSession) {
            _sessionExpired.tryEmit(Unit)
        }
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_ROLE = "role"
        const val KEY_NAME = "full_name"
    }
}
