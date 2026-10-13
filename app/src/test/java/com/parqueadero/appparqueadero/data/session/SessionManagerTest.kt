package com.parqueadero.appparqueadero.data.session

import com.parqueadero.appparqueadero.support.FakeSesionStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerTest {

    private val ahora = 1_000_000L

    @Test
    fun `estado inicial es Cargando`() = runTest {
        val sm = SessionManager(FakeSesionStorage(), this) { ahora }
        assertEquals(EstadoSesion.Cargando, sm.estado.value)
    }

    @Test
    fun `restaurar sin sesion guardada queda Inactiva`() = runTest {
        val storage = FakeSesionStorage()
        val sm = SessionManager(storage, this) { ahora }
        sm.restaurar()
        assertEquals(EstadoSesion.Inactiva(), sm.estado.value)
        assertEquals(0, storage.borrados)
    }

    @Test
    fun `restaurar con token vigente queda Activa`() = runTest {
        val sm = SessionManager(FakeSesionStorage(SesionGuardada("t1", ahora + 1)), this) { ahora }
        sm.restaurar()
        assertEquals(EstadoSesion.Activa, sm.estado.value)
        assertEquals("t1", sm.tokenActual())
    }

    @Test
    fun `restaurar sin fecha de vencimiento queda Activa`() = runTest {
        val sm = SessionManager(FakeSesionStorage(SesionGuardada("t1", null)), this) { ahora }
        sm.restaurar()
        assertEquals(EstadoSesion.Activa, sm.estado.value)
    }

    @Test
    fun `restaurar con token vencido (incluido el limite exacto) queda Inactiva y borra`() = runTest {
        listOf(ahora, ahora - 1).forEach { expira ->
            val storage = FakeSesionStorage(SesionGuardada("viejo", expira))
            val sm = SessionManager(storage, this) { ahora }
            sm.restaurar()
            assertEquals(EstadoSesion.Inactiva(), sm.estado.value)
            assertNull(sm.tokenActual())
            assertNull(storage.guardada)
            assertEquals(1, storage.borrados)
        }
    }

    @Test
    fun `iniciar guarda y activa, cerrar borra con el motivo`() = runTest {
        val storage = FakeSesionStorage()
        val sm = SessionManager(storage, this) { ahora }
        sm.iniciar("nuevo", 5L)
        assertEquals(SesionGuardada("nuevo", 5L), storage.guardada)
        assertEquals(EstadoSesion.Activa, sm.estado.value)
        sm.cerrar(MotivoCierre.CERRADA_POR_USUARIO)
        assertEquals(EstadoSesion.Inactiva(MotivoCierre.CERRADA_POR_USUARIO), sm.estado.value)
        assertNull(sm.tokenActual())
        assertNull(storage.guardada)
    }

    @Test
    fun `alNoAutorizado con un token viejo se ignora`() = runTest {
        val storage = FakeSesionStorage()
        val sm = SessionManager(storage, this) { ahora }
        sm.iniciar("vigente", null)
        sm.alNoAutorizado("viejo")
        advanceUntilIdle()
        assertEquals(EstadoSesion.Activa, sm.estado.value)
        assertEquals("vigente", sm.tokenActual())
        assertEquals(0, storage.borrados)
    }

    @Test
    fun `alNoAutorizado con el token vigente cierra por VENCIDA`() = runTest {
        val storage = FakeSesionStorage()
        val sm = SessionManager(storage, this) { ahora }
        sm.iniciar("vigente", null)
        sm.alNoAutorizado("vigente")
        assertNull(sm.tokenActual()) // se suelta de inmediato, antes de cerrar
        advanceUntilIdle()
        assertEquals(EstadoSesion.Inactiva(MotivoCierre.VENCIDA), sm.estado.value)
        assertNull(storage.guardada)
    }
}
