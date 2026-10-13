package com.parqueadero.appparqueadero.support

import com.google.gson.Gson
import com.parqueadero.appparqueadero.data.model.ActivacionDTO
import com.parqueadero.appparqueadero.data.model.AvisoPrivacidadDTO
import com.parqueadero.appparqueadero.data.model.CamaraDTO
import com.parqueadero.appparqueadero.data.model.CambioPinDTO
import com.parqueadero.appparqueadero.data.model.EventoAccesoDTO
import com.parqueadero.appparqueadero.data.model.LoginDTO
import com.parqueadero.appparqueadero.data.model.PagoDTO
import com.parqueadero.appparqueadero.data.model.ProximoPeriodoDTO
import com.parqueadero.appparqueadero.data.model.PuertaComandoDTO
import com.parqueadero.appparqueadero.data.model.PuertaDTO
import com.parqueadero.appparqueadero.data.model.ResumenUsuarioDTO
import com.parqueadero.appparqueadero.data.model.TarifaDTO
import com.parqueadero.appparqueadero.data.model.TokenDTO
import com.parqueadero.appparqueadero.data.model.UsuarioDTO
import com.parqueadero.appparqueadero.data.network.ApiService
import com.parqueadero.appparqueadero.data.session.Preferencias
import com.parqueadero.appparqueadero.data.session.SesionGuardada
import com.parqueadero.appparqueadero.data.session.SesionStorage
import com.parqueadero.appparqueadero.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import retrofit2.HttpException
import retrofit2.Response

val gsonPrueba = Gson()

fun cuerpoJson(json: String): ResponseBody = json.toResponseBody("application/problem+json".toMediaType())

fun <T> respuestaError(status: Int, json: String = ""): Response<T> = Response.error(status, cuerpoJson(json))

fun httpError(status: Int, json: String = ""): HttpException = HttpException(respuestaError<Any>(status, json))

fun problem(status: Int, detail: String? = null, codigo: String? = null, extra: String = ""): String {
    val partes = mutableListOf("\"status\":$status")
    detail?.let { partes += "\"detail\":\"$it\"" }
    codigo?.let { partes += "\"codigo\":\"$it\"" }
    if (extra.isNotBlank()) partes += extra
    return "{" + partes.joinToString(",") + "}"
}

private fun noConfigurado(): Nothing = throw AssertionError("Respuesta no configurada en FakeApiService")

/** API falsa: cada test asigna la respuesta de las rutas que usa. Registra lo enviado. */
class FakeApiService : ApiService {
    var avisoResp: suspend () -> AvisoPrivacidadDTO = { noConfigurado() }
    var activarResp: suspend (ActivacionDTO) -> Response<Unit> = { noConfigurado() }
    var loginResp: suspend (LoginDTO) -> TokenDTO = { noConfigurado() }
    var yoResp: suspend () -> UsuarioDTO = { noConfigurado() }
    var cambiarPinResp: suspend (CambioPinDTO) -> Response<Unit> = { noConfigurado() }
    var resumenResp: suspend () -> ResumenUsuarioDTO = { noConfigurado() }
    var tarifasResp: suspend () -> List<TarifaDTO> = { noConfigurado() }
    var proximoPeriodoResp: suspend () -> ProximoPeriodoDTO = { noConfigurado() }
    var subirPagoResp: suspend (MultipartBody.Part) -> PagoDTO = { noConfigurado() }
    var misPagosResp: suspend () -> List<PagoDTO> = { noConfigurado() }
    var pagoResp: suspend (Long) -> PagoDTO = { noConfigurado() }
    var misAccesosResp: suspend () -> List<EventoAccesoDTO> = { noConfigurado() }
    var puertaResp: suspend () -> PuertaDTO = { noConfigurado() }
    var abrirPuertaResp: suspend (PuertaComandoDTO) -> Response<PuertaDTO> = { noConfigurado() }
    var camarasResp: suspend () -> List<CamaraDTO> = { noConfigurado() }

    val activaciones = mutableListOf<ActivacionDTO>()
    val logins = mutableListOf<LoginDTO>()
    val cambiosPin = mutableListOf<CambioPinDTO>()
    val comandosPuerta = mutableListOf<PuertaComandoDTO>()
    val partesSubidas = mutableListOf<MultipartBody.Part>()

    override suspend fun avisoPrivacidad() = avisoResp()
    override suspend fun activar(dto: ActivacionDTO): Response<Unit> { activaciones += dto; return activarResp(dto) }
    override suspend fun login(dto: LoginDTO): TokenDTO { logins += dto; return loginResp(dto) }
    override suspend fun yo() = yoResp()
    override suspend fun cambiarPin(dto: CambioPinDTO): Response<Unit> { cambiosPin += dto; return cambiarPinResp(dto) }
    override suspend fun resumen() = resumenResp()
    override suspend fun tarifas() = tarifasResp()
    override suspend fun proximoPeriodo() = proximoPeriodoResp()
    override suspend fun subirPago(comprobante: MultipartBody.Part): PagoDTO { partesSubidas += comprobante; return subirPagoResp(comprobante) }
    override suspend fun misPagos() = misPagosResp()
    override suspend fun pago(id: Long) = pagoResp(id)
    override suspend fun misAccesos() = misAccesosResp()
    override suspend fun puerta() = puertaResp()
    override suspend fun abrirPuerta(dto: PuertaComandoDTO): Response<PuertaDTO> { comandosPuerta += dto; return abrirPuertaResp(dto) }
    override suspend fun camaras() = camarasResp()
}

class FakeSesionStorage(var guardada: SesionGuardada? = null) : SesionStorage {
    var borrados = 0
    override suspend fun leer() = guardada
    override suspend fun guardar(sesion: SesionGuardada) { guardada = sesion }
    override suspend fun borrar() { borrados++; guardada = null }
}

class FakePreferencias(telefono: String? = null) : Preferencias {
    val temaFlow = MutableStateFlow(ThemeMode.SISTEMA)
    val telefonoFlow = MutableStateFlow(telefono)
    override val tema: Flow<ThemeMode> = temaFlow
    override suspend fun guardarTema(modo: ThemeMode) { temaFlow.value = modo }
    override val telefonoRecordado: Flow<String?> = telefonoFlow
    override suspend fun recordarTelefono(telefono: String) { telefonoFlow.value = telefono }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

fun puerta(abierta: Boolean = true, evento: EventoAccesoDTO? = null) =
    PuertaDTO(abierta = abierta, abiertaHasta = null, ultimoEvento = null, evento = evento)
