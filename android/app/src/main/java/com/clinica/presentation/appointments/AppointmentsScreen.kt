package com.clinica.presentation.appointments

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
import com.clinica.domain.model.Appointment
import com.clinica.domain.model.AppointmentInput
import com.clinica.domain.model.Patient
import com.clinica.domain.model.UserBrief
import com.clinica.domain.repository.AppointmentRepository
import com.clinica.domain.repository.AuthRepository
import com.clinica.domain.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppointmentsUiState(
    val loading: Boolean = true,
    val items: List<Appointment> = emptyList(),
    val error: String? = null,
    val patients: List<Patient> = emptyList(),
    val doctors: List<UserBrief> = emptyList(),
)

@HiltViewModel
class AppointmentsViewModel @Inject constructor(
    private val repository: AppointmentRepository,
    private val patientsRepository: PatientRepository,
    auth: AuthRepository,
) : ViewModel() {

    val canCreate: Boolean get() = auth.role in setOf("admin", "recepcion")
    val canCancel: Boolean get() = auth.role in setOf("admin", "recepcion")

    private val _state = MutableStateFlow(AppointmentsUiState())
    val state: StateFlow<AppointmentsUiState> = _state.asStateFlow()

    private var dateFilter: String? = null

    init {
        refresh()
    }

    fun filterByDate(date: String?) {
        dateFilter = date?.takeIf { it.matches(Regex("""\d{4}-\d{2}-\d{2}""")) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val appointments = try {
                repository.list(dateFilter, null)
            } catch (error: Throwable) {
                _state.value = _state.value.copy(loading = false, error = ErrorMapper.message(error))
                return@launch
            }
            val patients = runCatching { patientsRepository.list(null) }.getOrDefault(emptyList())
            val doctors = runCatching { repository.doctors() }.getOrDefault(emptyList())
            _state.value = AppointmentsUiState(
                loading = false,
                items = appointments,
                patients = patients,
                doctors = doctors,
            )
        }
    }

    fun create(input: AppointmentInput, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.create(input)
                refresh()
                onDone()
            } catch (error: Throwable) {
                _state.value = _state.value.copy(loading = false, error = ErrorMapper.message(error))
                onDone()
            }
        }
    }

    fun setStatus(id: Int, status: String) {
        viewModelScope.launch {
            try {
                repository.update(id, com.clinica.domain.model.AppointmentUpdate(status = status))
                refresh()
            } catch (error: Throwable) {
                _state.value = _state.value.copy(loading = false, error = ErrorMapper.message(error))
            }
        }
    }

    fun cancel(id: Int) {
        viewModelScope.launch {
            try {
                repository.cancel(id)
                refresh()
            } catch (error: Throwable) {
                _state.value = _state.value.copy(loading = false, error = ErrorMapper.message(error))
            }
        }
    }
}

@Composable
fun AppointmentsScreen(
    onBack: () -> Unit,
    viewModel: AppointmentsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showForm by remember { mutableStateOf(false) }
    var dateQuery by remember { mutableStateOf("") }

    ClinicScaffold(
        title = "Citas",
        onBack = onBack,
        fab = {
            if (viewModel.canCreate) {
                FloatingActionButton(onClick = { showForm = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Nueva cita")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = dateQuery,
                onValueChange = {
                    dateQuery = it
                    viewModel.filterByDate(it.ifBlank { null })
                },
                label = { Text("Fecha (aaaa-mm-dd)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            state.error?.let { message ->
                ErrorView(message) { viewModel.refresh() }
            }
            if (state.error == null) {
                when {
                    state.loading -> LoadingView()
                    state.items.isEmpty() -> EmptyView("No hay citas para ese filtro")
                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.items, key = { it.id }) { appointment ->
                            AppointmentCard(
                                appointment = appointment,
                                canCancel = viewModel.canCancel,
                                onConfirm = { viewModel.setStatus(appointment.id, "confirmada") },
                                onAttend = { viewModel.setStatus(appointment.id, "atendida") },
                                onCancel = { viewModel.cancel(appointment.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        AppointmentFormDialog(
            patients = state.patients,
            doctors = state.doctors,
            onDismiss = { showForm = false },
            onCreate = { input -> viewModel.create(input) { showForm = false } },
        )
    }
}

@Composable
private fun AppointmentCard(
    appointment: Appointment,
    canCancel: Boolean,
    onConfirm: () -> Unit,
    onAttend: () -> Unit,
    onCancel: () -> Unit,
) {
    val patientName = appointment.patient?.let { "${it.last_name}, ${it.first_name}" } ?: "#${appointment.patient_id}"
    val doctorName = appointment.doctor?.full_name ?: "#${appointment.doctor_id}"
    val time = appointment.start_time.replace("T", " ").take(16)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = patientName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "$doctorName · $time",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            appointment.reason?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(appointment.status) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (appointment.status == "pendiente") {
                    TextButton(onClick = onConfirm) { Text("Confirmar") }
                }
                if (appointment.status == "confirmada") {
                    TextButton(onClick = onAttend) { Text("Atender") }
                }
                if (canCancel && appointment.status in listOf("pendiente", "confirmada")) {
                    TextButton(onClick = onCancel) { Text("Cancelar") }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AppointmentFormDialog(
    patients: List<Patient>,
    doctors: List<UserBrief>,
    onDismiss: () -> Unit,
    onCreate: (AppointmentInput) -> Unit,
) {
    var patient by remember { mutableStateOf<Patient?>(null) }
    var doctor by remember { mutableStateOf<UserBrief?>(null) }
    var patientExpanded by remember { mutableStateOf(false) }
    var doctorExpanded by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva cita") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExposedDropdownMenuBox(
                    expanded = patientExpanded,
                    onExpandedChange = { patientExpanded = it },
                ) {
                    OutlinedTextField(
                        value = patient?.let { "${it.last_name}, ${it.first_name}" } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Paciente") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = patientExpanded) },
                        modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = patientExpanded,
                        onDismissRequest = { patientExpanded = false },
                    ) {
                        patients.forEach { option ->
                            DropdownMenuItem(
                                text = { Text("${option.last_name}, ${option.first_name}") },
                                onClick = {
                                    patient = option
                                    patientExpanded = false
                                },
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = doctorExpanded,
                    onExpandedChange = { doctorExpanded = it },
                ) {
                    OutlinedTextField(
                        value = doctor?.full_name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Médico") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = doctorExpanded) },
                        modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = doctorExpanded,
                        onDismissRequest = { doctorExpanded = false },
                    ) {
                        doctors.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.full_name) },
                                onClick = {
                                    doctor = option
                                    doctorExpanded = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Fecha (aaaa-mm-dd)") }, singleLine = true)
                OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("Inicio (hh:mm)") }, singleLine = true)
                OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("Fin (hh:mm)") }, singleLine = true)
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Motivo") }, singleLine = true)
            }
        },
        confirmButton = {
            val valid = patient != null && doctor != null &&
                date.matches(Regex("""\d{4}-\d{2}-\d{2}""")) &&
                start.matches(Regex("""\d{2}:\d{2}""")) &&
                end.matches(Regex("""\d{2}:\d{2}"""))
            TextButton(
                enabled = valid,
                onClick = {
                    val selectedPatient = patient ?: return@TextButton
                    val selectedDoctor = doctor ?: return@TextButton
                    onCreate(
                        AppointmentInput(
                            patient_id = selectedPatient.id,
                            doctor_id = selectedDoctor.id,
                            start_time = "${date}T${start}:00",
                            end_time = "${date}T${end}:00",
                            reason = reason.trim().ifBlank { null },
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
