package com.clinica.presentation.billing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.clinica.core.network.ErrorMapper
import com.clinica.core.ui.ClinicScaffold
import com.clinica.core.ui.EmptyView
import com.clinica.core.ui.ErrorView
import com.clinica.core.ui.LoadingView
import com.clinica.core.ui.money
import com.clinica.domain.model.Invoice
import com.clinica.domain.model.InvoiceInput
import com.clinica.domain.model.InvoiceItemInput
import com.clinica.domain.model.Patient
import com.clinica.domain.model.PaymentInput
import com.clinica.domain.repository.AuthRepository
import com.clinica.domain.repository.BillingRepository
import com.clinica.domain.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BillingUiState(
    val loading: Boolean = true,
    val items: List<Invoice> = emptyList(),
    val error: String? = null,
    val patients: List<Patient> = emptyList(),
)

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val repository: BillingRepository,
    private val patientsRepository: PatientRepository,
    auth: AuthRepository,
) : ViewModel() {

    val canManage: Boolean get() = auth.role in setOf("admin", "recepcion")
    val isAdmin: Boolean get() = auth.role == "admin"

    private val _state = MutableStateFlow(BillingUiState())
    val state: StateFlow<BillingUiState> = _state.asStateFlow()

    private var statusFilter: String? = null

    init {
        refresh()
    }

    fun filter(status: String?) {
        statusFilter = status
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val invoices = try {
                repository.list(statusFilter)
            } catch (error: Throwable) {
                _state.value = _state.value.copy(loading = false, error = ErrorMapper.message(error))
                return@launch
            }
            val patients = runCatching { patientsRepository.list(null) }.getOrDefault(emptyList())
            _state.value = BillingUiState(loading = false, items = invoices, patients = patients)
        }
    }

    fun create(patientId: Int, concept: String, quantity: Int, unitPrice: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.create(
                    InvoiceInput(
                        patient_id = patientId,
                        items = listOf(InvoiceItemInput(concept, quantity, unitPrice)),
                    ),
                )
                refresh()
                onDone()
            } catch (error: Throwable) {
                _state.value = _state.value.copy(loading = false, error = ErrorMapper.message(error))
                onDone()
            }
        }
    }

    fun pay(invoiceId: Int, amount: String, method: String) {
        viewModelScope.launch {
            try {
                repository.pay(invoiceId, PaymentInput(amount = amount, method = method))
                refresh()
            } catch (error: Throwable) {
                _state.value = _state.value.copy(loading = false, error = ErrorMapper.message(error))
            }
        }
    }

    fun cancel(invoiceId: Int) {
        viewModelScope.launch {
            try {
                repository.cancel(invoiceId)
                refresh()
            } catch (error: Throwable) {
                _state.value = _state.value.copy(loading = false, error = ErrorMapper.message(error))
            }
        }
    }
}

private fun pendingOf(invoice: Invoice): Double {
    val total = invoice.total.toDoubleOrNull() ?: 0.0
    val paid = invoice.payments.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    return total - paid
}

@Composable
fun BillingScreen(
    onBack: () -> Unit,
    viewModel: BillingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var payTarget by remember { mutableStateOf<Invoice?>(null) }

    ClinicScaffold(
        title = "Facturación",
        onBack = onBack,
        fab = {
            if (viewModel.canManage) {
                FloatingActionButton(onClick = { showCreate = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Nueva factura")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(onClick = { viewModel.filter(null) }, label = { Text("Todas") })
                AssistChip(onClick = { viewModel.filter("pendiente") }, label = { Text("Pendientes") })
                AssistChip(onClick = { viewModel.filter("pagada") }, label = { Text("Pagadas") })
            }
            when {
                state.loading -> LoadingView()
                state.error != null -> ErrorView(state.error!!) { viewModel.refresh() }
                state.items.isEmpty() -> EmptyView("Sin facturas")
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.id }) { invoice ->
                        InvoiceCard(
                            invoice = invoice,
                            canPay = viewModel.canManage && invoice.status == "pendiente",
                            canCancel = viewModel.isAdmin && invoice.status == "pendiente" && invoice.payments.isEmpty(),
                            onPay = { payTarget = invoice },
                            onCancel = { viewModel.cancel(invoice.id) },
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        InvoiceFormDialog(
            patients = state.patients,
            onDismiss = { showCreate = false },
            onCreate = { patientId, concept, quantity, price ->
                viewModel.create(patientId, concept, quantity, price) { showCreate = false }
            },
        )
    }

    payTarget?.let { invoice ->
        PaymentDialog(
            invoice = invoice,
            onDismiss = { payTarget = null },
            onPay = { amount, method ->
                viewModel.pay(invoice.id, amount, method)
                payTarget = null
            },
        )
    }
}

@Composable
private fun InvoiceCard(
    invoice: Invoice,
    canPay: Boolean,
    canCancel: Boolean,
    onPay: () -> Unit,
    onCancel: () -> Unit,
) {
    val patientName = invoice.patient?.let { "${it.last_name}, ${it.first_name}" } ?: "#${invoice.patient_id}"

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = invoice.number, style = MaterialTheme.typography.titleMedium)
                AssistChip(onClick = {}, label = { Text(invoice.status) })
            }
            Text(text = patientName, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Total: ${invoice.total}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            if (invoice.status == "pendiente") {
                Text(
                    text = "Saldo: ${money(pendingOf(invoice))}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canPay) TextButton(onClick = onPay) { Text("Cobrar") }
                if (canCancel) TextButton(onClick = onCancel) { Text("Anular") }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun InvoiceFormDialog(
    patients: List<Patient>,
    onDismiss: () -> Unit,
    onCreate: (patientId: Int, concept: String, quantity: Int, unitPrice: String) -> Unit,
) {
    var patient by remember { mutableStateOf<Patient?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var concept by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var price by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva factura") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = patient?.let { "${it.last_name}, ${it.first_name}" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Paciente") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        patients.forEach { option ->
                            DropdownMenuItem(
                                text = { Text("${option.last_name}, ${option.first_name}") },
                                onClick = {
                                    patient = option
                                    expanded = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(value = concept, onValueChange = { concept = it }, label = { Text("Concepto") }, singleLine = true)
                OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Cantidad") }, singleLine = true)
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Precio unitario") }, singleLine = true)
            }
        },
        confirmButton = {
            val qty = quantity.toIntOrNull() ?: 0
            val valid = patient != null && concept.isNotBlank() && qty > 0 && price.toDoubleOrNull() != null
            TextButton(
                enabled = valid,
                onClick = {
                    val selected = patient ?: return@TextButton
                    onCreate(selected.id, concept.trim(), qty, price.trim())
                },
            ) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun PaymentDialog(
    invoice: Invoice,
    onDismiss: () -> Unit,
    onPay: (amount: String, method: String) -> Unit,
) {
    val pending = pendingOf(invoice)
    var amount by remember { mutableStateOf(money(pending)) }
    var method by remember { mutableStateOf("efectivo") }
    val methods = listOf("efectivo", "tarjeta", "transferencia", "seguro")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cobrar ${invoice.number}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Importe") }, singleLine = true)
                Text(text = "Método: $method", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    methods.forEach { option ->
                        AssistChip(
                            onClick = { method = option },
                            label = { Text(option) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = amount.toDoubleOrNull() != null && (amount.toDoubleOrNull() ?: 0.0) > 0.0,
                onClick = { onPay(amount.trim(), method) },
            ) { Text("Registrar pago") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
