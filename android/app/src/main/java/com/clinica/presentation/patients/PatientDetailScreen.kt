package com.clinica.presentation.patients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clinica.core.ui.ErrorBox
import com.clinica.core.ui.LoadingBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    patientId: Int,
    onBack: () -> Unit,
    onOpenRecords: () -> Unit,
    viewModel: PatientDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(state.patient?.let { "${it.firstName} ${it.lastName}" } ?: "Paciente") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
            },
            actions = {
                IconButton(onClick = onOpenRecords) {
                    Icon(Icons.Default.History, contentDescription = "Historia clínica")
                }
            }
        )

        when {
            state.loading -> LoadingBox()
            state.error != null -> ErrorBox(state.error ?: "")
            else -> state.patient?.let { patient ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Historia clínica: ${patient.fileNumber}", style = MaterialTheme.typography.titleMedium)
                            Text("DNI: ${patient.dni}")
                            Text("Nacimiento: ${patient.birthDate ?: "-"}")
                            Text("Sexo: ${patient.sex?.name ?: "-"}")
                            Text("Teléfono: ${patient.phone ?: "-"}")
                            Text("Email: ${patient.email ?: "-"}")
                            Text("Dirección: ${patient.address ?: "-"}")
                            Text("Obra social: ${patient.insurance ?: "-"}")
                            Text("Alergias: ${patient.allergies ?: "-"}")
                            Text("Notas: ${patient.notes ?: "-"}")
                            Text("Activo: ${if (patient.isActive) "Sí" else "No"}")
                        }
                    }
                    Button(onClick = onOpenRecords, modifier = Modifier.fillMaxWidth()) {
                        Text("Ver historia clínica")
                    }
                }
            }
        }
    }
}
