package com.parqueadero.appparqueadero.ui.auth.login

import com.parqueadero.appparqueadero.data.model.TokenDTO
import com.parqueadero.appparqueadero.data.network.ErrorParser
import com.parqueadero.appparqueadero.data.repository.AuthRepository
import com.parqueadero.appparqueadero.data.session.EstadoSesion
import com.parqueadero.appparqueadero.data.session.SessionManager
import com.parqueadero.appparqueadero.support.FakeApiService
import com.parqueadero.appparqueadero.support.FakePreferencias
import com.parqueadero.appparqueadero.support.FakeBovedaPin
import com.parqueadero.appparqueadero.support.MainDispatcherRule
import com.parqueadero.appparqueadero.support.gsonPrueba
import com.parqueadero.appparqueadero.support.httpError
import com.parqueadero.appparqueadero.support.problem
import com.parqueadero.appparqueadero.data.session.PreparacionHuella
import kotlinx.coroutines.runBlocking
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
    private val sesion = SessionManager { 0L }
    private val boveda = FakeBovedaPin()

    private fun vm(prefs: FakePreferencias = FakePreferencias()) =
        LoginViewModel(AuthRepository(api, ErrorParser(gsonPrueba), sesion, prefs, boveda), prefs, boveda)

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
        assertEquals("jwt", sesion.tokenActual())
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
        assertEquals(EstadoSesion.Inactiva(), sesion.estado.value)
        assertNull(sesion.tokenActual())
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

    // Huella

    private val pinCambiadoHuella =
        "Tu PIN cambió desde que activaste la huella. Entra con tu PIN y vuelve a activarla en Perfil."

    @Test
    fun `huellaConfigurada sigue a la boveda`() {
        val vm = vm()
        assertFalse(vm.estado.value.huellaConfigurada)
        boveda.configurar("3001234567", "482915")
        assertTrue(vm.estado.value.huellaConfigurada)
        runBlocking { boveda.borrar() }
        assertFalse(vm.estado.value.huellaConfigurada)
    }

    @Test
    fun `prepararHuella Lista devuelve el cifrador sin error`() = runBlocking {
        boveda.configurar("3001234567", "482915")
        val vm = vm()
        assertEquals(FakeBovedaPin.cifrador, vm.prepararHuella())
        assertNull(vm.estado.value.error)
    }

    @Test
    fun `prepararHuella NoConfigurada devuelve null sin error`() = runBlocking {
        val vm = vm()
        assertNull(vm.prepararHuella())
        assertNull(vm.estado.value.error)
    }

    @Test
    fun `prepararHuella Invalidada (huella nueva registrada) devuelve null con mensaje`() = runBlocking {
        boveda.preparacion = PreparacionHuella.Invalidada
        val vm = vm()
        assertNull(vm.prepararHuella())
        assertTrue(vm.estado.value.error!!.startsWith("Cambiaron las huellas registradas"))
    }

    @Test
    fun `ingresarConHuella lee PIN y telefono de la boveda y hace login`() {
        boveda.configurar("3009998877", "482915")
        api.loginResp = { TokenDTO("jwt-h", null, null) }
        val vm = vm(FakePreferencias("3001234567"))
        vm.ingresarConHuella(FakeBovedaPin.cifrador)
        val login = api.logins.single()
        assertEquals("3009998877", login.telefono)
        assertEquals("482915", login.pin)
        assertEquals(EstadoSesion.Activa, sesion.estado.value)
        assertEquals("3009998877", vm.estado.value.telefono)
        assertFalse(vm.estado.value.cargando)
        assertNull(vm.estado.value.error)
        assertEquals(FakeBovedaPin.cifrador, boveda.cifradoresUsados.single())
        assertEquals(0, boveda.borrados)
    }

    @Test
    fun `ingresarConHuella con 401 borra la boveda y avisa que el PIN cambio`() {
        boveda.configurar("3001234567", "111111")
        api.loginResp = { throw httpError(401, problem(401, "Credenciales invalidas")) }
        val vm = vm()
        vm.ingresarConHuella(FakeBovedaPin.cifrador)
        assertEquals(pinCambiadoHuella, vm.estado.value.error)
        assertEquals(1, boveda.borrados)
        assertFalse(vm.estado.value.huellaConfigurada)
        assertEquals(EstadoSesion.Inactiva(), sesion.estado.value)
    }

    @Test
    fun `ingresarConHuella con otro error no borra la boveda`() {
        boveda.configurar("3001234567", "482915")
        api.loginResp = { throw httpError(423, problem(423)) }
        val vm = vm()
        vm.ingresarConHuella(FakeBovedaPin.cifrador)
        assertTrue(vm.estado.value.error!!.startsWith("Tu cuenta está bloqueada"))
        assertEquals(0, boveda.borrados)
        assertTrue(vm.estado.value.huellaConfigurada)
    }

    @Test
    fun `ingresarConHuella sin PIN legible no llama a la API`() {
        boveda.configurar("3001234567", "482915")
        boveda.leerFalla = true
        val vm = vm()
        vm.ingresarConHuella(FakeBovedaPin.cifrador)
        assertEquals("No pudimos leer tu PIN guardado. Entra con tu PIN.", vm.estado.value.error)
        assertTrue(api.logins.isEmpty())
    }

    @Test
    fun `ingresarConHuella mientras carga se ignora`() {
        boveda.configurar("3001234567", "482915")
        val respuesta = CompletableDeferred<TokenDTO>()
        api.loginResp = { respuesta.await() }
        val vm = vm()
        vm.ingresarConHuella(FakeBovedaPin.cifrador)
        assertTrue(vm.estado.value.cargando)
        vm.ingresarConHuella(FakeBovedaPin.cifrador)
        respuesta.complete(TokenDTO("jwt", null, null))
        assertEquals(1, api.logins.size)
    }

    @Test
    fun `falloHuella con null (cancelar) no muestra error y con mensaje lo muestra`() {
        val vm = vm()
        vm.falloHuella(null)
        assertNull(vm.estado.value.error)
        vm.falloHuella("Demasiados intentos. Usa tu PIN.")
        assertEquals("Demasiados intentos. Usa tu PIN.", vm.estado.value.error)
        assertTrue(api.logins.isEmpty())
    }
}
