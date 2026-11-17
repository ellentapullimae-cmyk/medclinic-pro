package com.clinica.presentation.patients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.clinica.core.ui.UiState
import com.clinica.domain.model.Patient
import com.clinica.domain.model.PatientInput
import com.clinica.domain.repository.AuthRepository
import com.clinica.domain.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PatientsViewModel @Inject constructor(
    private val repository: PatientRepository,
    auth: AuthRepository,
) : ViewModel() {

    val canManage: Boolean get() = auth.role in setOf("admin", "recepcion")
    val canOpenHistory: Boolean get() = auth.role in setOf("admin", "medico")

    private val _state = MutableStateFlow<UiState<List<Patient>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Patient>>> = _state.asStateFlow()

    init {
        search(null)
    }

    fun search(query: String?) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Success(repository.list(query))
            } catch (error: Throwable) {
                UiState.Error(ErrorMapper.message(error))
            }
        }
    }

    fun create(input: PatientInput, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.create(input)
                search(null)
                onDone()
            } catch (error: Throwable) {
                _state.value = UiState.Error(ErrorMapper.message(error))
            }
        }
    }
}

@Composable
fun PatientsScreen(
    onBack: () -> Unit,
    onOpenClinical: (Int) -> Unit,
    viewModel: PatientsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }

    ClinicScaffold(
        title = "Pacientes",
        onBack = onBack,
        fab = {
            if (viewModel.canManage) {
                FloatingActionButton(onClick = { showForm = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Nuevo paciente")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.search(it.ifBlank { null })
                },
                label = { Text("Buscar por nombre, DNI o historia") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            when (val current = state) {
                is UiState.Loading -> LoadingView()
                is UiState.Error -> ErrorView(current.message) { viewModel.search(query.ifBlank { null }) }
                is UiState.Success -> {
                    if (current.data.isEmpty()) {
                        EmptyView("Sin pacientes")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(current.data, key = { it.id }) { patient ->
                                PatientCard(
                                    patient = patient,
                                    canOpenHistory = viewModel.canOpenHistory,
                                    onOpenClinical = { onOpenClinical(patient.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        PatientFormDialog(
            onDismiss = { showForm = false },
            onCreate = { input ->
                viewModel.create(input) { showForm = false }
            },
        )
    }
}

@Composable
private fun PatientCard(
    patient: Patient,
    canOpenHistory: Boolean,
    onOpenClinical: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${patient.last_name}, ${patient.first_name}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Historia ${patient.file_number} · DNI ${patient.dni}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            patient.phone?.let {
                Text(text = "Tel: $it", style = MaterialTheme.typography.bodySmall)
            }
            if (canOpenHistory) {
                Row {
                    TextButton(onClick = onOpenClinical) {
                        Text("Ver historia clínica")
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientFormDialog(
    onDismiss: () -> Unit,
    onCreate: (PatientInput) -> Unit,
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var dni by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo paciente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("Nombre") }, singleLine = true)
                OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Apellido") }, singleLine = true)
                OutlinedTextField(value = dni, onValueChange = { dni = it }, label = { Text("DNI") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                enabled = firstName.isNotBlank() && lastName.isNotBlank() && dni.isNotBlank(),
                onClick = {
                    onCreate(
                        PatientInput(
                            first_name = firstName.trim(),
                            last_name = lastName.trim(),
                            dni = dni.trim(),
                            phone = phone.trim().ifBlank { null },
                        ),
                    )
                },
            ) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
