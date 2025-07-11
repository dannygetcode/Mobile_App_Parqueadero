package com.parqueadero.appparqueadero.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

@Composable
fun BotonCircularPuerta(
    isAbierto: Boolean,
    onFinish: (Boolean) -> Unit
) {
    var progreso by remember { mutableStateOf(if (isAbierto) 1f else 0f) }
    var enProceso by remember { mutableStateOf(false) }
    var abriendo by remember { mutableStateOf(isAbierto) }

    val animatedProgress by animateFloatAsState(
        targetValue = progreso,
        animationSpec = tween(durationMillis = 2000),
        finishedListener = {
            enProceso = false
            onFinish(abriendo)
        }
    )

    val colorBorde = if (enProceso) {
        if (abriendo) Color(0xFF00C853) else Color(0xFFD50000)
    } else {
        if (abriendo) Color(0xFF00E676) else Color(0xFFFF1744)
    }

    Box(
        modifier = Modifier
            .size(200.dp)
            .clickable(enabled = !enProceso) {
                enProceso = true
                abriendo = !abriendo
                progreso = if (abriendo) 1f else 0f
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 16f, cap = StrokeCap.Round)
            drawArc(
                color = colorBorde,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = stroke
            )
        }

        Text(
            text = when {
                enProceso && abriendo -> "Abriendo..."
                enProceso && !abriendo -> "Cerrando..."
                !enProceso && abriendo -> "Abierto"
                else -> "Cerrado"
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}
