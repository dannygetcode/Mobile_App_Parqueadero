package com.parqueadero.appparqueadero.data.repository

import com.parqueadero.appparqueadero.data.model.CambioPinDTO
import com.parqueadero.appparqueadero.data.model.ResumenUsuarioDTO
import com.parqueadero.appparqueadero.data.model.TarifaDTO
import com.parqueadero.appparqueadero.data.model.UsuarioDTO
import com.parqueadero.appparqueadero.data.network.ApiService
import com.parqueadero.appparqueadero.data.network.ErrorParser
import com.parqueadero.appparqueadero.data.network.Resultado
import com.parqueadero.appparqueadero.data.session.BovedaPin
import com.parqueadero.appparqueadero.data.session.MotivoCierre
import com.parqueadero.appparqueadero.data.session.SessionManager

class UsuarioRepository(
    private val api: ApiService,
    private val errores: ErrorParser,
    private val sesion: SessionManager,
    private val bovedaPin: BovedaPin,
) {
    suspend fun yo(): Resultado<UsuarioDTO> = errores.llamar { api.yo() }

    suspend fun resumen(): Resultado<ResumenUsuarioDTO> = errores.llamar { api.resumen() }

    suspend fun tarifas(): Resultado<List<TarifaDTO>> = errores.llamar { api.tarifas() }

    /** Si el cambio funciona, el backend revoca el token: se cierra la sesión para volver a iniciar. */
    suspend fun cambiarPin(pinActual: String, pinNuevo: String): Resultado<Unit> {
        val resultado = errores.llamarSinCuerpo { api.cambiarPin(CambioPinDTO(pinActual, pinNuevo)) }
        if (resultado is Resultado.Ok) {
            // El PIN guardado para la huella ya no es válido.
            bovedaPin.borrar()
            sesion.cerrar(MotivoCierre.PIN_CAMBIADO)
        }
        return resultado
    }
}
