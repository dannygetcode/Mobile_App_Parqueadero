package com.parqueadero.appparqueadero.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class ImagenLeida(val bytes: ByteArray, val mime: String?) {
    override fun equals(other: Any?) = other is ImagenLeida && other.mime == mime && other.bytes.contentEquals(bytes)
    override fun hashCode() = 31 * bytes.contentHashCode() + (mime?.hashCode() ?: 0)
}

/** Lee imágenes elegidas por el usuario. Se abstrae para probar la pantalla de pagos sin Android. */
interface FuenteImagen {
    /** Null si no se pudo leer o si supera el límite de lectura. */
    suspend fun leer(uri: String): ImagenLeida?

    /** Reescala y recomprime a JPEG para que pese menos de [maxBytes]. Null si no se logra. */
    suspend fun reducir(bytes: ByteArray, maxBytes: Long): ByteArray?

    /** Borra las fotos temporales tomadas con la cámara (datos personales del comprobante). */
    suspend fun limpiarTemporales() {}
}

object TipoImagen {
    /** Tipo por contenido (firma de bytes), igual que el backend; no por extensión. */
    fun detectar(bytes: ByteArray): String? = when {
        bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte() ->
            PagoRepository.MIME_JPEG
        bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() &&
            bytes[2] == 'N'.code.toByte() && bytes[3] == 'G'.code.toByte() -> PagoRepository.MIME_PNG
        else -> null
    }
}

class ContentResolverFuenteImagen(context: Context) : FuenteImagen {

    private val resolver = context.applicationContext.contentResolver
    private val carpetaTemporal = java.io.File(context.applicationContext.cacheDir, CARPETA_TEMPORAL)

    override suspend fun limpiarTemporales() {
        withContext(Dispatchers.IO) { carpetaTemporal.listFiles()?.forEach { it.delete() } }
    }

    override suspend fun leer(uri: String): ImagenLeida? = withContext(Dispatchers.IO) {
        try {
            resolver.openInputStream(Uri.parse(uri))?.use { entrada ->
                val salida = ByteArrayOutputStream()
                val bufer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val leidos = entrada.read(bufer)
                    if (leidos < 0) break
                    total += leidos
                    if (total > LIMITE_LECTURA) return@withContext null
                    salida.write(bufer, 0, leidos)
                }
                val bytes = salida.toByteArray()
                ImagenLeida(bytes, TipoImagen.detectar(bytes))
            }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun reducir(bytes: ByteArray, maxBytes: Long): ByteArray? = withContext(Dispatchers.Default) {
        try {
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, limites)
            var muestreo = 1
            while (maxOf(limites.outWidth, limites.outHeight) / muestreo > LADO_MAXIMO) muestreo *= 2
            val bitmap = BitmapFactory.decodeByteArray(
                bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = muestreo },
            ) ?: return@withContext null
            try {
                for (calidad in intArrayOf(85, 75, 60)) {
                    val salida = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, salida)
                    if (salida.size() <= maxBytes) return@withContext salida.toByteArray()
                }
                null
            } finally {
                bitmap.recycle()
            }
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    companion object {
        /** Debe coincidir con res/xml/archivos_compartidos.xml y con PagosScreen. */
        const val CARPETA_TEMPORAL = "comprobantes"
        private const val LIMITE_LECTURA = 25L * 1024 * 1024
        private const val LADO_MAXIMO = 2560
    }
}
