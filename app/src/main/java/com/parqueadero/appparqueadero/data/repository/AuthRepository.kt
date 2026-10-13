package com.parqueadero.appparqueadero.data.repository

import com.parqueadero.appparqueadero.data.model.ActivacionDTO
import com.parqueadero.appparqueadero.data.model.AvisoPrivacidadDTO
import com.parqueadero.appparqueadero.data.model.LoginDTO
import com.parqueadero.appparqueadero.data.network.ApiService
import com.parqueadero.appparqueadero.data.network.ErrorParser
import com.parqueadero.appparqueadero.data.network.Resultado
import com.parqueadero.appparqueadero.data.network.map
import com.parqueadero.appparqueadero.data.session.BovedaPin
import com.parqueadero.appparqueadero.data.session.MotivoCierre
import com.parqueadero.appparqueadero.data.session.Preferencias
import com.parqueadero.appparqueadero.data.session.SessionManager
import com.parqueadero.appparqueadero.utils.aEpochMillisONull

class AuthRepository(
    private val api: ApiService,
    private val errores: ErrorParser,
    private val sesion: SessionManager,
    private val preferencias: Preferencias,
    private val bovedaPin: BovedaPin,
) {
    suspend fun avisoPrivacidad(): Resultado<AvisoPrivacidadDTO> = errores.llamar { api.avisoPrivacidad() }

    suspend fun activar(
        telefono: String,
        codigo: String,
        pin: String,
        versionConsentimiento: String,
    ): Resultado<Unit> {
        val resultado = errores.llamarSinCuerpo {
            api.activar(
                ActivacionDTO(
                    telefono = telefono,
                    codigo = codigo,
                    pin = pin,
                    aceptaTratamientoDatos = true,
                    versionConsentimiento = versionConsentimiento,
                )
            )
        }
        if (resultado is Resultado.Ok) {
            preferencias.recordarTelefono(telefono)
            // Activar con un código nuevo cambia el PIN: el guardado para la huella ya no sirve.
            bovedaPin.borrar()
        }
        return resultado
    }

    /** Inicia sesión; el token queda solo en memoria. Al cambiar el estado de la sesión, la navegación pasa al grafo principal. */
    suspend fun login(telefono: String, pin: String): Resultado<Unit> {
        val resultado = errores.llamar { api.login(LoginDTO(telefono, pin)) }
        if (resultado is Resultado.Ok) {
            preferencias.recordarTelefono(telefono)
            sesion.iniciar(resultado.datos.token, resultado.datos.expiraEn?.aEpochMillisONull())
        }
        return resultado.map { }
    }

    fun cerrarSesion() = sesion.cerrar(MotivoCierre.CERRADA_POR_USUARIO)
}
