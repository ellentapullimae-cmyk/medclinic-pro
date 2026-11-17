package com.clinica.presentation.navigation

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val PATIENTS = "patients"
    const val PATIENT_DETAIL = "patients/{patientId}"
    const val APPOINTMENTS = "appointments"
    const val CLINICAL = "patients/{patientId}/records"
    const val BILLING = "billing"
    const val INVENTORY = "inventory"

    fun patientDetail(patientId: Int) = "patients/$patientId"
    fun clinical(patientId: Int) = "patients/$patientId/records"
}
