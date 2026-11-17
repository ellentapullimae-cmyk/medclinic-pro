package com.clinica.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.clinica.core.session.SessionManager
import com.clinica.core.ui.Gold
import com.clinica.core.ui.GoldLight
import com.clinica.core.ui.LuxBlack
import com.clinica.presentation.appointments.AppointmentsScreen
import com.clinica.presentation.auth.LoginScreen
import com.clinica.presentation.billing.BillingScreen
import com.clinica.presentation.clinical.ClinicalScreen
import com.clinica.presentation.dashboard.DashboardScreen
import com.clinica.presentation.doctors.DoctorsScreen
import com.clinica.presentation.expenses.ExpensesScreen
import com.clinica.presentation.inventory.InventoryScreen
import com.clinica.presentation.patients.PatientDetailScreen
import com.clinica.presentation.patients.PatientsScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

@HiltViewModel
class SessionNavViewModel @Inject constructor(
    sessionManager: SessionManager
) : ViewModel() {
    val sessionExpired: SharedFlow<Unit> = sessionManager.sessionExpired
}

private data class BottomItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomItems = listOf(
    BottomItem(Routes.DASHBOARD, "Inicio", Icons.Default.Dashboard),
    BottomItem(Routes.PATIENTS, "Pacientes", Icons.Default.People),
    BottomItem(Routes.APPOINTMENTS, "Citas", Icons.Default.CalendarMonth),
    BottomItem(Routes.BILLING, "Caja", Icons.Default.Payments),
    BottomItem(Routes.INVENTORY, "Inventario", Icons.Default.Inventory2)
)

@Composable
fun ClinicaNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val sessionNavViewModel: SessionNavViewModel = hiltViewModel()

    LaunchedEffect(sessionNavViewModel) {
        sessionNavViewModel.sessionExpired.collect {
            val route = navController.currentBackStackEntry?.destination?.route
            if (route != null && route != Routes.LOGIN) {
                navController.navigate(Routes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    val showBottomBar = currentRoute != null &&
        currentRoute != Routes.LOGIN &&
        !currentRoute.startsWith("patients/")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = LuxBlack,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .background(LuxBlack)
                        .border(width = 1.dp, color = Gold.copy(alpha = 0.55f)),
                ) {
                    bottomItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = item.label,
                                    tint = if (selected) GoldLight else Gold.copy(alpha = 0.65f),
                                )
                            },
                            label = {
                                Text(
                                    item.label,
                                    color = if (selected) GoldLight else Gold.copy(alpha = 0.7f),
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = GoldLight,
                                selectedTextColor = GoldLight,
                                unselectedIconColor = Gold.copy(alpha = 0.65f),
                                unselectedTextColor = Gold.copy(alpha = 0.7f),
                                indicatorColor = Gold.copy(alpha = 0.18f),
                            ),
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onLogout = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.PATIENTS) {
                PatientsScreen(
                    onPatientClick = { id -> navController.navigate(Routes.patientDetail(id)) },
                    onOpenClinical = { id -> navController.navigate(Routes.clinical(id)) }
                )
            }
            composable(Routes.PATIENT_DETAIL) { entry ->
                val patientId = entry.arguments?.getString("patientId")?.toIntOrNull() ?: return@composable
                PatientDetailScreen(
                    patientId = patientId,
                    onBack = { navController.popBackStack() },
                    onOpenRecords = { navController.navigate(Routes.clinical(patientId)) }
                )
            }
            composable(Routes.CLINICAL) { entry ->
                val patientId = entry.arguments?.getString("patientId")?.toIntOrNull() ?: return@composable
                ClinicalScreen(
                    patientId = patientId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.APPOINTMENTS) {
                AppointmentsScreen()
            }
            composable(Routes.BILLING) {
                BillingScreen()
            }
            composable(Routes.INVENTORY) {
                InventoryScreen()
            }
            composable(Routes.DOCTORS) {
                DoctorsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.EXPENSES) {
                ExpensesScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
