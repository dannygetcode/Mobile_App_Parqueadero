package com.parqueadero.appparqueadero.data.network

import com.parqueadero.appparqueadero.support.cuerpoJson
import com.parqueadero.appparqueadero.support.gsonPrueba
import com.parqueadero.appparqueadero.support.httpError
import com.parqueadero.appparqueadero.support.respuestaError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class ErrorParserTest {

    private val parser = ErrorParser(gsonPrueba)

    @Test
    fun `desdeTexto lee detail, codigo, errores y referencia`() {
        val json = """
            {"type":"about:blank","title":"Bad Request","status":400,"detail":"Datos invalidos",
             "codigo":"VALIDACION","referencia":"REF-123",
             "errores":[{"campo":"telefono","mensaje":"formato"},{"campo":"pin","mensaje":null},{"campo":null,"mensaje":"x"}]}
        """.trimIndent()
        val e = parser.desdeTexto(400, json)
        assertEquals(400, e.status)
        assertEquals("Datos invalidos", e.detail)
        assertEquals("VALIDACION", e.codigo)
        assertEquals("REF-123", e.referencia)
        assertEquals(mapOf("telefono" to "formato", "pin" to "inválido"), e.errores)
    }

    @Test
    fun `desdeTexto con cuerpo vacio, nulo o no JSON devuelve solo el status`() {
        listOf(null, "", "   ", "<html>error</html>").forEach { texto ->
            val e = parser.desdeTexto(502, texto)
            assertEquals(AppError.Http(502), e)
        }
    }

    @Test
    fun `desdeExcepcion distingue HttpException, IOException y otros`() {
        val http = parser.desdeExcepcion(httpError(404, """{"detail":"No existe"}"""))
        assertEquals(AppError.Http(404, detail = "No existe"), http)
        assertEquals(AppError.SinConexion, parser.desdeExcepcion(IOException("sin red")))
        val otro = IllegalStateException("x")
        assertEquals(AppError.Desconocido(otro), parser.desdeExcepcion(otro))
    }

    @Test
    fun `llamar envuelve el valor o el error`() = runTest {
        assertEquals(Resultado.Ok(5), parser.llamar { 5 })
        assertEquals(Resultado.Falla(AppError.SinConexion), parser.llamar<Int> { throw IOException() })
        val r = parser.llamar<Int> { throw httpError(500, """{"referencia":"R1"}""") }
        assertEquals(Resultado.Falla(AppError.Http(500, referencia = "R1")), r)
    }

    @Test
    fun `llamar propaga la cancelacion`() = runTest {
        try {
            parser.llamar<Int> { throw CancellationException("cancelada") }
            fail("Debió propagar CancellationException")
        } catch (_: CancellationException) {
        }
    }

    @Test
    fun `llamarSinCuerpo con 2xx es Ok y con error lee el ProblemDetail`() = runTest {
        assertEquals(Resultado.Ok(Unit), parser.llamarSinCuerpo { Response.success(Unit) })
        val r = parser.llamarSinCuerpo { respuestaError(409, """{"detail":"Conflicto","codigo":"YA_ACTIVA"}""") }
        assertEquals(Resultado.Falla(AppError.Http(409, detail = "Conflicto", codigo = "YA_ACTIVA")), r)
        assertEquals(Resultado.Falla(AppError.SinConexion), parser.llamarSinCuerpo { throw IOException() })
    }

    @Test
    fun `desdeCuerpo con cuerpo nulo no falla`() {
        assertEquals(AppError.Http(400), parser.desdeCuerpo(400, null))
        assertEquals(AppError.Http(400, detail = "d"), parser.desdeCuerpo(400, cuerpoJson("""{"detail":"d"}""")))
    }
}

class AppErrorMensajeTest {

    @Test
    fun `mensajes por status`() {
        assertTrue(AppError.Http(401).mensaje().contains("sesión no es válida"))
        assertTrue(AppError.Http(423).mensaje().contains("bloqueada"))
        assertTrue(AppError.Http(429).mensaje().contains("Demasiados intentos"))
        assertTrue(AppError.SinConexion.mensaje().contains("conectar"))
        assertTrue(AppError.Desconocido().mensaje().contains("inesperado"))
    }

    @Test
    fun `500 con referencia la muestra y sin referencia no`() {
        val con = AppError.Http(500, referencia = "ABC-42").mensaje()
        assertTrue(con.contains("ABC-42"))
        val sin = AppError.Http(503).mensaje()
        assertEquals("El servidor no está disponible en este momento. Intenta más tarde.", sin)
    }

    @Test
    fun `otros status usan detail y si esta vacio un generico con el codigo`() {
        assertEquals("Placa repetida", AppError.Http(409, detail = "Placa repetida").mensaje())
        assertTrue(AppError.Http(404, detail = "  ").mensaje().contains("código 404"))
        assertTrue(AppError.Http(400).mensaje().contains("código 400"))
    }

    @Test
    fun `status y map`() {
        assertEquals(403, AppError.Http(403).status)
        assertNull(AppError.SinConexion.status)
        assertEquals(Resultado.Ok(4), Resultado.Ok(2).map { it * 2 })
        val falla: Resultado<Int> = Resultado.Falla(AppError.SinConexion)
        assertEquals(falla, falla.map { it * 2 })
    }
}

class AuthInterceptorTest {

    @Test
    fun `rutas publicas`() {
        assertTrue(AuthInterceptor.esPublica("/api/auth/login"))
        assertTrue(AuthInterceptor.esPublica("/api/auth/activar"))
        assertTrue(AuthInterceptor.esPublica("/api/legal/aviso-privacidad"))
        assertTrue(AuthInterceptor.esPublica("/api/ping"))
    }

    @Test
    fun `rutas protegidas`() {
        listOf("/api/usuarios/yo", "/api/usuarios/yo/pin", "/api/puerta", "/api/pagos", "/api/camaras")
            .forEach { assertTrue(it, !AuthInterceptor.esPublica(it)) }
    }
}
