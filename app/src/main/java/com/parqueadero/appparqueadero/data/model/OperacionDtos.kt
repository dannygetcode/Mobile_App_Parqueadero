package com.parqueadero.appparqueadero.data.model

data class ProximoPeriodoDTO(
    val inicio: String,
    val fin: String,
    val montoEsperado: Long,
)

data class PagoDTO(
    val id: Long,
    val usuarioId: Long?,
    val usuarioNombre: String?,
    val placa: String?,
    val periodoInicio: String?,
    val periodoFin: String?,
    val montoEsperado: Long?,
    val montoOcr: Long?,
    val fechaPagoOcr: String?,
    val ocrEstado: OcrEstado?,
    val montoConfirmado: Long?,
    val estado: EstadoPago?,
    val motivoRechazo: String?,
    val observacion: String?,
    val registradoPor: String?,
    val posibleDuplicado: Boolean?,
    val comprobanteUrl: String?,
    val creadoEn: String?,
    val revisadoEn: String?,
)

data class EventoAccesoDTO(
    val id: Long,
    val placaLeida: String?,
    val tipo: TipoEvento?,
    val tipoInferido: Boolean?,
    val resultado: ResultadoAcceso?,
    val motivo: MotivoAcceso?,
    val origen: OrigenAcceso?,
    val observacion: String?,
    val usuarioId: Long?,
    val usuarioNombre: String?,
    val vehiculo: VehiculoDTO?,
    val ocurridoEn: String?,
    val puertaAbierta: Boolean?,
    val duplicado: Boolean?,
)

data class PuertaDTO(
    val abierta: Boolean,
    val abiertaHasta: String?,
    val ultimoEvento: EventoAccesoDTO?,
    val evento: EventoAccesoDTO?,
)

data class PuertaComandoDTO(
    val abierta: Boolean,
)
