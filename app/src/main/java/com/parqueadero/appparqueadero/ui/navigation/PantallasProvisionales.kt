package com.parqueadero.appparqueadero.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.parqueadero.appparqueadero.ui.components.EmptyState
import com.parqueadero.appparqueadero.ui.components.SectionHeader
import com.parqueadero.appparqueadero.ui.theme.Spacing

// Pantalla provisional de Actividad; se reemplaza en la Etapa 3.

@Composable
internal fun PantallaProvisional(titulo: String, mensaje: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(Spacing.md),
    ) {
        SectionHeader(titulo)
        EmptyState(titulo = "En construcción", mensaje = mensaje, icono = Icons.Outlined.Construction)
    }
}
