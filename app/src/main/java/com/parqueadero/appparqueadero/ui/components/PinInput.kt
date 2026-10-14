package com.parqueadero.appparqueadero.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.parqueadero.appparqueadero.ui.theme.Radius
import com.parqueadero.appparqueadero.ui.theme.Spacing

/**
 * PIN numérico en casillas. Muestra puntos, nunca los dígitos.
 * [onCompleto] se llama una vez cuando el usuario escribe el último dígito.
 */
@Composable
fun PinInput(
    valor: String,
    onValorChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    etiqueta: String = "PIN",
    longitud: Int = 6,
    habilitado: Boolean = true,
    error: String? = null,
    focusRequester: FocusRequester? = null,
    imeAction: ImeAction = ImeAction.Done,
    onCompleto: (() -> Unit)? = null,
) {
    val interaccion = remember { MutableInteractionSource() }
    val enfocado by interaccion.collectIsFocusedAsState()
    val colores = MaterialTheme.colorScheme

    BasicTextField(
        value = valor,
        onValueChange = { nuevo ->
            val limpio = nuevo.filter { it.isDigit() }.take(longitud)
            if (limpio == valor) return@BasicTextField
            onValorChange(limpio)
            if (limpio.length == longitud && valor.length < longitud) onCompleto?.invoke()
        },
        enabled = habilitado,
        singleLine = true,
        interactionSource = interaccion,
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onCompleto?.invoke() }),
        modifier = modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .semantics {
                password()
                contentDescription = "$etiqueta, ${valor.length} de $longitud dígitos"
                if (error != null) error(error)
            },
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                repeat(longitud) { indice ->
                    val lleno = indice < valor.length
                    val actual = enfocado && indice == valor.length.coerceAtMost(longitud - 1)
                    val borde = when {
                        error != null -> colores.error
                        actual -> colores.primary
                        lleno -> colores.onSurfaceVariant
                        else -> colores.outlineVariant
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.85f)
                            .background(colores.surfaceContainerHighest, RoundedCornerShape(Radius.md))
                            .border(if (actual) 2.dp else 1.dp, borde, RoundedCornerShape(Radius.md)),
                    ) {
                        if (lleno) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .background(colores.onSurface, CircleShape)
                            )
                        }
                    }
                }
            }
        },
    )
}
