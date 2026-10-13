package com.parqueadero.appparqueadero.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parqueadero.appparqueadero.data.model.EstadoUsuario
import com.parqueadero.appparqueadero.data.model.TipoVehiculo
import com.parqueadero.appparqueadero.data.model.UsuarioDTO
import com.parqueadero.appparqueadero.ui.auth.CupoMarca
import com.parqueadero.appparqueadero.ui.common.UiState
import com.parqueadero.appparqueadero.ui.components.ErrorState
import com.parqueadero.appparqueadero.ui.components.PlateChip
import com.parqueadero.appparqueadero.ui.components.PlateSize
import com.parqueadero.appparqueadero.ui.components.SectionHeader
import com.parqueadero.appparqueadero.ui.components.Skeleton
import com.parqueadero.appparqueadero.ui.components.StatusBadge
import com.parqueadero.appparqueadero.ui.components.StatusTipo
import com.parqueadero.appparqueadero.ui.theme.Radius
import com.parqueadero.appparqueadero.ui.theme.Spacing
import com.parqueadero.appparqueadero.ui.theme.tabular
import com.parqueadero.appparqueadero.utils.aLocalDateONull
import com.parqueadero.appparqueadero.utils.formatoCorto
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    onAbrirCamaras: () -> Unit,
    viewModel: InicioViewModel = viewModel(factory = InicioViewModel.Factory),
) {
    val cuenta by viewModel.cuenta.collectAsStateWithLifecycle()
    val puerta by viewModel.puerta.collectAsStateWithLifecycle()
    val refrescando by viewModel.refrescando.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(saludo(cuenta), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { CupoMarca(tamano = 32.dp, modifier = Modifier.padding(horizontal = Spacing.md)) },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refrescando,
            onRefresh = { viewModel.cargarCuenta(refresco = true) },
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.md),
            ) {
                when (val c = cuenta) {
                    UiState.Loading -> Skeleton(
                        Modifier
                            .fillMaxWidth()
                            .height(148.dp),
                        shape = RoundedCornerShape(Radius.lg),
                    )
                    is UiState.Error -> ErrorState(
                        mensaje = c.mensaje,
                        titulo = "No pudimos cargar tu cuenta",
                        onReintentar = { viewModel.cargarCuenta() },
                    )
                    UiState.Empty -> Unit
                    is UiState.Success -> TarjetaCuenta(c.data)
                }

                SectionHeader("Puerta")
                DoorControl(
                    estado = puerta,
                    onAbrir = viewModel::abrir,
                    onCierreVisual = {
                        viewModel.cierreVisual()
                        scope.launch { snackbar.showSnackbar("La puerta se cierra sola; no necesitas cerrarla.") }
                    },
                    onReintentar = viewModel::abrir,
                )

                SectionHeader("Cámaras")
                AccesoCamaras(onAbrirCamaras)
            }
        }
    }
}

private fun saludo(cuenta: UiState<UsuarioDTO>): String {
    val nombre = (cuenta as? UiState.Success)?.data?.nombre?.substringBefore(' ')?.takeIf { it.isNotBlank() }
    return if (nombre != null) "Hola, $nombre" else "Inicio"
}

/** Estado de la cuenta para la insignia: vigente, por vencer (5 días o menos), en gracia, vencida o suspendida. */
internal fun estadoCuenta(usuario: UsuarioDTO, hoy: LocalDate = LocalDate.now()): Pair<StatusTipo, String> {
    val dias = usuario.vigenteHasta?.aLocalDateONull()?.let { ChronoUnit.DAYS.between(hoy, it) }
    return when (usuario.estado) {
        EstadoUsuario.SUSPENDIDO -> StatusTipo.NEUTRO to "Suspendida"
        EstadoUsuario.VENCIDO -> StatusTipo.PELIGRO to "Vencida"
        EstadoUsuario.ACTIVO -> when {
            dias == null -> StatusTipo.EXITO to "Al día"
            dias < 0 -> StatusTipo.ADVERTENCIA to "En periodo de gracia"
            dias <= 5 -> StatusTipo.ADVERTENCIA to "Por vencer"
            else -> StatusTipo.EXITO to "Al día"
        }
        null -> StatusTipo.NEUTRO to "Sin estado"
    }
}

@Composable
private fun TarjetaCuenta(usuario: UsuarioDTO) {
    val vigente = usuario.vigenteHasta?.aLocalDateONull()
    val dias = vigente?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }
    val (tipo, etiqueta) = estadoCuenta(usuario)

    Card(
        shape = RoundedCornerShape(Radius.lg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tu mensualidad", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                StatusBadge(tipo, etiqueta)
            }
            when {
                usuario.estado == EstadoUsuario.SUSPENDIDO -> Text(
                    usuario.suspendidoMotivo?.let { "Motivo: $it" } ?: "Comunícate con la administración.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                dias != null && dias >= 0 -> Row(verticalAlignment = Alignment.Bottom) {
                    Text("$dias", style = MaterialTheme.typography.displaySmall.tabular())
                    Text(
                        if (dias == 1L) " día restante" else " días restantes",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = Spacing.xs),
                    )
                }
                usuario.estado == EstadoUsuario.VENCIDO -> Text(
                    "Sube tu comprobante en Pagos para reactivar el acceso.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (vigente != null) {
                Text(
                    "Vigente hasta el ${vigente.formatoCorto()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                usuario.vehiculo?.let { PlateChip(it.placa, size = PlateSize.Medium) }
                Column {
                    val cupo = usuario.cupo
                    Text(
                        if (cupo != null) "Cupo ${cupo.codigo}" else "Sin cupo asignado",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    val vehiculo = usuario.vehiculo
                    if (vehiculo != null) {
                        Text(
                            listOfNotNull(
                                if (vehiculo.tipoVehiculo == TipoVehiculo.MOTO) "Moto" else "Carro",
                                vehiculo.marca,
                                vehiculo.color,
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccesoCamaras(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(Radius.md),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.padding(Spacing.md),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(Radius.md),
                modifier = Modifier.size(40.dp),
            ) {
                Icon(Icons.Rounded.Videocam, contentDescription = null, modifier = Modifier.padding(Spacing.xs))
            }
            Column(Modifier.weight(1f)) {
                Text("Ver cámaras", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Vista simulada de las entradas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
        }
    }
}
