package com.example.miiptvvodtv.ui.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay

private const val TAG = "ErrorView"

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ErrorView(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Error de conexión"
) {
    val retryFocusRequester = remember { FocusRequester() }

    // Foco automático en el botón "Reintentar" para que el usuario de TV pueda accionarlo de inmediato
    LaunchedEffect(Unit) {
        delay(150L)
        try {
            retryFocusRequester.requestFocus()
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo solicitar el foco en el botón de reintento", e)
        }
    }

    val friendlyMessage = translateErrorMessage(errorMessage)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = friendlyMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier
                    .height(48.dp)
                    .focusRequester(retryFocusRequester)
            ) {
                Text(
                    text = "Reintentar",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

// Mapeador de errores técnicos en inglés a mensajes claros y amigables en español
private fun translateErrorMessage(rawMessage: String): String {
    val lower = rawMessage.lowercase()
    return when {
        lower.contains("unable to resolve host") || lower.contains("no address associated") ->
            "No hay conexión a internet o el servidor DNS no responde. Verifica tu red Wi-Fi."
        lower.contains("timeout") || lower.contains("timed out") ->
            "La conexión con el servidor tardó demasiado tiempo en responder."
        lower.contains("connection refused") ->
            "El servidor rechazó la conexión. Es posible que esté caído temporalmente."
        lower.contains("empty") || lower.contains("vacía") ->
            "La lista obtenida no contiene canales disponibles."
        else ->
            rawMessage.ifEmpty { "Ocurrió un error inesperado al cargar la lista de canales." }
    }
}
