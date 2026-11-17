package com.clinica.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Gold = Color(0xFFD4A017)
val GoldDark = Color(0xFF8C5A00)
val GoldLight = Color(0xFFFFE8A3)
val LuxBlack = Color(0xFF241707)
val Ivory = Color(0xFFFAF7F0)
val PureWhite = Color(0xFFFFFFFF)

val MedBlue = Gold
val MedBlueSoft = GoldLight.copy(alpha = 0.35f)
val MedBackground = Ivory
val MedInk = LuxBlack
val MedGray = Color(0xFF7B8494)
val MedGreen = Color(0xFF16A34A)
val MedAmber = Gold
val MedRed = Color(0xFFDC2626)
val MedViolet = Color(0xFF7C3AED)

private val LightColors = lightColorScheme(
    primary = Gold,
    onPrimary = LuxBlack,
    primaryContainer = GoldLight,
    onPrimaryContainer = GoldDark,
    secondary = GoldDark,
    secondaryContainer = GoldLight.copy(alpha = 0.45f),
    onSecondaryContainer = LuxBlack,
    tertiary = MedGreen,
    background = Ivory,
    onBackground = LuxBlack,
    surface = PureWhite,
    onSurface = LuxBlack,
    surfaceVariant = GoldLight.copy(alpha = 0.25f),
    onSurfaceVariant = Color(0xFF5C4A2E),
    outline = Color(0xFFE8DFC8),
    error = MedRed,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = GoldLight,
    onPrimary = LuxBlack,
    primaryContainer = GoldDark,
    onPrimaryContainer = GoldLight,
    secondary = Gold,
    secondaryContainer = Color(0xFF3A2A10),
    onSecondaryContainer = GoldLight,
    tertiary = Color(0xFF4ADE80),
    background = Color(0xFF1A1208),
    onBackground = Color(0xFFFFF6E0),
    surface = Color(0xFF241707),
    onSurface = Color(0xFFFFF6E0),
    surfaceVariant = Color(0xFF3A2A10),
    onSurfaceVariant = Color(0xFFD4C4A0),
    outline = Color(0xFF4A3818),
    error = Color(0xFFF87171),
    onError = Color(0xFF3B0A0A),
)

@Composable
fun ClinicaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
