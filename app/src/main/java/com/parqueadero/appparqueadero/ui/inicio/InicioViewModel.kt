package com.parqueadero.appparqueadero.ui.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.parqueadero.appparqueadero.container
import com.parqueadero.appparqueadero.data.model.EventoAccesoDTO
import com.parqueadero.appparqueadero.data.model.MotivoAcceso
import com.parqueadero.appparqueadero.data.model.PuertaDTO
import com.parqueadero.appparqueadero.data.model.UsuarioDTO
import com.parqueadero.appparqueadero.data.network.AppError
import com.parqueadero.appparqueadero.data.network.Resultado
import com.parqueadero.appparqueadero.data.network.mensaje
import com.parqueadero.appparqueadero.data.repository.AccesoRepository
import com.parqueadero.appparqueadero.data.repository.ResultadoPuerta
import com.parqueadero.appparqueadero.data.repository.UsuarioRepository
import com.parqueadero.appparqueadero.ui.common.UiState
import com.parqueadero.appparqueadero.ui.common.aUiState
import com.parqueadero.appparqueadero.utils.aEpochMillisONull
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado de la puerta para el DoorControl. `intento` cambia en cada respuesta para repetir animaciones. */
sealed interface PuertaUiState {
    data class Cerrada(val aviso: String? = null) : PuertaUiState
    data object Enviando : PuertaUiState
    data class Abierta(
        val intento: Int,
        val evento: EventoAccesoDTO?,
        val duracionMs: Long,
        val aviso: String?,
    ) : PuertaUiState
    data class Denegada(val intento: Int, val motivo: MotivoAcceso?) : PuertaUiState
    data class Fallo(val intento: Int, val mensaje: String, val reintentable: Boolean) : PuertaUiState
}

class InicioViewModel(
    private val usuarios: UsuarioRepository,
    private val accesos: AccesoRepository,
    private val ahora: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _cuenta = MutableStateFlow<UiState<UsuarioDTO>>(UiState.Loading)
    val cuenta: StateFlow<UiState<UsuarioDTO>> = _cuenta.asStateFlow()

    private val _refrescando = MutableStateFlow(false)
    val refrescando: StateFlow<Boolean> = _refrescando.asStateFlow()

    private val _puerta = MutableStateFlow<PuertaUiState>(PuertaUiState.Cerrada())
    val puerta: StateFlow<PuertaUiState> = _puerta.asStateFlow()

    private var intentos = 0
    private var cierre: Job? = null

    init {
        cargarCuenta()
    }

    fun cargarCuenta(refresco: Boolean = false) {
        if (refresco) _refrescando.value = true else _cuenta.value = UiState.Loading
        viewModelScope.launch {
            val r = usuarios.yo().aUiState()
            // En un refresco fallido se conservan los datos que ya había.
            if (!(refresco && r is UiState.Error && _cuenta.value is UiState.Success)) _cuenta.value = r
            _refrescando.value = false
        }
    }

    /** Pide abrir la puerta. Solo el backend decide si es ENTRADA o SALIDA y si se permite. */
    fun abrir() {
        val actual = _puerta.value
        if (actual is PuertaUiState.Enviando || actual is PuertaUiState.Abierta) return
        _puerta.value = PuertaUiState.Enviando
        viewModelScope.launch {
            intentos++
            _puerta.value = when (val r = accesos.abrirPuerta()) {
                is Resultado.Ok -> when (val res = r.datos) {
                    is ResultadoPuerta.Permitida -> permitida(res.puerta)
                    is ResultadoPuerta.Denegada -> PuertaUiState.Denegada(intentos, res.motivo)
                }
                is Resultado.Falla -> fallo(r.error)
            }
        }
    }

    /** El usuario no puede cerrar la puerta (es del admin): solo se adelanta el cierre visual. */
    fun cierreVisual() {
        if (_puerta.value !is PuertaUiState.Abierta) return
        cierre?.cancel()
        _puerta.value = PuertaUiState.Cerrada()
    }

    /** Descarta un rechazo o error y vuelve al reposo. */
    fun descartar() {
        val actual = _puerta.value
        if (actual is PuertaUiState.Denegada || actual is PuertaUiState.Fallo) _puerta.value = PuertaUiState.Cerrada()
    }

    private fun permitida(puerta: PuertaDTO): PuertaUiState {
        val evento = puerta.evento
        val avisoDeuda = if (evento?.motivo == MotivoAcceso.SALIDA_CON_DEUDA) TextosPuerta.motivo(MotivoAcceso.SALIDA_CON_DEUDA) else null
        val duracion = duracionApertura(puerta.abiertaHasta)
        if (!puerta.abierta || duracion == null) {
            // Orden repetida en menos de 60 s: el backend devuelve el mismo evento y la puerta ya se cerró.
            return PuertaUiState.Cerrada(
                aviso = if (evento?.duplicado == true) "Ya registramos esta orden hace menos de un minuto." else avisoDeuda,
            )
        }
        cierre?.cancel()
        cierre = viewModelScope.launch {
            delay(duracion)
            if (_puerta.value is PuertaUiState.Abierta) _puerta.value = PuertaUiState.Cerrada()
        }
        return PuertaUiState.Abierta(intentos, evento, duracion, avisoDeuda)
    }

    /**
     * Tiempo que la puerta sigue abierta. Se calcula con la hora del teléfono, que puede diferir de la del
     * servidor: si el resultado no es razonable (1 a 20 s) se usan los 15 s configurados en el backend
     * (`accesos.apertura-segundos`).
     */
    private fun duracionApertura(abiertaHasta: String?): Long? {
        val hasta = abiertaHasta?.aEpochMillisONull() ?: return DURACION_POR_DEFECTO_MS
        val restante = hasta - ahora()
        return when {
            restante in 1_000..20_000 -> restante
            restante <= 0 && restante > -60_000 -> null
            else -> DURACION_POR_DEFECTO_MS
        }
    }

    private fun fallo(error: AppError): PuertaUiState.Fallo {
        val http = error as? AppError.Http
        return when {
            error is AppError.SinConexion -> PuertaUiState.Fallo(intentos, "Sin conexión con el parqueadero.", true)
            http?.status == 409 -> PuertaUiState.Fallo(
                intentos,
                http.detail ?: "No tienes un vehículo activo registrado. Comunícate con la administración.",
                false,
            )
            http != null && http.status >= 500 -> PuertaUiState.Fallo(intentos, error.mensaje(), true)
            else -> PuertaUiState.Fallo(intentos, error.mensaje(), false)
        }
    }

    companion object {
        const val DURACION_POR_DEFECTO_MS = 15_000L

        val Factory = viewModelFactory {
            initializer { InicioViewModel(container.usuarioRepository, container.accesoRepository) }
        }
    }
}
