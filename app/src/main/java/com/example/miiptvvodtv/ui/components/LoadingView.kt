package com.example.miiptvvodtv.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@Composable
fun TvCircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    strokeWidth: Dp = 4.dp
) {
    val transition = rememberInfiniteTransition(label = "loading")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing)
        ),
        label = "rotation"
    )
    // Optimización de rendimiento: graphicsLayer evita recomposiciones por cada frame de animación
    Canvas(modifier = modifier.graphicsLayer { rotationZ = angle }) {
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LoadingView(
    progressPercent: Int? = null,
    message: String = "Cargando contenido desde la nube...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Porcentaje numérico arriba del indicador
            if (progressPercent != null) {
                Text(
                    text = "$progressPercent%",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 42.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            TvCircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 5.dp,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}
