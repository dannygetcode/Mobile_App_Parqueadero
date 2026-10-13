package com.parqueadero.appparqueadero.ui.inicio

import com.parqueadero.appparqueadero.data.model.MotivoAcceso
import com.parqueadero.appparqueadero.ui.components.StatusTipo

/** Textos de los motivos de acceso (docs/diseno/sistema-cupo.md, DoorControl). */
object TextosPuerta {

    fun motivo(motivo: MotivoAcceso?): String = when (motivo) {
        MotivoAcceso.USUARIO_VENCIDO -> "Tu mensualidad venció. Sube tu comprobante en Pagos para reactivar el acceso."
        MotivoAcceso.USUARIO_SUSPENDIDO -> "Tu cuenta está suspendida. Comunícate con la administración."
        MotivoAcceso.USUARIO_DE_BAJA -> "Tu cuenta fue dada de baja. Comunícate con la administración."
        MotivoAcceso.SIN_CUPO -> "No hay cupos libres en este momento. Intenta más tarde."
        MotivoAcceso.TIPO_CUPO_DISTINTO -> "No hay cupo libre para tu tipo de vehículo."
        MotivoAcceso.YA_DENTRO -> "Tu vehículo ya figura dentro del parqueadero."
        MotivoAcceso.PLACA_DESCONOCIDA -> "Esta placa no está registrada en tu cuenta."
        MotivoAcceso.SALIDA_CON_DEUDA -> "Salida registrada. Tienes un pago pendiente; súbelo en Pagos."
        MotivoAcceso.FORZADO_ADMIN -> "La administración registró este movimiento."
        null -> "El acceso fue denegado. Si crees que es un error, comunícate con la administración."
    }

    fun tipoMotivo(motivo: MotivoAcceso?): StatusTipo = when (motivo) {
        MotivoAcceso.USUARIO_VENCIDO, MotivoAcceso.SALIDA_CON_DEUDA -> StatusTipo.ADVERTENCIA
        MotivoAcceso.USUARIO_SUSPENDIDO, MotivoAcceso.USUARIO_DE_BAJA -> StatusTipo.NEUTRO
        MotivoAcceso.YA_DENTRO, MotivoAcceso.FORZADO_ADMIN -> StatusTipo.INFO
        else -> StatusTipo.PELIGRO
    }

    fun etiquetaMotivo(motivo: MotivoAcceso?): String = when (motivo) {
        MotivoAcceso.USUARIO_VENCIDO -> "Mensualidad vencida"
        MotivoAcceso.USUARIO_SUSPENDIDO -> "Cuenta suspendida"
        MotivoAcceso.USUARIO_DE_BAJA -> "Cuenta dada de baja"
        MotivoAcceso.SIN_CUPO -> "Sin cupo"
        MotivoAcceso.TIPO_CUPO_DISTINTO -> "Cupo de otro tipo"
        MotivoAcceso.YA_DENTRO -> "Ya estás dentro"
        MotivoAcceso.PLACA_DESCONOCIDA -> "Placa desconocida"
        MotivoAcceso.SALIDA_CON_DEUDA -> "Pago pendiente"
        MotivoAcceso.FORZADO_ADMIN -> "Registro manual"
        null -> "Denegado"
    }
}
