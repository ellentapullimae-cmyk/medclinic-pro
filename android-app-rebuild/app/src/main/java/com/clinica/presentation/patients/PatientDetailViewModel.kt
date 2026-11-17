package com.clinica.presentation.patients

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clinica.core.ui.ErrorMapper
import com.clinica.domain.model.Patient
import com.clinica.domain.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PatientDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val patient: Patient? = null
)

@HiltViewModel
class PatientDetailViewModel @Inject constructor(
    private val repository: PatientRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val patientId: Int = savedStateHandle.get<String>("patientId")?.toIntOrNull() ?: -1

    private val _state = MutableStateFlow(PatientDetailUiState())
    val state: StateFlow<PatientDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun deactivate(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.deactivate(patientId)
                onDone()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = ErrorMapper.message(e))
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val patient = repository.get(patientId)
                _state.value = PatientDetailUiState(loading = false, patient = patient)
            } catch (e: Exception) {
                _state.value = PatientDetailUiState(
                    loading = false,
                    error = ErrorMapper.message(e)
                )
            }
        }
    }
}
