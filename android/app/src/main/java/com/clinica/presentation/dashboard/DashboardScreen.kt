package com.clinica.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.clinica.core.session.SessionManager
import com.clinica.domain.repository.AuthRepository
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class ModuleOption(
    val title: String,
    val subtitle: String,
    val route: String,
    val icon: ImageVector,
    val roles: Set<String>,
)

private val modules = listOf(
    ModuleOption("Pacientes", "Fichas e historial", Routes.PATIENTS, Icons.Filled.People, setOf("admin", "medico", "recepcion")),
    ModuleOption("Citas", "Agenda del día", Routes.APPOINTMENTS, Icons.Filled.CalendarMonth, setOf("admin", "medico", "recepcion")),
    ModuleOption("Facturación", "Cobros y pagos", Routes.BILLING, Icons.Filled.Payments, setOf("admin", "medico", "recepcion")),
    ModuleOption("Inventario", "Stock e insumos", Routes.INVENTORY, Icons.Filled.Inventory2, setOf("admin", "medico", "recepcion")),
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    val fullName: String get() = auth.fullName
    val role: String get() = auth.role

    fun logout() = auth.logout()
}

@Composable
fun DashboardScreen(
    onOpen: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    ClinicScaffold(title = "Menú principal") { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(text = viewModel.fullName, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = viewModel.role.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(modules.filter { viewModel.role in it.roles }) { module ->
                    Card(onClick = { onOpen(module.route) }) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(
                                imageVector = module.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = module.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = module.subtitle,
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = {
                    viewModel.logout()
                    onLogout()
                }) {
                    Text("Cerrar sesión")
                }
            }
        }
    }
}
