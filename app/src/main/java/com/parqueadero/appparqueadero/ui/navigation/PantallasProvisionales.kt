package com.parqueadero.appparqueadero.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.parqueadero.appparqueadero.CupoApp
import com.parqueadero.appparqueadero.ui.components.EmptyState
import com.parqueadero.appparqueadero.ui.components.SectionHeader
import com.parqueadero.appparqueadero.ui.theme.Spacing
import kotlinx.coroutines.launch

// Pantallas provisionales de la Etapa 1. Se reemplazan en la Etapa 2 (Inicio, Pagos, Perfil, Cámaras) y 3 (Actividad).

@Composable
internal fun PantallaProvisional(titulo: String, mensaje: String, onAtras: (() -> Unit)? = null) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(Spacing.md),
    ) {
        if (onAtras != null) {
            IconButton(onClick = onAtras) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
            }
        }
        SectionHeader(titulo)
        EmptyState(titulo = "En construcción", mensaje = mensaje, icono = Icons.Outlined.Construction)
    }
}

@Composable
internal fun InicioProvisional(onAbrirCamaras: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PantallaProvisional("Inicio", "Aquí verás tu cuenta y el control de la puerta.")
        OutlinedButton(onClick = onAbrirCamaras, modifier = Modifier.padding(horizontal = Spacing.md)) {
            Text("Ver cámaras")
        }
    }
}

@Composable
internal fun PerfilProvisional() {
    val container = (LocalContext.current.applicationContext as CupoApp).container
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        PantallaProvisional("Perfil", "Aquí verás tus datos, el cambio de PIN, la huella y el tema.")
        OutlinedButton(
            onClick = { scope.launch { container.authRepository.cerrarSesion() } },
            modifier = Modifier.padding(horizontal = Spacing.md),
        ) {
            Text("Cerrar sesión")
        }
    }
}
