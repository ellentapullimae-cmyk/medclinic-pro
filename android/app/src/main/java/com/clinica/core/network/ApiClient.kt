package com.clinica.core.network

import com.clinica.core.session.SessionManager
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType

object ApiClient {

    const val BASE_URL = "http://10.0.2.2:8000/"

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    fun authInterceptor(session: SessionManager): Interceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
        session.token?.let { builder.header("Authorization", "Bearer $it") }
        val response = chain.proceed(builder.build())
        if (response.code == 401 && session.isLoggedIn) {
            session.clear()
        }
        response
    }

    fun retrofit(okHttpClient: okhttp3.OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
