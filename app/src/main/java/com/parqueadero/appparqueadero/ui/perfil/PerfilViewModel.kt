package com.parqueadero.appparqueadero.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.parqueadero.appparqueadero.container
import com.parqueadero.appparqueadero.data.model.UsuarioDTO
import com.parqueadero.appparqueadero.data.network.AppError
import com.parqueadero.appparqueadero.data.network.Resultado
import com.parqueadero.appparqueadero.data.network.mensaje
import com.parqueadero.appparqueadero.data.repository.AuthRepository
import com.parqueadero.appparqueadero.data.repository.UsuarioRepository
import com.parqueadero.appparqueadero.data.session.Preferencias
import com.parqueadero.appparqueadero.ui.auth.Validaciones
import com.parqueadero.appparqueadero.ui.common.UiState
import com.parqueadero.appparqueadero.ui.common.aUiState
import com.parqueadero.appparqueadero.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CambioPinUiState(
    val pinActual: String = "",
    val pinNuevo: String = "",
    val confirmacion: String = "",
    val enviando: Boolean = false,
    val errorActual: String? = null,
    val errorNuevo: String? = null,
    val errorConfirmacion: String? = null,
    val error: String? = null,
)

class PerfilViewModel(
    private val usuarios: UsuarioRepository,
    private val auth: AuthRepository,
    private val preferencias: Preferencias,
) : ViewModel() {

    private val _usuario = MutableStateFlow<UiState<UsuarioDTO>>(UiState.Loading)
    val usuario: StateFlow<UiState<UsuarioDTO>> = _usuario.asStateFlow()

    val tema: StateFlow<ThemeMode> = preferencias.tema
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SISTEMA)

    /** Null si el diálogo de cambio de PIN está cerrado. */
    private val _cambioPin = MutableStateFlow<CambioPinUiState?>(null)
    val cambioPin: StateFlow<CambioPinUiState?> = _cambioPin.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _usuario.value = UiState.Loading
        viewModelScope.launch { _usuario.value = usuarios.yo().aUiState() }
    }

    fun elegirTema(modo: ThemeMode) {
        viewModelScope.launch { preferencias.guardarTema(modo) }
    }

    fun cerrarSesion() {
        viewModelScope.launch { auth.cerrarSesion() }
    }

    // Cambio de PIN

    fun abrirCambioPin() {
        _cambioPin.value = CambioPinUiState()
    }

    fun cerrarCambioPin() {
        if (_cambioPin.value?.enviando != true) _cambioPin.value = null
    }

    fun onPinActual(pin: String) = _cambioPin.update { it?.copy(pinActual = pin, errorActual = null, error = null) }

    fun onPinNuevo(pin: String) = _cambioPin.update { it?.copy(pinNuevo = pin, errorNuevo = null, errorConfirmacion = null, error = null) }

    fun onConfirmacion(pin: String) = _cambioPin.update { it?.copy(confirmacion = pin, errorConfirmacion = null, error = null) }

    /** Si funciona, el backend revoca el token y la sesión se cierra: la app vuelve al login con un aviso. */
    fun confirmarCambioPin() {
        val s = _cambioPin.value ?: return
        if (s.enviando) return
        val errorActual = if (!Validaciones.pinCompleto(s.pinActual)) "Escribe tu PIN actual." else null
        val errorNuevo = Validaciones.errorPinNuevo(s.pinNuevo)
            ?: if (s.pinNuevo == s.pinActual) "El PIN nuevo debe ser distinto del actual." else null
        val errorConfirmacion = if (errorNuevo == null && s.confirmacion != s.pinNuevo) "Los PIN no coinciden." else null
        if (errorActual != null || errorNuevo != null || errorConfirmacion != null) {
            _cambioPin.value = s.copy(
                errorActual = errorActual,
                errorNuevo = errorNuevo,
                errorConfirmacion = errorConfirmacion,
                confirmacion = if (errorConfirmacion != null) "" else s.confirmacion,
            )
            return
        }
        _cambioPin.value = s.copy(enviando = true, error = null)
        viewModelScope.launch {
            when (val r = usuarios.cambiarPin(s.pinActual, s.pinNuevo)) {
                is Resultado.Ok -> _cambioPin.value = null
                is Resultado.Falla -> _cambioPin.update { it?.let { actual -> conFalla(actual, r.error) } }
            }
        }
    }

    private fun conFalla(s: CambioPinUiState, error: AppError): CambioPinUiState {
        val base = s.copy(enviando = false)
        return when ((error as? AppError.Http)?.status) {
            401 -> base.copy(pinActual = "", errorActual = "El PIN actual no es correcto.")
            423 -> base.copy(error = "Tu cuenta quedó bloqueada por intentos fallidos. Intenta de nuevo en 15 minutos.")
            400 -> base.copy(pinNuevo = "", confirmacion = "", errorNuevo = (error as AppError.Http).detail ?: "Elige otro PIN.")
            else -> base.copy(error = error.mensaje())
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                PerfilViewModel(container.usuarioRepository, container.authRepository, container.preferencias)
            }
        }
    }
}
