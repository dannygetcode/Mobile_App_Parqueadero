package com.parqueadero.appparqueadero.data.repository

import com.parqueadero.appparqueadero.data.model.PagoDTO
import com.parqueadero.appparqueadero.data.model.ProximoPeriodoDTO
import com.parqueadero.appparqueadero.data.network.ApiService
import com.parqueadero.appparqueadero.data.network.ErrorParser
import com.parqueadero.appparqueadero.data.network.Resultado
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class PagoRepository(
    private val api: ApiService,
    private val errores: ErrorParser,
) {
    suspend fun proximoPeriodo(): Resultado<ProximoPeriodoDTO> = errores.llamar { api.proximoPeriodo() }

    suspend fun misPagos(): Resultado<List<PagoDTO>> = errores.llamar { api.misPagos() }

    suspend fun pago(id: Long): Resultado<PagoDTO> = errores.llamar { api.pago(id) }

    /**
     * Sube el comprobante en la parte multipart `comprobante`. El usuario y el periodo los deduce el backend.
     * La validación de tipo (JPEG/PNG) y tamaño (5 MB) se hace antes, en la pantalla de pagos.
     */
    suspend fun subirComprobante(bytes: ByteArray, mime: String): Resultado<PagoDTO> {
        val extension = if (mime == MIME_PNG) "png" else "jpg"
        val parte = MultipartBody.Part.createFormData(
            "comprobante",
            "comprobante.$extension",
            bytes.toRequestBody(mime.toMediaType()),
        )
        return errores.llamar { api.subirPago(parte) }
    }

    companion object {
        const val MIME_JPEG = "image/jpeg"
        const val MIME_PNG = "image/png"
        const val TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024
    }
}
