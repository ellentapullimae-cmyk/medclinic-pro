package com.clinica.data.di

import com.clinica.core.network.ApiClient
import com.clinica.core.session.SessionManager
import com.clinica.data.remote.api.AppointmentApi
import com.clinica.data.remote.api.AuthApi
import com.clinica.data.remote.api.BillingApi
import com.clinica.data.remote.api.ClinicalApi
import com.clinica.data.remote.api.InventoryApi
import com.clinica.data.remote.api.PatientApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(session: SessionManager): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(ApiClient.authInterceptor(session))
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = ApiClient.retrofit(client)

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun providePatientApi(retrofit: Retrofit): PatientApi = retrofit.create(PatientApi::class.java)

    @Provides
    @Singleton
    fun provideAppointmentApi(retrofit: Retrofit): AppointmentApi =
        retrofit.create(AppointmentApi::class.java)

    @Provides
    @Singleton
    fun provideClinicalApi(retrofit: Retrofit): ClinicalApi =
        retrofit.create(ClinicalApi::class.java)

    @Provides
    @Singleton
    fun provideBillingApi(retrofit: Retrofit): BillingApi = retrofit.create(BillingApi::class.java)

    @Provides
    @Singleton
    fun provideInventoryApi(retrofit: Retrofit): InventoryApi =
        retrofit.create(InventoryApi::class.java)
}
