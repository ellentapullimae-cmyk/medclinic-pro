package com.clinica.presentation.inventory

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.clinica.domain.model.InventoryItem
import com.clinica.domain.model.InventoryItemInput
import com.clinica.domain.model.ItemCategory
import com.clinica.domain.model.StockMovementInput
import com.clinica.domain.repository.AuthRepository
import com.clinica.domain.repository.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventoryUiState(
    val loading: Boolean = true,
    val items: List<InventoryItem> = emptyList(),
    val error: String? = null,
    val query: String = "",
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: InventoryRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val canCreate: Boolean get() = authRepository.role == "admin"

    private val _state = MutableStateFlow(InventoryUiState())
    val state: StateFlow<InventoryUiState> = _state.asStateFlow()

    init {
        search(null)
    }

    fun search(query: String?) {
        val clean = query?.trim()
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, query = clean ?: "")
            try {
                val items = repository.list(clean?.ifBlank { null })
                _state.value = InventoryUiState(loading = false, items = items, query = clean ?: "")
            } catch (error: Throwable) {
                _state.value = _state.value.copy(loading = false, error = ErrorMapper.message(error))
            }
        }
    }

    fun create(
        sku: String,
        name: String,
        stock: Int,
        minStock: Int,
        unitCost: String,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                repository.create(
                    InventoryItemInput(
                        sku = sku,
                        name = name,
                        category = ItemCategory.medicamento,
                        stock = stock,
                        minStock = minStock,
                        unitCost = unitCost,
                    ),
                )
                search(null)
                onDone()
            } catch (error: Throwable) {
                _state.value = _state.value.copy(loading = false, error = ErrorMapper.message(error))
                onDone()
            }
        }
    }

    fun move(itemId: Int, change: Int, reason: String?, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.move(itemId, StockMovementInput(change = change, reason = reason))
                search(_state.value.query.ifBlank { null })
                onDone()
            } catch (error: Throwable) {
                _state.value = _state.value.copy(loading = false, error = ErrorMapper.message(error))
                onDone()
            }
        }
    }
}

@Composable
fun InventoryScreen(
    onBack: (() -> Unit)? = null,
    viewModel: InventoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var moveTarget by remember { mutableStateOf<InventoryItem?>(null) }

    ClinicScaffold(
        title = "Inventario",
        onBack = onBack,
        fab = {
            if (viewModel.canCreate) {
                FloatingActionButton(
                    onClick = { showCreate = true },
                    containerColor = LuxBlack,
                    contentColor = GoldLight,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Nuevo ítem")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(Ivory).padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { viewModel.search(it) },
                label = { Text("Buscar por nombre o SKU", color = GoldDark) },
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
            when {
                state.loading -> LoadingView()
                state.error != null -> ErrorView(state.error!!, onRetry = { viewModel.search(state.query.ifBlank { null }) })
                state.items.isEmpty() -> EmptyView("Sin ítems en inventario")
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.id }) { item ->
                        InventoryCard(item = item, onClick = { moveTarget = item })
                    }
                }
            }
        }
    }

    if (showCreate) {
        ItemFormDialog(
            onDismiss = { showCreate = false },
            onCreate = { sku, name, stock, minStock, cost ->
                viewModel.create(sku, name, stock, minStock, cost) { showCreate = false }
            },
        )
    }

    moveTarget?.let { item ->
        MovementDialog(
            item = item,
            onDismiss = { moveTarget = null },
            onMove = { change, reason ->
                viewModel.move(item.id, change, reason) { moveTarget = null }
            },
        )
    }
}

@Composable
private fun InventoryCard(item: InventoryItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium, color = LuxBlack)
                if (item.lowStock) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Stock bajo", color = LuxBlack) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = GoldLight.copy(alpha = 0.55f)),
                    )
                }
            }
            Text(
                text = "${item.sku} · ${item.stock} ${item.unit} (mín. ${item.minStock})",
                style = MaterialTheme.typography.bodySmall,
                color = GoldDark,
            )
            Text(
                text = "Registrar movimiento",
                style = MaterialTheme.typography.labelMedium,
                color = GoldDark,
            )
        }
    }
}

@Composable
private fun ItemFormDialog(
    onDismiss: () -> Unit,
    onCreate: (sku: String, name: String, stock: Int, minStock: Int, unitCost: String) -> Unit,
) {
    var sku by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("0") }
    var minStock by remember { mutableStateOf("0") }
    var cost by remember { mutableStateOf("0.00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo ítem") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU") }, singleLine = true)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, singleLine = true)
                OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Stock inicial") }, singleLine = true)
                OutlinedTextField(value = minStock, onValueChange = { minStock = it }, label = { Text("Stock mínimo") }, singleLine = true)
                OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Costo unitario") }, singleLine = true)
            }
        },
        confirmButton = {
            val stockValue = stock.toIntOrNull() ?: -1
            val minValue = minStock.toIntOrNull() ?: -1
            val valid = sku.isNotBlank() && name.isNotBlank() && stockValue >= 0 && minValue >= 0 &&
                cost.toDoubleOrNull() != null
            TextButton(
                enabled = valid,
                onClick = { onCreate(sku.trim(), name.trim(), stockValue, minValue, cost.trim()) },
            ) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun MovementDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onMove: (change: Int, reason: String?) -> Unit,
) {
    var isEntry by remember { mutableStateOf(true) }
    var quantity by remember { mutableStateOf("1") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Movimiento: ${item.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Stock actual: ${item.stock} ${item.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { isEntry = true }, label = { Text(if (isEntry) "Entrada ✓" else "Entrada") })
                    AssistChip(onClick = { isEntry = false }, label = { Text(if (!isEntry) "Salida ✓" else "Salida") })
                }
                OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Cantidad") }, singleLine = true)
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Motivo") }, singleLine = true)
            }
        },
        confirmButton = {
            val amount = quantity.toIntOrNull() ?: 0
            TextButton(
                enabled = amount > 0,
                onClick = {
                    val change = if (isEntry) amount else -amount
                    onMove(change, reason.trim().ifBlank { null })
                },
            ) { Text("Registrar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
