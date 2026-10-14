package com.parqueadero.appparqueadero.ui.auth.activar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.parqueadero.appparqueadero.container
import com.parqueadero.appparqueadero.data.model.AvisoPrivacidadDTO
import com.parqueadero.appparqueadero.data.network.AppError
import com.parqueadero.appparqueadero.data.network.Resultado
import com.parqueadero.appparqueadero.data.network.mensaje
import com.parqueadero.appparqueadero.data.repository.AuthRepository
import com.parqueadero.appparqueadero.ui.auth.Validaciones
import com.parqueadero.appparqueadero.ui.common.UiState
import com.parqueadero.appparqueadero.ui.common.aUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PasoActivacion { DATOS, PIN, CONSENTIMIENTO, LISTO }

data class ActivarUiState(
    val paso: PasoActivacion = PasoActivacion.DATOS,
    val telefono: String = "",
    val codigo: String = "",
    val pin: String = "",
    val confirmacion: String = "",
    val aceptaConsentimiento: Boolean = false,
    val aviso: UiState<AvisoPrivacidadDTO> = UiState.Loading,
    val enviando: Boolean = false,
    val errorTelefono: String? = null,
    val errorCodigo: String? = null,
    val errorPin: String? = null,
    val errorConfirmacion: String? = null,
    val error: String? = null,
)

class ActivarViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _estado = MutableStateFlow(ActivarUiState())
    val estado: StateFlow<ActivarUiState> = _estado.asStateFlow()

    init {
        cargarAviso()
    }

    fun cargarAviso() {
        // Si el aviso cambia de versión hay que aceptarlo de nuevo.
        _estado.update { it.copy(aviso = UiState.Loading, aceptaConsentimiento = false) }
        viewModelScope.launch {
            val aviso = auth.avisoPrivacidad().aUiState()
            _estado.update { it.copy(aviso = aviso) }
        }
    }

    fun onTelefono(texto: String) = _estado.update {
        it.copy(telefono = Validaciones.normalizarTelefono(texto), errorTelefono = null, error = null)
    }

    fun onCodigo(texto: String) = _estado.update {
        it.copy(codigo = Validaciones.normalizarCodigo(texto), errorCodigo = null, error = null)
    }

    fun onPin(pin: String) = _estado.update { it.copy(pin = pin, errorPin = null, errorConfirmacion = null) }

    fun onConfirmacion(pin: String) = _estado.update { it.copy(confirmacion = pin, errorConfirmacion = null) }

    fun onAceptaConsentimiento(acepta: Boolean) = _estado.update { it.copy(aceptaConsentimiento = acepta, error = null) }

    /** Valida el paso actual y avanza. En el último paso envía la activación. */
    fun continuar() {
        val s = _estado.value
        when (s.paso) {
            PasoActivacion.DATOS -> {
                val errorTelefono = Validaciones.errorTelefono(s.telefono)
                val errorCodigo = if (s.codigo.isBlank()) "Escribe el código que te dio el administrador." else null
                if (errorTelefono != null || errorCodigo != null) {
                    _estado.update { it.copy(errorTelefono = errorTelefono, errorCodigo = errorCodigo) }
                } else {
                    _estado.update { it.copy(paso = PasoActivacion.PIN, error = null) }
                }
            }
            PasoActivacion.PIN -> {
                val errorPin = Validaciones.errorPinNuevo(s.pin)
                val errorConfirmacion = when {
                    errorPin != null -> null
                    s.confirmacion != s.pin -> "Los PIN no coinciden. Escríbelo de nuevo."
                    else -> null
                }
                when {
                    errorPin != null -> _estado.update { it.copy(errorPin = errorPin, pin = "", confirmacion = "") }
                    errorConfirmacion != null -> _estado.update { it.copy(errorConfirmacion = errorConfirmacion, confirmacion = "") }
                    else -> _estado.update { it.copy(paso = PasoActivacion.CONSENTIMIENTO, error = null) }
                }
            }
            PasoActivacion.CONSENTIMIENTO -> enviar()
            PasoActivacion.LISTO -> Unit
        }
    }

    /** Retrocede un paso. Devuelve false si ya está en el primero (la pantalla debe salir). */
    fun atras(): Boolean {
        val s = _estado.value
        if (s.enviando) return true
        val anterior = when (s.paso) {
            PasoActivacion.DATOS, PasoActivacion.LISTO -> return false
            PasoActivacion.PIN -> PasoActivacion.DATOS
            PasoActivacion.CONSENTIMIENTO -> PasoActivacion.PIN
        }
        _estado.update { it.copy(paso = anterior, error = null) }
        return true
    }

    private fun enviar() {
        val s = _estado.value
        val aviso = (s.aviso as? UiState.Success)?.data
        if (s.enviando) return
        if (aviso == null) {
            _estado.update { it.copy(error = "Primero debe cargarse el aviso de privacidad.") }
            return
        }
        if (!s.aceptaConsentimiento) {
            _estado.update { it.copy(error = "Para activar la cuenta debes aceptar el tratamiento de tus datos.") }
            return
        }
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            when (val r = auth.activar(s.telefono, s.codigo, s.pin, aviso.version)) {
                is Resultado.Ok -> _estado.update { it.copy(enviando = false, paso = PasoActivacion.LISTO, pin = "", confirmacion = "") }
                is Resultado.Falla -> manejarFalla(r.error)
            }
        }
    }

    private fun manejarFalla(error: AppError) {
        val http = error as? AppError.Http
        val campos = http?.errores.orEmpty()
        _estado.update { s ->
            val base = s.copy(enviando = false)
            when {
                http?.status == 401 -> base.copy(
                    paso = PasoActivacion.DATOS,
                    errorCodigo = "El código no es válido o ya venció. Pide uno nuevo al administrador.",
                )
                campos.containsKey("telefono") || campos.containsKey("codigo") -> base.copy(
                    paso = PasoActivacion.DATOS,
                    errorTelefono = campos["telefono"]?.let { "Teléfono: $it" },
                    errorCodigo = campos["codigo"]?.let { "Código: $it" },
                )
                http?.status == 400 && campos.containsKey("pin") -> base.copy(
                    paso = PasoActivacion.PIN,
                    pin = "",
                    confirmacion = "",
                    errorPin = "PIN: ${campos["pin"]}",
                )
                // 400 sin campo: PIN trivial o versión de consentimiento desactualizada (se recarga el aviso abajo).
                http?.status == 429 -> base.copy(error = "Demasiados intentos de activación. Espera unos minutos y vuelve a intentar.")
                else -> base.copy(error = error.mensaje())
            }
        }
        if (http?.status == 400 && campos.isEmpty()) cargarAviso()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ActivarViewModel(container.authRepository) }
        }
    }
}
