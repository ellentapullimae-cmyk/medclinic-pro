package com.clinica.domain.repository

import com.clinica.domain.model.ClinicalRecord
import com.clinica.domain.model.ClinicalRecordCreate

interface ClinicalRepository {
    suspend fun listByPatient(patientId: Int): List<ClinicalRecord>
    suspend fun create(patientId: Int, record: ClinicalRecordCreate): ClinicalRecord
    suspend fun get(id: Int): ClinicalRecord
}
