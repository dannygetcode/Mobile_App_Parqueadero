package com.parqueadero.appparqueadero.ui.perfil

import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import com.parqueadero.appparqueadero.ui.biometria.HardwareHuella
import com.parqueadero.appparqueadero.ui.biometria.fragmentActivity
import com.parqueadero.appparqueadero.ui.biometria.hardwareHuella
import com.parqueadero.appparqueadero.ui.biometria.pedirHuella
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parqueadero.appparqueadero.data.model.Carroceria
import com.parqueadero.appparqueadero.data.model.TipoVehiculo
import com.parqueadero.appparqueadero.data.model.UsuarioDTO
import com.parqueadero.appparqueadero.ui.auth.AvisoEnLinea
import com.parqueadero.appparqueadero.ui.auth.TipoAviso
import com.parqueadero.appparqueadero.ui.common.UiState
import com.parqueadero.appparqueadero.ui.components.ErrorState
import com.parqueadero.appparqueadero.ui.components.PinInput
import com.parqueadero.appparqueadero.ui.components.PlateChip
import com.parqueadero.appparqueadero.ui.components.PlateSize
import com.parqueadero.appparqueadero.ui.components.SectionHeader
import com.parqueadero.appparqueadero.ui.components.Skeleton
import com.parqueadero.appparqueadero.ui.components.StatusBadge
import com.parqueadero.appparqueadero.ui.inicio.estadoCuenta
import com.parqueadero.appparqueadero.ui.theme.Radius
import com.parqueadero.appparqueadero.ui.theme.Spacing
import com.parqueadero.appparqueadero.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(viewModel: PerfilViewModel = viewModel(factory = PerfilViewModel.Factory)) {
    val usuario by viewModel.usuario.collectAsStateWithLifecycle()
    val tema by viewModel.tema.collectAsStateWithLifecycle()
    val cambioPin by viewModel.cambioPin.collectAsStateWithLifecycle()
    val huellaActivada by viewModel.huellaActivada.collectAsStateWithLifecycle()
    val activacionHuella by viewModel.activacionHuella.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    var confirmarSalida by remember { mutableStateOf(false) }
    val contexto = LocalContext.current
    val hardware = remember { hardwareHuella(contexto) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbar.showSnackbar(it)
            viewModel.mensajeMostrado()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Text("Perfil") }) },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
        ) {
            when (val u = usuario) {
                UiState.Loading -> Skeleton(Modifier.fillMaxWidth().height(200.dp), shape = RoundedCornerShape(Radius.lg))
                is UiState.Error -> ErrorState(mensaje = u.mensaje, titulo = "No pudimos cargar tus datos", onReintentar = viewModel::cargar)
                UiState.Empty -> Unit
                is UiState.Success -> {
                    DatosPersonales(u.data)
                    Vehiculo(u.data)
                }
            }

            SectionHeader("Seguridad")
            FilaAccion(
                titulo = "Cambiar PIN",
                detalle = "Tendrás que volver a iniciar sesión",
                onClick = viewModel::abrirCambioPin,
            )
            if (hardware != HardwareHuella.NO_DISPONIBLE) {
                FilaHuella(
                    activada = huellaActivada,
                    habilitada = hardware == HardwareHuella.DISPONIBLE,
                    onCambio = { activar -> if (activar) viewModel.abrirActivacionHuella() else viewModel.desactivarHuella() },
                )
            }

            SectionHeader("Apariencia")
            SelectorTema(tema, viewModel::elegirTema)

            OutlinedButton(
                onClick = { confirmarSalida = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                Text("Cerrar sesión", modifier = Modifier.padding(start = Spacing.xs))
            }
        }
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Para volver a entrar necesitarás tu número de celular y tu PIN.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarSalida = false
                    viewModel.cerrarSesion()
                }) { Text("Cerrar sesión") }
            },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") } },
        )
    }

    cambioPin?.let { DialogoCambioPin(it, viewModel) }
    activacionHuella?.let { DialogoActivarHuella(it, viewModel) }
}

@Composable
private fun FilaHuella(activada: Boolean, habilitada: Boolean, onCambio: (Boolean) -> Unit) {
    Card(
        shape = RoundedCornerShape(Radius.md),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier
                .toggleable(value = activada, enabled = habilitada, role = Role.Switch, onValueChange = onCambio)
                .heightIn(min = 56.dp)
                .padding(Spacing.md),
        ) {
            Icon(Icons.Rounded.Fingerprint, contentDescription = null)
            Column(Modifier.weight(1f)) {
                Text("Entrar con huella", style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        !habilitada -> "Registra una huella en los ajustes del teléfono para usarla aquí."
                        activada -> "Al abrir Cupo te pediremos la huella en lugar del PIN."
                        else -> "Úsala en lugar del PIN al abrir la app."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = activada, onCheckedChange = null, enabled = habilitada)
        }
    }
}

@Composable
private fun DialogoActivarHuella(s: ActivacionHuellaUiState, vm: PerfilViewModel) {
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()
    val confirmar: () -> Unit = {
        val actividad = contexto.fragmentActivity()
        if (actividad != null) {
            scope.launch {
                val cifrador = vm.verificarPinHuella() ?: return@launch
                pedirHuella(
                    actividad = actividad,
                    titulo = "Activar huella",
                    subtitulo = "Toca el lector para guardar tu acceso",
                    textoAlternativo = "Cancelar",
                    cifrador = cifrador,
                    onExito = vm::guardarHuella,
                    onFalla = vm::falloHuella,
                )
            }
        }
    }
    AlertDialog(
        onDismissRequest = vm::cerrarActivacionHuella,
        icon = { Icon(Icons.Rounded.Fingerprint, contentDescription = null) },
        title = { Text("Activar huella") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    "Confirma tu PIN. Lo guardaremos cifrado en este teléfono y solo se podrá usar con tu huella.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                CampoPin("PIN", s.pin, s.error, !s.verificando, vm::onPinHuella)
            }
        },
        confirmButton = {
            TextButton(onClick = confirmar, enabled = !s.verificando) {
                Text(if (s.verificando) "Verificando..." else "Continuar")
            }
        },
        dismissButton = { TextButton(onClick = vm::cerrarActivacionHuella, enabled = !s.verificando) { Text("Cancelar") } },
    )
}

@Composable
private fun TarjetaPerfil(contenido: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(Radius.lg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) { contenido() }
    }
}

@Composable
private fun DatosPersonales(u: UsuarioDTO) {
    TarjetaPerfil {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                listOfNotNull(u.nombre, u.apellido).joinToString(" ").ifBlank { "Usuario" },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            val (tipo, texto) = estadoCuenta(u)
            StatusBadge(tipo, texto)
        }
        Dato("Celular", u.telefono)
        u.cupo?.let { Dato("Cupo", "${it.codigo} · ${if (it.tipoVehiculo == TipoVehiculo.MOTO) "moto" else "carro"}") }
    }
}

@Composable
private fun Vehiculo(u: UsuarioDTO) {
    val v = u.vehiculo ?: return
    TarjetaPerfil {
        Text("Vehículo", style = MaterialTheme.typography.titleSmall)
        PlateChip(v.placa, size = PlateSize.Large)
        Dato("Tipo", if (v.tipoVehiculo == TipoVehiculo.MOTO) "Moto" else "Carro")
        v.carroceria?.let { Dato("Carrocería", nombreCarroceria(it)) }
        v.marca?.let { Dato("Marca", it) }
        v.color?.let { Dato("Color", it) }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Row(Modifier.semantics(mergeDescendants = true) {}) {
        Text(
            etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(valor, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FilaAccion(titulo: String, detalle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(Radius.md),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.padding(Spacing.md),
        ) {
            Icon(Icons.Rounded.Password, contentDescription = null)
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorTema(actual: ThemeMode, onElegir: (ThemeMode) -> Unit) {
    val opciones = listOf(ThemeMode.SISTEMA to "Sistema", ThemeMode.CLARO to "Claro", ThemeMode.OSCURO to "Oscuro")
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        opciones.forEachIndexed { i, (modo, etiqueta) ->
            SegmentedButton(
                selected = actual == modo,
                onClick = { onElegir(modo) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = opciones.size),
            ) { Text(etiqueta) }
        }
    }
}

@Composable
private fun DialogoCambioPin(s: CambioPinUiState, vm: PerfilViewModel) {
    AlertDialog(
        onDismissRequest = vm::cerrarCambioPin,
        title = { Text("Cambiar PIN") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                CampoPin("PIN actual", s.pinActual, s.errorActual, !s.enviando, vm::onPinActual)
                CampoPin("PIN nuevo", s.pinNuevo, s.errorNuevo, !s.enviando, vm::onPinNuevo)
                CampoPin("Confirma el PIN nuevo", s.confirmacion, s.errorConfirmacion, !s.enviando, vm::onConfirmacion)
                s.error?.let { AvisoEnLinea(it, TipoAviso.ERROR) }
                Text(
                    "Al cambiarlo se cerrará tu sesión y entrarás de nuevo con el PIN nuevo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = vm::confirmarCambioPin, enabled = !s.enviando) {
                Text(if (s.enviando) "Cambiando..." else "Cambiar")
            }
        },
        dismissButton = { TextButton(onClick = vm::cerrarCambioPin, enabled = !s.enviando) { Text("Cancelar") } },
    )
}

@Composable
private fun CampoPin(etiqueta: String, valor: String, error: String?, habilitado: Boolean, onCambio: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        Text(etiqueta, style = MaterialTheme.typography.labelLarge)
        PinInput(valor = valor, onValorChange = onCambio, etiqueta = etiqueta, habilitado = habilitado, error = error)
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}

private fun nombreCarroceria(c: Carroceria): String = when (c) {
    Carroceria.SEDAN -> "Sedán"
    Carroceria.HATCHBACK -> "Hatchback"
    Carroceria.SUV -> "Camioneta SUV"
    Carroceria.PICKUP -> "Pick-up"
    Carroceria.VAN -> "Van"
    Carroceria.COUPE -> "Coupé"
    Carroceria.OTRO -> "Otra"
}
