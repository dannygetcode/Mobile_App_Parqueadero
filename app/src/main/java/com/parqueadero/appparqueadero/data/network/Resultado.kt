package com.parqueadero.appparqueadero.data.network

import com.google.gson.Gson
import com.parqueadero.appparqueadero.data.model.ProblemDetail
import kotlinx.coroutines.CancellationException
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.IOException

sealed interface Resultado<out T> {
    data class Ok<out T>(val datos: T) : Resultado<T>
    data class Falla(val error: AppError) : Resultado<Nothing>
}

sealed interface AppError {
    /** Respuesta HTTP con error. `errores` va de campo a mensaje. */
    data class Http(
        val status: Int,
        val detail: String? = null,
        val codigo: String? = null,
        val errores: Map<String, String> = emptyMap(),
        val referencia: String? = null,
    ) : AppError

    data object SinConexion : AppError

    data class Desconocido(val causa: Throwable? = null) : AppError
}

/** Mensaje en español para el usuario. La lógica decide por `status`, nunca por el texto de `detail`. */
fun AppError.mensaje(): String = when (this) {
    AppError.SinConexion -> "No pudimos conectar con el servidor. Revisa tu conexión e intenta de nuevo."
    is AppError.Desconocido -> "Ocurrió un error inesperado. Intenta de nuevo."
    is AppError.Http -> when (status) {
        401 -> "Tu sesión no es válida. Vuelve a iniciar sesión."
        423 -> "Tu cuenta está bloqueada por intentos fallidos. Intenta de nuevo en 15 minutos."
        429 -> "Demasiados intentos. Espera unos minutos antes de volver a intentar."
        in 500..599 -> if (referencia != null) {
            "El servidor tuvo un problema. Si continúa, informa al administrador la referencia $referencia."
        } else {
            "El servidor no está disponible en este momento. Intenta más tarde."
        }
        else -> detail?.takeIf { it.isNotBlank() } ?: "No se pudo completar la operación (código $status)."
    }
}

val AppError.status: Int? get() = (this as? AppError.Http)?.status

inline fun <T, R> Resultado<T>.map(transform: (T) -> R): Resultado<R> = when (this) {
    is Resultado.Ok -> Resultado.Ok(transform(datos))
    is Resultado.Falla -> this
}

/** Convierte errores de Retrofit/OkHttp en [AppError] leyendo el ProblemDetail del cuerpo. */
class ErrorParser(private val gson: Gson) {

    fun desdeCuerpo(status: Int, cuerpo: ResponseBody?): AppError.Http =
        desdeTexto(status, try { cuerpo?.string() } catch (_: IOException) { null })

    fun desdeTexto(status: Int, json: String?): AppError.Http {
        val pd = try {
            json?.takeIf { it.isNotBlank() }?.let { gson.fromJson(it, ProblemDetail::class.java) }
        } catch (_: Exception) {
            null
        }
        return AppError.Http(
            status = status,
            detail = pd?.detail,
            codigo = pd?.codigo,
            errores = pd?.errores.orEmpty()
                .mapNotNull { e -> e.campo?.let { campo -> campo to (e.mensaje ?: "inválido") } }
                .toMap(),
            referencia = pd?.referencia,
        )
    }

    fun desdeExcepcion(e: Throwable): AppError = when (e) {
        is HttpException -> desdeCuerpo(e.code(), e.response()?.errorBody())
        is IOException -> AppError.SinConexion
        else -> AppError.Desconocido(e)
    }

    /** Ejecuta una llamada y la envuelve en [Resultado]. Propaga la cancelación de corrutinas. */
    suspend fun <T> llamar(bloque: suspend () -> T): Resultado<T> = try {
        Resultado.Ok(bloque())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Resultado.Falla(desdeExcepcion(e))
    }

    /** Para rutas que responden 204 sin cuerpo. */
    suspend fun llamarSinCuerpo(bloque: suspend () -> retrofit2.Response<Unit>): Resultado<Unit> =
        when (val r = llamar(bloque)) {
            is Resultado.Falla -> r
            is Resultado.Ok -> if (r.datos.isSuccessful) {
                Resultado.Ok(Unit)
            } else {
                Resultado.Falla(desdeCuerpo(r.datos.code(), r.datos.errorBody()))
            }
        }
}
