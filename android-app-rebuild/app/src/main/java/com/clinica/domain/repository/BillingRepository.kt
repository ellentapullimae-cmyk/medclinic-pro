package com.clinica.domain.repository

import com.clinica.domain.model.Invoice
import com.clinica.domain.model.InvoiceCreate
import com.clinica.domain.model.InvoiceStatus
import com.clinica.domain.model.PaymentIn

interface BillingRepository {
    suspend fun list(status: InvoiceStatus? = null, patientId: Int? = null): List<Invoice>
    suspend fun create(invoice: InvoiceCreate): Invoice
    suspend fun get(id: Int): Invoice
    suspend fun pay(id: Int, payment: PaymentIn): Invoice
    suspend fun cancel(id: Int): Invoice
}
