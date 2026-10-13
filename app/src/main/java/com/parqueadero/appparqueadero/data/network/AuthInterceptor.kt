package com.parqueadero.appparqueadero.data.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Añade `Authorization: Bearer` a las rutas protegidas. Ante un 401 en una ruta protegida avisa con el
 * token usado, para que la sesión se cierre solo si ese token sigue siendo el vigente.
 * En las rutas públicas (login, activación) un 401 significa credenciales inválidas y no cierra nada.
 */
class AuthInterceptor(
    private val tokenActual: () -> String?,
    private val alNoAutorizado: (tokenUsado: String) -> Unit,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val peticion = chain.request()
        if (esPublica(peticion.url.encodedPath)) return chain.proceed(peticion)

        val token = tokenActual() ?: return chain.proceed(peticion)
        val respuesta = chain.proceed(
            peticion.newBuilder().header("Authorization", "Bearer $token").build()
        )
        if (respuesta.code == 401 && !es401DeCredenciales(peticion.method, peticion.url.encodedPath)) {
            alNoAutorizado(token)
        }
        return respuesta
    }

    companion object {
        private val PUBLICAS = listOf("/api/auth/", "/api/legal/", "/api/ping")

        fun esPublica(ruta: String): Boolean = PUBLICAS.any { ruta.contains(it) }

        /**
         * `PUT /usuarios/yo/pin` responde 401 cuando el PIN actual es incorrecto: es un error del formulario,
         * no una sesión vencida, así que no cierra la sesión.
         */
        fun es401DeCredenciales(metodo: String, ruta: String): Boolean =
            metodo == "PUT" && ruta.endsWith("/api/usuarios/yo/pin")
    }
}
