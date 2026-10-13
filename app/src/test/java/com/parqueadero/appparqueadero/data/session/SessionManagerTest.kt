package com.parqueadero.appparqueadero.data.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionManagerTest {

    private var ahora = 1_000_000L
    private val sm = SessionManager { ahora }

    @Test
    fun `estado inicial es Inactiva sin motivo y sin token`() {
        assertEquals(EstadoSesion.Inactiva(), sm.estado.value)
        assertNull(sm.tokenActual())
    }

    @Test
    fun `iniciar activa y deja el token en memoria`() {
        sm.iniciar("t1", ahora + 1)
        assertEquals(EstadoSesion.Activa, sm.estado.value)
        assertEquals("t1", sm.tokenActual())
    }

    @Test
    fun `sin fecha de vencimiento el token no expira`() {
        sm.iniciar("t1", null)
        ahora += 365L * 24 * 3_600_000
        assertEquals("t1", sm.tokenActual())
    }

    @Test
    fun `tokenActual devuelve null al llegar al limite exacto de vencimiento y despues`() {
        sm.iniciar("t1", ahora + 10)
        ahora += 9
        assertEquals("t1", sm.tokenActual())
        ahora += 1
        assertNull(sm.tokenActual())
        ahora += 1_000
        assertNull(sm.tokenActual())
    }

    @Test
    fun `cerrar suelta el token y deja el motivo`() {
        for (motivo in MotivoCierre.entries) {
            sm.iniciar("t", null)
            sm.cerrar(motivo)
            assertEquals(EstadoSesion.Inactiva(motivo), sm.estado.value)
            assertNull(sm.tokenActual())
        }
    }

    @Test
    fun `cerrar al cerrar la app sin sesion no cambia el motivo previo`() {
        sm.iniciar("t", null)
        sm.cerrar(MotivoCierre.PIN_CAMBIADO)
        sm.cerrar(MotivoCierre.APP_CERRADA)
        assertEquals(EstadoSesion.Inactiva(MotivoCierre.PIN_CAMBIADO), sm.estado.value)
    }

    @Test
    fun `cerrar la app con sesion activa deja APP_CERRADA`() {
        sm.iniciar("t", null)
        sm.cerrar(MotivoCierre.APP_CERRADA)
        assertEquals(EstadoSesion.Inactiva(MotivoCierre.APP_CERRADA), sm.estado.value)
    }

    @Test
    fun `una nueva instancia no recuerda la sesion anterior (token solo en memoria)`() {
        sm.iniciar("t", null)
        val nueva = SessionManager { ahora }
        assertEquals(EstadoSesion.Inactiva(), nueva.estado.value)
        assertNull(nueva.tokenActual())
    }

    @Test
    fun `alNoAutorizado con un token viejo se ignora`() {
        sm.iniciar("viejo", null)
        sm.iniciar("vigente", null)
        sm.alNoAutorizado("viejo")
        assertEquals(EstadoSesion.Activa, sm.estado.value)
        assertEquals("vigente", sm.tokenActual())
    }

    @Test
    fun `alNoAutorizado con el token vigente cierra por VENCIDA`() {
        sm.iniciar("vigente", null)
        sm.alNoAutorizado("vigente")
        assertNull(sm.tokenActual())
        assertEquals(EstadoSesion.Inactiva(MotivoCierre.VENCIDA), sm.estado.value)
    }
}
