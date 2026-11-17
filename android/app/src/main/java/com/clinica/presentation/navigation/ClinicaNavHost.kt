package com.clinica.presentation.navigation

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.clinica.presentation.appointments.AppointmentsScreen
import com.clinica.presentation.auth.LoginScreen
import com.clinica.presentation.billing.BillingScreen
import com.clinica.presentation.clinical.ClinicalScreen
import com.clinica.presentation.dashboard.DashboardScreen
import com.clinica.presentation.inventory.InventoryScreen
import com.clinica.presentation.patients.PatientDetailScreen
import com.clinica.presentation.patients.PatientsScreen

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

    val showBottomBar = currentRoute != null &&
        currentRoute != Routes.LOGIN &&
        !currentRoute.startsWith("patients/")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
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
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
            composable(Routes.PATIENTS) {
                PatientsScreen(
                    onPatientClick = { id -> navController.navigate(Routes.patientDetail(id)) }
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
        }
    }
}
