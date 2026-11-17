package com.clinica.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Role { admin, medico, recepcion }

@Serializable
data class UserBrief(
    val id: Int,
    @SerialName("full_name") val fullName: String,
    val specialty: String? = null
)
