package com.parqueadero.appparqueadero.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Preferencia de tema; el selector vive en Perfil. */
enum class ThemeMode { SISTEMA, CLARO, OSCURO }

/**
 * Colores de estado y de placa que Material 3 no cubre.
 * Cada estado tiene un color de contenido (icono/texto sobre surface) y un contenedor con su "on".
 */
@Immutable
data class CupoExtendedColors(
    val exito: Color,
    val exitoContainer: Color,
    val onExitoContainer: Color,
    val peligro: Color,
    val peligroContainer: Color,
    val onPeligroContainer: Color,
    val advertencia: Color,
    val advertenciaContainer: Color,
    val onAdvertenciaContainer: Color,
    val neutro: Color,
    val neutroContainer: Color,
    val onNeutroContainer: Color,
    val plate: Color,
    val onPlate: Color,
    val plateBorder: Color,
)

private val LocalCupoColors = staticCompositionLocalOf { LightExtendedColors }

/** Acceso a los colores extendidos: `MaterialTheme.cupo.exito`. */
val MaterialTheme.cupo: CupoExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalCupoColors.current

@Composable
fun CupoTheme(
    themeMode: ThemeMode = ThemeMode.SISTEMA,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SISTEMA -> isSystemInDarkTheme()
        ThemeMode.CLARO -> false
        ThemeMode.OSCURO -> true
    }

    // enableEdgeToEdge sigue al sistema; si el usuario fuerza un modo, ajustamos los iconos de las barras.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalCupoColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = CupoTypography,
            shapes = CupoShapes,
            content = content,
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
