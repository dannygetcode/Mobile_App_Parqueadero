package com.parqueadero.appparqueadero.ui.inicio

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.parqueadero.appparqueadero.data.model.EventoAccesoDTO
import com.parqueadero.appparqueadero.data.model.TipoEvento
import com.parqueadero.appparqueadero.ui.components.PlateChip
import com.parqueadero.appparqueadero.ui.components.PlateSize
import com.parqueadero.appparqueadero.ui.components.StatusBadge
import com.parqueadero.appparqueadero.ui.components.StatusTipo
import com.parqueadero.appparqueadero.ui.components.rememberReducedMotion
import com.parqueadero.appparqueadero.ui.theme.Radius
import com.parqueadero.appparqueadero.ui.theme.Spacing
import com.parqueadero.appparqueadero.ui.theme.cupo
import com.parqueadero.appparqueadero.ui.theme.tabular
import com.parqueadero.appparqueadero.utils.aOffsetDateTimeONull
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor

private val ANCHO_MARCO = 240.dp
private val ALTO_CAJON = 32.dp
private val ALTO_HUECO = 224.dp
private const val LAMAS = 8
private val ALTO_LAMA = ALTO_HUECO / LAMAS

/**
 * Puerta tipo persiana (simulada). Se arrastra hacia arriba para abrir; al pasar el 50 % se confirma.
 * El usuario no puede cerrarla: arrastrar hacia abajo con la puerta abierta solo adelanta el cierre visual.
 */
@Composable
fun DoorControl(
    estado: PuertaUiState,
    onAbrir: () -> Unit,
    onCierreVisual: () -> Unit,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducir = rememberReducedMotion()
    val haptica = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val recorridoPx = with(LocalDensity.current) { ALTO_HUECO.toPx() }
    val umbralAbajoPx = with(LocalDensity.current) { 48.dp.toPx() }

    val progreso = remember { Animatable(0f) }
    val sacudida = remember { Animatable(0f) }
    var arrastrando by remember { mutableStateOf(false) }
    var lamasRecogidas by remember { mutableIntStateOf(0) }
    var arrastreAbajo by remember { mutableFloatStateOf(0f) }

    val puedeAbrir = estado is PuertaUiState.Cerrada || estado is PuertaUiState.Denegada || estado is PuertaUiState.Fallo

    // Reacciona a cada cambio de estado del ViewModel.
    LaunchedEffect(estado) {
        when (estado) {
            PuertaUiState.Enviando, is PuertaUiState.Abierta ->
                if (reducir) progreso.snapTo(1f) else progreso.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 400f))
            is PuertaUiState.Cerrada ->
                if (reducir) progreso.snapTo(0f) else progreso.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
            is PuertaUiState.Denegada -> {
                haptica.performHapticFeedback(HapticFeedbackType.LongPress)
                if (reducir) {
                    progreso.snapTo(0f)
                } else {
                    launch { progreso.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 800f)) }
                    sacudida.snapTo(0f)
                    sacudida.animateTo(0f, keyframes {
                        durationMillis = 360
                        -8f at 60
                        8f at 120
                        -6f at 180
                        6f at 240
                        -3f at 300
                    })
                }
            }
            is PuertaUiState.Fallo ->
                if (reducir) progreso.snapTo(0f) else progreso.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 800f))
        }
        lamasRecogidas = floor(progreso.value * LAMAS).toInt()
    }

    val arrastre = rememberDraggableState { delta ->
        if (puedeAbrir) {
            val nuevo = (progreso.value - delta / recorridoPx).coerceIn(0f, 1f)
            val lamas = floor(nuevo * LAMAS).toInt()
            if (lamas != lamasRecogidas) {
                if (lamas > lamasRecogidas) haptica.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (progreso.value < 0.5f && nuevo >= 0.5f) haptica.performHapticFeedback(HapticFeedbackType.LongPress)
                lamasRecogidas = lamas
            }
            scope.launch { progreso.snapTo(nuevo) }
        } else if (estado is PuertaUiState.Abierta && delta > 0) {
            arrastreAbajo += delta
        }
    }

    val textoEstado = when (estado) {
        is PuertaUiState.Cerrada -> if (arrastrando && progreso.value >= 0.5f) "Suelta para abrir" else "Desliza hacia arriba para abrir"
        PuertaUiState.Enviando -> "Abriendo..."
        is PuertaUiState.Abierta -> "Puerta abierta"
        is PuertaUiState.Denegada -> "No pudimos abrir"
        is PuertaUiState.Fallo -> "No pudimos abrir"
    }
    val descripcionEstado = when (estado) {
        is PuertaUiState.Cerrada, is PuertaUiState.Denegada, is PuertaUiState.Fallo -> "Cerrada"
        PuertaUiState.Enviando -> "Abriendo"
        is PuertaUiState.Abierta -> "Abierta, se cierra sola en unos segundos"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(Modifier.widthIn(max = 280.dp).fillMaxWidth()) {
            EtiquetaSimulacion(Modifier.align(Alignment.TopEnd))
        }

        // Marco de la persiana
        Box(
            modifier = Modifier
                .offset { IntOffset(sacudida.value.dp.roundToPx(), 0) }
                .size(ANCHO_MARCO, ALTO_CAJON + ALTO_HUECO)
                .clip(RoundedCornerShape(Radius.md))
                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(Radius.md))
                .draggable(
                    state = arrastre,
                    orientation = Orientation.Vertical,
                    enabled = puedeAbrir || estado is PuertaUiState.Abierta,
                    onDragStarted = {
                        arrastrando = true
                        arrastreAbajo = 0f
                    },
                    onDragStopped = {
                        arrastrando = false
                        when {
                            puedeAbrir && progreso.value >= 0.5f -> onAbrir()
                            puedeAbrir -> if (reducir) {
                                progreso.snapTo(0f)
                            } else {
                                progreso.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 800f))
                            }
                            estado is PuertaUiState.Abierta && arrastreAbajo > umbralAbajoPx -> onCierreVisual()
                        }
                        lamasRecogidas = floor(progreso.value * LAMAS).toInt()
                    },
                )
                .clearAndSetSemantics {
                    role = Role.Button
                    contentDescription = "Puerta del parqueadero"
                    stateDescription = descripcionEstado
                    if (puedeAbrir) {
                        onClick(label = "abrir") {
                            onAbrir()
                            true
                        }
                    }
                },
        ) {
            // Lo que se ve detrás de la persiana
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(top = ALTO_CAJON)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                Icon(
                    Icons.Rounded.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(72.dp),
                )
            }
            Persiana(progreso = { progreso.value }, modifier = Modifier.fillMaxSize())
            if (estado == PuertaUiState.Enviando) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .padding(top = ALTO_CAJON - 4.dp)
                        .fillMaxWidth()
                        .height(4.dp),
                )
            }
        }

        // Estado y resultado (anunciados por TalkBack)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Text(textoEstado, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            AnimatedContent(
                targetState = estado,
                contentKey = { it::class },
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                label = "resultadoPuerta",
            ) { e ->
                when (e) {
                    is PuertaUiState.Abierta -> PuertaAbierta(e, reducir)
                    is PuertaUiState.Denegada -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        StatusBadge(TextosPuerta.tipoMotivo(e.motivo), TextosPuerta.etiquetaMotivo(e.motivo))
                        Text(
                            TextosPuerta.motivo(e.motivo),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                    is PuertaUiState.Fallo -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        Text(e.mensaje, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        if (e.reintentable) OutlinedButton(onClick = onReintentar) { Text("Reintentar") }
                    }
                    is PuertaUiState.Cerrada -> e.aviso?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    PuertaUiState.Enviando -> Unit
                }
            }
        }

        FilledTonalButton(
            onClick = onAbrir,
            enabled = puedeAbrir,
            modifier = Modifier.height(48.dp),
        ) {
            Icon(Icons.Rounded.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Abrir puerta", modifier = Modifier.padding(start = Spacing.xs))
        }
    }
}

/** Dibuja cajón, lamas y manija. `progreso` 0 = cerrada, 1 = recogida en el cajón. */
@Composable
private fun Persiana(progreso: () -> Float, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    val placa = MaterialTheme.cupo
    Canvas(modifier) {
        val cajon = ALTO_CAJON.toPx()
        val hueco = ALTO_HUECO.toPx()
        val lama = ALTO_LAMA.toPx()
        val borde = (cajon + hueco * (1f - progreso())).coerceAtLeast(cajon)

        // Lamas, desde el borde inferior hacia el cajón
        for (i in 0 until LAMAS) {
            val abajo = borde - i * lama
            if (abajo <= cajon) break
            val arriba = (abajo - lama).coerceAtLeast(cajon)
            drawRect(colores.surfaceContainerHighest, Offset(0f, arriba), Size(size.width, abajo - arriba))
            if (abajo - lama >= cajon) {
                drawLine(colores.outlineVariant, Offset(0f, arriba), Offset(size.width, arriba), strokeWidth = 1.dp.toPx())
            }
            drawLine(
                colores.outline.copy(alpha = 0.35f),
                Offset(0f, abajo - 1.dp.toPx()),
                Offset(size.width, abajo - 1.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
            )
        }

        // Manija sobre la lama inferior
        val altoManija = 8.dp.toPx()
        val anchoManija = 56.dp.toPx()
        val yManija = borde - lama / 2 - altoManija / 2
        if (yManija > cajon) {
            val origen = Offset((size.width - anchoManija) / 2, yManija)
            val radio = CornerRadius(4.dp.toPx())
            drawRoundRect(placa.plate, origen, Size(anchoManija, altoManija), radio)
            drawRoundRect(placa.plateBorder, origen, Size(anchoManija, altoManija), radio, style = Stroke(1.dp.toPx()))
        }

        // Cajón superior (las lamas se recogen debajo de él)
        drawRect(colores.surfaceContainerHigh, Offset.Zero, Size(size.width, cajon))
        drawLine(colores.outline, Offset(0f, cajon), Offset(size.width, cajon), strokeWidth = 1.dp.toPx())
    }
}

@Composable
private fun PuertaAbierta(estado: PuertaUiState.Abierta, reducir: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.padding(Spacing.md),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                EventoResumen(estado.evento)
                estado.aviso?.let {
                    StatusBadge(StatusTipo.ADVERTENCIA, "Pago pendiente")
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
            CuentaRegresiva(clave = estado.intento, duracionMs = estado.duracionMs, reducir = reducir)
        }
    }
}

@Composable
private fun EventoResumen(evento: EventoAccesoDTO?) {
    val salida = evento?.tipo == TipoEvento.SALIDA
    StatusBadge(if (salida) StatusTipo.INFO else StatusTipo.EXITO, if (salida) "SALIDA" else "ENTRADA")
    val placa = evento?.vehiculo?.placa ?: evento?.placaLeida
    if (placa != null) PlateChip(placa, size = PlateSize.Small)
    val hora = evento?.ocurridoEn?.aOffsetDateTimeONull()
        ?.atZoneSameInstant(ZoneId.systemDefault())
        ?.format(DateTimeFormatter.ofPattern("H:mm"))
    if (hora != null) {
        Text(
            "${if (salida) "Salida" else "Entrada"} registrada a las $hora",
            style = MaterialTheme.typography.bodyMedium.tabular(),
        )
    }
}

/** Anillo de cuenta regresiva. Usa el reloj de cuadros (no animaciones) para no depender de la escala del sistema. */
@Composable
private fun CuentaRegresiva(clave: Int, duracionMs: Long, reducir: Boolean) {
    var restante by remember(clave) { mutableLongStateOf(duracionMs) }
    LaunchedEffect(clave) {
        val inicio = withFrameMillis { it }
        while (restante > 0) {
            withFrameMillis { t -> restante = (duracionMs - (t - inicio)).coerceAtLeast(0) }
        }
    }
    val segundos = ceil(restante / 1000.0).toInt()
    if (reducir) {
        Text("Se cierra en $segundos s", style = MaterialTheme.typography.titleSmall.tabular())
        return
    }
    val pista = MaterialTheme.colorScheme.outlineVariant
    val color = MaterialTheme.colorScheme.primary
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(64.dp)
            .clearAndSetSemantics { },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val trazo = 6.dp.toPx()
            val inset = trazo / 2
            val area = Size(size.width - trazo, size.height - trazo)
            drawArc(pista, 0f, 360f, false, Offset(inset, inset), area, style = Stroke(trazo))
            drawArc(
                color,
                -90f,
                360f * (restante.toFloat() / duracionMs),
                false,
                Offset(inset, inset),
                area,
                style = Stroke(trazo, cap = StrokeCap.Round),
            )
        }
        Text("$segundos", style = MaterialTheme.typography.titleMedium.tabular())
    }
}

@Composable
private fun EtiquetaSimulacion(modifier: Modifier = Modifier) {
    Text(
        "Simulación",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radius.sm))
            .padding(horizontal = Spacing.xs, vertical = 2.dp),
    )
}
