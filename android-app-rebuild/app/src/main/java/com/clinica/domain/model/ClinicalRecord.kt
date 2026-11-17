package com.clinica.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClinicalRecord(
    val id: Int,
    @SerialName("patient_id") val patientId: Int,
    @SerialName("doctor_id") val doctorId: Int,
    @SerialName("visit_date") val visitDate: String,
    val diagnosis: String? = null,
    val treatment: String? = null,
    val notes: String? = null,
    val vitals: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val patient: PatientBrief,
    val doctor: UserBrief
)

@Serializable
data class ClinicalRecordCreate(
    val diagnosis: String? = null,
    val treatment: String? = null,
    val notes: String? = null,
    val vitals: String? = null,
    @SerialName("visit_date") val visitDate: String? = null
)

typealias ClinicalRecordInput = ClinicalRecordCreate
