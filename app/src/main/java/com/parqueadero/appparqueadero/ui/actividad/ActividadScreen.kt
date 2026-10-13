package com.parqueadero.appparqueadero.ui.actividad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parqueadero.appparqueadero.data.model.EventoAccesoDTO
import com.parqueadero.appparqueadero.data.model.OrigenAcceso
import com.parqueadero.appparqueadero.data.model.ResultadoAcceso
import com.parqueadero.appparqueadero.data.model.ResumenUsuarioDTO
import com.parqueadero.appparqueadero.data.model.TipoEvento
import com.parqueadero.appparqueadero.ui.common.UiState
import com.parqueadero.appparqueadero.ui.components.EmptyState
import com.parqueadero.appparqueadero.ui.components.ErrorState
import com.parqueadero.appparqueadero.ui.components.KpiCard
import com.parqueadero.appparqueadero.ui.components.SectionHeader
import com.parqueadero.appparqueadero.ui.components.Skeleton
import com.parqueadero.appparqueadero.ui.components.StatusBadge
import com.parqueadero.appparqueadero.ui.components.StatusTipo
import com.parqueadero.appparqueadero.ui.components.rememberReducedMotion
import com.parqueadero.appparqueadero.ui.inicio.TextosPuerta
import com.parqueadero.appparqueadero.ui.theme.Radius
import com.parqueadero.appparqueadero.ui.theme.Spacing
import com.parqueadero.appparqueadero.ui.theme.tabular
import com.parqueadero.appparqueadero.utils.LOCALE_CO
import com.parqueadero.appparqueadero.utils.aOffsetDateTimeONull
import com.parqueadero.appparqueadero.utils.comoPesos
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import java.text.DecimalFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val formatoSemana = DateTimeFormatter.ofPattern("d MMM", LOCALE_CO)
private val formatoEvento = DateTimeFormatter.ofPattern("EEE d MMM, H:mm", LOCALE_CO)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActividadScreen(viewModel: ActividadViewModel = viewModel(factory = ActividadViewModel.Factory)) {
    val s by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text("Actividad") }) },
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
                SectionHeader("Tu resumen")
                Resumen(s.resumen, onReintentar = { viewModel.cargar() })

                SectionHeader("Visitas por semana")
                when (val a = s.actividad) {
                    UiState.Loading -> Skeleton(Modifier.fillMaxWidth().height(220.dp), shape = RoundedCornerShape(Radius.lg))
                    is UiState.Error -> ErrorState(mensaje = a.mensaje, onReintentar = { viewModel.cargar() })
                    UiState.Empty -> EmptyState(
                        titulo = "Aún no hay movimientos",
                        mensaje = "Cuando entres o salgas del parqueadero verás aquí tus visitas.",
                        icono = Icons.Outlined.DirectionsCar,
                    )
                    is UiState.Success -> {
                        GraficoVisitas(a.data.semanas)
                        SectionHeader("Entradas y salidas")
                        ListaEventos(a.data.eventos, s.eventosVisibles, onVerMas = viewModel::verMas)
                    }
                }
            }
        }
    }
}

@Composable
private fun Resumen(resumen: UiState<ResumenUsuarioDTO>, onReintentar: () -> Unit) {
    when (resumen) {
        UiState.Loading -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            repeat(3) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    repeat(2) { Skeleton(Modifier.weight(1f).height(104.dp), shape = RoundedCornerShape(Radius.md)) }
                }
            }
        }
        is UiState.Error -> ErrorState(mensaje = resumen.mensaje, titulo = "No pudimos cargar tu resumen", onReintentar = onReintentar)
        UiState.Empty -> EmptyState(
            titulo = "Sin resumen todavía",
            mensaje = "Cuando tengas pagos y visitas registrados verás aquí tus indicadores.",
            icono = Icons.Rounded.EventAvailable,
        )
        is UiState.Success -> {
            val r = resumen.data
            // Los montos en pesos van a ancho completo: en 360 dp no caben a tamaño KPI en media tarjeta.
            val filas = listOf(
                listOf(
                    Kpi("Días restantes", r.diasRestantes?.toString(), "de tu mensualidad", Icons.Rounded.EventAvailable),
                    Kpi("Visitas del mes", r.visitasMes?.toString(), "entradas este mes", Icons.Rounded.CalendarMonth),
                ),
                listOf(Kpi("Pagado este año", r.totalPagadoAnio?.comoPesos(), "en lo que va del año", Icons.Rounded.Payments)),
                listOf(Kpi("Promedio mensual", r.promedioMensualPagado?.comoPesos(), "pagado por mes", Icons.AutoMirrored.Rounded.TrendingUp)),
                listOf(
                    Kpi("Permanencia", ActividadViewModel.duracionLegible(r.permanenciaPromedioMin), "promedio por visita", Icons.Rounded.Timer),
                    Kpi("Llegada habitual", r.horaHabitualLlegada, "hora más frecuente", Icons.Rounded.Schedule),
                ),
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                filas.forEach { fila ->
                    // Altura intrínseca: las dos tarjetas de una fila quedan iguales aunque una tenga más texto.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.height(IntrinsicSize.Min),
                    ) {
                        fila.forEach { k ->
                            KpiCard(
                                titulo = k.titulo,
                                valor = k.valor,
                                apoyo = k.apoyo,
                                icono = k.icono,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Kpi(
    val titulo: String,
    val valor: String?,
    val apoyo: String?,
    val icono: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
private fun GraficoVisitas(semanas: List<VisitasSemana>) {
    val productor = remember { CartesianChartModelProducer() }
    LaunchedEffect(semanas) {
        productor.runTransaction { columnSeries { series(semanas.map { it.visitas }) } }
    }
    val etiquetas = remember(semanas) { semanas.map { it.inicio.format(formatoSemana) } }
    val total = semanas.sumOf { it.visitas }
    val reducir = rememberReducedMotion()
    val colorBarra = MaterialTheme.colorScheme.primary
    val colorTexto = MaterialTheme.colorScheme.onSurfaceVariant
    val descripcion = "Gráfico de visitas por semana. " + semanas.joinToString(". ") {
        "Semana del ${it.inicio.format(formatoSemana)}: ${it.visitas} ${if (it.visitas == 1) "visita" else "visitas"}"
    }

    Card(
        shape = RoundedCornerShape(Radius.lg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                "$total ${if (total == 1) "visita" else "visitas"} en las últimas ${semanas.size} semanas",
                style = MaterialTheme.typography.titleSmall.tabular(),
            )
            ProvideVicoTheme(rememberM3VicoTheme()) {
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberColumnCartesianLayer(
                            columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                                rememberLineComponent(
                                    fill = fill(colorBarra),
                                    thickness = 18.dp,
                                    shape = CorneredShape.rounded(allDp = 4f),
                                )
                            ),
                        ),
                        startAxis = VerticalAxis.rememberStart(
                            label = rememberAxisLabelComponent(color = colorTexto),
                            valueFormatter = CartesianValueFormatter.decimal(DecimalFormat("#")),
                            itemPlacer = remember { VerticalAxis.ItemPlacer.step({ 1.0 }) },
                        ),
                        bottomAxis = HorizontalAxis.rememberBottom(
                            label = rememberAxisLabelComponent(color = colorTexto),
                            valueFormatter = remember(etiquetas) {
                                CartesianValueFormatter { _, x, _ -> etiquetas.getOrElse(x.toInt()) { "" } }
                            },
                            guideline = null,
                        ),
                    ),
                    modelProducer = productor,
                    animateIn = !reducir,
                    animationSpec = if (reducir) null else androidx.compose.animation.core.tween(400),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clearAndSetSemantics { contentDescription = descripcion },
                )
            }
        }
    }
}

@Composable
private fun ListaEventos(eventos: List<EventoAccesoDTO>, visibles: Int, onVerMas: () -> Unit) {
    Card(
        shape = RoundedCornerShape(Radius.lg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        eventos.take(visibles).forEachIndexed { i, evento ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            FilaEvento(evento)
        }
        if (eventos.size > visibles) {
            TextButton(onClick = onVerMas, modifier = Modifier.fillMaxWidth()) {
                Text("Ver más (${eventos.size - visibles} restantes)")
            }
        }
    }
}

@Composable
private fun FilaEvento(evento: EventoAccesoDTO) {
    val salida = evento.tipo == TipoEvento.SALIDA
    val denegado = evento.resultado == ResultadoAcceso.DENEGADO
    val fecha = evento.ocurridoEn?.aOffsetDateTimeONull()
        ?.atZoneSameInstant(ZoneId.systemDefault())
        ?.format(formatoEvento)
        ?.replaceFirstChar { it.uppercase() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Icon(
            if (salida) Icons.AutoMirrored.Rounded.Logout else Icons.AutoMirrored.Rounded.Login,
            contentDescription = null,
            tint = if (denegado) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(if (salida) "Salida" else "Entrada", style = MaterialTheme.typography.titleSmall)
            Text(
                listOfNotNull(fecha, origen(evento.origen)).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (denegado) {
            StatusBadge(StatusTipo.PELIGRO, TextosPuerta.etiquetaMotivo(evento.motivo))
        }
    }
}

private fun origen(origen: OrigenAcceso?): String? = when (origen) {
    OrigenAcceso.CAMARA -> "Cámara"
    OrigenAcceso.APP_USUARIO -> "App"
    OrigenAcceso.MANUAL_ADMIN -> "Administración"
    null -> null
}
