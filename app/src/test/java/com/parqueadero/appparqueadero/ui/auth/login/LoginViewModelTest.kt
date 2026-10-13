package com.parqueadero.appparqueadero.ui.auth.login

import com.parqueadero.appparqueadero.data.model.TokenDTO
import com.parqueadero.appparqueadero.data.network.ErrorParser
import com.parqueadero.appparqueadero.data.repository.AuthRepository
import com.parqueadero.appparqueadero.data.session.EstadoSesion
import com.parqueadero.appparqueadero.data.session.SessionManager
import com.parqueadero.appparqueadero.support.FakeApiService
import com.parqueadero.appparqueadero.support.FakePreferencias
import com.parqueadero.appparqueadero.support.FakeSesionStorage
import com.parqueadero.appparqueadero.support.MainDispatcherRule
import com.parqueadero.appparqueadero.support.gsonPrueba
import com.parqueadero.appparqueadero.support.httpError
import com.parqueadero.appparqueadero.support.problem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class LoginViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val api = FakeApiService()
    private val storage = FakeSesionStorage()
    private val sesion = SessionManager(storage, CoroutineScope(mainRule.dispatcher))

    private fun vm(prefs: FakePreferencias = FakePreferencias()) =
        LoginViewModel(AuthRepository(api, ErrorParser(gsonPrueba), sesion, prefs), prefs)

    private fun LoginViewModel.llenar(tel: String = "300 123 4567", pin: String = "482915") {
        onTelefono(tel); onPin(pin)
    }

    @Test
    fun `prellena el telefono recordado`() {
        assertEquals("3001234567", vm(FakePreferencias("3001234567")).estado.value.telefono)
        assertEquals("", vm().estado.value.telefono)
    }

    @Test
    fun `login OK guarda token, sesion Activa y limpia el PIN`() {
        api.loginResp = { TokenDTO("jwt", "USUARIO", null) }
        val vm = vm()
        vm.llenar()
        vm.ingresar()
        assertEquals(EstadoSesion.Activa, sesion.estado.value)
        assertEquals("jwt", storage.guardada?.token)
        assertEquals("3001234567", api.logins.single().telefono)
        val s = vm.estado.value
        assertEquals("", s.pin)
        assertFalse(s.cargando)
        assertNull(s.error)
    }

    @Test
    fun `mientras espera muestra cargando e ignora un segundo toque`() {
        val respuesta = CompletableDeferred<TokenDTO>()
        api.loginResp = { respuesta.await() }
        val vm = vm()
        vm.llenar()
        vm.ingresar()
        assertTrue(vm.estado.value.cargando)
        vm.ingresar()
        respuesta.complete(TokenDTO("jwt", null, null))
        assertEquals(1, api.logins.size)
        assertFalse(vm.estado.value.cargando)
    }

    private fun fallaCon(status: Int, esperado: String) {
        api.loginResp = { throw httpError(status, problem(status, "detalle del servidor")) }
        val vm = vm()
        vm.llenar()
        vm.ingresar()
        val s = vm.estado.value
        assertEquals(esperado, s.error)
        assertEquals("", s.pin)
        assertFalse(s.cargando)
        assertEquals(EstadoSesion.Cargando, sesion.estado.value)
        assertNull(storage.guardada)
    }

    @Test
    fun `401 credenciales incorrectas`() = fallaCon(
        401,
        "Teléfono o PIN incorrectos. Tras 5 intentos fallidos la cuenta se bloquea 15 minutos.",
    )

    @Test
    fun `423 cuenta bloqueada`() = fallaCon(
        423,
        "Tu cuenta está bloqueada por intentos fallidos. Espera 15 minutos o pide ayuda al administrador.",
    )

    @Test
    fun `429 demasiados intentos`() = fallaCon(
        429,
        "Demasiados intentos desde este dispositivo. Espera unos minutos y vuelve a intentar.",
    )

    @Test
    fun `400 con error de campo telefono lo muestra en el campo`() {
        api.loginResp = { throw httpError(400, problem(400, extra = "\"errores\":[{\"campo\":\"telefono\",\"mensaje\":\"no valido\"}]")) }
        val vm = vm()
        vm.llenar()
        vm.ingresar()
        assertEquals("no valido", vm.estado.value.errorTelefono)
        assertTrue(vm.estado.value.error!!.startsWith("Revisa los datos"))
    }

    @Test
    fun `sin conexion muestra el mensaje de red`() {
        api.loginResp = { throw IOException() }
        val vm = vm()
        vm.llenar()
        vm.ingresar()
        assertTrue(vm.estado.value.error!!.contains("No pudimos conectar"))
    }

    @Test
    fun `telefono vacio o invalido no llama a la API`() {
        val vm = vm()
        vm.llenar(tel = "", pin = "482915")
        vm.ingresar()
        assertEquals("Escribe tu número de celular.", vm.estado.value.errorTelefono)
        vm.onTelefono("12")
        assertNull(vm.estado.value.errorTelefono)
        vm.ingresar()
        assertNotNull(vm.estado.value.errorTelefono)
        assertTrue(api.logins.isEmpty())
    }

    @Test
    fun `PIN incompleto no llama a la API`() {
        val vm = vm()
        vm.llenar(pin = "48291")
        vm.ingresar()
        assertEquals("Escribe los 6 dígitos de tu PIN.", vm.estado.value.error)
        assertTrue(api.logins.isEmpty())
        vm.onPin("482915")
        assertNull(vm.estado.value.error)
    }
}
