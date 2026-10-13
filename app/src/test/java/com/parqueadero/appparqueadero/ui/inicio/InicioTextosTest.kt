package com.parqueadero.appparqueadero.ui.inicio

import com.parqueadero.appparqueadero.data.model.EstadoUsuario
import com.parqueadero.appparqueadero.data.model.MotivoAcceso
import com.parqueadero.appparqueadero.support.usuario
import com.parqueadero.appparqueadero.ui.components.StatusTipo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EstadoCuentaTest {

    private val hoy = LocalDate.of(2026, 10, 13)

    private fun estado(estado: EstadoUsuario?, dias: Long?) =
        estadoCuenta(usuario(estado, dias?.let { hoy.plusDays(it).toString() }), hoy)

    @Test
    fun `suspendida y vencida no dependen de la fecha`() {
        assertEquals(StatusTipo.NEUTRO to "Suspendida", estado(EstadoUsuario.SUSPENDIDO, 30))
        assertEquals(StatusTipo.PELIGRO to "Vencida", estado(EstadoUsuario.VENCIDO, 30))
        assertEquals(StatusTipo.PELIGRO to "Vencida", estado(EstadoUsuario.VENCIDO, -3))
    }

    @Test
    fun `activo con mas de 5 dias o sin fecha esta al dia`() {
        assertEquals(StatusTipo.EXITO to "Al día", estado(EstadoUsuario.ACTIVO, 6))
        assertEquals(StatusTipo.EXITO to "Al día", estado(EstadoUsuario.ACTIVO, null))
    }

    @Test
    fun `activo con fecha ilegible se trata como al dia`() {
        assertEquals(StatusTipo.EXITO to "Al día", estadoCuenta(usuario(EstadoUsuario.ACTIVO, "13/10/2026"), hoy))
    }

    @Test
    fun `activo con 5 dias o menos esta por vencer, incluido hoy`() {
        for (d in 0L..5L) assertEquals("dias=$d", StatusTipo.ADVERTENCIA to "Por vencer", estado(EstadoUsuario.ACTIVO, d))
    }

    @Test
    fun `activo con fecha pasada esta en periodo de gracia`() {
        assertEquals(StatusTipo.ADVERTENCIA to "En periodo de gracia", estado(EstadoUsuario.ACTIVO, -1))
    }

    @Test
    fun `estado desconocido`() {
        assertEquals(StatusTipo.NEUTRO to "Sin estado", estado(null, 10))
    }
}

class TextosPuertaTest {

    @Test
    fun `todos los motivos y el nulo tienen texto y etiqueta`() {
        for (m in MotivoAcceso.entries + listOf<MotivoAcceso?>(null)) {
            assertTrue("motivo $m", TextosPuerta.motivo(m).isNotBlank())
            assertTrue("etiqueta $m", TextosPuerta.etiquetaMotivo(m).isNotBlank())
        }
    }

    @Test
    fun `los textos de los motivos son distintos entre si`() {
        val textos = MotivoAcceso.entries.map { TextosPuerta.motivo(it) }
        assertEquals(textos.size, textos.toSet().size)
    }
}
