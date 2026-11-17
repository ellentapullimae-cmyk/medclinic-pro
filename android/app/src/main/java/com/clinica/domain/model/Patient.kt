package com.clinica.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Sex { M, F, O }

@Serializable
data class PatientBrief(
    val id: Int,
    @SerialName("file_number") val fileNumber: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String
)

@Serializable
data class Patient(
    val id: Int,
    @SerialName("file_number") val fileNumber: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val dni: String,
    @SerialName("birth_date") val birthDate: String? = null,
    val sex: Sex? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val insurance: String? = null,
    val allergies: String? = null,
    val notes: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class PatientCreate(
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val dni: String,
    @SerialName("birth_date") val birthDate: String? = null,
    val sex: Sex? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val insurance: String? = null,
    val allergies: String? = null,
    val notes: String? = null
)

typealias PatientInput = PatientCreate
