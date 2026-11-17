package com.clinica.presentation.patients

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clinica.core.ui.ErrorBox
import com.clinica.core.ui.GoldLight
import com.clinica.core.ui.Ivory
import com.clinica.core.ui.LoadingBox
import com.clinica.core.ui.LuxBlack
import com.clinica.core.ui.PureWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    patientId: Int,
    onBack: () -> Unit,
    onOpenRecords: () -> Unit,
    viewModel: PatientDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Ivory)) {
        TopAppBar(
            title = {
                Text(
                    state.patient?.let { "${it.firstName} ${it.lastName}" } ?: "Paciente",
                    color = GoldLight,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = GoldLight)
                }
            },
            actions = {
                IconButton(onClick = onOpenRecords) {
                    Icon(Icons.Default.History, contentDescription = "Historia clínica", tint = GoldLight)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = LuxBlack,
                titleContentColor = GoldLight,
                navigationIconContentColor = GoldLight,
                actionIconContentColor = GoldLight,
            ),
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Historia clínica: ${patient.fileNumber}",
                                style = MaterialTheme.typography.titleMedium,
                                color = LuxBlack,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text("DNI: ${patient.dni}", color = LuxBlack)
                            Text("Nacimiento: ${patient.birthDate ?: "-"}", color = LuxBlack)
                            Text("Sexo: ${patient.sex?.name ?: "-"}", color = LuxBlack)
                            Text("Teléfono: ${patient.phone ?: "-"}", color = LuxBlack)
                            Text("Email: ${patient.email ?: "-"}", color = LuxBlack)
                            Text("Dirección: ${patient.address ?: "-"}", color = LuxBlack)
                            Text("Obra social: ${patient.insurance ?: "-"}", color = LuxBlack)
                            Text("Alergias: ${patient.allergies ?: "-"}", color = LuxBlack)
                            Text("Notas: ${patient.notes ?: "-"}", color = LuxBlack)
                            Text("Activo: ${if (patient.isActive) "Sí" else "No"}", color = LuxBlack)
                        }
                    }
                    Button(
                        onClick = onOpenRecords,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LuxBlack,
                            contentColor = GoldLight,
                        ),
                    ) {
                        Text("Ver historia clínica", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
