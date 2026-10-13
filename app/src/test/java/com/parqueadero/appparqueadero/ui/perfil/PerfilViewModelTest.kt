package com.parqueadero.appparqueadero.ui.perfil

import com.parqueadero.appparqueadero.data.network.ErrorParser
import com.parqueadero.appparqueadero.data.repository.AuthRepository
import com.parqueadero.appparqueadero.data.repository.UsuarioRepository
import com.parqueadero.appparqueadero.data.session.EstadoSesion
import com.parqueadero.appparqueadero.data.session.MotivoCierre
import com.parqueadero.appparqueadero.data.session.SessionManager
import com.parqueadero.appparqueadero.support.FakeApiService
import com.parqueadero.appparqueadero.support.FakePreferencias
import com.parqueadero.appparqueadero.support.FakeSesionStorage
import com.parqueadero.appparqueadero.support.MainDispatcherRule
import com.parqueadero.appparqueadero.support.gsonPrueba
import com.parqueadero.appparqueadero.support.problem
import com.parqueadero.appparqueadero.support.respuestaError
import com.parqueadero.appparqueadero.support.usuario
import com.parqueadero.appparqueadero.ui.common.UiState
import com.parqueadero.appparqueadero.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class PerfilViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val api = FakeApiService().apply { yoResp = { usuario() } }
    private val storage = FakeSesionStorage()
    private val prefs = FakePreferencias()
    private val sesion = SessionManager(storage, CoroutineScope(mainRule.dispatcher))
    private val parser = ErrorParser(gsonPrueba)

    @Before
    fun iniciarSesion() = runBlocking { sesion.iniciar("jwt", null) }

    private fun vm() = PerfilViewModel(
        UsuarioRepository(api, parser, sesion),
        AuthRepository(api, parser, sesion, prefs),
        prefs,
    )

    private fun PerfilViewModel.llenar(actual: String = "482915", nuevo: String = "739164", confirmacion: String = nuevo) {
        abrirCambioPin()
        onPinActual(actual)
        onPinNuevo(nuevo)
        onConfirmacion(confirmacion)
        confirmarCambioPin()
    }

    private val PerfilViewModel.pin get() = cambioPin.value!!

    @Test
    fun `carga el usuario`() {
        assertEquals(UiState.Success(usuario()), vm().usuario.value)
    }

    // Validaciones locales: no llaman a la API

    @Test
    fun `PIN actual incompleto`() {
        val vm = vm()
        vm.llenar(actual = "123")
        assertEquals("Escribe tu PIN actual.", vm.pin.errorActual)
        assertTrue(api.cambiosPin.isEmpty())
    }

    @Test
    fun `PIN nuevo trivial o incompleto`() {
        val vm = vm()
        vm.llenar(nuevo = "123456")
        assertTrue(vm.pin.errorNuevo!!.contains("predecible"))
        vm.llenar(nuevo = "111111")
        assertTrue(vm.pin.errorNuevo!!.contains("predecible"))
        vm.llenar(nuevo = "73916")
        assertEquals("El PIN debe tener 6 dígitos.", vm.pin.errorNuevo)
        assertNull("sin error de confirmacion si el nuevo ya falla", vm.pin.errorConfirmacion)
        assertTrue(api.cambiosPin.isEmpty())
    }

    @Test
    fun `PIN nuevo igual al actual`() {
        val vm = vm()
        vm.llenar(actual = "482915", nuevo = "482915")
        assertEquals("El PIN nuevo debe ser distinto del actual.", vm.pin.errorNuevo)
        assertTrue(api.cambiosPin.isEmpty())
    }

    @Test
    fun `confirmacion distinta limpia la confirmacion`() {
        val vm = vm()
        vm.llenar(confirmacion = "739165")
        assertEquals("Los PIN no coinciden.", vm.pin.errorConfirmacion)
        assertEquals("", vm.pin.confirmacion)
        assertTrue(api.cambiosPin.isEmpty())
    }

    @Test
    fun `escribir limpia el error del campo`() {
        val vm = vm()
        vm.llenar(actual = "1")
        vm.onPinActual("4829")
        assertNull(vm.pin.errorActual)
    }

    // Respuestas del backend

    @Test
    fun `cambio OK cierra el dialogo y la sesion con PIN_CAMBIADO`() {
        api.cambiarPinResp = { Response.success(204, Unit) }
        val vm = vm()
        vm.llenar()
        assertNull(vm.cambioPin.value)
        assertEquals(EstadoSesion.Inactiva(MotivoCierre.PIN_CAMBIADO), sesion.estado.value)
        assertNull(storage.guardada)
        assertEquals("482915", api.cambiosPin.single().pinActual)
        assertEquals("739164", api.cambiosPin.single().pinNuevo)
    }

    @Test
    fun `401 marca el PIN actual y la sesion sigue Activa`() {
        api.cambiarPinResp = { respuestaError(401, problem(401, "PIN incorrecto")) }
        val vm = vm()
        vm.llenar()
        val s = vm.pin
        assertEquals("El PIN actual no es correcto.", s.errorActual)
        assertEquals("", s.pinActual)
        assertFalse(s.enviando)
        assertEquals(EstadoSesion.Activa, sesion.estado.value)
        assertEquals("jwt", sesion.tokenActual())
    }

    @Test
    fun `423 muestra bloqueo`() {
        api.cambiarPinResp = { respuestaError(423, problem(423)) }
        val vm = vm()
        vm.llenar()
        assertTrue(vm.pin.error!!.contains("bloqueada"))
        assertEquals(EstadoSesion.Activa, sesion.estado.value)
    }

    @Test
    fun `400 limpia el PIN nuevo y muestra el detail`() {
        api.cambiarPinResp = { respuestaError(400, problem(400, "PIN usado recientemente")) }
        val vm = vm()
        vm.llenar()
        val s = vm.pin
        assertEquals("PIN usado recientemente", s.errorNuevo)
        assertEquals("", s.pinNuevo)
        assertEquals("", s.confirmacion)
        assertEquals("482915", s.pinActual)
    }

    @Test
    fun `cerrar el dialogo`() {
        val vm = vm()
        vm.abrirCambioPin()
        assertNotNull(vm.cambioPin.value)
        vm.cerrarCambioPin()
        assertNull(vm.cambioPin.value)
    }

    // Tema y sesión

    @Test
    fun `elegirTema guarda en preferencias`() {
        val vm = vm()
        vm.elegirTema(ThemeMode.OSCURO)
        assertEquals(ThemeMode.OSCURO, prefs.temaFlow.value)
    }

    @Test
    fun `cerrarSesion deja Inactiva por usuario`() {
        val vm = vm()
        vm.cerrarSesion()
        assertEquals(EstadoSesion.Inactiva(MotivoCierre.CERRADA_POR_USUARIO), sesion.estado.value)
        assertEquals(1, storage.borrados)
    }
}
