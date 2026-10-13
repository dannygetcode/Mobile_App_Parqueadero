package com.parqueadero.appparqueadero.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.parqueadero.appparqueadero.ui.theme.Spacing

/** Estado vacío: explica por qué no hay nada y, si aplica, ofrece el siguiente paso. */
@Composable
fun EmptyState(
    titulo: String,
    mensaje: String,
    modifier: Modifier = Modifier,
    icono: ImageVector = Icons.Outlined.Inbox,
    accion: String? = null,
    onAccion: (() -> Unit)? = null,
) {
    StateLayout(
        titulo = titulo,
        mensaje = mensaje,
        icono = icono,
        iconoFondo = MaterialTheme.colorScheme.surfaceContainerHigh,
        iconoColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    ) {
        if (accion != null && onAccion != null) {
            FilledTonalButton(onClick = onAccion) { Text(accion) }
        }
    }
}

/** Plantilla compartida por EmptyState y ErrorState: icono en círculo, título, mensaje y acción. */
@Composable
internal fun StateLayout(
    titulo: String,
    mensaje: String,
    icono: ImageVector,
    iconoFondo: Color,
    iconoColor: Color,
    modifier: Modifier = Modifier,
    mensajeModifier: Modifier = Modifier,
    accion: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(iconoFondo, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icono, contentDescription = null, tint = iconoColor, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(Spacing.md))
        Text(
            titulo,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = mensajeModifier,
        )
        Spacer(Modifier.height(Spacing.lg))
        accion()
    }
}

@CupoPreviews
@Composable
private fun EmptyStatePreview() {
    PreviewSurface {
        EmptyState(
            titulo = "Aún no hay pagos",
            mensaje = "Cuando subas tu primer comprobante lo verás aquí con su estado.",
            accion = "Subir comprobante",
            onAccion = {},
        )
    }
}
