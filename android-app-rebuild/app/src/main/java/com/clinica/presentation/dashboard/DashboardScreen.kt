package com.clinica.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.clinica.core.ui.ClinicScaffold
import com.clinica.core.ui.Gold
import com.clinica.core.ui.GoldDark
import com.clinica.core.ui.GoldLight
import com.clinica.core.ui.Ivory
import com.clinica.core.ui.LuxBlack
import com.clinica.core.ui.MedAmber
import com.clinica.core.ui.MedBlue
import com.clinica.core.ui.MedGray
import com.clinica.core.ui.MedGreen
import com.clinica.core.ui.MedRed
import com.clinica.core.ui.PureWhite
import com.clinica.presentation.navigation.Routes
import com.clinica.domain.model.AppointmentStatus
import com.clinica.domain.model.InvoiceStatus
import com.clinica.domain.repository.AppointmentRepository
import com.clinica.domain.repository.AuthRepository
import com.clinica.domain.repository.BillingRepository
import com.clinica.domain.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class DashboardStats(
    val loading: Boolean = true,
    val patients: Int = 0,
    val todayAppointments: Int = 0,
    val pendingInvoices: Int = 0,
    val statusCounts: List<Pair<String, Int>> = emptyList(),
)

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
    ModuleOption("Médicos", "Equipo y especialidades", Routes.DOCTORS, Icons.Filled.LocalHospital, setOf("admin", "medico", "recepcion")),
    ModuleOption("Facturación", "Cobros y pagos", Routes.BILLING, Icons.Filled.Payments, setOf("admin", "medico", "recepcion")),
    ModuleOption("Gastos", "Consumo de insumos", Routes.EXPENSES, Icons.AutoMirrored.Filled.ReceiptLong, setOf("admin")),
    ModuleOption("Inventario", "Stock e insumos", Routes.INVENTORY, Icons.Filled.Inventory2, setOf("admin", "medico", "recepcion")),
)

private val statusColors = mapOf(
    "pendiente" to Gold,
    "confirmada" to GoldDark,
    "atendida" to MedGreen,
    "cancelada" to MedRed,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val patientRepository: PatientRepository,
    private val appointmentRepository: AppointmentRepository,
    private val billingRepository: BillingRepository,
) : ViewModel() {

    val fullName: String get() = authRepository.fullName
    val role: String get() = authRepository.role

    private val _stats = MutableStateFlow(DashboardStats())
    val stats: StateFlow<DashboardStats> = _stats.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _stats.value = DashboardStats()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            val patients = runCatching { patientRepository.list(null, 1).total }.getOrDefault(0)
            val appointments = runCatching { appointmentRepository.list() }.getOrDefault(emptyList())
            val pending = runCatching { billingRepository.list(InvoiceStatus.pendiente).size }.getOrDefault(0)

            val counts = AppointmentStatus.entries.map { status ->
                status.name to appointments.count { it.status == status }
            }

            _stats.value = DashboardStats(
                loading = false,
                patients = patients,
                todayAppointments = appointments.count { it.startTime.startsWith(today) },
                pendingInvoices = pending,
                statusCounts = counts,
            )
        }
    }

    fun logout() = authRepository.logout()
}

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    onLogout: (() -> Unit)? = null,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    ClinicScaffold(title = "Menú principal") { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Ivory)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(LuxBlack, Color(0xFF3A2408), GoldDark)),
                        RoundedCornerShape(18.dp),
                    )
                    .padding(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Gold, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = LuxBlack,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Hola, ${viewModel.fullName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight,
                        )
                        Text(
                            text = viewModel.role.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gold.copy(alpha = 0.95f),
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile("Pacientes", stats.patients.toString(), Icons.Filled.People, Modifier.weight(1f))
                StatTile("Citas hoy", stats.todayAppointments.toString(), Icons.Filled.CalendarMonth, Modifier.weight(1f))
                StatTile("Por cobrar", stats.pendingInvoices.toString(), Icons.Filled.Payments, Modifier.weight(1f))
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.QueryStats,
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Citas por estado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = LuxBlack,
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    if (stats.loading) {
                        Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                            Text("Cargando…", color = GoldDark)
                        }
                    } else {
                        StatusBars(data = stats.statusCounts)
                    }
                }
            }

            Text(
                text = "Módulos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = LuxBlack,
                modifier = Modifier.padding(top = 4.dp),
            )

            modules.filter { viewModel.role in it.roles }.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { module ->
                        ModuleCard(
                            module = module,
                            onClick = { onNavigate(module.route) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }

            Button(
                onClick = {
                    viewModel.logout()
                    onLogout?.invoke()
                },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LuxBlack,
                    contentColor = GoldLight,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar sesión", fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(GoldLight, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = GoldDark,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = LuxBlack,
            )
        }
    }
}

@Composable
private fun StatusBars(data: List<Pair<String, Int>>) {
    val max = (data.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        data.forEach { (label, count) ->
            val fraction = count.toFloat() / max
            val barHeight by androidx.compose.animation.core.animateFloatAsState(
                targetValue = fraction,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 700),
                label = "bar",
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LuxBlack,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height((barHeight * 92f).dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    statusColors[label] ?: Gold,
                                    (statusColors[label] ?: Gold).copy(alpha = 0.65f),
                                ),
                            ),
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                        ),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ModuleCard(module: ModuleOption, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.height(116.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(GoldLight, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = module.icon,
                    contentDescription = null,
                    tint = GoldDark,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = module.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = LuxBlack,
            )
            Text(
                text = module.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MedGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
