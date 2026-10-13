package com.parqueadero.appparqueadero.ui.inicio

import com.parqueadero.appparqueadero.data.model.MotivoAcceso
import com.parqueadero.appparqueadero.data.model.PuertaDTO
import com.parqueadero.appparqueadero.data.model.ResultadoAcceso
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
import com.parqueadero.appparqueadero.support.respuestaError
import com.parqueadero.appparqueadero.support.usuario
import com.parqueadero.appparqueadero.ui.common.UiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset

/**
 * El temporizador de cierre usa delay en viewModelScope (Dispatchers.Main = UnconfinedTestDispatcher):
 * el tiempo es virtual y avanza solo con el scheduler de la regla.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InicioViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val scheduler get() = mainRule.dispatcher.scheduler
    private val api = FakeApiService().apply { yoResp = { usuario() } }
    private val parser = ErrorParser(gsonPrueba)
    private val sesion = SessionManager { 0L }
    private val reloj = 1_760_000_000_000L

    private fun vm() = InicioViewModel(
        UsuarioRepository(api, parser, sesion, FakeBovedaPin()),
        AccesoRepository(api, parser, gsonPrueba),
        ahora = { reloj },
    )

    /** Instante ISO-8601 (con zona de Colombia) a [ms] del reloj inyectado. */
    private fun hasta(ms: Long): String = Instant.ofEpochMilli(reloj + ms).atOffset(ZoneOffset.ofHours(-5)).toString()

    private fun permitir(p: PuertaDTO) {
        api.abrirPuertaResp = { Response.success(p) }
    }

    private fun avanzar(ms: Long) {
        scheduler.advanceTimeBy(ms)
        scheduler.runCurrent()
    }

    // Cuenta

    @Test
    fun `cargar cuenta OK deja Success`() {
        assertEquals(UiState.Success(usuario()), vm().cuenta.value)
    }

    @Test
    fun `cargar cuenta con error deja Error`() {
        api.yoResp = { throw IOException() }
        assertTrue(vm().cuenta.value is UiState.Error)
    }

    @Test
    fun `refresco fallido conserva los datos previos y apaga refrescando`() {
        val vm = vm()
        api.yoResp = { throw httpError(500, problem(500, "fallo")) }
        vm.cargarCuenta(refresco = true)
        assertEquals(UiState.Success(usuario()), vm.cuenta.value)
        assertFalse(vm.refrescando.value)
    }

    @Test
    fun `refresco OK reemplaza los datos`() {
        val vm = vm()
        api.yoResp = { usuario(nombre = "Luis") }
        vm.cargarCuenta(refresco = true)
        assertEquals("Luis", (vm.cuenta.value as UiState.Success).data.nombre)
        assertFalse(vm.refrescando.value)
    }

    // Puerta

    @Test
    fun `abrir 200 usa abiertaHasta con el reloj inyectado y se cierra sola`() {
        permitir(PuertaDTO(true, hasta(7_000), null, evento()))
        val vm = vm()
        vm.abrir()
        val abierta = vm.puerta.value as PuertaUiState.Abierta
        assertEquals(7_000L, abierta.duracionMs)
        assertEquals(1, abierta.intento)
        assertNull(abierta.aviso)
        assertEquals(true, api.comandosPuerta.single().abierta)
        avanzar(6_999)
        assertTrue(vm.puerta.value is PuertaUiState.Abierta)
        avanzar(2)
        assertEquals(PuertaUiState.Cerrada(), vm.puerta.value)
    }

    @Test
    fun `abiertaHasta nulo usa 15 s y luego se cierra`() {
        permitir(PuertaDTO(true, null, null, evento()))
        val vm = vm()
        vm.abrir()
        assertEquals(15_000L, InicioViewModel.DURACION_POR_DEFECTO_MS)
        assertEquals(InicioViewModel.DURACION_POR_DEFECTO_MS, (vm.puerta.value as PuertaUiState.Abierta).duracionMs)
        avanzar(14_999)
        assertTrue(vm.puerta.value is PuertaUiState.Abierta)
        avanzar(2)
        assertEquals(PuertaUiState.Cerrada(), vm.puerta.value)
    }

    @Test
    fun `abiertaHasta irrazonable usa 15 s`() {
        // Lejano (relojes desfasados), menor a 1 s, muy en el pasado e ilegible.
        for (valor in listOf(hasta(120_000), hasta(20_001), hasta(500), hasta(-120_000), "no-es-fecha")) {
            permitir(PuertaDTO(true, valor, null, evento()))
            val vm = vm()
            vm.abrir()
            assertEquals(valor, 15_000L, (vm.puerta.value as PuertaUiState.Abierta).duracionMs)
        }
    }

    @Test
    fun `limites de duracion razonable 1 s y 20 s se respetan`() {
        for (ms in listOf(1_000L, 20_000L)) {
            permitir(PuertaDTO(true, hasta(ms), null, evento()))
            val vm = vm()
            vm.abrir()
            assertEquals(ms, (vm.puerta.value as PuertaUiState.Abierta).duracionMs)
        }
    }

    @Test
    fun `403 con PuertaDTO queda Denegada con motivo y descartar vuelve a Cerrada`() {
        val cuerpo = gsonPrueba.toJson(
            PuertaDTO(false, null, null, evento(MotivoAcceso.USUARIO_VENCIDO, resultado = ResultadoAcceso.DENEGADO)),
        )
        api.abrirPuertaResp = { respuestaError(403, cuerpo) }
        val vm = vm()
        vm.abrir()
        assertEquals(PuertaUiState.Denegada(1, MotivoAcceso.USUARIO_VENCIDO), vm.puerta.value)
        vm.descartar()
        assertEquals(PuertaUiState.Cerrada(), vm.puerta.value)
    }

    @Test
    fun `403 sin PuertaDTO es Fallo no reintentable`() {
        api.abrirPuertaResp = { respuestaError(403, problem(403, "Prohibido")) }
        val vm = vm()
        vm.abrir()
        val f = vm.puerta.value as PuertaUiState.Fallo
        assertFalse(f.reintentable)
    }

    @Test
    fun `SALIDA_CON_DEUDA permitida abre con aviso`() {
        permitir(PuertaDTO(true, hasta(10_000), null, evento(MotivoAcceso.SALIDA_CON_DEUDA)))
        val vm = vm()
        vm.abrir()
        val abierta = vm.puerta.value as PuertaUiState.Abierta
        assertEquals(TextosPuerta.motivo(MotivoAcceso.SALIDA_CON_DEUDA), abierta.aviso)
        assertEquals(MotivoAcceso.SALIDA_CON_DEUDA, abierta.evento?.motivo)
    }

    @Test
    fun `orden duplicada con la puerta ya cerrada queda Cerrada con aviso`() {
        permitir(PuertaDTO(false, hasta(-20_000), null, evento(duplicado = true)))
        val vm = vm()
        vm.abrir()
        val cerrada = vm.puerta.value as PuertaUiState.Cerrada
        assertTrue(cerrada.aviso!!.contains("menos de un minuto"))
        val antes = scheduler.currentTime
        scheduler.advanceUntilIdle()
        assertEquals("sin temporizador de cierre", antes, scheduler.currentTime)
    }

    @Test
    fun `sin conexion es Fallo reintentable`() {
        api.abrirPuertaResp = { throw IOException() }
        val vm = vm()
        vm.abrir()
        assertEquals(PuertaUiState.Fallo(1, "Sin conexión con el parqueadero.", true), vm.puerta.value)
    }

    @Test
    fun `409 es Fallo no reintentable con el detail del backend`() {
        api.abrirPuertaResp = { respuestaError(409, problem(409, "No tienes un vehiculo activo")) }
        val vm = vm()
        vm.abrir()
        assertEquals(PuertaUiState.Fallo(1, "No tienes un vehiculo activo", false), vm.puerta.value)
    }

    @Test
    fun `409 sin detail usa el texto por defecto`() {
        api.abrirPuertaResp = { respuestaError(409, "") }
        val vm = vm()
        vm.abrir()
        val f = vm.puerta.value as PuertaUiState.Fallo
        assertFalse(f.reintentable)
        assertTrue(f.mensaje.contains("vehículo activo"))
    }

    @Test
    fun `500 es Fallo reintentable`() {
        api.abrirPuertaResp = { respuestaError(500, problem(500, "x")) }
        val vm = vm()
        vm.abrir()
        assertTrue((vm.puerta.value as PuertaUiState.Fallo).reintentable)
    }

    @Test
    fun `abrir mientras Enviando o Abierta se ignora`() {
        val respuesta = CompletableDeferred<Response<PuertaDTO>>()
        api.abrirPuertaResp = { respuesta.await() }
        val vm = vm()
        vm.abrir()
        assertEquals(PuertaUiState.Enviando, vm.puerta.value)
        vm.abrir()
        assertEquals(1, api.comandosPuerta.size)
        respuesta.complete(Response.success(PuertaDTO(true, null, null, evento())))
        assertTrue(vm.puerta.value is PuertaUiState.Abierta)
        vm.abrir()
        assertEquals(1, api.comandosPuerta.size)
    }

    @Test
    fun `el intento aumenta en cada respuesta`() {
        api.abrirPuertaResp = { throw IOException() }
        val vm = vm()
        vm.abrir()
        vm.descartar()
        vm.abrir()
        assertEquals(2, (vm.puerta.value as PuertaUiState.Fallo).intento)
    }

    @Test
    fun `cierreVisual cierra y cancela el temporizador`() {
        permitir(PuertaDTO(true, null, null, evento()))
        val vm = vm()
        vm.abrir()
        avanzar(3_000)
        vm.cierreVisual()
        assertEquals(PuertaUiState.Cerrada(), vm.puerta.value)
        val antes = scheduler.currentTime
        scheduler.advanceUntilIdle()
        assertEquals("no debe quedar un delay pendiente", antes, scheduler.currentTime)
        assertEquals(PuertaUiState.Cerrada(), vm.puerta.value)
    }

    @Test
    fun `cierreVisual sin puerta abierta no hace nada`() {
        api.abrirPuertaResp = { throw IOException() }
        val vm = vm()
        vm.abrir()
        vm.cierreVisual()
        assertTrue(vm.puerta.value is PuertaUiState.Fallo)
    }
}
