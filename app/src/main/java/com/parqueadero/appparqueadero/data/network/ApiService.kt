package com.parqueadero.appparqueadero.data.network

import com.parqueadero.appparqueadero.data.model.CamaraDTO
import com.parqueadero.appparqueadero.data.model.PaymentDTO
import com.parqueadero.appparqueadero.data.model.PuertaDTO
import com.parqueadero.appparqueadero.data.model.UsuarioDTO
import com.parqueadero.appparqueadero.data.model.UsuarioRegistroDTO
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiService {

    @POST("usuarios/registro")
    suspend fun registrarUsuario(@Body dto: UsuarioRegistroDTO)


    @POST("usuarios/verificar-codigo")
    suspend fun verificarCodigo(
        @Query("telefono") telefono: String,
        @Query("codigo") codigo: String
    ): retrofit2.Response<Void>

    @POST("usuarios/validar")
    suspend fun validarCodigoYPin(
        @Query("telefono") telefono: String,
        @Query("codigo") codigo: String,
        @Query("nuevoPin") nuevoPin: String
    ): retrofit2.Response<Void>

    @POST("usuarios/login")
    suspend fun login(
        @Query("telefono") telefono: String,
        @Query("pin") pin: String
    ): Response<Map<String, String>>

    @GET("puerta")
    suspend fun obtenerEstadoPuerta(
        @Header("Authorization") token: String
    ): Response<PuertaDTO>

    @PUT("puerta")
    suspend fun actualizarEstadoPuerta(
        @Header("Authorization") token: String,
        @Body dto: PuertaDTO
    ): Response<PuertaDTO>

    @Multipart
    @POST("pagos")
    suspend fun subirPago(
        @Part("userId") userId: RequestBody,
        @Part("placa") placa: RequestBody,
        @Part("start") start: RequestBody,
        @Part("end") end: RequestBody,
        @Part image: MultipartBody.Part?
    ): Response<PaymentDTO>


    @GET("camaras")
    suspend fun getCamaras(): List<CamaraDTO>

    @GET("usuarios/yo")
    suspend fun obtenerMiUsuario(@Header("Authorization") token: String): Response<UsuarioDTO>








}
