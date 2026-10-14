package com.parqueadero.appparqueadero.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.parqueadero.appparqueadero.ui.theme.Radius
import com.parqueadero.appparqueadero.ui.theme.Spacing
import com.parqueadero.appparqueadero.ui.theme.tabular

/**
 * Indicador grande. `valor` debe incluir su unidad (p. ej. "4 de 7") y `apoyo` el período o contexto.
 * Si `valor` es null se muestra "Sin datos todavía" atenuado (nunca un 0 inventado).
 */
@Composable
fun KpiCard(
    titulo: String,
    valor: String?,
    modifier: Modifier = Modifier,
    apoyo: String? = null,
    icono: ImageVector? = null,
) {
    Card(
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(Radius.md),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icono != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(Radius.sm)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            icono,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(Spacing.xs))
                }
                Text(
                    titulo,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.size(Spacing.sm))
            if (valor != null) {
                Text(
                    valor,
                    style = MaterialTheme.typography.headlineLarge.tabular(),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                Text(
                    "Sin datos todavía",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (apoyo != null) {
                Spacer(Modifier.size(Spacing.xxs))
                Text(
                    apoyo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@CupoPreviews
@Composable
private fun KpiCardPreview() {
    PreviewSurface {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            KpiCard("Cupos libres", "4 de 7", apoyo = "Ahora", icono = Icons.Rounded.LocalParking)
            KpiCard("Ocupación promedio", null, apoyo = "Últimos 7 días")
        }
    }
}
