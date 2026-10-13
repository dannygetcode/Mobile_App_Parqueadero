package com.parqueadero.appparqueadero.ui.camaras

import com.parqueadero.appparqueadero.data.model.CamaraDTO
import com.parqueadero.appparqueadero.data.network.ErrorParser
import com.parqueadero.appparqueadero.data.repository.AccesoRepository
import com.parqueadero.appparqueadero.support.FakeApiService
import com.parqueadero.appparqueadero.support.MainDispatcherRule
import com.parqueadero.appparqueadero.support.gsonPrueba
import com.parqueadero.appparqueadero.ui.common.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class CamarasViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val api = FakeApiService()
    private val lista = listOf(CamaraDTO(1, "Entrada", true), CamaraDTO(2, "Patio", false))

    private fun vm() = CamarasViewModel(AccesoRepository(api, ErrorParser(gsonPrueba), gsonPrueba))

    @Test
    fun `carga la lista`() {
        api.camarasResp = { lista }
        assertEquals(UiState.Success(lista), vm().camaras.value)
    }

    @Test
    fun `lista vacia es Empty`() {
        api.camarasResp = { emptyList() }
        assertEquals(UiState.Empty, vm().camaras.value)
    }

    @Test
    fun `error y reintento`() {
        api.camarasResp = { throw IOException() }
        val vm = vm()
        assertTrue(vm.camaras.value is UiState.Error)
        api.camarasResp = { lista }
        vm.cargar()
        assertEquals(UiState.Success(lista), vm.camaras.value)
    }

    @Test
    fun `seleccionar y volver a la lista`() {
        api.camarasResp = { lista }
        val vm = vm()
        assertNull(vm.seleccionada.value)
        vm.seleccionar(lista[1])
        assertEquals(lista[1], vm.seleccionada.value)
        vm.seleccionar(null)
        assertNull(vm.seleccionada.value)
    }
}
