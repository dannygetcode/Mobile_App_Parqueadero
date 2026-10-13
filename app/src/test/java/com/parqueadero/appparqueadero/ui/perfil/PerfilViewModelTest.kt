package com.parqueadero.appparqueadero.ui.perfil

import com.parqueadero.appparqueadero.data.network.ErrorParser
import com.parqueadero.appparqueadero.data.repository.AuthRepository
import com.parqueadero.appparqueadero.data.repository.UsuarioRepository
import com.parqueadero.appparqueadero.data.session.EstadoSesion
import com.parqueadero.appparqueadero.data.session.MotivoCierre
import com.parqueadero.appparqueadero.data.session.SessionManager
import com.parqueadero.appparqueadero.support.FakeApiService
import com.parqueadero.appparqueadero.support.FakePreferencias
import com.parqueadero.appparqueadero.data.model.TokenDTO
import com.parqueadero.appparqueadero.support.FakeBovedaPin
import com.parqueadero.appparqueadero.support.httpError
import kotlinx.coroutines.launch
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
    private val prefs = FakePreferencias()
    private val sesion = SessionManager { 0L }
    private val boveda = FakeBovedaPin()
    private val parser = ErrorParser(gsonPrueba)

    @Before
    fun iniciarSesion() = sesion.iniciar("jwt", null)

    private fun vm() = PerfilViewModel(
        UsuarioRepository(api, parser, sesion, boveda),
        AuthRepository(api, parser, sesion, prefs, boveda),
        prefs,
        boveda,
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
        assertNull(sesion.tokenActual())
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
        assertNull(sesion.tokenActual())
        assertEquals(0, boveda.borrados)
    }

    // Huella

    private fun PerfilViewModel.abrirHuellaCon(pin: String) {
        abrirActivacionHuella()
        onPinHuella(pin)
    }

    private fun loginOk() {
        api.loginResp = { TokenDTO("jwt-2", null, null) }
    }

    @Test
    fun `huellaActivada sigue a la boveda`() {
        val vm = vm()
        val job = CoroutineScope(mainRule.dispatcher).launch { vm.huellaActivada.collect {} }
        assertFalse(vm.huellaActivada.value)
        boveda.configurar("3001234567", "482915")
        assertTrue(vm.huellaActivada.value)
        job.cancel()
    }

    @Test
    fun `abrir y cerrar la activacion de huella`() {
        val vm = vm()
        assertNull(vm.activacionHuella.value)
        vm.abrirActivacionHuella()
        assertEquals(ActivacionHuellaUiState(), vm.activacionHuella.value)
        vm.onPinHuella("12")
        assertEquals("12", vm.activacionHuella.value!!.pin)
        vm.cerrarActivacionHuella()
        assertNull(vm.activacionHuella.value)
    }

    @Test
    fun `verificarPinHuella con PIN incompleto no llama a la API`() = runBlocking {
        val vm = vm()
        vm.abrirHuellaCon("4829")
        assertNull(vm.verificarPinHuella())
        assertEquals("Escribe los 6 dígitos de tu PIN.", vm.activacionHuella.value!!.error)
        assertTrue(api.logins.isEmpty())
    }

    @Test
    fun `verificarPinHuella sin dialogo abierto devuelve null`() = runBlocking {
        assertNull(vm().verificarPinHuella())
        assertTrue(api.logins.isEmpty())
    }

    @Test
    fun `verificarPinHuella con 401 dice que el PIN no es correcto y limpia el PIN`() = runBlocking {
        api.loginResp = { throw httpError(401, problem(401)) }
        val vm = vm()
        vm.abrirHuellaCon("000000")
        assertNull(vm.verificarPinHuella())
        val s = vm.activacionHuella.value!!
        assertEquals("El PIN no es correcto.", s.error)
        assertEquals("", s.pin)
        assertFalse(s.verificando)
        vm.guardarHuella(FakeBovedaPin.cifrador) // sin PIN pendiente no guarda
        assertNull(boveda.pinGuardado)
    }

    @Test
    fun `verificarPinHuella con otro error usa el mensaje del login`() = runBlocking {
        api.loginResp = { throw httpError(423, problem(423)) }
        val vm = vm()
        vm.abrirHuellaCon("482915")
        assertNull(vm.verificarPinHuella())
        assertTrue(vm.activacionHuella.value!!.error!!.startsWith("Tu cuenta está bloqueada"))
    }

    @Test
    fun `verificarPinHuella OK usa el telefono del usuario y devuelve el cifrador`() = runBlocking {
        loginOk()
        val vm = vm()
        vm.abrirHuellaCon("482915")
        assertEquals(FakeBovedaPin.cifrador, vm.verificarPinHuella())
        assertEquals("3001234567", api.logins.single().telefono)
        assertEquals("482915", api.logins.single().pin)
        val s = vm.activacionHuella.value!!
        assertFalse(s.verificando)
        assertNull(s.error)
        assertEquals(EstadoSesion.Activa, sesion.estado.value)
    }

    @Test
    fun `verificarPinHuella sin cifrador disponible muestra error`() = runBlocking {
        loginOk()
        boveda.cifradorGuardarResp = { null }
        val vm = vm()
        vm.abrirHuellaCon("482915")
        assertNull(vm.verificarPinHuella())
        assertEquals("No pudimos preparar la huella en este teléfono.", vm.activacionHuella.value!!.error)
        vm.guardarHuella(FakeBovedaPin.cifrador)
        assertNull(boveda.pinGuardado)
    }

    @Test
    fun `guardarHuella tras verificar guarda telefono y PIN, cierra el dialogo y avisa`() = runBlocking {
        loginOk()
        val vm = vm()
        vm.abrirHuellaCon("482915")
        val cifrador = vm.verificarPinHuella()!!
        vm.guardarHuella(cifrador)
        assertEquals("3001234567", boveda.telefonoGuardado)
        assertEquals("482915", boveda.pinGuardado)
        assertNull(vm.activacionHuella.value)
        assertTrue(vm.mensaje.value!!.startsWith("Huella activada"))
        vm.mensajeMostrado()
        assertNull(vm.mensaje.value)
        vm.guardarHuella(cifrador) // el PIN pendiente ya se consumió
        assertEquals(1, boveda.cifradoresUsados.size)
    }

    @Test
    fun `guardarHuella que falla en la boveda avisa del error`() = runBlocking {
        loginOk()
        boveda.guardarResp = false
        val vm = vm()
        vm.abrirHuellaCon("482915")
        vm.guardarHuella(vm.verificarPinHuella()!!)
        assertEquals("No pudimos guardar la huella. Intenta de nuevo.", vm.mensaje.value)
        assertNull(vm.activacionHuella.value)
    }

    @Test
    fun `falloHuella null (cancelar) cierra el dialogo y descarta el PIN`() = runBlocking {
        loginOk()
        val vm = vm()
        vm.abrirHuellaCon("482915")
        val cifrador = vm.verificarPinHuella()!!
        vm.falloHuella(null)
        assertNull(vm.activacionHuella.value)
        vm.guardarHuella(cifrador)
        assertNull(boveda.pinGuardado)
    }

    @Test
    fun `falloHuella con mensaje lo muestra, limpia el PIN y descarta el pendiente`() = runBlocking {
        loginOk()
        val vm = vm()
        vm.abrirHuellaCon("482915")
        val cifrador = vm.verificarPinHuella()!!
        vm.falloHuella("Huella no reconocida.")
        val s = vm.activacionHuella.value!!
        assertEquals("Huella no reconocida.", s.error)
        assertEquals("", s.pin)
        vm.guardarHuella(cifrador)
        assertNull(boveda.pinGuardado)
    }

    @Test
    fun `desactivarHuella borra la boveda y avisa`() {
        boveda.configurar("3001234567", "482915")
        val vm = vm()
        vm.desactivarHuella()
        assertEquals(1, boveda.borrados)
        assertEquals("Huella desactivada. Entrarás con tu PIN.", vm.mensaje.value)
    }

    @Test
    fun `cambio de PIN OK borra la huella guardada`() {
        boveda.configurar("3001234567", "482915")
        api.cambiarPinResp = { Response.success(204, Unit) }
        vm().llenar()
        assertEquals(1, boveda.borrados)
        assertNull(boveda.pinGuardado)
    }
}
