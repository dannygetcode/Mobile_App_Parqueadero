package com.parqueadero.appparqueadero.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.parqueadero.appparqueadero.ui.theme.Spacing

/** Encabezado de sección; TalkBack lo expone como heading para saltar entre secciones. */
@Composable
fun SectionHeader(
    titulo: String,
    modifier: Modifier = Modifier,
    accion: String? = null,
    onAccion: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            titulo,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (accion != null && onAccion != null) {
            TextButton(onClick = onAccion) { Text(accion) }
        }
    }
}

@CupoPreviews
@Composable
private fun SectionHeaderPreview() {
    PreviewSurface {
        Column(Modifier.padding(horizontal = Spacing.md)) {
            SectionHeader("Movimientos recientes", accion = "Ver todo", onAccion = {})
            SectionHeader("Mi vehículo")
        }
    }
}
