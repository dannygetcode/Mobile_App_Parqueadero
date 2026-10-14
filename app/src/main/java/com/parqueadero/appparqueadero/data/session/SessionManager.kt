package com.parqueadero.appparqueadero.data.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class MotivoCierre { VENCIDA, CERRADA_POR_USUARIO, PIN_CAMBIADO, APP_CERRADA }

sealed interface EstadoSesion {
    data object Activa : EstadoSesion
    data class Inactiva(val motivo: MotivoCierre? = null) : EstadoSesion
}

/**
 * Fuente única de la sesión. El token vive solo en memoria: al cerrar la app (o si el sistema mata el
 * proceso) se pierde y hay que entrar de nuevo con PIN o huella. Nunca se guarda en disco.
 * Cualquier 401 en una ruta protegida cierra la sesión (el token pudo vencer o ser revocado).
 */
class SessionManager(private val ahora: () -> Long = System::currentTimeMillis) {

    @Volatile
    private var token: String? = null

    @Volatile
    private var expiraEnMillis: Long? = null

    private val _estado = MutableStateFlow<EstadoSesion>(EstadoSesion.Inactiva())
    val estado: StateFlow<EstadoSesion> = _estado.asStateFlow()

    /** Token vigente, o null si no hay sesión o ya venció según `expiraEn`. */
    fun tokenActual(): String? {
        val expira = expiraEnMillis
        if (expira != null && expira <= ahora()) return null
        return token
    }

    @Synchronized
    fun iniciar(nuevoToken: String, expiraEn: Long?) {
        token = nuevoToken
        expiraEnMillis = expiraEn
        _estado.value = EstadoSesion.Activa
    }

    @Synchronized
    fun cerrar(motivo: MotivoCierre) {
        val habiaSesion = token != null
        token = null
        expiraEnMillis = null
        if (habiaSesion || _estado.value is EstadoSesion.Activa) _estado.value = EstadoSesion.Inactiva(motivo)
    }

    /** Llamado desde el interceptor (hilo de OkHttp). Ignora respuestas de un token ya reemplazado. */
    @Synchronized
    fun alNoAutorizado(tokenUsado: String) {
        if (tokenUsado != token) return
        cerrar(MotivoCierre.VENCIDA)
    }
}
