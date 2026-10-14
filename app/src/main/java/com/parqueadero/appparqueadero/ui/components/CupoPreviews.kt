package com.parqueadero.appparqueadero.ui.components

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.parqueadero.appparqueadero.ui.theme.CupoTheme

/** Multipreview: el mismo componente en modo claro y oscuro. */
@Preview(name = "Claro", showBackground = true)
@Preview(name = "Oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
annotation class CupoPreviews

/** Envoltorio para previews: tema Cupo + superficie de fondo. */
@Composable
internal fun PreviewSurface(content: @Composable () -> Unit) {
    CupoTheme {
        Surface(content = content)
    }
}
