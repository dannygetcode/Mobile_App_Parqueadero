package com.parqueadero.appparqueadero.ui.components

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * true si el usuario desactivó las animaciones del sistema
 * (Opciones de accesibilidad / desarrollador: escala de animación = 0).
 * Se lee una vez por composición del llamador; suficiente para decidir si animar.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}
