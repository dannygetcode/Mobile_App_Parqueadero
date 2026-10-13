package com.parqueadero.appparqueadero.ui.camaras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.parqueadero.appparqueadero.container
import com.parqueadero.appparqueadero.data.model.CamaraDTO
import com.parqueadero.appparqueadero.data.repository.AccesoRepository
import com.parqueadero.appparqueadero.ui.common.UiState
import com.parqueadero.appparqueadero.ui.common.aUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CamarasViewModel(private val accesos: AccesoRepository) : ViewModel() {

    private val _camaras = MutableStateFlow<UiState<List<CamaraDTO>>>(UiState.Loading)
    val camaras: StateFlow<UiState<List<CamaraDTO>>> = _camaras.asStateFlow()

    /** Cámara abierta en la vista de demostración, o null para la lista. */
    private val _seleccionada = MutableStateFlow<CamaraDTO?>(null)
    val seleccionada: StateFlow<CamaraDTO?> = _seleccionada.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _camaras.value = UiState.Loading
        viewModelScope.launch {
            _camaras.value = accesos.camaras().aUiState { it.isEmpty() }
        }
    }

    fun seleccionar(camara: CamaraDTO?) {
        _seleccionada.value = camara
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { CamarasViewModel(container.accesoRepository) }
        }
    }
}
