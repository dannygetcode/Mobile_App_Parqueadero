package com.parqueadero.appparqueadero.ui.pagos

import com.parqueadero.appparqueadero.data.model.EstadoPago
import com.parqueadero.appparqueadero.data.model.ProximoPeriodoDTO
import com.parqueadero.appparqueadero.data.network.AppError
import com.parqueadero.appparqueadero.data.network.ErrorParser
import com.parqueadero.appparqueadero.data.network.mensaje
import com.parqueadero.appparqueadero.data.repository.PagoRepository
import com.parqueadero.appparqueadero.support.FIRMA_JPEG
import com.parqueadero.appparqueadero.support.FIRMA_PNG
import com.parqueadero.appparqueadero.support.FakeApiService
import com.parqueadero.appparqueadero.support.FakeFuenteImagen
import com.parqueadero.appparqueadero.support.MainDispatcherRule
import com.parqueadero.appparqueadero.support.gsonPrueba
import com.parqueadero.appparqueadero.support.httpError
import com.parqueadero.appparqueadero.support.imagen
import com.parqueadero.appparqueadero.support.pago
import com.parqueadero.appparqueadero.support.problem
import com.parqueadero.appparqueadero.ui.common.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class PagosViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val maximo = PagoRepository.TAMANO_MAXIMO_BYTES.toInt()
    private val periodo = ProximoPeriodoDTO("2026-11-01", "2026-11-30", 120_000)
    private var llamadasHistorial = 0
    private val api = FakeApiService().apply {
        proximoPeriodoResp = { periodo }
        misPagosResp = { llamadasHistorial++; emptyList() }
    }
    private val fuente = FakeFuenteImagen()

    private fun vm() = PagosViewModel(PagoRepository(api, ErrorParser(gsonPrueba)), fuente)

    private fun PagosViewModel.conComprobante(): PagosViewModel {
        fuente.imagenes["content://foto"] = imagen(FIRMA_JPEG, 1_000)
        elegirComprobante("content://foto")
        assertNotNull(estado.value.comprobante)
        return this
    }

    // Carga

    @Test
    fun `carga periodo e historial vacio es Empty`() {
        val s = vm().estado.value
        assertEquals(UiState.Success(periodo), s.periodo)
        assertEquals(UiState.Empty, s.historial)
        assertFalse(s.hayPendiente)
        assertFalse(s.refrescando)
    }

    @Test
    fun `historial agrupado por anio y error de periodo independiente`() {
        api.proximoPeriodoResp = { throw IOException() }
        api.misPagosResp = { listOf(pago(1, "2025-12-01"), pago(2, "2026-01-01")) }
        val s = vm().estado.value
        assertTrue(s.periodo is UiState.Error)
        val grupos = (s.historial as UiState.Success).data
        assertEquals(listOf(2026, 2025), grupos.map { it.anio })
    }

    @Test
    fun `error del historial es Error`() {
        api.misPagosResp = { throw httpError(500, problem(500)) }
        assertTrue(vm().estado.value.historial is UiState.Error)
    }

    @Test
    fun `hayPendiente si algun pago esta PENDIENTE`() {
        api.misPagosResp = { listOf(pago(1, "2026-09-01"), pago(2, "2026-10-01", estado = EstadoPago.PENDIENTE)) }
        assertTrue(vm().estado.value.hayPendiente)
        api.misPagosResp = { listOf(pago(1, "2026-09-01"), pago(3, "2026-08-01", estado = EstadoPago.RECHAZADO)) }
        assertFalse(vm().estado.value.hayPendiente)
    }

    @Test
    fun `agrupar ordena por anio y por periodo descendente, con respaldo en creadoEn`() {
        val grupos = PagosViewModel.agrupar(
            listOf(
                pago(1, "2025-03-01"),
                pago(2, "2026-02-01"),
                pago(3, "2025-11-01"),
                pago(4, null, creadoEn = "2024-06-10T10:00:00-05:00"),
                pago(5, "2026-09-01"),
                pago(6, null, null),
            ),
        )
        assertEquals(listOf(2026, 2025, 2024, 0), grupos.map { it.anio })
        assertEquals(listOf(5L, 2L), grupos[0].pagos.map { it.id })
        assertEquals(listOf(3L, 1L), grupos[1].pagos.map { it.id })
        assertEquals(listOf(4L), grupos[2].pagos.map { it.id })
    }

    @Test
    fun `agrupar lista vacia`() {
        assertTrue(PagosViewModel.agrupar(emptyList()).isEmpty())
    }

    // Comprobante

    @Test
    fun `imagen ilegible se rechaza`() {
        val vm = vm()
        vm.elegirComprobante("content://no-existe")
        val s = vm.estado.value
        assertEquals("No pudimos leer la imagen. Intenta con otra.", s.errorComprobante)
        assertNull(s.comprobante)
        assertFalse(s.leyendo)
    }

    @Test
    fun `archivo que no es JPEG ni PNG se rechaza por firma`() {
        fuente.imagenes["content://doc.jpg"] = "%PDF-1.7 falso".toByteArray()
        val vm = vm()
        vm.elegirComprobante("content://doc.jpg")
        assertEquals("Solo se aceptan fotos JPEG o PNG.", vm.estado.value.errorComprobante)
        assertNull(vm.estado.value.comprobante)
    }

    @Test
    fun `imagen de hasta 5 MB se acepta con su mime y sin reducir`() {
        fuente.imagenes["content://png"] = imagen(FIRMA_PNG, maximo)
        val vm = vm()
        vm.elegirComprobante("content://png")
        assertEquals(Comprobante("content://png", maximo, PagoRepository.MIME_PNG), vm.estado.value.comprobante)
        assertNull(vm.estado.value.errorComprobante)
        assertTrue(fuente.reducciones.isEmpty())
    }

    @Test
    fun `imagen de mas de 5 MB se reduce y queda como JPEG`() {
        fuente.imagenes["content://grande"] = imagen(FIRMA_PNG, maximo + 1)
        val reducida = imagen(FIRMA_JPEG, 900_000)
        fuente.reducirResp = { _, _ -> reducida }
        val vm = vm()
        vm.elegirComprobante("content://grande")
        assertEquals(Comprobante("content://grande", 900_000, PagoRepository.MIME_JPEG), vm.estado.value.comprobante)
        assertEquals(listOf(PagoRepository.TAMANO_MAXIMO_BYTES), fuente.reducciones)
    }

    @Test
    fun `imagen de mas de 5 MB que no se puede reducir se rechaza`() {
        fuente.imagenes["content://grande"] = imagen(FIRMA_JPEG, maximo + 1)
        val vm = vm().conComprobante()
        vm.elegirComprobante("content://grande")
        val s = vm.estado.value
        assertNull(s.comprobante)
        assertTrue(s.errorComprobante!!.contains("5 MB"))
        // Sin comprobante válido no se sube nada.
        vm.subir()
        assertTrue(api.partesSubidas.isEmpty())
    }

    @Test
    fun `quitar comprobante`() {
        val vm = vm().conComprobante()
        vm.quitarComprobante()
        assertNull(vm.estado.value.comprobante)
        vm.subir()
        assertTrue(api.partesSubidas.isEmpty())
    }

    // Subida

    @Test
    fun `subir OK limpia el comprobante, deja mensaje y recarga`() {
        api.subirPagoResp = { pago(9, "2026-11-01", estado = EstadoPago.PENDIENTE) }
        val vm = vm().conComprobante()
        assertEquals(1, llamadasHistorial)
        api.misPagosResp = { llamadasHistorial++; listOf(pago(9, "2026-11-01", estado = EstadoPago.PENDIENTE)) }
        vm.subir()
        val s = vm.estado.value
        assertEquals(1, api.partesSubidas.size)
        assertEquals("comprobante", api.partesSubidas.single().headers?.get("Content-Disposition")?.substringAfter("name=\"")?.substringBefore("\""))
        assertNull(s.comprobante)
        assertFalse(s.subiendo)
        assertEquals("Comprobante enviado. Queda pendiente de aprobación.", s.mensaje)
        assertEquals(2, llamadasHistorial)
        assertTrue(s.hayPendiente)
        vm.mensajeMostrado()
        assertNull(vm.estado.value.mensaje)
    }

    @Test
    fun `subir 409 muestra el detail y conserva el comprobante`() {
        api.subirPagoResp = { throw httpError(409, problem(409, "Ya tienes un pago pendiente")) }
        val vm = vm().conComprobante()
        vm.subir()
        val s = vm.estado.value
        assertEquals("Ya tienes un pago pendiente", s.errorComprobante)
        assertNotNull(s.comprobante)
        assertFalse(s.subiendo)
        assertEquals(1, llamadasHistorial)
    }

    @Test
    fun `mensajeSubida para 409 sin detail, 413, 415 y otros`() {
        assertTrue(PagosViewModel.mensajeSubida(AppError.Http(409)).contains("pago pendiente"))
        assertEquals("La imagen supera el máximo de 5 MB.", PagosViewModel.mensajeSubida(AppError.Http(413, "x")))
        assertEquals("El archivo no es una imagen JPEG o PNG válida.", PagosViewModel.mensajeSubida(AppError.Http(415)))
        assertEquals(AppError.SinConexion.mensaje(), PagosViewModel.mensajeSubida(AppError.SinConexion))
    }

    @Test
    fun `subir 413 y 415 desde la API dan su mensaje`() {
        for ((status, texto) in listOf(413 to "5 MB", 415 to "JPEG o PNG")) {
            api.subirPagoResp = { throw httpError(status, problem(status, "detalle")) }
            val vm = vm().conComprobante()
            vm.subir()
            assertTrue("status $status", vm.estado.value.errorComprobante!!.contains(texto))
        }
    }
}
