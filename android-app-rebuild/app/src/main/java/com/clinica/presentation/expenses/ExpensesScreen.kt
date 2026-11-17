package com.clinica.presentation.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.clinica.core.ui.MedAmber
import com.clinica.core.ui.MedBlue
import com.clinica.core.ui.MedGray
import com.clinica.core.ui.PureWhite
import com.clinica.domain.model.InventoryItem
import com.clinica.domain.model.StockMovement
import com.clinica.domain.repository.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExpenseLine(
    val itemName: String,
    val sku: String,
    val change: Int,
    val value: Double,
    val reason: String?,
    val createdAt: String?,
)

data class ExpensesUiState(
    val loading: Boolean = true,
    val inventoryValue: Double = 0.0,
    val consumption: Double = 0.0,
    val outflowCount: Int = 0,
    val lines: List<ExpenseLine> = emptyList(),
    val error: String? = null,
)

private fun Double.toMoney(): String = "$" + String.format("%,.2f", this)

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val repository: InventoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ExpensesUiState())
    val state: StateFlow<ExpensesUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = ExpensesUiState(loading = true)
            try {
                val items = repository.list(null)
                val byId = items.associateBy { it.id }

                val allMovements: List<Pair<InventoryItem, StockMovement>> = coroutineScope {
                    items.map { item ->
                        async {
                            runCatching { repository.movements(item.id) }
                                .getOrDefault(emptyList())
                                .map { movement -> item to movement }
                        }
                    }.flatMap { it.await() }
                }

                val inventoryValue = items.sumOf { item ->
                    item.stock * (item.unitCost.toDoubleOrNull() ?: 0.0)
                }

                val outflows = allMovements
                    .filter { (_, movement) -> movement.change < 0 }
                    .map { (item, movement) ->
                        ExpenseLine(
                            itemName = item.name,
                            sku = item.sku,
                            change = movement.change,
                            value = kotlin.math.abs(movement.change) * (item.unitCost.toDoubleOrNull() ?: 0.0),
                            reason = movement.reason,
                            createdAt = movement.createdAt,
                        )
                    }
                    .sortedByDescending { it.createdAt ?: "" }

                _state.value = ExpensesUiState(
                    loading = false,
                    inventoryValue = inventoryValue,
                    consumption = outflows.sumOf { it.value },
                    outflowCount = outflows.size,
                    lines = outflows,
                )
            } catch (error: Throwable) {
                _state.value = ExpensesUiState(loading = false, error = ErrorMapper.message(error))
            }
        }
    }
}

@Composable
fun ExpensesScreen(
    onBack: (() -> Unit)? = null,
    viewModel: ExpensesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    ClinicScaffold(title = "Gastos", onBack = onBack) { padding ->
        when {
            state.loading -> LoadingView(Modifier.padding(padding))
            state.error != null -> ErrorView(state.error!!, onRetry = { viewModel.load() }, modifier = Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().background(Ivory).padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        ExpenseStat("Inventario", state.inventoryValue.toMoney(), Modifier.weight(1f))
                        ExpenseStat("Consumo", state.consumption.toMoney(), Modifier.weight(1f), emphasize = true)
                        ExpenseStat("Salidas", state.outflowCount.toString(), Modifier.weight(1f))
                    }
                }
                item {
                    Text(
                        text = "Movimientos de salida",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (state.lines.isEmpty()) {
                    item { EmptyView("Sin consumos registrados") }
                } else {
                    items(state.lines.size) { index ->
                        ExpenseRow(line = state.lines[index])
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseStat(label: String, value: String, modifier: Modifier = Modifier, emphasize: Boolean = false) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = GoldDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (emphasize) Gold else LuxBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ExpenseRow(line: ExpenseLine) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = line.itemName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = LuxBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.padding(4.dp))
                Text(
                    text = "-${line.change}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text = "${line.sku} · ${line.reason ?: "Salida de almacén"}",
                style = MaterialTheme.typography.bodySmall,
                color = GoldDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Valor: ${line.value.toMoney()}",
                style = MaterialTheme.typography.labelMedium,
                color = Gold,
            )
        }
    }
}
