package com.parqueadero.appparqueadero.ui.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parqueadero.appparqueadero.data.session.MotivoCierre
import com.parqueadero.appparqueadero.ui.auth.AuthScaffold
import com.parqueadero.appparqueadero.ui.auth.AvisoEnLinea
import com.parqueadero.appparqueadero.ui.auth.BotonPrincipal
import com.parqueadero.appparqueadero.ui.auth.TipoAviso
import com.parqueadero.appparqueadero.ui.biometria.HardwareHuella
import com.parqueadero.appparqueadero.ui.biometria.fragmentActivity
import com.parqueadero.appparqueadero.ui.biometria.hardwareHuella
import com.parqueadero.appparqueadero.ui.biometria.pedirHuella
import com.parqueadero.appparqueadero.ui.components.PinInput
import com.parqueadero.appparqueadero.ui.theme.Radius
import com.parqueadero.appparqueadero.ui.theme.Spacing
import kotlinx.coroutines.launch

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
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()
    val puedeUsarHuella = estado.huellaConfigurada && hardwareHuella(contexto) == HardwareHuella.DISPONIBLE
    var yaPidioHuella by rememberSaveable { mutableStateOf(false) }

    val abrirHuella: () -> Unit = {
        val actividad = contexto.fragmentActivity()
        if (actividad != null) {
            scope.launch {
                val cifrador = viewModel.prepararHuella() ?: return@launch
                pedirHuella(
                    actividad = actividad,
                    titulo = "Entrar a Cupo",
                    subtitulo = "Usa tu huella para iniciar sesión",
                    textoAlternativo = "Usar PIN",
                    cifrador = cifrador,
                    onExito = viewModel::ingresarConHuella,
                    onFalla = viewModel::falloHuella,
                )
            }
        }
    }

    // Al abrir la app con la huella activada se pide directamente (no tras cerrar sesión a propósito).
    LaunchedEffect(puedeUsarHuella) {
        val pedirSola = motivoCierre == null || motivoCierre == MotivoCierre.APP_CERRADA || motivoCierre == MotivoCierre.VENCIDA
        if (puedeUsarHuella && !yaPidioHuella && pedirSola) {
            yaPidioHuella = true
            abrirHuella()
        }
    }

    // Si el teléfono ya está prellenado, el foco va directo al PIN (salvo que se vaya a pedir la huella).
    LaunchedEffect(estado.telefono.isNotEmpty(), puedeUsarHuella) {
        if (estado.telefono.isNotEmpty() && estado.pin.isEmpty() && !puedeUsarHuella) focoPin.requestFocus()
    }

    AuthScaffold(
        titulo = "Inicia sesión",
        subtitulo = "Usa tu número de celular y el PIN de 6 dígitos que creaste al activar la cuenta.",
        onAtras = onAtras,
        pie = {
            if (puedeUsarHuella) {
                OutlinedButton(
                    onClick = abrirHuella,
                    enabled = !estado.cargando,
                    shape = RoundedCornerShape(Radius.md),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) {
                    Icon(Icons.Rounded.Fingerprint, contentDescription = null)
                    Text("Entrar con huella", modifier = Modifier.padding(start = Spacing.xs))
                }
            }
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
            // El campo ya anuncia "PIN": la etiqueta visible no se repite en TalkBack.
            Text("PIN", style = MaterialTheme.typography.labelLarge, modifier = Modifier.clearAndSetSemantics { })
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
    MotivoCierre.CERRADA_POR_USUARIO, MotivoCierre.APP_CERRADA, null -> null
}
