package com.parqueadero.appparqueadero.data.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class MotivoCierre { VENCIDA, CERRADA_POR_USUARIO, PIN_CAMBIADO }

sealed interface EstadoSesion {
    data object Cargando : EstadoSesion
    data object Activa : EstadoSesion
    data class Inactiva(val motivo: MotivoCierre? = null) : EstadoSesion
}

/**
 * Fuente única de la sesión. El token vive en memoria para el interceptor y cifrado en [SesionStorage].
 * Cualquier 401 en una ruta protegida cierra la sesión (el token pudo vencer o ser revocado).
 */
class SessionManager(
    private val storage: SesionStorage,
    private val scope: CoroutineScope,
    private val ahora: () -> Long = System::currentTimeMillis,
) {
    @Volatile
    private var token: String? = null

    private val _estado = MutableStateFlow<EstadoSesion>(EstadoSesion.Cargando)
    val estado: StateFlow<EstadoSesion> = _estado.asStateFlow()

    fun tokenActual(): String? = token

    suspend fun restaurar() {
        val guardada = storage.leer()
        val expira = guardada?.expiraEnMillis
        if (guardada == null || (expira != null && expira <= ahora())) {
            if (guardada != null) storage.borrar()
            token = null
            _estado.value = EstadoSesion.Inactiva()
        } else {
            token = guardada.token
            _estado.value = EstadoSesion.Activa
        }
    }

    suspend fun iniciar(nuevoToken: String, expiraEnMillis: Long?) {
        storage.guardar(SesionGuardada(nuevoToken, expiraEnMillis))
        token = nuevoToken
        _estado.value = EstadoSesion.Activa
    }

    suspend fun cerrar(motivo: MotivoCierre) {
        token = null
        storage.borrar()
        _estado.value = EstadoSesion.Inactiva(motivo)
    }

    /** Llamado desde el interceptor (hilo de OkHttp). Ignora respuestas de un token ya reemplazado. */
    fun alNoAutorizado(tokenUsado: String) {
        if (tokenUsado != token) return
        token = null
        scope.launch { cerrar(MotivoCierre.VENCIDA) }
    }
}
