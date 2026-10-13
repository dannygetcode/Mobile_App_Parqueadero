package com.parqueadero.appparqueadero.data.model

// Espejo de las enumeraciones del backend (docs/contrato-movil.md). Gson deja en null un valor desconocido.

enum class EstadoUsuario { ACTIVO, VENCIDO, SUSPENDIDO }

enum class EstadoPago { PENDIENTE, APROBADO, RECHAZADO }

enum class OcrEstado { EXITOSO, PARCIAL, FALLIDO, NO_APLICA }

enum class TipoVehiculo { CARRO, MOTO }

enum class Carroceria { SEDAN, HATCHBACK, SUV, PICKUP, VAN, COUPE, OTRO }

enum class TipoEvento { ENTRADA, SALIDA }

enum class ResultadoAcceso { PERMITIDO, DENEGADO }

enum class OrigenAcceso { CAMARA, APP_USUARIO, MANUAL_ADMIN }

enum class MotivoAcceso {
    PLACA_DESCONOCIDA,
    USUARIO_VENCIDO,
    USUARIO_SUSPENDIDO,
    USUARIO_DE_BAJA,
    SIN_CUPO,
    TIPO_CUPO_DISTINTO,
    FORZADO_ADMIN,
    SALIDA_CON_DEUDA,
    YA_DENTRO,
}
