package com.parqueadero.appparqueadero

import android.content.Context
import com.google.gson.Gson
import com.parqueadero.appparqueadero.data.network.ApiService
import com.parqueadero.appparqueadero.data.network.AuthInterceptor
import com.parqueadero.appparqueadero.data.network.ErrorParser
import com.parqueadero.appparqueadero.data.repository.AccesoRepository
import com.parqueadero.appparqueadero.data.repository.AuthRepository
import com.parqueadero.appparqueadero.data.repository.ContentResolverFuenteImagen
import com.parqueadero.appparqueadero.data.repository.FuenteImagen
import com.parqueadero.appparqueadero.data.repository.PagoRepository
import com.parqueadero.appparqueadero.data.repository.UsuarioRepository
import com.parqueadero.appparqueadero.data.session.DataStorePreferencias
import com.parqueadero.appparqueadero.data.session.BovedaPin
import com.parqueadero.appparqueadero.data.session.KeystoreBovedaPin
import com.parqueadero.appparqueadero.data.session.Preferencias
import com.parqueadero.appparqueadero.data.session.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Inyección de dependencias manual (sin Hilt). Una instancia por proceso, en [CupoApp]. */
class AppContainer(context: Context) {

    init {
        borrarSesionPersistidaAntigua(context)
    }

    private val gson = Gson()

    val preferencias: Preferencias = DataStorePreferencias(context)

    val sesion = SessionManager()

    val bovedaPin: BovedaPin = KeystoreBovedaPin(context)

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor(sesion::tokenActual, sesion::alNoAutorizado))
        .apply {
            // Solo en debug y sin cabeceras ni cuerpos: nunca se registran el token ni el PIN.
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
        }
        .build()

    private val api: ApiService = Retrofit.Builder()
        .baseUrl(BuildConfig.API_URL + "api/")
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(ApiService::class.java)

    private val errores = ErrorParser(gson)

    val authRepository = AuthRepository(api, errores, sesion, preferencias, bovedaPin)
    val usuarioRepository = UsuarioRepository(api, errores, sesion, bovedaPin)
    val pagoRepository = PagoRepository(api, errores)
    val accesoRepository = AccesoRepository(api, errores, gson)
    val fuenteImagen: FuenteImagen = ContentResolverFuenteImagen(context)
}

/**
 * Hasta la Etapa 2 el token se guardaba cifrado en disco. Ahora vive solo en memoria:
 * se borran el archivo y la clave que hayan quedado de versiones anteriores.
 */
private fun borrarSesionPersistidaAntigua(context: Context) {
    try {
        java.io.File(context.filesDir, "datastore/sesion.preferences_pb").delete()
        java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry("cupo_sesion")
    } catch (_: Exception) {
    }
}
