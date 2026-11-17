package com.clinica.presentation.patients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clinica.domain.model.Page
import com.clinica.domain.model.Patient
import com.clinica.domain.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PatientsUiState(
    val query: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val patients: List<Patient> = emptyList(),
    val page: Int = 1,
    val pages: Int = 1
)

@HiltViewModel
class PatientsViewModel @Inject constructor(
    private val repository: PatientRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PatientsUiState())
    val state: StateFlow<PatientsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun onQueryChange(value: String) {
        _state.value = _state.value.copy(query = value)
    }

    fun search() = load()

    fun nextPage() {
        if (_state.value.page < _state.value.pages) load(_state.value.page + 1)
    }

    fun previousPage() {
        if (_state.value.page > 1) load(_state.value.page - 1)
    }

    private fun load(page: Int = 1) {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val result: Page<Patient> = repository.list(
                    _state.value.query.ifBlank { null },
                    page
                )
                _state.value = _state.value.copy(
                    loading = false,
                    patients = result.items,
                    page = result.page,
                    pages = result.pages.coerceAtLeast(1)
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Error cargando pacientes"
                )
            }
        }
    }
}
