package com.clinica.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AppointmentStatus { pendiente, confirmada, atendida, cancelada }

@Serializable
data class Appointment(
    val id: Int,
    @SerialName("patient_id") val patientId: Int,
    @SerialName("doctor_id") val doctorId: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val status: AppointmentStatus,
    val reason: String? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val patient: PatientBrief,
    val doctor: UserBrief
)

@Serializable
data class AppointmentCreate(
    @SerialName("patient_id") val patientId: Int,
    @SerialName("doctor_id") val doctorId: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    val reason: String? = null,
    val notes: String? = null
)

typealias AppointmentInput = AppointmentCreate
@Serializable
data class AppointmentUpdate(
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    val status: AppointmentStatus? = null,
    val reason: String? = null,
    val notes: String? = null
)
