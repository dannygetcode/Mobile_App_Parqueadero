package com.parqueadero.appparqueadero.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Paleta de marca "Cupo" (señalética vial).
 * Azul de la señal P como primario, amarillo de placa colombiana como acento
 * (siempre de fondo con texto oscuro) y neutros pizarra/asfalto.
 * Los ratios de contraste de los pares clave están en docs/diseno/sistema-cupo.md.
 */

// Marca
val CupoAzulSenal = Color(0xFF0B5BD3)
val CupoAzulClaro = Color(0xFF8AB4FF)
val CupoAmarilloPlaca = Color(0xFFFFC400)
val CupoAsfalto = Color(0xFF14171C)
val CupoAsfaltoProfundo = Color(0xFF101318)
val CupoNiebla = Color(0xFFF7F8FA)

internal val LightColors = lightColorScheme(
    primary = CupoAzulSenal,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E5FF),
    onPrimaryContainer = Color(0xFF00214F),
    inversePrimary = Color(0xFFADC6FF),

    secondary = Color(0xFF4F5B6B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD8E0EC),
    onSecondaryContainer = Color(0xFF0F1A27),

    // Terciario = familia del amarillo de placa. El amarillo solo vive en el contenedor.
    tertiary = Color(0xFF745B00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = CupoAmarilloPlaca,
    onTertiaryContainer = CupoAsfalto,

    background = CupoNiebla,
    onBackground = CupoAsfalto,
    surface = CupoNiebla,
    onSurface = CupoAsfalto,
    surfaceVariant = Color(0xFFDEE2E8),
    onSurfaceVariant = Color(0xFF4A5361),
    surfaceTint = CupoAzulSenal,
    inverseSurface = Color(0xFF2B3038),
    inverseOnSurface = Color(0xFFEEF0F4),

    surfaceBright = CupoNiebla,
    surfaceDim = Color(0xFFD7DBE1),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F3F6),
    surfaceContainer = Color(0xFFEBEEF2),
    surfaceContainerHigh = Color(0xFFE5E8ED),
    surfaceContainerHighest = Color(0xFFDEE2E8),

    outline = Color(0xFF737C89),
    outlineVariant = Color(0xFFC4CAD3),
    scrim = Color(0xFF000000),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

internal val DarkColors = darkColorScheme(
    primary = CupoAzulClaro,
    onPrimary = Color(0xFF002F6C),
    primaryContainer = Color(0xFF0A47A6),
    onPrimaryContainer = Color(0xFFD8E5FF),
    inversePrimary = CupoAzulSenal,

    secondary = Color(0xFFBCC7D8),
    onSecondary = Color(0xFF26313F),
    secondaryContainer = Color(0xFF3C4757),
    onSecondaryContainer = Color(0xFFD8E3F4),

    tertiary = CupoAmarilloPlaca,
    onTertiary = CupoAsfalto,
    tertiaryContainer = Color(0xFF574400),
    onTertiaryContainer = Color(0xFFFFE08A),

    background = CupoAsfaltoProfundo,
    onBackground = Color(0xFFE2E5EA),
    surface = CupoAsfaltoProfundo,
    onSurface = Color(0xFFE2E5EA),
    surfaceVariant = Color(0xFF31353C),
    onSurfaceVariant = Color(0xFFC0C7D1),
    surfaceTint = CupoAzulClaro,
    inverseSurface = Color(0xFFE2E5EA),
    inverseOnSurface = Color(0xFF2B3038),

    surfaceBright = Color(0xFF363A41),
    surfaceDim = CupoAsfaltoProfundo,
    surfaceContainerLowest = Color(0xFF0B0E12),
    surfaceContainerLow = Color(0xFF181B21),
    surfaceContainer = Color(0xFF1C2026),
    surfaceContainerHigh = Color(0xFF262A31),
    surfaceContainerHighest = Color(0xFF31353C),

    outline = Color(0xFF8A93A0),
    outlineVariant = Color(0xFF404754),
    scrim = Color(0xFF000000),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

internal val LightExtendedColors = CupoExtendedColors(
    exito = Color(0xFF1B7F3B),
    exitoContainer = Color(0xFFD3F5DC),
    onExitoContainer = Color(0xFF00391A),
    peligro = Color(0xFFC62828),
    peligroContainer = Color(0xFFFFDAD6),
    onPeligroContainer = Color(0xFF5C0A0A),
    advertencia = Color(0xFF8A5300),
    advertenciaContainer = Color(0xFFFFE2A8),
    onAdvertenciaContainer = Color(0xFF3D2600),
    neutro = Color(0xFF5B6472),
    neutroContainer = Color(0xFFE3E7ED),
    onNeutroContainer = Color(0xFF2B3038),
    plate = CupoAmarilloPlaca,
    onPlate = CupoAsfalto,
    plateBorder = CupoAsfalto,
)

internal val DarkExtendedColors = CupoExtendedColors(
    exito = Color(0xFF6FD48E),
    exitoContainer = Color(0xFF0E4F27),
    onExitoContainer = Color(0xFFB7F2C8),
    peligro = Color(0xFFFF8A80),
    peligroContainer = Color(0xFF7A1616),
    onPeligroContainer = Color(0xFFFFDAD6),
    advertencia = Color(0xFFFFC857),
    advertenciaContainer = Color(0xFF5A3D00),
    onAdvertenciaContainer = Color(0xFFFFE2A8),
    neutro = Color(0xFFAEB6C2),
    neutroContainer = Color(0xFF31353C),
    onNeutroContainer = Color(0xFFE2E5EA),
    // La placa es un objeto físico: mismo amarillo y borde en ambos modos.
    plate = CupoAmarilloPlaca,
    onPlate = CupoAsfalto,
    plateBorder = CupoAsfalto,
)
