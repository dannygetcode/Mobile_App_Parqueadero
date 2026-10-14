package com.parqueadero.appparqueadero.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Escala de espaciado en múltiplos de 4 dp. */
object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

/** Radios de esquina: controles (sm), tarjetas (md), hojas, diálogos y contenedores grandes (lg). */
object Radius {
    val sm = 8.dp
    val md = 12.dp
    val lg = 20.dp
}

val CupoShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.sm),
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg),
    extraLarge = RoundedCornerShape(Radius.lg),
)
