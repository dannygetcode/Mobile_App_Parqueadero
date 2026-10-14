package com.parqueadero.appparqueadero.ui.auth

/** Validaciones del lado del cliente; el backend sigue siendo la autoridad. */
object Validaciones {
    const val LONGITUD_PIN = 6
    const val LONGITUD_CODIGO = 8
    private const val MAX_CODIGO = 20
    private val REGEX_TELEFONO = Regex("^\\+?[0-9]{7,15}$")

    /** Quita espacios, guiones y paréntesis que el usuario suele escribir. */
    fun normalizarTelefono(texto: String): String = texto.filter { it.isDigit() || it == '+' }

    fun telefonoValido(telefono: String): Boolean = REGEX_TELEFONO.matches(telefono)

    fun normalizarCodigo(texto: String): String =
        texto.filter { it.isLetterOrDigit() }.uppercase().take(MAX_CODIGO)

    fun pinCompleto(pin: String): Boolean = pin.length == LONGITUD_PIN && pin.all { it.isDigit() }

    /** Todos iguales (000000) o consecutivos ascendentes/descendentes (123456, 654321). */
    fun pinTrivial(pin: String): Boolean {
        if (!pinCompleto(pin)) return false
        val digitos = pin.map { it - '0' }
        val pasos = digitos.zipWithNext { a, b -> b - a }.toSet()
        return pasos == setOf(0) || pasos == setOf(1) || pasos == setOf(-1)
    }

    fun errorTelefono(telefono: String): String? = when {
        telefono.isBlank() -> "Escribe tu número de celular."
        !telefonoValido(telefono) -> "Revisa el número: solo dígitos, entre 7 y 15."
        else -> null
    }

    fun errorPinNuevo(pin: String): String? = when {
        !pinCompleto(pin) -> "El PIN debe tener 6 dígitos."
        pinTrivial(pin) -> "Elige un PIN menos predecible: evita repetidos y consecutivos."
        else -> null
    }
}
