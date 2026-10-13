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
        if (respuesta.code == 401) alNoAutorizado(token)
        return respuesta
    }

    companion object {
        private val PUBLICAS = listOf("/api/auth/", "/api/legal/", "/api/ping")

        fun esPublica(ruta: String): Boolean = PUBLICAS.any { ruta.contains(it) }
    }
}
