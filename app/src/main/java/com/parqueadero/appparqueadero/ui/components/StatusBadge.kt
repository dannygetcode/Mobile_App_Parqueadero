package com.parqueadero.appparqueadero.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.parqueadero.appparqueadero.ui.theme.Radius
import com.parqueadero.appparqueadero.ui.theme.Spacing
import com.parqueadero.appparqueadero.ui.theme.cupo

/** Estados con icono propio: el color nunca es la única señal. */
enum class StatusTipo(val icono: ImageVector) {
    EXITO(Icons.Rounded.CheckCircle),
    PELIGRO(Icons.Rounded.Cancel),
    ADVERTENCIA(Icons.Rounded.Schedule),
    NEUTRO(Icons.Rounded.PauseCircle),
    INFO(Icons.Rounded.Info),
}

@Composable
private fun StatusTipo.colores(): Pair<Color, Color> {
    val c = MaterialTheme.cupo
    return when (this) {
        StatusTipo.EXITO -> c.exitoContainer to c.onExitoContainer
        StatusTipo.PELIGRO -> c.peligroContainer to c.onPeligroContainer
        StatusTipo.ADVERTENCIA -> c.advertenciaContainer to c.onAdvertenciaContainer
        StatusTipo.NEUTRO -> c.neutroContainer to c.onNeutroContainer
        StatusTipo.INFO -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
}

/** Insignia de estado: icono + texto en contenedor redondeado. Se lee como un solo elemento. */
@Composable
fun StatusBadge(
    tipo: StatusTipo,
    texto: String,
    modifier: Modifier = Modifier,
) {
    val (container, onContainer) = tipo.colores()
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(Radius.lg),
        color = container,
        contentColor = onContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
        ) {
            Icon(tipo.icono, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(texto, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@CupoPreviews
@Composable
private fun StatusBadgePreview() {
    PreviewSurface {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            StatusBadge(StatusTipo.EXITO, "Al día")
            StatusBadge(StatusTipo.PELIGRO, "Vencido")
            StatusBadge(StatusTipo.ADVERTENCIA, "Vence en 3 días")
            StatusBadge(StatusTipo.NEUTRO, "Suspendido")
            StatusBadge(StatusTipo.INFO, "En revisión")
        }
    }
}
