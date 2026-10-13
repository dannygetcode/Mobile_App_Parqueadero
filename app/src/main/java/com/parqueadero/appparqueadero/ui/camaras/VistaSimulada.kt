package com.parqueadero.appparqueadero.ui.camaras

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.parqueadero.appparqueadero.data.model.CamaraDTO
import com.parqueadero.appparqueadero.ui.components.rememberReducedMotion
import com.parqueadero.appparqueadero.ui.theme.Radius
import com.parqueadero.appparqueadero.ui.theme.Spacing
import com.parqueadero.appparqueadero.ui.theme.cupo
import com.parqueadero.appparqueadero.ui.theme.tabular
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

private val FONDO_A = Color(0xFF0B1A33)
private val FONDO_B = Color(0xFF14171C)
private val VELO = Color.Black.copy(alpha = 0.5f)
private val formatoReloj = DateTimeFormatter.ofPattern("HH:mm:ss")

/** Vista de demostración de una cámara: no hay video ni red, solo una imagen animada con reloj. */
@Composable
fun VistaSimulada(camara: CamaraDTO, modifier: Modifier = Modifier) {
    val reducir = rememberReducedMotion()
    val animar = camara.activa && !reducir
    val transicion = rememberInfiniteTransition(label = "camara")
    val angulo by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20_000, easing = LinearEasing)),
        label = "angulo",
    )
    val banda by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4_000, easing = LinearEasing)),
        label = "banda",
    )
    val pulso by transicion.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(1_000), RepeatMode.Reverse),
        label = "pulso",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(Radius.md))
            .clearAndSetSemantics {
                contentDescription = "Vista simulada de ${camara.nombre}, ${if (camara.activa) "en vivo" else "sin señal"}"
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val a = Math.toRadians((if (animar) angulo else 45f).toDouble())
            val centro = Offset(size.width / 2, size.height / 2)
            val radio = maxOf(size.width, size.height) / 2
            val dx = (cos(a) * radio).toFloat()
            val dy = (sin(a) * radio).toFloat()
            drawRect(
                Brush.linearGradient(
                    listOf(FONDO_A, FONDO_B),
                    start = centro - Offset(dx, dy),
                    end = centro + Offset(dx, dy),
                )
            )
            // Líneas de escaneo cada 4 dp
            val paso = 4.dp.toPx()
            var y = 0f
            while (y < size.height) {
                drawRect(Color.White.copy(alpha = 0.06f), Offset(0f, y), Size(size.width, 1.dp.toPx()))
                y += paso
            }
            if (camara.activa) {
                val alto = 24.dp.toPx()
                val top = (if (animar) banda else 0.3f) * (size.height + alto) - alto
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.08f), Color.Transparent),
                        startY = top,
                        endY = top + alto,
                    ),
                    Offset(0f, top),
                    Size(size.width, alto),
                )
            }
        }

        if (camara.activa) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(Spacing.sm)
                    .background(VELO, RoundedCornerShape(Radius.sm))
                    .padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .alpha(if (animar) pulso else 1f)
                        .background(MaterialTheme.cupo.peligro, CircleShape)
                )
                Text("EN VIVO", style = MaterialTheme.typography.labelMedium, color = Color.White)
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(VELO, RoundedCornerShape(Radius.sm))
                    .padding(Spacing.sm),
            ) {
                Icon(Icons.Rounded.VideocamOff, contentDescription = null, tint = Color.White)
                Text("Sin señal", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }

        Reloj(
            Modifier
                .align(Alignment.TopEnd)
                .padding(Spacing.sm)
        )
        Text(
            "Vista simulada",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Spacing.sm)
                .background(VELO, RoundedCornerShape(Radius.sm))
                .padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
        )
    }
}

@Composable
private fun Reloj(modifier: Modifier = Modifier) {
    var hora by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            hora = LocalTime.now()
            delay(1_000L - System.currentTimeMillis() % 1_000L)
        }
    }
    Text(
        hora.format(formatoReloj),
        style = MaterialTheme.typography.labelMedium.tabular(),
        color = Color.White,
        modifier = modifier
            .background(VELO, RoundedCornerShape(Radius.sm))
            .padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
    )
}
