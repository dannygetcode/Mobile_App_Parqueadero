package com.parqueadero.appparqueadero.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.parqueadero.appparqueadero.ui.theme.Radius
import com.parqueadero.appparqueadero.ui.theme.Spacing

/**
 * Bloque de carga con un brillo que recorre el bloque. Estático si el sistema reduce animaciones.
 * Es invisible para TalkBack: el contenedor de la pantalla debe anunciar "Cargando".
 * El tamaño lo define el llamador vía `modifier`.
 */
@Composable
fun Skeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Radius.sm),
) {
    val reducedMotion = rememberReducedMotion()
    val base = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlight = remember(base) {
        val alpha = if (base.luminance() > 0.5f) 0.6f else 0.08f
        Color.White.copy(alpha = alpha).compositeOver(base)
    }

    val progress: State<Float> = if (reducedMotion) {
        remember { mutableFloatStateOf(0f) }
    } else {
        rememberInfiniteTransition(label = "skeleton").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 1200, easing = LinearEasing), RepeatMode.Restart),
            label = "skeletonShimmer",
        )
    }

    Box(
        modifier = modifier
            .clearAndSetSemantics { }
            .clip(shape)
            .drawBehind {
                drawRect(base)
                if (!reducedMotion) {
                    // Se lee el progreso en la fase de dibujo: anima sin recomponer.
                    val band = size.width * 0.6f
                    val x = -band + (size.width + band) * progress.value
                    drawRect(
                        Brush.linearGradient(
                            colors = listOf(Color.Transparent, highlight, Color.Transparent),
                            start = Offset(x, 0f),
                            end = Offset(x + band, size.height),
                        ),
                    )
                }
            },
    )
}

@CupoPreviews
@Composable
private fun SkeletonPreview() {
    PreviewSurface {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Skeleton(Modifier.fillMaxWidth(0.5f).height(20.dp))
            Skeleton(Modifier.fillMaxWidth().height(96.dp), RoundedCornerShape(Radius.md))
        }
    }
}
