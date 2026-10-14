package com.parqueadero.appparqueadero.ui.common

import com.parqueadero.appparqueadero.data.network.AppError
import com.parqueadero.appparqueadero.data.network.Resultado
import com.parqueadero.appparqueadero.data.network.mensaje

/** Estado de una pantalla que carga datos. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Error(val mensaje: String, val error: AppError? = null) : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
}

/** Convierte un resultado en estado; `vacio` decide cuándo mostrar Empty (por ejemplo, una lista sin elementos). */
fun <T> Resultado<T>.aUiState(vacio: (T) -> Boolean = { false }): UiState<T> = when (this) {
    is Resultado.Ok -> if (vacio(datos)) UiState.Empty else UiState.Success(datos)
    is Resultado.Falla -> UiState.Error(error.mensaje(), error)
}
