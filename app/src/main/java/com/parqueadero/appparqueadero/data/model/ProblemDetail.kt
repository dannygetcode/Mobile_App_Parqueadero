package com.parqueadero.appparqueadero.data.model

/** Error del backend (RFC 9457). `codigo`, `errores` y `referencia` son opcionales. */
data class ProblemDetail(
    val type: String?,
    val title: String?,
    val status: Int?,
    val detail: String?,
    val codigo: String?,
    val errores: List<ErrorCampo>?,
    val referencia: String?,
)

data class ErrorCampo(
    val campo: String?,
    val mensaje: String?,
)
