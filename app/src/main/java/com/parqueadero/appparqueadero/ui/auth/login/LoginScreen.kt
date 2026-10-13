package com.parqueadero.appparqueadero.ui.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parqueadero.appparqueadero.data.session.MotivoCierre
import com.parqueadero.appparqueadero.ui.auth.AuthScaffold
import com.parqueadero.appparqueadero.ui.auth.AvisoEnLinea
import com.parqueadero.appparqueadero.ui.auth.BotonPrincipal
import com.parqueadero.appparqueadero.ui.auth.TipoAviso
import com.parqueadero.appparqueadero.ui.components.PinInput
import com.parqueadero.appparqueadero.ui.theme.Spacing

@Composable
fun LoginScreen(
    motivoCierre: MotivoCierre?,
    onAtras: (() -> Unit)?,
    onActivarCuenta: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val foco = LocalFocusManager.current
    val focoPin = remember { FocusRequester() }

    // Si el teléfono ya está prellenado, el foco va directo al PIN.
    LaunchedEffect(estado.telefono.isNotEmpty()) {
        if (estado.telefono.isNotEmpty() && estado.pin.isEmpty()) focoPin.requestFocus()
    }

    AuthScaffold(
        titulo = "Inicia sesión",
        subtitulo = "Usa tu número de celular y el PIN de 6 dígitos que creaste al activar la cuenta.",
        onAtras = onAtras,
        pie = {
            BotonPrincipal(
                texto = "Ingresar",
                cargando = estado.cargando,
                onClick = {
                    foco.clearFocus()
                    viewModel.ingresar()
                },
            )
            TextButton(onClick = onActivarCuenta, modifier = Modifier.fillMaxWidth()) {
                Text("¿Tienes un código nuevo? Activa tu cuenta")
            }
        },
    ) {
        mensajeCierre(motivoCierre)?.let { AvisoEnLinea(it, TipoAviso.INFO) }

        OutlinedTextField(
            value = estado.telefono,
            onValueChange = viewModel::onTelefono,
            label = { Text("Número de celular") },
            leadingIcon = { Icon(Icons.Rounded.PhoneAndroid, contentDescription = null) },
            isError = estado.errorTelefono != null,
            supportingText = estado.errorTelefono?.let { { Text(it) } },
            singleLine = true,
            enabled = !estado.cargando,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focoPin.requestFocus() }),
            modifier = Modifier.fillMaxWidth(),
        )

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text("PIN", style = MaterialTheme.typography.labelLarge)
            PinInput(
                valor = estado.pin,
                onValorChange = viewModel::onPin,
                habilitado = !estado.cargando,
                error = estado.error,
                focusRequester = focoPin,
                onCompleto = {
                    foco.clearFocus()
                    viewModel.ingresar()
                },
            )
        }

        estado.error?.let { AvisoEnLinea(it, TipoAviso.ERROR) }

        Text(
            "¿Olvidaste tu PIN? Pide al administrador un código nuevo y vuelve a activar tu cuenta.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun mensajeCierre(motivo: MotivoCierre?): String? = when (motivo) {
    MotivoCierre.VENCIDA -> "Tu sesión terminó. Vuelve a iniciar sesión para continuar."
    MotivoCierre.PIN_CAMBIADO -> "Cambiaste tu PIN. Inicia sesión con el PIN nuevo."
    MotivoCierre.CERRADA_POR_USUARIO, null -> null
}
