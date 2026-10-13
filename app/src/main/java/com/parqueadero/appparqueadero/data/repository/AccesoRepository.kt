package com.parqueadero.appparqueadero.data.repository

import com.google.gson.Gson
import com.parqueadero.appparqueadero.data.model.CamaraDTO
import com.parqueadero.appparqueadero.data.model.EventoAccesoDTO
import com.parqueadero.appparqueadero.data.model.MotivoAcceso
import com.parqueadero.appparqueadero.data.model.PuertaComandoDTO
import com.parqueadero.appparqueadero.data.model.PuertaDTO
import com.parqueadero.appparqueadero.data.network.ApiService
import com.parqueadero.appparqueadero.data.network.ErrorParser
import com.parqueadero.appparqueadero.data.network.Resultado
import java.io.IOException

sealed interface ResultadoPuerta {
    val puerta: PuertaDTO

    data class Permitida(override val puerta: PuertaDTO) : ResultadoPuerta

    /** 403 con PuertaDTO: `motivo` explica por qué (USUARIO_VENCIDO, YA_DENTRO...). */
    data class Denegada(override val puerta: PuertaDTO, val motivo: MotivoAcceso?) : ResultadoPuerta
}

/** Puerta, cámaras e historial de accesos (todo simulado en el backend). */
class AccesoRepository(
    private val api: ApiService,
    private val errores: ErrorParser,
    private val gson: Gson,
) {
    suspend fun misAccesos(): Resultado<List<EventoAccesoDTO>> = errores.llamar { api.misAccesos() }

    suspend fun camaras(): Resultado<List<CamaraDTO>> = errores.llamar { api.camaras() }

    suspend fun estadoPuerta(): Resultado<PuertaDTO> = errores.llamar { api.puerta() }

    /** El usuario solo puede abrir; la puerta se cierra sola en `abiertaHasta`. */
    suspend fun abrirPuerta(): Resultado<ResultadoPuerta> {
        val respuesta = when (val r = errores.llamar { api.abrirPuerta(PuertaComandoDTO(abierta = true)) }) {
            is Resultado.Falla -> return r
            is Resultado.Ok -> r.datos
        }
        respuesta.body()?.takeIf { respuesta.isSuccessful }?.let {
            return Resultado.Ok(ResultadoPuerta.Permitida(it))
        }
        val cuerpo = try { respuesta.errorBody()?.string() } catch (_: IOException) { null }
        if (respuesta.code() == 403) {
            val puerta = try { gson.fromJson(cuerpo, PuertaDTO::class.java) } catch (_: Exception) { null }
            if (puerta?.evento != null) {
                return Resultado.Ok(ResultadoPuerta.Denegada(puerta, puerta.evento.motivo))
            }
        }
        return Resultado.Falla(errores.desdeTexto(respuesta.code(), cuerpo))
    }
}
