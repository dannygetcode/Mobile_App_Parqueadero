package com.parqueadero.appparqueadero.ui.auth.activar

import com.parqueadero.appparqueadero.data.model.AvisoPrivacidadDTO
import com.parqueadero.appparqueadero.data.network.ErrorParser
import com.parqueadero.appparqueadero.data.repository.AuthRepository
import com.parqueadero.appparqueadero.data.session.SessionManager
import com.parqueadero.appparqueadero.support.FakeApiService
import com.parqueadero.appparqueadero.support.FakePreferencias
import com.parqueadero.appparqueadero.support.FakeBovedaPin
import com.parqueadero.appparqueadero.support.MainDispatcherRule
import com.parqueadero.appparqueadero.support.gsonPrueba
import com.parqueadero.appparqueadero.support.problem
import com.parqueadero.appparqueadero.support.respuestaError
import com.parqueadero.appparqueadero.ui.common.UiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class ActivarViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val api = FakeApiService().apply { avisoResp = { AvisoPrivacidadDTO("2026-01", "Texto del aviso") } }
    private val prefs = FakePreferencias()
    private val sesion = SessionManager { 0L }

    private fun vm() = ActivarViewModel(AuthRepository(api, ErrorParser(gsonPrueba), sesion, prefs, FakeBovedaPin()))

    private fun ActivarViewModel.hastaConsentimiento() {
        onTelefono("300 123 4567"); onCodigo("abcd-1234"); continuar()
        onPin("482915"); onConfirmacion("482915"); continuar()
        assertEquals(PasoActivacion.CONSENTIMIENTO, estado.value.paso)
    }

    @Test
    fun `carga el aviso al iniciar`() {
        assertEquals(UiState.Success(AvisoPrivacidadDTO("2026-01", "Texto del aviso")), vm().estado.value.aviso)
    }

    @Test
    fun `DATOS no avanza con telefono o codigo vacios`() {
        val vm = vm()
        vm.continuar()
        val s = vm.estado.value
        assertEquals(PasoActivacion.DATOS, s.paso)
        assertNotNull(s.errorTelefono)
        assertNotNull(s.errorCodigo)
        vm.onTelefono("123")
        vm.onCodigo("X1")
        vm.continuar()
        assertEquals(PasoActivacion.DATOS, vm.estado.value.paso)
        assertNotNull(vm.estado.value.errorTelefono)
        assertNull(vm.estado.value.errorCodigo)
    }

    @Test
    fun `PIN incompleto o trivial no avanza y limpia los campos`() {
        val vm = vm()
        vm.onTelefono("3001234567"); vm.onCodigo("ABCD1234"); vm.continuar()
        assertEquals(PasoActivacion.PIN, vm.estado.value.paso)
        vm.onPin("12345"); vm.onConfirmacion("12345"); vm.continuar()
        assertEquals("El PIN debe tener 6 dígitos.", vm.estado.value.errorPin)
        vm.onPin("123456"); vm.onConfirmacion("123456"); vm.continuar()
        val s = vm.estado.value
        assertEquals(PasoActivacion.PIN, s.paso)
        assertTrue(s.errorPin!!.contains("menos predecible"))
        assertEquals("", s.pin)
        assertEquals("", s.confirmacion)
    }

    @Test
    fun `confirmacion distinta no avanza`() {
        val vm = vm()
        vm.onTelefono("3001234567"); vm.onCodigo("ABCD1234"); vm.continuar()
        vm.onPin("482915"); vm.onConfirmacion("482916"); vm.continuar()
        val s = vm.estado.value
        assertEquals(PasoActivacion.PIN, s.paso)
        assertEquals("Los PIN no coinciden. Escríbelo de nuevo.", s.errorConfirmacion)
        assertEquals("482915", s.pin)
        assertEquals("", s.confirmacion)
    }

    @Test
    fun `sin consentimiento no envia`() {
        val vm = vm()
        vm.hastaConsentimiento()
        vm.continuar()
        assertEquals(PasoActivacion.CONSENTIMIENTO, vm.estado.value.paso)
        assertTrue(vm.estado.value.error!!.contains("debes aceptar"))
        assertTrue(api.activaciones.isEmpty())
    }

    @Test
    fun `sin aviso cargado no envia`() {
        api.avisoResp = { throw IOException() }
        val vm = vm()
        assertTrue(vm.estado.value.aviso is UiState.Error)
        vm.hastaConsentimiento()
        vm.onAceptaConsentimiento(true)
        vm.continuar()
        assertEquals("Primero debe cargarse el aviso de privacidad.", vm.estado.value.error)
        assertTrue(api.activaciones.isEmpty())
    }

    @Test
    fun `OK pasa a LISTO y envia la version del aviso`() {
        api.activarResp = { Response.success(Unit) }
        val vm = vm()
        vm.hastaConsentimiento()
        vm.onAceptaConsentimiento(true)
        vm.continuar()
        val s = vm.estado.value
        assertEquals(PasoActivacion.LISTO, s.paso)
        assertEquals("", s.pin)
        assertFalse(s.enviando)
        val dto = api.activaciones.single()
        assertEquals("2026-01", dto.versionConsentimiento)
        assertEquals("3001234567", dto.telefono)
        assertEquals("ABCD1234", dto.codigo)
        assertEquals("482915", dto.pin)
        assertTrue(dto.aceptaTratamientoDatos)
        assertEquals("3001234567", runBlocking { prefs.telefonoRecordado.first() })
        assertFalse(vm.atras())
    }

    @Test
    fun `401 vuelve a DATOS con error en el codigo`() {
        api.activarResp = { respuestaError(401, problem(401, "Codigo invalido")) }
        val vm = vm()
        vm.hastaConsentimiento()
        vm.onAceptaConsentimiento(true)
        vm.continuar()
        val s = vm.estado.value
        assertEquals(PasoActivacion.DATOS, s.paso)
        assertEquals("El código no es válido o ya venció. Pide uno nuevo al administrador.", s.errorCodigo)
        assertFalse(s.enviando)
    }

    @Test
    fun `400 con error en pin vuelve a PIN`() {
        api.activarResp = { respuestaError(400, problem(400, extra = "\"errores\":[{\"campo\":\"pin\",\"mensaje\":\"muy debil\"}]")) }
        val vm = vm()
        vm.hastaConsentimiento()
        vm.onAceptaConsentimiento(true)
        vm.continuar()
        assertEquals(PasoActivacion.PIN, vm.estado.value.paso)
        assertEquals("PIN: muy debil", vm.estado.value.errorPin)
    }

    @Test
    fun `400 sin campos recarga el aviso y pide aceptar de nuevo`() {
        api.activarResp = { respuestaError(400, problem(400, "Version de consentimiento desactualizada")) }
        val vm = vm()
        vm.hastaConsentimiento()
        vm.onAceptaConsentimiento(true)
        api.avisoResp = { AvisoPrivacidadDTO("2026-02", "Nuevo") }
        vm.continuar()
        val s = vm.estado.value
        assertEquals(PasoActivacion.CONSENTIMIENTO, s.paso)
        assertFalse(s.aceptaConsentimiento)
        assertEquals("2026-02", (s.aviso as UiState.Success).data.version)
    }

    @Test
    fun `atras retrocede pasos y en DATOS devuelve false`() {
        val vm = vm()
        vm.hastaConsentimiento()
        assertTrue(vm.atras())
        assertEquals(PasoActivacion.PIN, vm.estado.value.paso)
        assertTrue(vm.atras())
        assertEquals(PasoActivacion.DATOS, vm.estado.value.paso)
        assertFalse(vm.atras())
    }
}
