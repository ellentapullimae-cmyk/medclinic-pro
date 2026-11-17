package com.clinica.data.repository

import com.clinica.data.remote.api.AppointmentApi
import com.clinica.domain.model.Appointment
import com.clinica.domain.model.AppointmentCreate
import com.clinica.domain.model.AppointmentStatus
import com.clinica.domain.model.AppointmentUpdate
import com.clinica.domain.repository.AppointmentRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppointmentRepositoryImpl @Inject constructor(
    private val api: AppointmentApi
) : AppointmentRepository {

    override suspend fun list(
        date: String?,
        doctorId: Int?,
        status: AppointmentStatus?
    ): List<Appointment> = api.list(date, doctorId, status?.name).items

    override suspend fun create(appointment: AppointmentCreate): Appointment =
        api.create(appointment)

    override suspend fun updateStatus(id: Int, status: AppointmentStatus): Appointment =
        api.update(id, AppointmentUpdate(status = status))

    override suspend fun cancel(id: Int) {
        api.cancel(id)
    }
}
