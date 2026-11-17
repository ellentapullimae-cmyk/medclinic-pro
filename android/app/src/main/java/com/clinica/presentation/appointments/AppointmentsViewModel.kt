package com.clinica.presentation.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clinica.domain.model.Appointment
import com.clinica.domain.model.AppointmentStatus
import com.clinica.domain.repository.AppointmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppointmentsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val appointments: List<Appointment> = emptyList(),
    val statusFilter: AppointmentStatus? = null
)

@HiltViewModel
class AppointmentsViewModel @Inject constructor(
    private val repository: AppointmentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AppointmentsUiState())
    val state: StateFlow<AppointmentsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun setFilter(status: AppointmentStatus?) {
        _state.value = _state.value.copy(statusFilter = status)
        load(status)
    }

    fun updateStatus(id: Int, status: AppointmentStatus) {
        viewModelScope.launch {
            try {
                repository.updateStatus(id, status)
                load(_state.value.statusFilter)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    private fun load(status: AppointmentStatus? = _state.value.statusFilter) {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val items = repository.list(status = status)
                _state.value = _state.value.copy(loading = false, appointments = items)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Error cargando citas"
                )
            }
        }
    }
}
