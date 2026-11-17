package com.clinica.data.repository

import com.clinica.data.remote.api.PatientApi
import com.clinica.domain.model.Page
import com.clinica.domain.model.Patient
import com.clinica.domain.model.PatientCreate
import com.clinica.domain.repository.PatientRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatientRepositoryImpl @Inject constructor(
    private val api: PatientApi
) : PatientRepository {

    override suspend fun list(query: String?, page: Int): Page<Patient> =
        api.list(query, page)

    override suspend fun get(id: Int): Patient = api.get(id)

    override suspend fun create(patient: PatientCreate): Patient = api.create(patient)

    override suspend fun update(id: Int, patient: PatientCreate): Patient =
        api.update(id, patient)

    override suspend fun deactivate(id: Int) {
        api.delete(id)
    }
}
