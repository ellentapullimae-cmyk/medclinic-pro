package com.clinica.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class InvoiceStatus { pendiente, pagada, anulada }

@Serializable
enum class PaymentMethod { efectivo, tarjeta, transferencia, seguro }

@Serializable
data class InvoiceItem(
    val id: Int,
    val concept: String,
    val quantity: Int,
    @SerialName("unit_price") val unitPrice: String,
    val subtotal: String
)

@Serializable
data class Payment(
    val id: Int,
    val amount: String,
    val method: PaymentMethod,
    @SerialName("paid_at") val paidAt: String
)

@Serializable
data class Invoice(
    val id: Int,
    val number: String,
    @SerialName("patient_id") val patientId: Int,
    @SerialName("appointment_id") val appointmentId: Int? = null,
    @SerialName("issue_date") val issueDate: String,
    val status: InvoiceStatus,
    val total: String,
    @SerialName("created_at") val createdAt: String? = null,
    val patient: PatientBrief,
    val items: List<InvoiceItem> = emptyList(),
    val payments: List<Payment> = emptyList()
)

@Serializable
data class InvoiceItemIn(
    val concept: String,
    val quantity: Int = 1,
    @SerialName("unit_price") val unitPrice: String
)

@Serializable
data class InvoiceCreate(
    @SerialName("patient_id") val patientId: Int,
    @SerialName("appointment_id") val appointmentId: Int? = null,
    val items: List<InvoiceItemIn>
)

@Serializable
data class PaymentIn(
    val amount: String,
    val method: PaymentMethod
)

typealias InvoiceInput = InvoiceCreate
typealias PaymentInput = PaymentIn

typealias InvoiceItemInput = InvoiceItemIn
