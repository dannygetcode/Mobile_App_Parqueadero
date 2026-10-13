package com.parqueadero.appparqueadero.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.parqueadero.appparqueadero.container
import com.parqueadero.appparqueadero.data.network.AppError
import com.parqueadero.appparqueadero.data.network.Resultado
import com.parqueadero.appparqueadero.data.network.mensaje
import com.parqueadero.appparqueadero.data.repository.AuthRepository
import com.parqueadero.appparqueadero.data.session.Preferencias
import com.parqueadero.appparqueadero.ui.auth.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val telefono: String = "",
    val pin: String = "",
    val cargando: Boolean = false,
    val errorTelefono: String? = null,
    /** Error general (credenciales, bloqueo, red). */
    val error: String? = null,
)

class LoginViewModel(
    private val auth: AuthRepository,
    private val preferencias: Preferencias,
) : ViewModel() {

    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val recordado = preferencias.telefonoRecordado.first()
            if (recordado != null && _estado.value.telefono.isEmpty()) {
                _estado.update { it.copy(telefono = recordado) }
            }
        }
    }

    fun onTelefono(texto: String) {
        _estado.update { it.copy(telefono = Validaciones.normalizarTelefono(texto), errorTelefono = null, error = null) }
    }

    fun onPin(pin: String) {
        _estado.update { it.copy(pin = pin, error = null) }
    }

    fun ingresar() {
        val actual = _estado.value
        if (actual.cargando) return
        val errorTelefono = Validaciones.errorTelefono(actual.telefono)
        if (errorTelefono != null) {
            _estado.update { it.copy(errorTelefono = errorTelefono) }
            return
        }
        if (!Validaciones.pinCompleto(actual.pin)) {
            _estado.update { it.copy(error = "Escribe los 6 dígitos de tu PIN.") }
            return
        }
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            when (val r = auth.login(actual.telefono, actual.pin)) {
                // La sesión pasa a Activa y la raíz de navegación cambia al grafo principal.
                is Resultado.Ok -> _estado.update { it.copy(cargando = false, pin = "") }
                is Resultado.Falla -> _estado.update {
                    it.copy(
                        cargando = false,
                        pin = "",
                        errorTelefono = (r.error as? AppError.Http)?.errores?.get("telefono"),
                        error = mensajeLogin(r.error),
                    )
                }
            }
        }
    }

    companion object {
        fun mensajeLogin(error: AppError): String = when ((error as? AppError.Http)?.status) {
            401 -> "Teléfono o PIN incorrectos. Tras 5 intentos fallidos la cuenta se bloquea 15 minutos."
            423 -> "Tu cuenta está bloqueada por intentos fallidos. Espera 15 minutos o pide ayuda al administrador."
            429 -> "Demasiados intentos desde este dispositivo. Espera unos minutos y vuelve a intentar."
            400 -> "Revisa los datos: el teléfono o el PIN no tienen el formato correcto."
            else -> error.mensaje()
        }

        val Factory = viewModelFactory {
            initializer { LoginViewModel(container.authRepository, container.preferencias) }
        }
    }
}
