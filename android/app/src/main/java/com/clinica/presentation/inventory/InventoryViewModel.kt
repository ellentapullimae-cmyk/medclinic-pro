package com.clinica.presentation.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clinica.domain.model.InventoryItem
import com.clinica.domain.repository.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventoryUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val items: List<InventoryItem> = emptyList(),
    val query: String = "",
    val selected: InventoryItem? = null,
    val movementAmount: String = "",
    val movementError: String? = null,
    val moving: Boolean = false
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: InventoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryUiState())
    val state: StateFlow<InventoryUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun onQueryChange(v: String) {
        _state.value = _state.value.copy(query = v)
    }

    fun search() = load(_state.value.query.ifBlank { null })

    fun select(item: InventoryItem?) {
        _state.value = _state.value.copy(selected = item, movementAmount = "", movementError = null)
    }

    fun onMovementAmountChange(v: String) {
        _state.value = _state.value.copy(movementAmount = v, movementError = null)
    }

    fun applyMovement(isEntry: Boolean) {
        val current = _state.value
        val item = current.selected ?: return
        val amount = current.movementAmount.toIntOrNull()
        if (amount == null || amount <= 0) {
            _state.value = current.copy(movementError = "Importe inválido")
            return
        }
        _state.value = current.copy(moving = true, movementError = null)
        viewModelScope.launch {
            try {
                repository.move(
                    item.id,
                    com.clinica.domain.model.StockMovementIn(
                        change = if (isEntry) amount else -amount,
                        reason = if (isEntry) "Entrada" else "Salida"
                    )
                )
                _state.value = _state.value.copy(moving = false, selected = null, movementAmount = "")
                load(_state.value.query.ifBlank { null })
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    moving = false,
                    movementError = e.message ?: "Error en movimiento"
                )
            }
        }
    }

    private fun load(query: String? = null) {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val items = repository.list(query)
                _state.value = _state.value.copy(loading = false, items = items)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Error cargando inventario"
                )
            }
        }
    }
}
