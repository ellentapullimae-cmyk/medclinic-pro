package com.clinica.presentation.doctors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
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
import com.clinica.domain.model.UserBrief
import com.clinica.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DoctorsUiState(
    val loading: Boolean = true,
    val doctors: List<UserBrief> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class DoctorsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DoctorsUiState())
    val state: StateFlow<DoctorsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = DoctorsUiState(loading = true)
            try {
                val doctors = authRepository.doctors()
                _state.value = DoctorsUiState(loading = false, doctors = doctors)
            } catch (error: Throwable) {
                _state.value = DoctorsUiState(loading = false, error = ErrorMapper.message(error))
            }
        }
    }
}

@Composable
fun DoctorsScreen(
    onBack: (() -> Unit)? = null,
    viewModel: DoctorsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    ClinicScaffold(title = "Médicos", onBack = onBack) { padding ->
        when {
            state.loading -> LoadingView(Modifier.padding(padding))
            state.error != null -> ErrorView(state.error!!, onRetry = { viewModel.load() }, modifier = Modifier.padding(padding))
            state.doctors.isEmpty() -> EmptyView("Sin médicos registrados", Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().background(Ivory).padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.doctors, key = { it.id }) { doctor ->
                    DoctorCard(doctor = doctor)
                }
            }
        }
    }
}

@Composable
private fun DoctorCard(doctor: UserBrief) {
    val initials = doctor.fullName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(Gold, GoldLight)),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials.ifBlank { "?" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LuxBlack,
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = doctor.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LuxBlack,
                )
                Text(
                    text = doctor.specialty ?: "Medicina general",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GoldDark,
                )
                Text(
                    text = "ID #${doctor.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldDark.copy(alpha = 0.8f),
                )
            }
        }
    }
}
