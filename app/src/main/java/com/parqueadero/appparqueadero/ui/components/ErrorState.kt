package com.parqueadero.appparqueadero.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.parqueadero.appparqueadero.ui.theme.cupo

/** Estado de error: el mensaje se anuncia con TalkBack (live region) y ofrece reintentar. */
@Composable
fun ErrorState(
    mensaje: String,
    modifier: Modifier = Modifier,
    titulo: String = "Algo salió mal",
    onReintentar: (() -> Unit)? = null,
) {
    StateLayout(
        titulo = titulo,
        mensaje = mensaje,
        icono = Icons.Rounded.ErrorOutline,
        iconoFondo = MaterialTheme.cupo.peligroContainer,
        iconoColor = MaterialTheme.cupo.onPeligroContainer,
        modifier = modifier,
        mensajeModifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        if (onReintentar != null) {
            Button(onClick = onReintentar, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("Reintentar")
            }
        }
    }
}

@CupoPreviews
@Composable
private fun ErrorStatePreview() {
    PreviewSurface {
        ErrorState(
            mensaje = "No pudimos conectar con el servidor. Revisa tu conexión.",
            onReintentar = {},
        )
    }
}
