package com.parqueadero.appparqueadero.ui.actividad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.parqueadero.appparqueadero.container
import com.parqueadero.appparqueadero.data.model.EventoAccesoDTO
import com.parqueadero.appparqueadero.data.model.ResultadoAcceso
import com.parqueadero.appparqueadero.data.model.ResumenUsuarioDTO
import com.parqueadero.appparqueadero.data.model.TipoEvento
import com.parqueadero.appparqueadero.data.network.Resultado
import com.parqueadero.appparqueadero.data.repository.AccesoRepository
import com.parqueadero.appparqueadero.data.repository.UsuarioRepository
import com.parqueadero.appparqueadero.ui.common.UiState
import com.parqueadero.appparqueadero.ui.common.aUiState
import com.parqueadero.appparqueadero.utils.aOffsetDateTimeONull
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Visitas (entradas permitidas) de la semana que empieza el lunes [inicio]. */
data class VisitasSemana(val inicio: LocalDate, val visitas: Int)

data class Actividad(
    val semanas: List<VisitasSemana>,
    /** Del más reciente al más antiguo. */
    val eventos: List<EventoAccesoDTO>,
)

data class ActividadUiState(
    val resumen: UiState<ResumenUsuarioDTO> = UiState.Loading,
    val actividad: UiState<Actividad> = UiState.Loading,
    val refrescando: Boolean = false,
    /** Cuántos eventos se muestran en la lista (se amplía con "Ver más"). */
    val eventosVisibles: Int = PAGINA_EVENTOS,
)

const val PAGINA_EVENTOS = 20
const val SEMANAS_GRAFICO = 8

class ActividadViewModel(
    private val usuarios: UsuarioRepository,
    private val accesos: AccesoRepository,
    private val hoy: () -> LocalDate = { LocalDate.now() },
    private val zona: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val _estado = MutableStateFlow(ActividadUiState())
    val estado: StateFlow<ActividadUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar(refresco: Boolean = false) {
        _estado.update {
            if (refresco) it.copy(refrescando = true) else it.copy(resumen = UiState.Loading, actividad = UiState.Loading)
        }
        viewModelScope.launch {
            val resumen = async { usuarios.resumen().aUiState() }
            val actividad = async {
                when (val r = accesos.misAccesos()) {
                    is Resultado.Ok -> Resultado.Ok(armar(r.datos, hoy(), zona))
                    is Resultado.Falla -> r
                }.aUiState { it.eventos.isEmpty() }
            }
            _estado.update {
                it.copy(
                    resumen = resumen.await(),
                    actividad = actividad.await(),
                    refrescando = false,
                    eventosVisibles = PAGINA_EVENTOS,
                )
            }
        }
    }

    fun verMas() = _estado.update { it.copy(eventosVisibles = it.eventosVisibles + PAGINA_EVENTOS) }

    companion object {
        fun armar(eventos: List<EventoAccesoDTO>, hoy: LocalDate, zona: ZoneId): Actividad {
            val ordenados = eventos.sortedByDescending { it.ocurridoEn?.aOffsetDateTimeONull()?.toInstant() }
            return Actividad(visitasPorSemana(eventos, hoy, zona), ordenados)
        }

        /** Entradas permitidas por semana (lunes a domingo), las últimas [semanas] incluida la actual. */
        fun visitasPorSemana(
            eventos: List<EventoAccesoDTO>,
            hoy: LocalDate,
            zona: ZoneId,
            semanas: Int = SEMANAS_GRAFICO,
        ): List<VisitasSemana> {
            val lunesActual = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val inicios = (semanas - 1 downTo 0).map { lunesActual.minusWeeks(it.toLong()) }
            val conteo = eventos
                .asSequence()
                .filter { it.tipo == TipoEvento.ENTRADA && it.resultado == ResultadoAcceso.PERMITIDO }
                .mapNotNull { it.ocurridoEn?.aOffsetDateTimeONull()?.atZoneSameInstant(zona)?.toLocalDate() }
                .map { it.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
                .groupingBy { it }
                .eachCount()
            return inicios.map { VisitasSemana(it, conteo[it] ?: 0) }
        }

        /** "2 h 15 min", "45 min" o null. */
        fun duracionLegible(minutos: Int?): String? {
            if (minutos == null || minutos < 0) return null
            val h = minutos / 60
            val m = minutos % 60
            return when {
                h == 0 -> "$m min"
                m == 0 -> "$h h"
                else -> "$h h $m min"
            }
        }

        val Factory = viewModelFactory {
            initializer { ActividadViewModel(container.usuarioRepository, container.accesoRepository) }
        }
    }
}
