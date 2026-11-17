package com.clinica.presentation.clinical

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clinica.domain.model.ClinicalRecord
import com.clinica.domain.model.ClinicalRecordCreate
import com.clinica.domain.repository.ClinicalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClinicalUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val records: List<ClinicalRecord> = emptyList(),
    val diagnosis: String = "",
    val treatment: String = "",
    val notes: String = "",
    val saving: Boolean = false,
    val saveError: String? = null
)

@HiltViewModel
class ClinicalViewModel @Inject constructor(
    private val repository: ClinicalRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val patientId: Int = savedStateHandle.get<String>("patientId")?.toIntOrNull() ?: -1

    private val _state = MutableStateFlow(ClinicalUiState())
    val state: StateFlow<ClinicalUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun onDiagnosisChange(v: String) { _state.value = _state.value.copy(diagnosis = v) }
    fun onTreatmentChange(v: String) { _state.value = _state.value.copy(treatment = v) }
    fun onNotesChange(v: String) { _state.value = _state.value.copy(notes = v) }

    fun save() {
        val current = _state.value
        _state.value = current.copy(saving = true, saveError = null)
        viewModelScope.launch {
            try {
                repository.create(
                    patientId,
                    ClinicalRecordCreate(
                        diagnosis = current.diagnosis.ifBlank { null },
                        treatment = current.treatment.ifBlank { null },
                        notes = current.notes.ifBlank { null }
                    )
                )
                _state.value = _state.value.copy(
                    saving = false,
                    diagnosis = "",
                    treatment = "",
                    notes = ""
                )
                load()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    saving = false,
                    saveError = e.message ?: "Error guardando registro"
                )
            }
        }
    }

    private fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val records = repository.listByPatient(patientId)
                _state.value = _state.value.copy(loading = false, records = records)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Error cargando historia clínica"
                )
            }
        }
    }
}
