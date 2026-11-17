package com.clinica.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Gold)
    }
}

@Composable
fun ErrorView(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LuxBlack,
                    contentColor = GoldLight,
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            ) {
                Text("Reintentar", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun EmptyView(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = GoldDark,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    fab: @Composable (() -> Unit)? = null,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        color = GoldLight,
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Atras",
                                tint = GoldLight,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LuxBlack,
                    titleContentColor = GoldLight,
                    navigationIconContentColor = GoldLight,
                ),
            )
        },
        floatingActionButton = { fab?.invoke() }
    ) { padding -> content(padding) }
}

object ErrorMapper {
    fun message(error: Throwable): String = when (error) {
        is retrofit2.HttpException -> when (error.code()) {
            401 -> "Sesión expirada o no autorizada. Inicie sesión nuevamente."
            403 -> "No tiene permisos para esta operación."
            404 -> "Recurso no encontrado."
            409 -> "Operación en conflicto con los datos actuales."
            422 -> fastApiDetail(error) ?: "Datos inválidos. Revise los campos e intente de nuevo."
            in 500..599 -> "Error del servidor. Intente más tarde."
            else -> "Error HTTP ${error.code()}. Intente de nuevo."
        }
        is java.io.IOException -> "Sin conexión con el servidor. Verifique su red."
        else -> error.message ?: "Error desconocido"
    }

    private fun fastApiDetail(error: retrofit2.HttpException): String? = try {
        val body = error.response()?.errorBody()?.string() ?: return null
        val root = kotlinx.serialization.json.Json.parseToJsonElement(body)
        val detail = (root as? kotlinx.serialization.json.JsonObject)?.get("detail") ?: return null
        when (detail) {
            is kotlinx.serialization.json.JsonPrimitive -> detail.content.takeIf { it.isNotBlank() }
            is kotlinx.serialization.json.JsonArray -> {
                val msgs = detail.mapNotNull { item ->
                    (item as? kotlinx.serialization.json.JsonObject)?.get("msg")?.let {
                        (it as? kotlinx.serialization.json.JsonPrimitive)?.content
                    }
                }.filter { it.isNotBlank() }
                msgs.joinToString(". ").takeIf { it.isNotBlank() }
            }
            else -> null
        }?.let { raw ->
            when {
                raw.contains("email", ignoreCase = true) && raw.contains("valid", ignoreCase = true) ->
                    "El correo no tiene un formato válido."
                raw.contains("password", ignoreCase = true) || raw.contains("string too short", ignoreCase = true) ->
                    "La contraseña debe tener al menos 4 caracteres."
                raw.contains("at least 2", ignoreCase = true) || raw.contains("ensure this value has at least 2", ignoreCase = true) ->
                    "Los nombres deben tener al menos 2 caracteres."
                raw.contains("at least 3", ignoreCase = true) ->
                    "El DNI debe tener al menos 3 caracteres."
                else -> null
            } ?: "Datos inválidos. Revise los campos e intente de nuevo."
        }
    } catch (_: Exception) {
        null
    }
}

@Composable
fun ErrorBox(message: String, modifier: Modifier = Modifier) = ErrorView(message, modifier = modifier)
@Composable
fun LoadingBox(modifier: Modifier = Modifier) = LoadingView(modifier = modifier)
