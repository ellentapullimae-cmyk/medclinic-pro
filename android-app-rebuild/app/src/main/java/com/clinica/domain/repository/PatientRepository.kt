package com.clinica.domain.repository

import com.clinica.domain.model.Page
import com.clinica.domain.model.Patient
import com.clinica.domain.model.PatientCreate

interface PatientRepository {
    suspend fun list(query: String?, page: Int): Page<Patient>
    suspend fun get(id: Int): Patient
    suspend fun create(patient: PatientCreate): Patient
    suspend fun update(id: Int, patient: PatientCreate): Patient
    suspend fun deactivate(id: Int)
}
