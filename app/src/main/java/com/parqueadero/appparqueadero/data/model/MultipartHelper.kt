package com.parqueadero.appparqueadero.data.api

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate

data class PartesPago(
    val userId: okhttp3.RequestBody,
    val placa: okhttp3.RequestBody,
    val start: okhttp3.RequestBody,
    val end: okhttp3.RequestBody,
    val imagen: MultipartBody.Part?
)

fun crearPartesPago(
    context: Context,
    userId: Long,
    placa: String,
    start: LocalDate,
    end: LocalDate,
    imageUri: Uri?
): PartesPago {
    val mediaType = "text/plain".toMediaTypeOrNull()

    val userIdPart = userId.toString().toRequestBody(mediaType)
    val placaPart = placa.toRequestBody(mediaType)
    val startPart = start.toString().toRequestBody(mediaType)
    val endPart = end.toString().toRequestBody(mediaType)

    val imagePart = imageUri?.let {
        val inputStream = context.contentResolver.openInputStream(it)
        val bytes = inputStream!!.readBytes()
        val reqBody = bytes.toRequestBody("image/*".toMediaTypeOrNull())
        MultipartBody.Part.createFormData("image", "comprobante.jpg", reqBody)
    }

    return PartesPago(userIdPart, placaPart, startPart, endPart, imagePart)
}
