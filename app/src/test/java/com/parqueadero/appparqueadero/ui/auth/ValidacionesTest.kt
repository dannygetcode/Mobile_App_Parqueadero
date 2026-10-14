package com.parqueadero.appparqueadero.ui.auth

import com.parqueadero.appparqueadero.data.network.AppError
import com.parqueadero.appparqueadero.data.network.Resultado
import com.parqueadero.appparqueadero.ui.common.UiState
import com.parqueadero.appparqueadero.ui.common.aUiState
import com.parqueadero.appparqueadero.utils.aEpochMillisONull
import com.parqueadero.appparqueadero.utils.aLocalDateONull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ValidacionesTest {

    @Test
    fun `normalizarTelefono quita espacios, guiones y parentesis`() {
        assertEquals("+573001234567", Validaciones.normalizarTelefono("+57 (300) 123-4567"))
        assertEquals("", Validaciones.normalizarTelefono(" -() "))
    }

    @Test
    fun `telefono valido entre 7 y 15 digitos con + opcional al inicio`() {
        assertTrue(Validaciones.telefonoValido("1234567"))
        assertTrue(Validaciones.telefonoValido("123456789012345"))
        assertTrue(Validaciones.telefonoValido("+573001234567"))
        assertFalse(Validaciones.telefonoValido("123456"))
        assertFalse(Validaciones.telefonoValido("1234567890123456"))
        assertFalse(Validaciones.telefonoValido("300+1234567"))
        assertFalse(Validaciones.telefonoValido(""))
    }

    @Test
    fun `errorTelefono vacio, invalido y valido`() {
        assertEquals("Escribe tu número de celular.", Validaciones.errorTelefono(""))
        assertEquals("Escribe tu número de celular.", Validaciones.errorTelefono("   "))
        assertNotNull(Validaciones.errorTelefono("12"))
        assertNull(Validaciones.errorTelefono("3001234567"))
    }

    @Test
    fun `normalizarCodigo deja alfanumericos en mayuscula hasta 20`() {
        assertEquals("ABCD1234", Validaciones.normalizarCodigo("ab-cd 12.34"))
        assertEquals(20, Validaciones.normalizarCodigo("a".repeat(30)).length)
    }

    @Test
    fun `pinCompleto exige 6 digitos`() {
        assertTrue(Validaciones.pinCompleto("482915"))
        assertFalse(Validaciones.pinCompleto("48291"))
        assertFalse(Validaciones.pinCompleto("4829150"))
        assertFalse(Validaciones.pinCompleto("48a915"))
        assertFalse(Validaciones.pinCompleto(""))
    }

    @Test
    fun `pinTrivial detecta repetidos y consecutivos`() {
        listOf("000000", "999999", "123456", "654321", "456789").forEach {
            assertTrue(it, Validaciones.pinTrivial(it))
        }
        listOf("482915", "123457", "112233", "12345").forEach {
            assertFalse(it, Validaciones.pinTrivial(it))
        }
    }

    @Test
    fun `errorPinNuevo`() {
        assertEquals("El PIN debe tener 6 dígitos.", Validaciones.errorPinNuevo("123"))
        assertTrue(Validaciones.errorPinNuevo("111111")!!.contains("menos predecible"))
        assertNull(Validaciones.errorPinNuevo("482915"))
    }
}

class UiStateTest {

    @Test
    fun `aUiState convierte Ok, vacio y Falla`() {
        assertEquals(UiState.Success(listOf(1)), Resultado.Ok(listOf(1)).aUiState { it.isEmpty() })
        assertEquals(UiState.Empty, Resultado.Ok(emptyList<Int>()).aUiState { it.isEmpty() })
        val error = Resultado.Falla(AppError.Http(500, referencia = "R9")).aUiState()
        assertTrue(error is UiState.Error && error.mensaje.contains("R9") && error.error == AppError.Http(500, referencia = "R9"))
    }
}

class FechasTest {

    @Test
    fun `interpreta instantes y fechas ISO o devuelve null`() {
        assertEquals(0L, "1970-01-01T00:00:00Z".aEpochMillisONull())
        assertEquals(3_600_000L, "1970-01-01T00:00:00-01:00".aEpochMillisONull())
        assertNull("ayer".aEpochMillisONull())
        assertEquals(LocalDate.of(2024, 2, 29), "2024-02-29".aLocalDateONull())
        assertNull("2023-02-29".aLocalDateONull())
    }
}
