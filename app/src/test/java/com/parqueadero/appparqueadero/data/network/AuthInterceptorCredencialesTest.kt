package com.parqueadero.appparqueadero.data.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthInterceptorCredencialesTest {

    @Test
    fun `PUT de cambio de PIN es 401 de credenciales`() {
        assertTrue(AuthInterceptor.es401DeCredenciales("PUT", "/api/usuarios/yo/pin"))
    }

    @Test
    fun `otros metodos o rutas no son 401 de credenciales`() {
        assertFalse(AuthInterceptor.es401DeCredenciales("GET", "/api/usuarios/yo/pin"))
        assertFalse(AuthInterceptor.es401DeCredenciales("PUT", "/api/usuarios/yo"))
        assertFalse(AuthInterceptor.es401DeCredenciales("GET", "/api/usuarios/yo"))
        assertFalse(AuthInterceptor.es401DeCredenciales("POST", "/api/puerta"))
        assertFalse(AuthInterceptor.es401DeCredenciales("PUT", "/api/usuarios/yo/pin/otro"))
    }
}
