package com.parqueadero.appparqueadero.data.model

data class AvisoPrivacidadDTO(
    val version: String,
    val texto: String,
)

data class ActivacionDTO(
    val telefono: String,
    val codigo: String,
    val pin: String,
    val aceptaTratamientoDatos: Boolean,
    val versionConsentimiento: String,
) {
    override fun toString() = "ActivacionDTO(telefono=$telefono)"
}

data class LoginDTO(
    val telefono: String,
    val pin: String,
) {
    override fun toString() = "LoginDTO(telefono=$telefono)"
}

/** `expiraEn` es un OffsetDateTime ISO-8601. */
data class TokenDTO(
    val token: String,
    val rol: String?,
    val expiraEn: String?,
) {
    override fun toString() = "TokenDTO(rol=$rol, expiraEn=$expiraEn)"
}

data class CambioPinDTO(
    val pinActual: String,
    val pinNuevo: String,
) {
    override fun toString() = "CambioPinDTO"
}
