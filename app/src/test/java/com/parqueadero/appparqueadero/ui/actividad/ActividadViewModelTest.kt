package com.parqueadero.appparqueadero.ui.actividad

import com.parqueadero.appparqueadero.data.model.EstadoUsuario
import com.parqueadero.appparqueadero.data.model.EventoAccesoDTO
import com.parqueadero.appparqueadero.data.model.ResultadoAcceso
import com.parqueadero.appparqueadero.data.model.ResumenUsuarioDTO
import com.parqueadero.appparqueadero.data.model.TipoEvento
import com.parqueadero.appparqueadero.data.network.AppError
import com.parqueadero.appparqueadero.data.network.ErrorParser
import com.parqueadero.appparqueadero.data.repository.AccesoRepository
import com.parqueadero.appparqueadero.data.repository.UsuarioRepository
import com.parqueadero.appparqueadero.data.session.SessionManager
import com.parqueadero.appparqueadero.support.FakeApiService
import com.parqueadero.appparqueadero.support.FakeBovedaPin
import com.parqueadero.appparqueadero.support.MainDispatcherRule
import com.parqueadero.appparqueadero.support.evento
import com.parqueadero.appparqueadero.support.gsonPrueba
import com.parqueadero.appparqueadero.support.httpError
import com.parqueadero.appparqueadero.support.problem
import com.parqueadero.appparqueadero.ui.common.UiState
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class ActividadViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val api = FakeApiService()
    private val parser = ErrorParser(gsonPrueba)
    private val bogota = ZoneId.of("America/Bogota")

    /** Miércoles: la semana actual empieza el lunes 2026-10-12. */
    private val hoy = LocalDate.of(2026, 10, 14)
    private val lunesActual = LocalDate.of(2026, 10, 12)

    private val resumen = ResumenUsuarioDTO(EstadoUsuario.ACTIVO, 20, 300_000, 100_000, 12, 135, "07:40")

    private fun vm() = ActividadViewModel(
        UsuarioRepository(api, parser, SessionManager { 0L }, FakeBovedaPin()),
        AccesoRepository(api, parser, gsonPrueba),
        hoy = { hoy },
        zona = bogota,
    )

    private fun ev(
        ocurridoEn: String?,
        tipo: TipoEvento? = TipoEvento.ENTRADA,
        resultado: ResultadoAcceso? = ResultadoAcceso.PERMITIDO,
        id: Long = 1,
    ): EventoAccesoDTO = evento(resultado = resultado).copy(id = id, tipo = tipo, ocurridoEn = ocurridoEn)

    // Carga

    @Test
    fun `carga resumen y accesos en paralelo`() {
        val r = CompletableDeferred<ResumenUsuarioDTO>()
        val a = CompletableDeferred<List<EventoAccesoDTO>>()
        var pidioResumen = false
        var pidioAccesos = false
        api.resumenResp = { pidioResumen = true; r.await() }
        api.misAccesosResp = { pidioAccesos = true; a.await() }
        val vm = vm()
        assertTrue(pidioResumen && pidioAccesos)
        assertEquals(UiState.Loading, vm.estado.value.resumen)
        assertEquals(UiState.Loading, vm.estado.value.actividad)
        a.complete(listOf(ev("2026-10-13T08:00:00-05:00")))
        r.complete(resumen)
        assertEquals(UiState.Success(resumen), vm.estado.value.resumen)
        val actividad = (vm.estado.value.actividad as UiState.Success).data
        assertEquals(1, actividad.eventos.size)
        assertEquals(1, actividad.semanas.last().visitas)
    }

    @Test
    fun `sin eventos la actividad queda Empty y el resumen Success`() {
        api.resumenResp = { resumen }
        api.misAccesosResp = { emptyList() }
        val vm = vm()
        assertEquals(UiState.Empty, vm.estado.value.actividad)
        assertEquals(UiState.Success(resumen), vm.estado.value.resumen)
    }

    @Test
    fun `error en accesos no tumba el resumen`() {
        api.resumenResp = { resumen }
        api.misAccesosResp = { throw IOException() }
        val vm = vm()
        val error = vm.estado.value.actividad as UiState.Error
        assertEquals(AppError.SinConexion, error.error)
        assertEquals(UiState.Success(resumen), vm.estado.value.resumen)
    }

    @Test
    fun `401 en resumen queda Error 401`() {
        api.resumenResp = { throw httpError(401, problem(401)) }
        api.misAccesosResp = { emptyList() }
        val vm = vm()
        assertEquals(401, ((vm.estado.value.resumen as UiState.Error).error as AppError.Http).status)
    }

    @Test
    fun `refresco mantiene los datos mientras carga y reinicia la paginacion`() {
        api.resumenResp = { resumen }
        api.misAccesosResp = { listOf(ev("2026-10-13T08:00:00-05:00")) }
        val vm = vm()
        vm.verMas()
        assertEquals(PAGINA_EVENTOS * 2, vm.estado.value.eventosVisibles)
        val pendiente = CompletableDeferred<ResumenUsuarioDTO>()
        api.resumenResp = { pendiente.await() }
        vm.cargar(refresco = true)
        assertTrue(vm.estado.value.refrescando)
        assertEquals(UiState.Success(resumen), vm.estado.value.resumen)
        pendiente.complete(resumen)
        assertFalse(vm.estado.value.refrescando)
        assertEquals(PAGINA_EVENTOS, vm.estado.value.eventosVisibles)
    }

    @Test
    fun `verMas amplia de a una pagina`() {
        api.resumenResp = { resumen }
        api.misAccesosResp = { emptyList() }
        val vm = vm()
        assertEquals(20, vm.estado.value.eventosVisibles)
        vm.verMas()
        vm.verMas()
        assertEquals(60, vm.estado.value.eventosVisibles)
    }

    // armar

    @Test
    fun `armar ordena del mas reciente al mas antiguo con fechas nulas o ilegibles al final`() {
        val eventos = listOf(
            ev("2026-10-10T08:00:00-05:00", id = 1),
            ev(null, id = 2),
            ev("2026-10-13T08:00:00-05:00", id = 3),
            ev("no-es-fecha", id = 4),
            ev("2026-10-12T08:00:00Z", id = 5),
        )
        val ids = ActividadViewModel.armar(eventos, hoy, bogota).eventos.map { it.id }
        assertEquals(listOf(3L, 5L, 1L), ids.take(3))
        assertEquals(setOf(2L, 4L), ids.drop(3).toSet())
    }

    // visitasPorSemana

    @Test
    fun `sin eventos devuelve 8 semanas en 0 de lunes a lunes terminando en la actual`() {
        val semanas = ActividadViewModel.visitasPorSemana(emptyList(), hoy, bogota)
        assertEquals(SEMANAS_GRAFICO, semanas.size)
        assertEquals(lunesActual, semanas.last().inicio)
        assertEquals(lunesActual.minusWeeks(7), semanas.first().inicio)
        assertTrue(semanas.all { it.visitas == 0 && it.inicio.dayOfWeek == java.time.DayOfWeek.MONDAY })
    }

    @Test
    fun `cuenta solo entradas permitidas`() {
        val t = "2026-10-13T08:00:00-05:00"
        val eventos = listOf(
            ev(t),
            ev(t, id = 2),
            ev(t, tipo = TipoEvento.SALIDA),
            ev(t, resultado = ResultadoAcceso.DENEGADO),
            ev(t, tipo = null),
            ev(t, resultado = null),
            ev(null),
            ev("no-es-fecha"),
        )
        val semanas = ActividadViewModel.visitasPorSemana(eventos, hoy, bogota)
        assertEquals(2, semanas.last().visitas)
        assertEquals(2, semanas.sumOf { it.visitas })
    }

    @Test
    fun `respeta la zona horaria al asignar la semana`() {
        // Domingo 22:00 en Bogota es lunes 03:00 UTC.
        val eventos = listOf(ev("2026-10-12T03:00:00Z"))
        val enBogota = ActividadViewModel.visitasPorSemana(eventos, hoy, bogota)
        assertEquals(0, enBogota.last().visitas)
        assertEquals(1, enBogota[enBogota.size - 2].visitas)
        val enUtc = ActividadViewModel.visitasPorSemana(eventos, hoy, ZoneOffset.UTC)
        assertEquals(1, enUtc.last().visitas)
    }

    @Test
    fun `bordes de semana lunes 00 00 y domingo 23 59 y semanas fuera del rango`() {
        val eventos = listOf(
            ev("2026-10-12T00:00:00-05:00"), // lunes actual, inicio
            ev("2026-10-18T23:59:59-05:00"), // domingo actual, final
            ev("2026-10-11T23:59:59-05:00"), // domingo anterior
            ev("2026-08-24T00:00:00-05:00"), // lunes de la semana más antigua incluida
            ev("2026-08-23T23:59:59-05:00"), // fuera: 9 semanas atrás
            ev("2026-10-19T00:00:00-05:00"), // fuera: semana siguiente
        )
        val semanas = ActividadViewModel.visitasPorSemana(eventos, hoy, bogota)
        assertEquals(2, semanas.last().visitas)
        assertEquals(1, semanas[6].visitas)
        assertEquals(1, semanas.first().visitas)
        assertEquals(4, semanas.sumOf { it.visitas })
    }

    @Test
    fun `hoy lunes o domingo usa la semana correcta y el numero de semanas es configurable`() {
        val lunes = ActividadViewModel.visitasPorSemana(emptyList(), lunesActual, bogota, semanas = 3)
        assertEquals(listOf(lunesActual.minusWeeks(2), lunesActual.minusWeeks(1), lunesActual), lunes.map { it.inicio })
        val domingo = ActividadViewModel.visitasPorSemana(emptyList(), LocalDate.of(2026, 10, 18), bogota, semanas = 1)
        assertEquals(listOf(lunesActual), domingo.map { it.inicio })
    }

    // duracionLegible

    @Test
    fun `duracionLegible`() {
        assertEquals("0 min", ActividadViewModel.duracionLegible(0))
        assertEquals("45 min", ActividadViewModel.duracionLegible(45))
        assertEquals("1 h", ActividadViewModel.duracionLegible(60))
        assertEquals("2 h 15 min", ActividadViewModel.duracionLegible(135))
        assertEquals("25 h 1 min", ActividadViewModel.duracionLegible(1_501))
        assertNull(ActividadViewModel.duracionLegible(null))
        assertNull(ActividadViewModel.duracionLegible(-1))
    }
}
