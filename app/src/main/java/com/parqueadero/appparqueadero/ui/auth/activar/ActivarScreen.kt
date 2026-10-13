package com.parqueadero.appparqueadero.ui.auth.activar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parqueadero.appparqueadero.ui.auth.AuthScaffold
import com.parqueadero.appparqueadero.ui.auth.AvisoEnLinea
import com.parqueadero.appparqueadero.ui.auth.BotonPrincipal
import com.parqueadero.appparqueadero.ui.auth.TipoAviso
import com.parqueadero.appparqueadero.ui.auth.Validaciones
import com.parqueadero.appparqueadero.ui.common.UiState
import com.parqueadero.appparqueadero.ui.components.ErrorState
import com.parqueadero.appparqueadero.ui.components.PinInput
import com.parqueadero.appparqueadero.ui.components.Skeleton
import com.parqueadero.appparqueadero.ui.theme.Radius
import com.parqueadero.appparqueadero.ui.theme.Spacing
import com.parqueadero.appparqueadero.ui.theme.cupo

@Composable
fun ActivarScreen(
    onSalir: () -> Unit,
    onIrALogin: () -> Unit,
    viewModel: ActivarViewModel = viewModel(factory = ActivarViewModel.Factory),
) {
    val s by viewModel.estado.collectAsStateWithLifecycle()
    val foco = LocalFocusManager.current
    val atras = { if (!viewModel.atras()) onSalir() }

    BackHandler(enabled = s.paso != PasoActivacion.DATOS && s.paso != PasoActivacion.LISTO) { atras() }

    if (s.paso == PasoActivacion.LISTO) {
        CuentaActivada(onIrALogin)
        return
    }

    val numeroPaso = s.paso.ordinal + 1
    val (titulo, subtitulo) = when (s.paso) {
        PasoActivacion.DATOS -> "Activa tu cuenta" to
            "Escribe tu celular y el código de 8 caracteres que te entregó el administrador del parqueadero."
        PasoActivacion.PIN -> "Crea tu PIN" to
            "Lo usarás para entrar a la app. Evita números repetidos o consecutivos como 111111 o 123456."
        else -> "Tus datos" to "Lee el aviso de privacidad y acéptalo para terminar."
    }

    AuthScaffold(
        titulo = titulo,
        subtitulo = subtitulo,
        onAtras = atras,
        pie = {
            BotonPrincipal(
                texto = if (s.paso == PasoActivacion.CONSENTIMIENTO) "Activar cuenta" else "Continuar",
                cargando = s.enviando,
                habilitado = s.paso != PasoActivacion.CONSENTIMIENTO ||
                    (s.aceptaConsentimiento && s.aviso is UiState.Success),
                onClick = {
                    foco.clearFocus()
                    viewModel.continuar()
                },
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                "Paso $numeroPaso de 3",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { numeroPaso / 3f },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        when (s.paso) {
            PasoActivacion.DATOS -> PasoDatos(s, viewModel)
            PasoActivacion.PIN -> PasoPin(s, viewModel)
            PasoActivacion.CONSENTIMIENTO -> PasoConsentimiento(s, viewModel)
            PasoActivacion.LISTO -> Unit
        }

        s.error?.let { AvisoEnLinea(it, TipoAviso.ERROR) }
    }
}

@Composable
private fun PasoDatos(s: ActivarUiState, vm: ActivarViewModel) {
    val focoCodigo = remember { FocusRequester() }
    OutlinedTextField(
        value = s.telefono,
        onValueChange = vm::onTelefono,
        label = { Text("Número de celular") },
        leadingIcon = { Icon(Icons.Rounded.PhoneAndroid, contentDescription = null) },
        isError = s.errorTelefono != null,
        supportingText = s.errorTelefono?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focoCodigo.requestFocus() }),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = s.codigo,
        onValueChange = vm::onCodigo,
        label = { Text("Código de activación") },
        placeholder = { Text("Ej. 7KQ2M9XA") },
        leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
        isError = s.errorCodigo != null,
        supportingText = {
            Text(s.errorCodigo ?: "Vence 72 horas después de generado y sirve una sola vez.")
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Ascii,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { vm.continuar() }),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focoCodigo),
    )
}

@Composable
private fun PasoPin(s: ActivarUiState, vm: ActivarViewModel) {
    val focoPin = remember { FocusRequester() }
    val focoConfirmacion = remember { FocusRequester() }
    LaunchedEffect(Unit) { focoPin.requestFocus() }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text("PIN nuevo", style = MaterialTheme.typography.labelLarge)
        PinInput(
            valor = s.pin,
            onValorChange = vm::onPin,
            etiqueta = "PIN nuevo",
            error = s.errorPin,
            focusRequester = focoPin,
            imeAction = ImeAction.Next,
            onCompleto = { focoConfirmacion.requestFocus() },
        )
        s.errorPin?.let { TextoError(it) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text("Confirma el PIN", style = MaterialTheme.typography.labelLarge)
        PinInput(
            valor = s.confirmacion,
            onValorChange = vm::onConfirmacion,
            etiqueta = "Confirmación del PIN",
            error = s.errorConfirmacion,
            habilitado = Validaciones.pinCompleto(s.pin),
            focusRequester = focoConfirmacion,
            onCompleto = { vm.continuar() },
        )
        s.errorConfirmacion?.let { TextoError(it) }
    }
}

@Composable
private fun PasoConsentimiento(s: ActivarUiState, vm: ActivarViewModel) {
    when (val aviso = s.aviso) {
        UiState.Loading -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            repeat(6) { Skeleton(Modifier.fillMaxWidth().height(16.dp)) }
        }
        is UiState.Error -> ErrorState(
            mensaje = aviso.mensaje,
            titulo = "No pudimos cargar el aviso de privacidad",
            onReintentar = vm::cargarAviso,
        )
        UiState.Empty -> Unit
        is UiState.Success -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.md))
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    "Aviso de privacidad, versión ${aviso.data.version}",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(aviso.data.texto, style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(
                        value = s.aceptaConsentimiento,
                        role = Role.Checkbox,
                        enabled = !s.enviando,
                        onValueChange = vm::onAceptaConsentimiento,
                    ),
            ) {
                Checkbox(checked = s.aceptaConsentimiento, onCheckedChange = null)
                Text(
                    "Leí el aviso y autorizo el tratamiento de mis datos personales.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun CuentaActivada(onIrALogin: () -> Unit) {
    AuthScaffold(
        titulo = "Cuenta activada",
        subtitulo = "Ya puedes iniciar sesión con tu número de celular y el PIN que acabas de crear.",
        onAtras = null,
        pie = { BotonPrincipal(texto = "Iniciar sesión", onClick = onIrALogin) },
    ) {
        Icon(
            Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.cupo.exito,
            modifier = Modifier
                .size(72.dp)
                .semantics { contentDescription = "Activación exitosa" },
        )
    }
}

@Composable
private fun TextoError(texto: String) {
    Text(texto, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}
