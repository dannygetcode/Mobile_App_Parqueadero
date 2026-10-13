package com.parqueadero.appparqueadero.data.model

// Fechas LocalDate como "2026-10-13" e instantes OffsetDateTime ISO-8601: se guardan como String
// y se interpretan en la capa de UI (utils/Fechas.kt).

data class UsuarioDTO(
    val id: Long,
    val telefono: String,
    val nombre: String?,
    val apellido: String?,
    val estado: EstadoUsuario?,
    val suspendidoMotivo: String?,
    val validado: Boolean?,
    val bloqueadoHasta: String?,
    val cupo: CupoDTO?,
    val vehiculo: VehiculoDTO?,
    val vigenteHasta: String?,
    val creadoEn: String?,
    val dadoDeBajaEn: String?,
)

data class CupoDTO(
    val id: Long,
    val codigo: String,
    val tipoVehiculo: TipoVehiculo?,
)

data class VehiculoDTO(
    val id: Long,
    val placa: String,
    val tipoVehiculo: TipoVehiculo?,
    val carroceria: Carroceria?,
    val color: String?,
    val marca: String?,
)

data class ResumenUsuarioDTO(
    val estado: EstadoUsuario?,
    val diasRestantes: Int?,
    val totalPagadoAnio: Long?,
    val promedioMensualPagado: Long?,
    val visitasMes: Int?,
    val permanenciaPromedioMin: Int?,
    /** "07:40" o null. */
    val horaHabitualLlegada: String?,
)

data class CamaraDTO(
    val id: Long,
    val nombre: String,
    val activa: Boolean,
)
