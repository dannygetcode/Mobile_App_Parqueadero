package com.parqueadero.appparqueadero.data.model

import java.time.LocalDate

data class PaymentDTO(
    val id: Long?,
    val userId: Long,
    val paymentDate: String?,
    val serviceStart: String,
    val serviceEnd: String,
    val ocrData: Map<String, String>?,
    val placa: String,
    val imageUrl: String?,
    val amount: Long?
)
