package com.parqueadero.appparqueadero.data.repository

import com.parqueadero.appparqueadero.data.model.EventoAccesoDTO
import com.parqueadero.appparqueadero.data.model.MotivoAcceso
import com.parqueadero.appparqueadero.data.model.PuertaComandoDTO
import com.parqueadero.appparqueadero.data.model.ResultadoAcceso
import com.parqueadero.appparqueadero.data.model.TokenDTO
import com.parqueadero.appparqueadero.data.network.AppError
import com.parqueadero.appparqueadero.data.network.ErrorParser
import com.parqueadero.appparqueadero.data.network.Resultado
import com.parqueadero.appparqueadero.data.session.EstadoSesion
import com.parqueadero.appparqueadero.data.session.MotivoCierre
import com.parqueadero.appparqueadero.data.session.SesionGuardada
import com.parqueadero.appparqueadero.data.session.SessionManager
import com.parqueadero.appparqueadero.support.FakeApiService
import com.parqueadero.appparqueadero.support.FakePreferencias
import com.parqueadero.appparqueadero.support.FakeSesionStorage
import com.parqueadero.appparqueadero.support.gsonPrueba
import com.parqueadero.appparqueadero.support.httpError
import com.parqueadero.appparqueadero.support.problem
import com.parqueadero.appparqueadero.support.puerta
import com.parqueadero.appparqueadero.support.respuestaError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

private val parser = ErrorParser(gsonPrueba)

class AuthRepositoryTest {

    private val api = FakeApiService()
    private val storage = FakeSesionStorage()
    private val prefs = FakePreferencias()

    private fun TestScope.repo(): Pair<AuthRepository, SessionManager> {
        val sm = SessionManager(storage, this)
        return AuthRepository(api, parser, sm, prefs) to sm
    }

    @Test
    fun `login OK guarda token cifrable, recuerda telefono y activa la sesion`() = runTest {
        val (repo, sm) = repo()
        api.loginResp = { TokenDTO("jwt-1", "USUARIO", "2026-10-14T10:00:00-05:00") }
        val r = repo.login("3001234567", "482915")
        assertEquals(Resultado.Ok(Unit), r)
        assertEquals(EstadoSesion.Activa, sm.estado.value)
        assertEquals("jwt-1", sm.tokenActual())
        assertEquals(SesionGuardada("jwt-1", java.time.OffsetDateTime.parse("2026-10-14T15:00:00Z").toInstant().toEpochMilli()), storage.guardada)
        assertEquals("3001234567", prefs.telefonoRecordado.first())
        assertEquals("482915", api.logins.single().pin)
    }

    @Test
    fun `login con expiraEn nulo o invalido guarda sin vencimiento`() = runTest {
        val (repo, _) = repo()
        api.loginResp = { TokenDTO("jwt", null, "no-es-fecha") }
        repo.login("3001234567", "482915")
        assertNull(storage.guardada?.expiraEnMillis)
    }

    @Test
    fun `login 401 no inicia sesion ni recuerda telefono`() = runTest {
        val (repo, sm) = repo()
        api.loginResp = { throw httpError(401, problem(401, "Credenciales invalidas")) }
        val r = repo.login("3001234567", "000001")
        assertEquals(401, ((r as Resultado.Falla).error as AppError.Http).status)
        assertEquals(EstadoSesion.Cargando, sm.estado.value)
        assertNull(storage.guardada)
        assertNull(prefs.telefonoRecordado.first())
    }

    @Test
    fun `activar envia consentimiento y recuerda telefono solo si funciona`() = runTest {
        val (repo, _) = repo()
        api.activarResp = { Response.success(Unit) }
        assertEquals(Resultado.Ok(Unit), repo.activar("3001234567", "ABCD1234", "482915", "v2"))
        val dto = api.activaciones.single()
        assertTrue(dto.aceptaTratamientoDatos)
        assertEquals("v2", dto.versionConsentimiento)
        assertEquals("3001234567", prefs.telefonoRecordado.first())

        val prefs2 = FakePreferencias()
        val repo2 = AuthRepository(api, parser, SessionManager(storage, this), prefs2)
        api.activarResp = { respuestaError(401, problem(401)) }
        assertTrue(repo2.activar("3009999999", "X", "482915", "v2") is Resultado.Falla)
        assertNull(prefs2.telefonoRecordado.first())
    }

    @Test
    fun `cerrarSesion deja Inactiva por usuario`() = runTest {
        val (repo, sm) = repo()
        sm.iniciar("t", null)
        repo.cerrarSesion()
        assertEquals(EstadoSesion.Inactiva(MotivoCierre.CERRADA_POR_USUARIO), sm.estado.value)
    }

    @Test
    fun `avisoPrivacidad sin conexion`() = runTest {
        val (repo, _) = repo()
        api.avisoResp = { throw IOException() }
        assertEquals(Resultado.Falla(AppError.SinConexion), repo.avisoPrivacidad())
    }
}

class UsuarioRepositoryTest {

    private val api = FakeApiService()
    private val storage = FakeSesionStorage()

    @Test
    fun `cambiarPin OK cierra la sesion por PIN_CAMBIADO`() = runTest {
        val sm = SessionManager(storage, this).apply { iniciar("t", null) }
        api.cambiarPinResp = { Response.success(Unit) }
        val r = UsuarioRepository(api, parser, sm).cambiarPin("482915", "739164")
        assertEquals(Resultado.Ok(Unit), r)
        assertEquals(EstadoSesion.Inactiva(MotivoCierre.PIN_CAMBIADO), sm.estado.value)
        assertNull(storage.guardada)
        assertEquals("739164", api.cambiosPin.single().pinNuevo)
    }

    @Test
    fun `cambiarPin con error mantiene la sesion`() = runTest {
        val sm = SessionManager(storage, this).apply { iniciar("t", null) }
        api.cambiarPinResp = { respuestaError(400, problem(400, "PIN actual incorrecto")) }
        val r = UsuarioRepository(api, parser, sm).cambiarPin("000000", "739164")
        assertEquals(Resultado.Falla(AppError.Http(400, detail = "PIN actual incorrecto")), r)
        assertEquals(EstadoSesion.Activa, sm.estado.value)
    }

    @Test
    fun `tarifas vacias es Ok con lista vacia`() = runTest {
        api.tarifasResp = { emptyList() }
        val sm = SessionManager(storage, this)
        assertEquals(Resultado.Ok(emptyList<Any>()), UsuarioRepository(api, parser, sm).tarifas())
    }
}

class AccesoRepositoryTest {

    private val api = FakeApiService()
    private val repo = AccesoRepository(api, parser, gsonPrueba)

    private fun evento(motivo: MotivoAcceso?) = EventoAccesoDTO(
        id = 1, placaLeida = "ABC123", tipo = null, tipoInferido = null, resultado = ResultadoAcceso.DENEGADO,
        motivo = motivo, origen = null, observacion = null, usuarioId = null, usuarioNombre = null,
        vehiculo = null, ocurridoEn = null, puertaAbierta = false, duplicado = false,
    )

    @Test
    fun `200 es Permitida y envia abierta=true`() = runTest {
        api.abrirPuertaResp = { Response.success(puerta(abierta = true)) }
        val r = repo.abrirPuerta()
        assertEquals(Resultado.Ok(ResultadoPuerta.Permitida(puerta(abierta = true))), r)
        assertEquals(PuertaComandoDTO(abierta = true), api.comandosPuerta.single())
    }

    @Test
    fun `403 con PuertaDTO es Denegada con el motivo`() = runTest {
        val cuerpo = gsonPrueba.toJson(puerta(abierta = false, evento = evento(MotivoAcceso.USUARIO_VENCIDO)))
        api.abrirPuertaResp = { respuestaError(403, cuerpo) }
        val r = repo.abrirPuerta() as Resultado.Ok
        val denegada = r.datos as ResultadoPuerta.Denegada
        assertEquals(MotivoAcceso.USUARIO_VENCIDO, denegada.motivo)
        assertEquals(false, denegada.puerta.abierta)
    }

    @Test
    fun `403 con motivo desconocido es Denegada con motivo nulo`() = runTest {
        val cuerpo = """{"abierta":false,"evento":{"id":1,"motivo":"NUEVO_MOTIVO"}}"""
        api.abrirPuertaResp = { respuestaError(403, cuerpo) }
        val denegada = (repo.abrirPuerta() as Resultado.Ok).datos as ResultadoPuerta.Denegada
        assertNull(denegada.motivo)
    }

    @Test
    fun `403 con ProblemDetail es Falla Http 403`() = runTest {
        api.abrirPuertaResp = { respuestaError(403, problem(403, "Rol no permitido", "PROHIBIDO")) }
        assertEquals(
            Resultado.Falla(AppError.Http(403, detail = "Rol no permitido", codigo = "PROHIBIDO")),
            repo.abrirPuerta(),
        )
    }

    @Test
    fun `409 es Falla con el detail`() = runTest {
        api.abrirPuertaResp = { respuestaError(409, problem(409, "La puerta ya esta abierta")) }
        assertEquals(Resultado.Falla(AppError.Http(409, detail = "La puerta ya esta abierta")), repo.abrirPuerta())
    }

    @Test
    fun `sin conexion es Falla SinConexion`() = runTest {
        api.abrirPuertaResp = { throw IOException() }
        assertEquals(Resultado.Falla(AppError.SinConexion), repo.abrirPuerta())
    }

    @Test
    fun `camaras con 401 es Falla Http 401`() = runTest {
        api.camarasResp = { throw httpError(401) }
        assertEquals(Resultado.Falla(AppError.Http(401)), repo.camaras())
    }
}

class PagoRepositoryTest {

    private val api = FakeApiService()
    private val repo = PagoRepository(api, parser)

    @Test
    fun `subirComprobante usa la parte comprobante con extension y tipo segun mime`() = runTest {
        api.subirPagoResp = { throw httpError(413, problem(413, "Archivo muy grande")) }
        val r = repo.subirComprobante(byteArrayOf(1, 2, 3), PagoRepository.MIME_PNG)
        assertEquals(Resultado.Falla(AppError.Http(413, detail = "Archivo muy grande")), r)
        val parte = api.partesSubidas.single()
        val disposicion = parte.headers!!["Content-Disposition"]!!
        assertTrue(disposicion, disposicion.contains("name=\"comprobante\""))
        assertTrue(disposicion, disposicion.contains("filename=\"comprobante.png\""))
        assertEquals("image/png", parte.body.contentType().toString())
        assertEquals(3L, parte.body.contentLength())
        val buffer = Buffer().also { parte.body.writeTo(it) }
        assertEquals(listOf<Byte>(1, 2, 3), buffer.readByteArray().toList())
    }

    @Test
    fun `subirComprobante jpeg usa extension jpg`() = runTest {
        api.subirPagoResp = { throw IOException() }
        repo.subirComprobante(byteArrayOf(9), PagoRepository.MIME_JPEG)
        val disposicion = api.partesSubidas.single().headers!!["Content-Disposition"]!!
        assertTrue(disposicion.contains("filename=\"comprobante.jpg\""))
    }

    @Test
    fun `misPagos 404 y limite de tamano`() = runTest {
        api.pagoResp = { throw httpError(404, problem(404, "Pago no encontrado")) }
        assertEquals(Resultado.Falla(AppError.Http(404, detail = "Pago no encontrado")), repo.pago(99))
        assertEquals(5_242_880L, PagoRepository.TAMANO_MAXIMO_BYTES)
    }
}
