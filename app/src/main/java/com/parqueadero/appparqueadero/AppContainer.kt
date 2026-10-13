package com.parqueadero.appparqueadero

import android.content.Context
import com.google.gson.Gson
import com.parqueadero.appparqueadero.data.network.ApiService
import com.parqueadero.appparqueadero.data.network.AuthInterceptor
import com.parqueadero.appparqueadero.data.network.ErrorParser
import com.parqueadero.appparqueadero.data.repository.AccesoRepository
import com.parqueadero.appparqueadero.data.repository.AuthRepository
import com.parqueadero.appparqueadero.data.repository.PagoRepository
import com.parqueadero.appparqueadero.data.repository.UsuarioRepository
import com.parqueadero.appparqueadero.data.session.DataStorePreferencias
import com.parqueadero.appparqueadero.data.session.DataStoreSesionStorage
import com.parqueadero.appparqueadero.data.session.Preferencias
import com.parqueadero.appparqueadero.data.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Inyección de dependencias manual (sin Hilt). Una instancia por proceso, en [CupoApp]. */
class AppContainer(context: Context) {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val gson = Gson()

    val preferencias: Preferencias = DataStorePreferencias(context)

    val sesion = SessionManager(DataStoreSesionStorage(context), appScope).also { s ->
        appScope.launch { s.restaurar() }
    }

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

    val authRepository = AuthRepository(api, errores, sesion, preferencias)
    val usuarioRepository = UsuarioRepository(api, errores, sesion)
    val pagoRepository = PagoRepository(api, errores)
    val accesoRepository = AccesoRepository(api, errores, gson)
}
