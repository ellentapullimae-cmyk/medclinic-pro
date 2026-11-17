package com.clinica.data.repository

import com.clinica.data.remote.api.BillingApi
import com.clinica.domain.model.Invoice
import com.clinica.domain.model.InvoiceCreate
import com.clinica.domain.model.InvoiceStatus
import com.clinica.domain.model.PaymentIn
import com.clinica.domain.repository.BillingRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingRepositoryImpl @Inject constructor(
    private val api: BillingApi
) : BillingRepository {

    override suspend fun list(status: InvoiceStatus?, patientId: Int?): List<Invoice> =
        api.list(status?.name, patientId).items

    override suspend fun create(invoice: InvoiceCreate): Invoice = api.create(invoice)

    override suspend fun get(id: Int): Invoice = api.get(id)

    override suspend fun pay(id: Int, payment: PaymentIn): Invoice = api.pay(id, payment)

    override suspend fun cancel(id: Int): Invoice = api.cancel(id)
}
