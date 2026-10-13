package com.parqueadero.appparqueadero.data.network

import com.parqueadero.appparqueadero.data.model.ActivacionDTO
import com.parqueadero.appparqueadero.data.model.AvisoPrivacidadDTO
import com.parqueadero.appparqueadero.data.model.CamaraDTO
import com.parqueadero.appparqueadero.data.model.CambioPinDTO
import com.parqueadero.appparqueadero.data.model.EventoAccesoDTO
import com.parqueadero.appparqueadero.data.model.LoginDTO
import com.parqueadero.appparqueadero.data.model.PagoDTO
import com.parqueadero.appparqueadero.data.model.ProximoPeriodoDTO
import com.parqueadero.appparqueadero.data.model.PuertaComandoDTO
import com.parqueadero.appparqueadero.data.model.PuertaDTO
import com.parqueadero.appparqueadero.data.model.ResumenUsuarioDTO
import com.parqueadero.appparqueadero.data.model.TokenDTO
import com.parqueadero.appparqueadero.data.model.UsuarioDTO
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

/** Rutas relativas a `{API_URL}api/`. El Bearer lo pone [AuthInterceptor]; la app nunca envía usuarioId. */
interface ApiService {

    // Públicas
    @GET("legal/aviso-privacidad")
    suspend fun avisoPrivacidad(): AvisoPrivacidadDTO

    @POST("auth/activar")
    suspend fun activar(@Body dto: ActivacionDTO): Response<Unit>

    @POST("auth/login")
    suspend fun login(@Body dto: LoginDTO): TokenDTO

    // Usuario
    @GET("usuarios/yo")
    suspend fun yo(): UsuarioDTO

    @PUT("usuarios/yo/pin")
    suspend fun cambiarPin(@Body dto: CambioPinDTO): Response<Unit>

    @GET("usuarios/yo/resumen")
    suspend fun resumen(): ResumenUsuarioDTO

    // Pagos
    @GET("pagos/proximo-periodo")
    suspend fun proximoPeriodo(): ProximoPeriodoDTO

    @Multipart
    @POST("pagos")
    suspend fun subirPago(@Part comprobante: MultipartBody.Part): PagoDTO

    @GET("pagos/mios")
    suspend fun misPagos(): List<PagoDTO>

    // Accesos, puerta y cámaras
    @GET("accesos/mios")
    suspend fun misAccesos(): List<EventoAccesoDTO>

    /** 200 si se permite; 403 con un PuertaDTO (no ProblemDetail) si se deniega. */
    @PUT("puerta")
    suspend fun abrirPuerta(@Body dto: PuertaComandoDTO): Response<PuertaDTO>

    @GET("camaras")
    suspend fun camaras(): List<CamaraDTO>
}
