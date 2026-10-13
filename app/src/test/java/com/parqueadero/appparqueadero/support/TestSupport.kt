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

// Etapa 2: constructores de DTO y fuente de imágenes falsa.

fun evento(
    motivo: com.parqueadero.appparqueadero.data.model.MotivoAcceso? = null,
    duplicado: Boolean? = false,
    resultado: com.parqueadero.appparqueadero.data.model.ResultadoAcceso? = com.parqueadero.appparqueadero.data.model.ResultadoAcceso.PERMITIDO,
) = EventoAccesoDTO(
    id = 1, placaLeida = "ABC123", tipo = null, tipoInferido = null, resultado = resultado, motivo = motivo,
    origen = null, observacion = null, usuarioId = 1, usuarioNombre = null, vehiculo = null, ocurridoEn = null,
    puertaAbierta = null, duplicado = duplicado,
)

fun usuario(
    estado: com.parqueadero.appparqueadero.data.model.EstadoUsuario? = com.parqueadero.appparqueadero.data.model.EstadoUsuario.ACTIVO,
    vigenteHasta: String? = null,
    nombre: String = "Ana",
) = UsuarioDTO(
    id = 1, telefono = "3001234567", nombre = nombre, apellido = null, estado = estado, suspendidoMotivo = null,
    validado = true, bloqueadoHasta = null, cupo = null, vehiculo = null, vigenteHasta = vigenteHasta,
    creadoEn = null, dadoDeBajaEn = null,
)

fun pago(
    id: Long,
    periodoInicio: String? = null,
    creadoEn: String? = null,
    estado: com.parqueadero.appparqueadero.data.model.EstadoPago? = com.parqueadero.appparqueadero.data.model.EstadoPago.APROBADO,
) = PagoDTO(
    id = id, usuarioId = 1, usuarioNombre = null, placa = null, periodoInicio = periodoInicio, periodoFin = null,
    montoEsperado = null, montoOcr = null, fechaPagoOcr = null, ocrEstado = null, montoConfirmado = null,
    estado = estado, motivoRechazo = null, observacion = null, registradoPor = null, posibleDuplicado = null,
    comprobanteUrl = null, creadoEn = creadoEn, revisadoEn = null,
)

val FIRMA_JPEG = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
val FIRMA_PNG = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 0x0D, 0x0A, 0x1A, 0x0A)

/** Imagen con la firma dada y rellena hasta [tamano] bytes. */
fun imagen(firma: ByteArray, tamano: Int): ByteArray = ByteArray(tamano).also { firma.copyInto(it) }

/** Fuente de imágenes falsa: el tipo se detecta por firma igual que la real. Registra las reducciones. */
class FakeFuenteImagen : com.parqueadero.appparqueadero.data.repository.FuenteImagen {
    val imagenes = mutableMapOf<String, ByteArray>()
    var reducirResp: (ByteArray, Long) -> ByteArray? = { _, _ -> null }
    val reducciones = mutableListOf<Long>()

    override suspend fun leer(uri: String): com.parqueadero.appparqueadero.data.repository.ImagenLeida? =
        imagenes[uri]?.let {
            com.parqueadero.appparqueadero.data.repository.ImagenLeida(it, com.parqueadero.appparqueadero.data.repository.TipoImagen.detectar(it))
        }

    override suspend fun reducir(bytes: ByteArray, maxBytes: Long): ByteArray? {
        reducciones += maxBytes
        return reducirResp(bytes, maxBytes)
    }
}
