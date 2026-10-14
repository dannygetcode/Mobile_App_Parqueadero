package com.parqueadero.appparqueadero.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parqueadero.appparqueadero.ui.theme.CupoFonts
import com.parqueadero.appparqueadero.ui.theme.Spacing
import com.parqueadero.appparqueadero.ui.theme.cupo

enum class PlateSize(
    internal val fontSize: TextUnit,
    internal val horizontalPadding: Dp,
    internal val verticalPadding: Dp,
    internal val border: Dp,
    internal val corner: Dp,
) {
    // Esquinas proporcionales al tamaño de la placa (excepción documentada a la escala 8/12/20).
    Small(fontSize = 13.sp, horizontalPadding = 8.dp, verticalPadding = 2.dp, border = 1.5.dp, corner = 4.dp),
    Medium(fontSize = 18.sp, horizontalPadding = 12.dp, verticalPadding = 4.dp, border = 2.dp, corner = 6.dp),
    Large(fontSize = 28.sp, horizontalPadding = 16.dp, verticalPadding = 8.dp, border = 3.dp, corner = 8.dp),
}

/** "abc123" -> "ABC 123"; motos "abc12d" -> "ABC 12D". Si no empieza por 3 letras, se deja limpia. */
internal fun formatPlaca(placa: String): String {
    val limpia = placa.filter { it.isLetterOrDigit() }.uppercase()
    return if (limpia.length > 3 && limpia.take(3).all { it.isLetter() }) {
        limpia.substring(0, 3) + " " + limpia.substring(3)
    } else {
        limpia
    }
}

/** "ABC 123" -> "Placa A B C 1 2 3", para que TalkBack la lea carácter por carácter. */
internal fun describePlaca(placa: String): String =
    "Placa " + placa.filter { it.isLetterOrDigit() }.uppercase().toList().joinToString(" ")

/** Placa colombiana: fondo amarillo, borde y texto oscuros, cifras tabulares. */
@Composable
fun PlateChip(
    placa: String,
    modifier: Modifier = Modifier,
    size: PlateSize = PlateSize.Medium,
) {
    val colors = MaterialTheme.cupo
    val shape = RoundedCornerShape(size.corner)
    Box(
        modifier = modifier
            .clearAndSetSemantics { contentDescription = describePlaca(placa) }
            .background(colors.plate, shape)
            .border(size.border, colors.plateBorder, shape)
            .padding(horizontal = size.horizontalPadding, vertical = size.verticalPadding),
    ) {
        Text(
            text = formatPlaca(placa),
            color = colors.onPlate,
            maxLines = 1,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontFamily = CupoFonts.SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = size.fontSize,
                letterSpacing = 1.sp,
                fontFeatureSettings = "tnum",
            ),
        )
    }
}

@CupoPreviews
@Composable
private fun PlateChipPreview() {
    PreviewSurface {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            PlateChip("abc123", size = PlateSize.Small)
            PlateChip("KLM 456")
            PlateChip("xyz12d", size = PlateSize.Large)
        }
    }
}
