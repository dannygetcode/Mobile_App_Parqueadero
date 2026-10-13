package com.parqueadero.appparqueadero.utils

import java.text.NumberFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// java.time está disponible desde minSdk 24 gracias al desugaring de la biblioteca estándar.

val LOCALE_CO: Locale = Locale("es", "CO")

fun String.aOffsetDateTimeONull(): OffsetDateTime? = try {
    OffsetDateTime.parse(this)
} catch (_: Exception) {
    null
}

fun String.aLocalDateONull(): LocalDate? = try {
    LocalDate.parse(this)
} catch (_: Exception) {
    null
}

/** Milisegundos epoch de un instante ISO-8601, o null si no se puede interpretar. */
fun String.aEpochMillisONull(): Long? = aOffsetDateTimeONull()?.toInstant()?.toEpochMilli()

private val formatoFecha = DateTimeFormatter.ofPattern("d MMM yyyy", LOCALE_CO)

fun LocalDate.formatoCorto(): String = format(formatoFecha)

/** "$ 80.000" */
fun Long.comoPesos(): String {
    val formato = NumberFormat.getCurrencyInstance(LOCALE_CO).apply { maximumFractionDigits = 0 }
    return formato.format(this)
}
