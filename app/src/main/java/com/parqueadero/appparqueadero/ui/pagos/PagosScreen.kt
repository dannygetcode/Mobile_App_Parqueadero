package com.parqueadero.appparqueadero.ui.pagos

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.parqueadero.appparqueadero.data.model.EstadoPago
import com.parqueadero.appparqueadero.data.model.PagoDTO
import com.parqueadero.appparqueadero.data.model.ProximoPeriodoDTO
import com.parqueadero.appparqueadero.ui.auth.AvisoEnLinea
import com.parqueadero.appparqueadero.ui.auth.BotonPrincipal
import com.parqueadero.appparqueadero.ui.auth.TipoAviso
import com.parqueadero.appparqueadero.ui.common.UiState
import com.parqueadero.appparqueadero.ui.components.EmptyState
import com.parqueadero.appparqueadero.ui.components.ErrorState
import com.parqueadero.appparqueadero.ui.components.SectionHeader
import com.parqueadero.appparqueadero.ui.components.Skeleton
import com.parqueadero.appparqueadero.ui.components.StatusBadge
import com.parqueadero.appparqueadero.ui.components.StatusTipo
import com.parqueadero.appparqueadero.ui.theme.Radius
import com.parqueadero.appparqueadero.ui.theme.Spacing
import com.parqueadero.appparqueadero.ui.theme.tabular
import com.parqueadero.appparqueadero.utils.aLocalDateONull
import com.parqueadero.appparqueadero.utils.comoPesos
import com.parqueadero.appparqueadero.utils.formatoCorto
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagosScreen(viewModel: PagosViewModel = viewModel(factory = PagosViewModel.Factory)) {
    val s by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var uriCamara by rememberSaveable { mutableStateOf<String?>(null) }

    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.elegirComprobante(it.toString()) }
    }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = uriCamara
        if (ok && uri != null) viewModel.elegirComprobante(uri)
    }

    LaunchedEffect(s.mensaje) {
        s.mensaje?.let {
            snackbar.showSnackbar(it)
            viewModel.mensajeMostrado()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Text("Pagos") }) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = s.refrescando,
            onRefresh = { viewModel.cargar(refresco = true) },
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
                ProximoPeriodo(s.periodo, onReintentar = { viewModel.cargar() })

                SectionHeader("Subir comprobante")
                when {
                    s.hayPendiente -> AvisoEnLinea(
                        "Tienes un comprobante en revisión. Podrás enviar otro cuando la administración lo apruebe o lo rechace.",
                        TipoAviso.INFO,
                    )
                    else -> SubirComprobante(
                        s = s,
                        onCamara = {
                            val uri = nuevaUriCamara(contexto)
                            uriCamara = uri.toString()
                            try {
                                camara.launch(uri)
                            } catch (_: ActivityNotFoundException) {
                                galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        },
                        onGaleria = {
                            galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onQuitar = viewModel::quitarComprobante,
                        onSubir = viewModel::subir,
                    )
                }

                SectionHeader("Historial")
                Historial(s.historial, onReintentar = { viewModel.cargar() })
            }
        }
    }
}

@Composable
private fun ProximoPeriodo(periodo: UiState<ProximoPeriodoDTO>, onReintentar: () -> Unit) {
    when (periodo) {
        UiState.Loading -> Skeleton(Modifier.fillMaxWidth().height(112.dp), shape = RoundedCornerShape(Radius.lg))
        is UiState.Error -> ErrorState(
            mensaje = periodo.mensaje,
            titulo = "No pudimos calcular tu próximo pago",
            onReintentar = onReintentar,
        )
        UiState.Empty -> Unit
        is UiState.Success -> Card(
            shape = RoundedCornerShape(Radius.lg),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text("Próximo periodo", style = MaterialTheme.typography.titleSmall)
                Text(
                    rangoPeriodo(periodo.data.inicio, periodo.data.fin),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    periodo.data.montoEsperado.comoPesos(),
                    style = MaterialTheme.typography.headlineMedium.tabular(),
                )
            }
        }
    }
}

@Composable
private fun SubirComprobante(
    s: PagosUiState,
    onCamara: () -> Unit,
    onGaleria: () -> Unit,
    onQuitar: () -> Unit,
    onSubir: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        val comprobante = s.comprobante
        if (comprobante == null) {
            Text(
                "Toma una foto clara del comprobante o elígela de tu galería. JPEG o PNG, hasta 5 MB.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedButton(onClick = onCamara, enabled = !s.leyendo, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Tomar foto", modifier = Modifier.padding(start = Spacing.xs))
                }
                OutlinedButton(onClick = onGaleria, enabled = !s.leyendo, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Galería", modifier = Modifier.padding(start = Spacing.xs))
                }
            }
            if (s.leyendo) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Revisando la imagen...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(Radius.md)),
            ) {
                AsyncImage(
                    model = Uri.parse(comprobante.uri),
                    contentDescription = "Vista previa del comprobante",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${if (comprobante.mime.endsWith("png")) "PNG" else "JPEG"} · ${tamanoLegible(comprobante.tamanoBytes)}",
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onQuitar, enabled = !s.subiendo) { Text("Cambiar imagen") }
            }
            BotonPrincipal(texto = "Enviar comprobante", onClick = onSubir, cargando = s.subiendo)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Icon(
                    Icons.Rounded.HourglassTop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "Al enviarlo queda pendiente de aprobación por la administración.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        s.errorComprobante?.let {
            Box(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) { AvisoEnLinea(it, TipoAviso.ERROR) }
        }
    }
}

@Composable
private fun Historial(historial: UiState<List<GrupoPagos>>, onReintentar: () -> Unit) {
    when (historial) {
        UiState.Loading -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            repeat(3) { Skeleton(Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(Radius.md)) }
        }
        UiState.Empty -> EmptyState(
            titulo = "Aún no hay pagos",
            mensaje = "Cuando envíes tu primer comprobante lo verás aquí con su estado.",
            icono = Icons.AutoMirrored.Outlined.ReceiptLong,
        )
        is UiState.Error -> ErrorState(mensaje = historial.mensaje, onReintentar = onReintentar)
        is UiState.Success -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            historial.data.forEach { grupo ->
                Text(
                    if (grupo.anio > 0) grupo.anio.toString() else "Sin fecha",
                    style = MaterialTheme.typography.titleMedium.tabular(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
                Card(
                    shape = RoundedCornerShape(Radius.md),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    grupo.pagos.forEachIndexed { i, pago ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        FilaPago(pago)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaPago(pago: PagoDTO) {
    Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                rangoPeriodo(pago.periodoInicio, pago.periodoFin),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            val (tipo, texto) = when (pago.estado) {
                EstadoPago.APROBADO -> StatusTipo.EXITO to "Aprobado"
                EstadoPago.PENDIENTE -> StatusTipo.ADVERTENCIA to "Pendiente"
                EstadoPago.RECHAZADO -> StatusTipo.PELIGRO to "Rechazado"
                null -> StatusTipo.NEUTRO to "Sin estado"
            }
            StatusBadge(tipo, texto)
        }
        val monto = pago.montoConfirmado ?: pago.montoEsperado
        if (monto != null) {
            Text(
                monto.comoPesos() + if (pago.montoConfirmado == null) " (esperado)" else "",
                style = MaterialTheme.typography.bodyMedium.tabular(),
            )
        }
        if (pago.estado == EstadoPago.RECHAZADO && !pago.motivoRechazo.isNullOrBlank()) {
            Text(
                "Motivo: ${pago.motivoRechazo}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private fun rangoPeriodo(inicio: String?, fin: String?): String {
    val i = inicio?.aLocalDateONull()
    val f = fin?.aLocalDateONull()
    return when {
        i != null && f != null -> "${i.formatoCorto()} al ${f.formatoCorto()}"
        i != null -> "Desde el ${i.formatoCorto()}"
        else -> "Periodo por definir"
    }
}

private fun tamanoLegible(bytes: Int): String =
    if (bytes >= 1024 * 1024) String.format(Locale("es", "CO"), "%.1f MB", bytes / (1024.0 * 1024.0))
    else "${bytes / 1024} KB"

/** Archivo temporal en caché para la foto de la cámara, compartido con FileProvider. */
private fun nuevaUriCamara(contexto: Context): Uri {
    val carpeta = File(contexto.cacheDir, "comprobantes").apply { mkdirs() }
    carpeta.listFiles()?.forEach { it.delete() }
    val archivo = File.createTempFile("comprobante_", ".jpg", carpeta)
    return FileProvider.getUriForFile(contexto, "${contexto.packageName}.archivos", archivo)
}
