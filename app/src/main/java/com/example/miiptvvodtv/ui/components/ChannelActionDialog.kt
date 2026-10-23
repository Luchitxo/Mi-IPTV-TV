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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.miiptvvodtv.Channel
import com.example.miiptvvodtv.ui.theme.GoldStar
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelActionDialog(
    channel: Channel,
    isFavorite: Boolean,
    onPlayClick: (Channel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val favoriteFocusRequester = remember { FocusRequester() }

    BackHandler {
        onDismiss()
    }

    LaunchedEffect(Unit) {
        delay(100)
        try {
            favoriteFocusRequester.requestFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Trampa de foco (Focus Trap): Atrapa las teclas Izquierda/Derecha para que el D-Pad no escape a los canales de fondo
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            // Consumir el evento de flecha izquierda/derecha para bloquear la fuga de foco
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
                .widthIn(max = 450.dp)
                .padding(24.dp),
            shape = RoundedCornerShape(16.dp),
            border = Border(border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo del Canal
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(channel.logo)
                        .crossfade(true)
                        .build(),
                    contentDescription = channel.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(80.dp)
                        .padding(bottom = 12.dp)
                )

                // Nombre del Canal
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (channel.category.isNotEmpty()) {
                    Text(
                        text = "Categoría: ${channel.category}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Opciones del Menú
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            onToggleFavorite(channel.id)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .focusRequester(favoriteFocusRequester),
                        colors = ButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = GoldStar
                        )
                    ) {
                        Text(
                            text = if (isFavorite) "★ Quitar de Favoritos" else "★ Añadir a Favoritos",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black
                        )
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onPlayClick(channel)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "▶ Reproducir Canal",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "Cancelar",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
