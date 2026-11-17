package com.clinica.data.repository

import com.clinica.data.remote.api.ClinicalApi
import com.clinica.domain.model.ClinicalRecord
import com.clinica.domain.model.ClinicalRecordCreate
import com.clinica.domain.repository.ClinicalRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClinicalRepositoryImpl @Inject constructor(
    private val api: ClinicalApi
) : ClinicalRepository {

    override suspend fun listByPatient(patientId: Int): List<ClinicalRecord> =
        api.listByPatient(patientId)

    override suspend fun create(patientId: Int, record: ClinicalRecordCreate): ClinicalRecord =
        api.create(patientId, record)

    override suspend fun get(id: Int): ClinicalRecord = api.get(id)
}
