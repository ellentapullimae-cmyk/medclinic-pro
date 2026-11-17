package com.clinica.domain.repository

import com.clinica.domain.model.Appointment
import com.clinica.domain.model.AppointmentCreate
import com.clinica.domain.model.AppointmentStatus

interface AppointmentRepository {
    suspend fun list(
        date: String? = null,
        doctorId: Int? = null,
        status: AppointmentStatus? = null
    ): List<Appointment>

    suspend fun create(appointment: AppointmentCreate): Appointment
    suspend fun updateStatus(id: Int, status: AppointmentStatus): Appointment
    suspend fun cancel(id: Int)
}
