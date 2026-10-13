package com.parqueadero.appparqueadero.ui.pagos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.parqueadero.appparqueadero.container
import com.parqueadero.appparqueadero.data.model.EstadoPago
import com.parqueadero.appparqueadero.data.model.PagoDTO
import com.parqueadero.appparqueadero.data.model.ProximoPeriodoDTO
import com.parqueadero.appparqueadero.data.network.AppError
import com.parqueadero.appparqueadero.data.network.Resultado
import com.parqueadero.appparqueadero.data.network.mensaje
import com.parqueadero.appparqueadero.data.repository.FuenteImagen
import com.parqueadero.appparqueadero.data.repository.PagoRepository
import com.parqueadero.appparqueadero.ui.common.UiState
import com.parqueadero.appparqueadero.ui.common.aUiState
import com.parqueadero.appparqueadero.utils.aLocalDateONull
import com.parqueadero.appparqueadero.utils.aOffsetDateTimeONull
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GrupoPagos(val anio: Int, val pagos: List<PagoDTO>)

data class Comprobante(val uri: String, val tamanoBytes: Int, val mime: String)

data class PagosUiState(
    val periodo: UiState<ProximoPeriodoDTO> = UiState.Loading,
    val historial: UiState<List<GrupoPagos>> = UiState.Loading,
    val refrescando: Boolean = false,
    val comprobante: Comprobante? = null,
    val leyendo: Boolean = false,
    val subiendo: Boolean = false,
    val errorComprobante: String? = null,
    /** Mensaje de una sola vez (se muestra en un snackbar y se consume). */
    val mensaje: String? = null,
) {
    /** El backend admite un solo pago PENDIENTE a la vez. */
    val hayPendiente: Boolean
        get() = (historial as? UiState.Success)?.data.orEmpty()
            .any { g -> g.pagos.any { it.estado == EstadoPago.PENDIENTE } }
}

class PagosViewModel(
    private val pagos: PagoRepository,
    private val fuente: FuenteImagen,
) : ViewModel() {

    private val _estado = MutableStateFlow(PagosUiState())
    val estado: StateFlow<PagosUiState> = _estado.asStateFlow()

    private var bytesComprobante: ByteArray? = null

    init {
        cargar()
    }

    fun cargar(refresco: Boolean = false) {
        _estado.update {
            if (refresco) it.copy(refrescando = true) else it.copy(periodo = UiState.Loading, historial = UiState.Loading)
        }
        viewModelScope.launch {
            val periodo = async { pagos.proximoPeriodo().aUiState() }
            val historial = async {
                when (val r = pagos.misPagos()) {
                    is Resultado.Ok -> Resultado.Ok(agrupar(r.datos))
                    is Resultado.Falla -> r
                }.aUiState { it.isEmpty() }
            }
            _estado.update { it.copy(periodo = periodo.await(), historial = historial.await(), refrescando = false) }
        }
    }

    /** Lee y valida la imagen elegida (cámara o galería) antes de permitir subirla. */
    fun elegirComprobante(uri: String) {
        _estado.update { it.copy(leyendo = true, errorComprobante = null) }
        viewModelScope.launch {
            val imagen = fuente.leer(uri)
            val mime = imagen?.mime
            when {
                imagen == null -> rechazar("No pudimos leer la imagen. Intenta con otra.")
                mime == null -> rechazar("Solo se aceptan fotos JPEG o PNG.")
                imagen.bytes.size <= PagoRepository.TAMANO_MAXIMO_BYTES -> aceptar(uri, imagen.bytes, mime)
                else -> {
                    // Fotos de cámara de alta resolución: se reducen a JPEG antes de rechazarlas.
                    val reducida = fuente.reducir(imagen.bytes, PagoRepository.TAMANO_MAXIMO_BYTES)
                    if (reducida == null) {
                        rechazar("La imagen pesa más de 5 MB y no pudimos reducirla. Elige otra.")
                    } else {
                        aceptar(uri, reducida, PagoRepository.MIME_JPEG)
                    }
                }
            }
        }
    }

    private fun aceptar(uri: String, bytes: ByteArray, mime: String) {
        bytesComprobante = bytes
        _estado.update { it.copy(leyendo = false, comprobante = Comprobante(uri, bytes.size, mime)) }
    }

    fun quitarComprobante() {
        bytesComprobante = null
        _estado.update { it.copy(comprobante = null, errorComprobante = null) }
    }

    fun subir() {
        val s = _estado.value
        val comprobante = s.comprobante ?: return
        val bytes = bytesComprobante ?: return
        if (s.subiendo) return
        _estado.update { it.copy(subiendo = true, errorComprobante = null) }
        viewModelScope.launch {
            when (val r = pagos.subirComprobante(bytes, comprobante.mime)) {
                is Resultado.Ok -> {
                    bytesComprobante = null
                    _estado.update {
                        it.copy(
                            subiendo = false,
                            comprobante = null,
                            mensaje = "Comprobante enviado. Queda pendiente de aprobación.",
                        )
                    }
                    cargar(refresco = true)
                }
                is Resultado.Falla -> _estado.update {
                    it.copy(subiendo = false, errorComprobante = mensajeSubida(r.error))
                }
            }
        }
    }

    fun mensajeMostrado() = _estado.update { it.copy(mensaje = null) }

    private fun rechazar(mensaje: String) {
        bytesComprobante = null
        _estado.update { it.copy(leyendo = false, comprobante = null, errorComprobante = mensaje) }
    }

    companion object {
        fun mensajeSubida(error: AppError): String = when ((error as? AppError.Http)?.status) {
            409 -> (error as AppError.Http).detail
                ?: "Ya tienes un pago pendiente de revisión o este comprobante ya se había enviado."
            413 -> "La imagen supera el máximo de 5 MB."
            415 -> "El archivo no es una imagen JPEG o PNG válida."
            else -> error.mensaje()
        }

        /** Agrupa por año del periodo (o de la fecha de envío), del más reciente al más antiguo. */
        fun agrupar(lista: List<PagoDTO>): List<GrupoPagos> {
            fun anio(p: PagoDTO) = p.periodoInicio?.aLocalDateONull()?.year
                ?: p.creadoEn?.aOffsetDateTimeONull()?.year
                ?: 0
            fun orden(p: PagoDTO) = p.periodoInicio ?: p.creadoEn ?: ""
            return lista
                .sortedByDescending { orden(it) }
                .groupBy { anio(it) }
                .map { (anio, pagos) -> GrupoPagos(anio, pagos) }
                .sortedByDescending { it.anio }
        }

        val Factory = viewModelFactory {
            initializer { PagosViewModel(container.pagoRepository, container.fuenteImagen) }
        }
    }
}
