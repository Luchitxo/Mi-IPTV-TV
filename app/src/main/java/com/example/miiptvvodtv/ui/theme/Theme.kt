package com.example.miiptvvodtv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MiIPTVVODTVTheme(
    content: @Composable () -> Unit,
) {
    val darkColorScheme = darkColorScheme(
        primary = ElectricCyan,
        onPrimary = DeepSlateBlack,
        secondary = LightCyan,
        onSecondary = DeepSlateBlack,
        tertiary = LightCyan,
        background = DeepSlateBlack, // #0B0F19 - Fondo raíz profundo
        onBackground = TextPrimaryWhite,
        surface = Color(0xFF161E2E), // Superficie de tarjetas claramente distinguible sobre el fondo
        onSurface = TextPrimaryWhite,
        surfaceVariant = Color(0xFF1E2842), // Variante para acordeones y cabeceras
        onSurfaceVariant = TextSecondaryGrey
    )

    MaterialTheme(
        colorScheme = darkColorScheme,
        typography = Typography,
        content = content
    )
}
