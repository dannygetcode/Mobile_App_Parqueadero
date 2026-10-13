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
import com.parqueadero.appparqueadero.data.session.BovedaPin
import com.parqueadero.appparqueadero.data.session.Preferencias
import com.parqueadero.appparqueadero.data.session.PreparacionHuella
import com.parqueadero.appparqueadero.ui.auth.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.crypto.Cipher

data class LoginUiState(
    val telefono: String = "",
    val pin: String = "",
    val cargando: Boolean = false,
    val errorTelefono: String? = null,
    /** Error general (credenciales, bloqueo, red). */
    val error: String? = null,
    /** Hay un PIN guardado con huella en este teléfono. */
    val huellaConfigurada: Boolean = false,
)

class LoginViewModel(
    private val auth: AuthRepository,
    private val preferencias: Preferencias,
    private val bovedaPin: BovedaPin,
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
        viewModelScope.launch {
            bovedaPin.configurada.collect { configurada -> _estado.update { it.copy(huellaConfigurada = configurada) } }
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
        iniciar(actual.telefono, actual.pin, conHuella = false)
    }

    /** Prepara el cifrador del Keystore para el diálogo de huella. Null si no se puede usar la huella. */
    suspend fun prepararHuella(): Cipher? = when (val p = bovedaPin.cifradorParaLeer()) {
        is PreparacionHuella.Lista -> p.cifrador
        PreparacionHuella.Invalidada -> {
            _estado.update {
                it.copy(error = "Cambiaron las huellas registradas en el teléfono. Entra con tu PIN y vuelve a activar la huella en Perfil.")
            }
            null
        }
        PreparacionHuella.NoConfigurada -> null
    }

    /** Con el cifrador autenticado por la huella se descifra el PIN y se hace un login normal. */
    fun ingresarConHuella(cifrador: Cipher) {
        if (_estado.value.cargando) return
        viewModelScope.launch {
            val pin = bovedaPin.leer(cifrador)
            val telefono = bovedaPin.telefono()
            if (pin == null || telefono == null) {
                _estado.update { it.copy(error = "No pudimos leer tu PIN guardado. Entra con tu PIN.") }
                return@launch
            }
            _estado.update { it.copy(telefono = telefono, errorTelefono = null) }
            iniciar(telefono, pin, conHuella = true)
        }
    }

    /** Error del diálogo de huella; null si el usuario lo canceló para usar el PIN. */
    fun falloHuella(mensaje: String?) {
        if (mensaje != null) _estado.update { it.copy(error = mensaje) }
    }

    private fun iniciar(telefono: String, pin: String, conHuella: Boolean) {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            when (val r = auth.login(telefono, pin)) {
                // La sesión pasa a Activa y la raíz de navegación cambia al grafo principal.
                is Resultado.Ok -> _estado.update { it.copy(cargando = false, pin = "") }
                is Resultado.Falla -> {
                    val http = r.error as? AppError.Http
                    // Con huella, un 401 significa que el PIN guardado ya no es el vigente.
                    if (conHuella && http?.status == 401) bovedaPin.borrar()
                    _estado.update {
                        it.copy(
                            cargando = false,
                            pin = "",
                            errorTelefono = http?.errores?.get("telefono"),
                            error = if (conHuella && http?.status == 401) {
                                "Tu PIN cambió desde que activaste la huella. Entra con tu PIN y vuelve a activarla en Perfil."
                            } else {
                                mensajeLogin(r.error)
                            },
                        )
                    }
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
            initializer { LoginViewModel(container.authRepository, container.preferencias, container.bovedaPin) }
        }
    }
}
