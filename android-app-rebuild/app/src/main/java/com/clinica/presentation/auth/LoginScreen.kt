package com.clinica.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.clinica.core.ui.ErrorMapper
import com.clinica.core.ui.Gold
import com.clinica.core.ui.GoldDark
import com.clinica.core.ui.GoldLight
import com.clinica.core.ui.Ivory
import com.clinica.core.ui.LuxBlack
import com.clinica.core.ui.PureWhite
import com.clinica.domain.model.LoginRequest
import com.clinica.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun login(email: String, password: String) {
        val trimmedEmail = email.trim()
        when {
            trimmedEmail.isBlank() || password.isBlank() -> {
                _state.value = LoginUiState(error = "Ingrese correo y contraseña")
                return
            }
            !trimmedEmail.contains('@') || !trimmedEmail.substringAfter('@', "").contains('.') -> {
                _state.value = LoginUiState(error = "El correo no tiene un formato válido.")
                return
            }
            password.length < 4 -> {
                _state.value = LoginUiState(error = "La contraseña debe tener al menos 4 caracteres.")
                return
            }
        }
        viewModelScope.launch {
            _state.value = LoginUiState(loading = true)
            try {
                repository.login(LoginRequest(email = trimmedEmail, password = password))
                _state.value = LoginUiState(loggedIn = true)
            } catch (error: Throwable) {
                _state.value = LoginUiState(error = ErrorMapper.message(error))
            }
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.loggedIn) {
        if (state.loggedIn) onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ivory)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(LuxBlack, Color(0xFF3A2408), GoldDark.copy(alpha = 0.85f)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 48.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(
                            Brush.linearGradient(listOf(Gold, GoldLight, Gold)),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalHospital,
                        contentDescription = null,
                        tint = LuxBlack,
                        modifier = Modifier.size(48.dp),
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "MEDCLINIC PRO",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight,
                    letterSpacing = 3.sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Gestión médica de alta gama",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gold.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Bienvenido",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = LuxBlack,
            )
            Text(
                text = "Ingrese sus credenciales",
                style = MaterialTheme.typography.bodyMedium,
                color = GoldDark,
            )
            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo", color = GoldDark) },
                leadingIcon = {
                    Icon(Icons.Filled.Email, contentDescription = null, tint = Gold)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = goldFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña", color = GoldDark) },
                leadingIcon = {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = Gold)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(
                            text = if (passwordVisible) "Ocultar" else "Ver",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldDark,
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = goldFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            state.error?.let { message ->
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF0F0), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = message,
                        color = Color(0xFFB91C1C),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = { viewModel.login(email, password) },
                enabled = !state.loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LuxBlack,
                    contentColor = GoldLight,
                    disabledContainerColor = LuxBlack.copy(alpha = 0.55f),
                    disabledContentColor = GoldLight.copy(alpha = 0.7f),
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
            ) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = GoldLight,
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Text(
                        text = "INGRESAR",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "© MedClinic Pro · Acceso seguro",
                style = MaterialTheme.typography.labelSmall,
                color = GoldDark.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun goldFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Gold,
    unfocusedBorderColor = Gold.copy(alpha = 0.45f),
    focusedLabelColor = GoldDark,
    unfocusedLabelColor = GoldDark.copy(alpha = 0.8f),
    focusedTextColor = LuxBlack,
    unfocusedTextColor = LuxBlack,
    cursorColor = GoldDark,
    focusedLeadingIconColor = Gold,
    unfocusedLeadingIconColor = Gold.copy(alpha = 0.75f),
    focusedContainerColor = PureWhite,
    unfocusedContainerColor = PureWhite,
)
