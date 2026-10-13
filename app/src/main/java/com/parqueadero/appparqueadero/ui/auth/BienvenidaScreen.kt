package com.parqueadero.appparqueadero.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.SensorDoor
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.parqueadero.appparqueadero.ui.components.CupoPreviews
import com.parqueadero.appparqueadero.ui.theme.CupoTheme
import com.parqueadero.appparqueadero.ui.theme.Radius
import com.parqueadero.appparqueadero.ui.theme.Spacing

@Composable
fun BienvenidaScreen(
    onActivarCuenta: () -> Unit,
    onIniciarSesion: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
            ) {
                CupoMarca(tamano = 64.dp)
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        "Cupo",
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        "Tu parqueadero desde el celular.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Beneficio(Icons.Rounded.SensorDoor, "Abre la puerta", "Desliza y entra o sal sin buscar a nadie.")
                    Beneficio(Icons.AutoMirrored.Rounded.ReceiptLong, "Paga con una foto", "Sube el comprobante de tu mensualidad.")
                    Beneficio(Icons.Rounded.Insights, "Revisa tu actividad", "Tus entradas, salidas y pagos en un solo lugar.")
                }
                Text(
                    "Para empezar necesitas el código de activación que te entrega el administrador del parqueadero.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            ) {
                BotonPrincipal(texto = "Activar cuenta", onClick = onActivarCuenta)
                OutlinedButton(
                    onClick = onIniciarSesion,
                    shape = RoundedCornerShape(Radius.md),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text("Ya tengo cuenta: iniciar sesión", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun Beneficio(icono: ImageVector, titulo: String, texto: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(Radius.md),
            modifier = Modifier.size(48.dp),
        ) {
            Icon(icono, contentDescription = null, modifier = Modifier.padding(Spacing.sm))
        }
        Column {
            Text(titulo, style = MaterialTheme.typography.titleSmall)
            Text(
                texto,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@CupoPreviews
@Composable
private fun BienvenidaPreview() {
    CupoTheme { BienvenidaScreen(onActivarCuenta = {}, onIniciarSesion = {}) }
}
