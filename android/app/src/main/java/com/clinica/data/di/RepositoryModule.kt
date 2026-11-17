package com.clinica.data.di

import com.clinica.data.repository.AppointmentRepositoryImpl
import com.clinica.data.repository.AuthRepositoryImpl
import com.clinica.data.repository.BillingRepositoryImpl
import com.clinica.data.repository.ClinicalRepositoryImpl
import com.clinica.data.repository.InventoryRepositoryImpl
import com.clinica.data.repository.PatientRepositoryImpl
import com.clinica.domain.repository.AppointmentRepository
import com.clinica.domain.repository.AuthRepository
import com.clinica.domain.repository.BillingRepository
import com.clinica.domain.repository.ClinicalRepository
import com.clinica.domain.repository.InventoryRepository
import com.clinica.domain.repository.PatientRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindPatientRepository(impl: PatientRepositoryImpl): PatientRepository

    @Binds
    abstract fun bindAppointmentRepository(impl: AppointmentRepositoryImpl): AppointmentRepository

    @Binds
    abstract fun bindClinicalRepository(impl: ClinicalRepositoryImpl): ClinicalRepository

    @Binds
    abstract fun bindBillingRepository(impl: BillingRepositoryImpl): BillingRepository

    @Binds
    abstract fun bindInventoryRepository(impl: InventoryRepositoryImpl): InventoryRepository
}
