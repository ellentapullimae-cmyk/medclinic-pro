package com.clinica.presentation.clinical

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.clinica.domain.model.ClinicalRecord
import com.clinica.domain.model.ClinicalRecordInput
import com.clinica.domain.model.Patient
import com.clinica.domain.repository.AuthRepository
import com.clinica.domain.repository.ClinicalRepository
import com.clinica.domain.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClinicalUiState(
    val loading: Boolean = true,
    val items: List<ClinicalRecord> = emptyList(),
    val error: String? = null,
    val patient: Patient? = null,
)

@HiltViewModel
class ClinicalViewModel @Inject constructor(
    private val repository: ClinicalRepository,
    private val patientsRepository: PatientRepository,
    auth: AuthRepository,
) : ViewModel() {

    val canCreate: Boolean get() = auth.role in setOf("admin", "medico")

    private val _state = MutableStateFlow(ClinicalUiState())
    val state: StateFlow<ClinicalUiState> = _state.asStateFlow()

    fun load(patientId: Int) {
        viewModelScope.launch {
            _state.value = ClinicalUiState()
            val patient = runCatching { patientsRepository.get(patientId) }.getOrNull()
            try {
                val records = repository.list(patientId)
                _state.value = ClinicalUiState(loading = false, items = records, patient = patient)
            } catch (error: Throwable) {
                _state.value = ClinicalUiState(loading = false, error = ErrorMapper.message(error), patient = patient)
            }
        }
    }

    fun create(patientId: Int, input: ClinicalRecordInput, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.create(patientId, input)
                load(patientId)
                onDone()
            } catch (error: Throwable) {
                _state.value = _state.value.copy(loading = false, error = ErrorMapper.message(error))
                onDone()
            }
        }
    }
}

@Composable
fun ClinicalScreen(
    patientId: Int,
    onBack: () -> Unit,
    viewModel: ClinicalViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showForm by remember { mutableStateOf(false) }

    LaunchedEffect(patientId) { viewModel.load(patientId) }

    val patient = state.patient
    val title = if (patient != null) {
        "Historia ${patient.file_number}"
    } else {
        "Historia clínica"
    }

    ClinicScaffold(
        title = title,
        onBack = onBack,
        fab = {
            if (viewModel.canCreate) {
                FloatingActionButton(onClick = { showForm = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Nuevo registro")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> LoadingView()
                state.error != null -> ErrorView(state.error!!) { viewModel.load(patientId) }
                state.items.isEmpty() -> EmptyView("Sin consultas registradas")
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.id }) { record ->
                        RecordCard(record = record)
                    }
                }
            }
        }
    }

    if (showForm) {
        RecordFormDialog(
            onDismiss = { showForm = false },
            onCreate = { input ->
                viewModel.create(patientId, input) { showForm = false }
            },
        )
    }
}

@Composable
private fun RecordCard(record: ClinicalRecord) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = record.visit_date.replace("T", " ").take(16),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            record.diagnosis?.let {
                Text(text = "Diagnóstico: $it", style = MaterialTheme.typography.titleMedium)
            }
            record.treatment?.let {
                Text(text = "Tratamiento: $it", style = MaterialTheme.typography.bodyMedium)
            }
            record.notes?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "Firma: ${record.doctor?.full_name ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecordFormDialog(
    onDismiss: () -> Unit,
    onCreate: (ClinicalRecordInput) -> Unit,
) {
    var diagnosis by remember { mutableStateOf("") }
    var treatment by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva consulta") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(value = diagnosis, onValueChange = { diagnosis = it }, label = { Text("Diagnóstico") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = treatment, onValueChange = { treatment = it }, label = { Text("Tratamiento") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notas") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(
                        ClinicalRecordInput(
                            diagnosis = diagnosis.trim().ifBlank { null },
                            treatment = treatment.trim().ifBlank { null },
                            notes = notes.trim().ifBlank { null },
                        ),
                    )
                },
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
