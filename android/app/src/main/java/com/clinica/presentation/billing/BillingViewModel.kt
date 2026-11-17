package com.clinica.presentation.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clinica.domain.model.Invoice
import com.clinica.domain.model.InvoiceCreate
import com.clinica.domain.model.InvoiceItemIn
import com.clinica.domain.model.InvoiceStatus
import com.clinica.domain.model.PaymentIn
import com.clinica.domain.model.PaymentMethod
import com.clinica.domain.repository.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BillingUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val invoices: List<Invoice> = emptyList(),
    val patientIdInput: String = "",
    val conceptInput: String = "",
    val amountInput: String = "",
    val saving: Boolean = false,
    val saveError: String? = null
)

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val repository: BillingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BillingUiState())
    val state: StateFlow<BillingUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun onPatientIdChange(v: String) { _state.value = _state.value.copy(patientIdInput = v) }
    fun onConceptChange(v: String) { _state.value = _state.value.copy(conceptInput = v) }
    fun onAmountChange(v: String) { _state.value = _state.value.copy(amountInput = v) }

    fun createInvoice() {
        val current = _state.value
        val patientId = current.patientIdInput.toIntOrNull()
        if (patientId == null || current.conceptInput.isBlank() || current.amountInput.isBlank()) {
            _state.value = current.copy(saveError = "Completa paciente, concepto e importe")
            return
        }
        _state.value = current.copy(saving = true, saveError = null)
        viewModelScope.launch {
            try {
                repository.create(
                    InvoiceCreate(
                        patientId = patientId,
                        items = listOf(
                            InvoiceItemIn(
                                concept = current.conceptInput.trim(),
                                quantity = 1,
                                unitPrice = current.amountInput.trim()
                            )
                        )
                    )
                )
                _state.value = _state.value.copy(
                    saving = false,
                    patientIdInput = "",
                    conceptInput = "",
                    amountInput = ""
                )
                load()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    saving = false,
                    saveError = e.message ?: "Error creando factura"
                )
            }
        }
    }

    fun pay(invoiceId: Int, amount: String) {
        viewModelScope.launch {
            try {
                repository.pay(invoiceId, PaymentIn(amount = amount, method = PaymentMethod.efectivo))
                load()
            } catch (e: Exception) {
                _state.value = _state.value.copy(saveError = e.message)
            }
        }
    }

    private fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val invoices = repository.list(status = InvoiceStatus.pendiente)
                _state.value = _state.value.copy(loading = false, invoices = invoices)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Error cargando facturas"
                )
            }
        }
    }
}
