package com.example.miiptvvodtv.ui.components

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ExitConfirmationDialog(
    onConfirmExit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cancelFocusRequester = remember { FocusRequester() }

    // Intercepta el botón Atrás cuando el diálogo está abierto para cerrarlo sin salir de la app
    BackHandler {
        onDismiss()
    }

    // Mueve el foco del D-Pad automáticamente al botón Cancelar para evitar salidas accidentales
    LaunchedEffect(Unit) {
        delay(100)
        try {
            cancelFocusRequester.requestFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Trampa de foco (Focus Trap): Atrapa las teclas Arriba/Abajo para que el D-Pad no escape a los canales de fondo
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                            // Consumir el evento de flecha arriba/abajo para bloquear la fuga de foco
                            return@onKeyEvent true
                        }
                    }
                }
                false
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .padding(24.dp),
            shape = RoundedCornerShape(16.dp),
            border = Border(border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "¿Deseas salir de la aplicación?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Selecciona 'Cancelar' para continuar viendo tus canales o 'Salir' para cerrar la aplicación.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.focusRequester(cancelFocusRequester)
                    ) {
                        Text(
                            text = "Cancelar",
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }

                    Button(
                        onClick = onConfirmExit,
                        colors = ButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                            focusedContainerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            text = "Salir",
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
