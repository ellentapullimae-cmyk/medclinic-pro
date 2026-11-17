package com.clinica.presentation.patients

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.clinica.core.ui.ClinicScaffold
import com.clinica.core.ui.EmptyView
import com.clinica.core.ui.ErrorMapper
import com.clinica.core.ui.ErrorView
import com.clinica.core.ui.Gold
import com.clinica.core.ui.GoldDark
import com.clinica.core.ui.GoldLight
import com.clinica.core.ui.Ivory
import com.clinica.core.ui.LoadingView
import com.clinica.core.ui.LuxBlack
import com.clinica.core.ui.PureWhite
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
    private val authRepository: AuthRepository,
) : ViewModel() {

    val canManage: Boolean get() = authRepository.role in setOf("admin", "recepcion")
    val canOpenHistory: Boolean get() = authRepository.role in setOf("admin", "medico")

    private val _state = MutableStateFlow<UiState<List<Patient>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Patient>>> = _state.asStateFlow()

    init {
        search(null)
    }

    fun search(query: String?) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Success(repository.list(query, 1).items)
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
    onPatientClick: (Int) -> Unit,
    onOpenClinical: (Int) -> Unit,
    viewModel: PatientsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }

    ClinicScaffold(
        title = "Pacientes",
        fab = {
            if (viewModel.canManage) {
                FloatingActionButton(
                    onClick = { showForm = true },
                    containerColor = LuxBlack,
                    contentColor = GoldLight,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Nuevo paciente")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(Ivory).padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.search(it.ifBlank { null })
                },
                label = { Text("Buscar por nombre, DNI o historia", color = GoldDark) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = Gold.copy(alpha = 0.45f),
                    focusedLabelColor = GoldDark,
                    unfocusedLabelColor = GoldDark,
                    cursorColor = GoldDark,
                    focusedContainerColor = PureWhite,
                    unfocusedContainerColor = PureWhite,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            when (val current = state) {
                is UiState.Loading -> LoadingView()
                is UiState.Error -> ErrorView(current.message, onRetry = { viewModel.search(query.ifBlank { null }) })
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
                                    onClick = { onPatientClick(patient.id) },
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
    onClick: () -> Unit,
    onOpenClinical: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${patient.lastName}, ${patient.firstName}",
                style = MaterialTheme.typography.titleMedium,
                color = LuxBlack,
            )
            Text(
                text = "Historia ${patient.fileNumber} · DNI ${patient.dni}",
                style = MaterialTheme.typography.bodySmall,
                color = GoldDark,
            )
            patient.phone?.let {
                Text(text = "Tel: $it", style = MaterialTheme.typography.bodySmall, color = LuxBlack)
            }
            if (canOpenHistory) {
                Row {
                    TextButton(onClick = onOpenClinical) {
                        Text("Ver historia clínica", color = GoldDark)
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
                OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("Nombre (mín. 2)") }, singleLine = true)
                OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Apellido (mín. 2)") }, singleLine = true)
                OutlinedTextField(value = dni, onValueChange = { dni = it }, label = { Text("DNI (mín. 3)") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono") }, singleLine = true)
            }
        },
        confirmButton = {
            val canCreate = firstName.trim().length >= 2 &&
                lastName.trim().length >= 2 &&
                dni.trim().length >= 3
            TextButton(
                enabled = canCreate,
                onClick = {
                    onCreate(
                        PatientInput(
                            firstName = firstName.trim(),
                            lastName = lastName.trim(),
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
