package com.parqueadero.appparqueadero.ui.camaras

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parqueadero.appparqueadero.data.model.CamaraDTO
import com.parqueadero.appparqueadero.ui.common.UiState
import com.parqueadero.appparqueadero.ui.components.EmptyState
import com.parqueadero.appparqueadero.ui.components.ErrorState
import com.parqueadero.appparqueadero.ui.components.Skeleton
import com.parqueadero.appparqueadero.ui.components.StatusBadge
import com.parqueadero.appparqueadero.ui.components.StatusTipo
import com.parqueadero.appparqueadero.ui.theme.Radius
import com.parqueadero.appparqueadero.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CamarasScreen(
    onAtras: () -> Unit,
    viewModel: CamarasViewModel = viewModel(factory = CamarasViewModel.Factory),
) {
    val camaras by viewModel.camaras.collectAsStateWithLifecycle()
    val seleccionada by viewModel.seleccionada.collectAsStateWithLifecycle()

    BackHandler(enabled = seleccionada != null) { viewModel.seleccionar(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(seleccionada?.nombre ?: "Cámaras") },
                navigationIcon = {
                    IconButton(onClick = { if (seleccionada != null) viewModel.seleccionar(null) else onAtras() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        val camara = seleccionada
        if (camara != null) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.md),
            ) {
                VistaSimulada(camara)
                EstadoCamara(camara)
                Text(
                    "Las cámaras del parqueadero son simuladas: esta vista es una demostración y no transmite video.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        when (val c = camaras) {
            UiState.Loading -> Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.padding(padding).padding(Spacing.md),
            ) {
                repeat(3) {
                    Skeleton(Modifier.fillMaxWidth().height(72.dp), shape = RoundedCornerShape(Radius.md))
                }
            }
            UiState.Empty -> EmptyState(
                titulo = "Sin cámaras",
                mensaje = "Aún no hay cámaras configuradas.",
                icono = Icons.Outlined.VideocamOff,
                modifier = Modifier.padding(padding),
            )
            is UiState.Error -> ErrorState(
                mensaje = c.mensaje,
                titulo = "No pudimos cargar las cámaras",
                onReintentar = viewModel::cargar,
                modifier = Modifier.padding(padding),
            )
            is UiState.Success -> LazyColumn(
                contentPadding = PaddingValues(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                items(c.data, key = { it.id }) { cam ->
                    FilaCamara(cam, onClick = { viewModel.seleccionar(cam) })
                }
            }
        }
    }
}

@Composable
private fun FilaCamara(camara: CamaraDTO, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(Radius.md),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.padding(Spacing.md),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(Radius.md),
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    if (camara.activa) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff,
                    contentDescription = null,
                    modifier = Modifier.padding(Spacing.xs),
                )
            }
            Text(camara.nombre, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            EstadoCamara(camara)
        }
    }
}

@Composable
private fun EstadoCamara(camara: CamaraDTO) {
    if (camara.activa) StatusBadge(StatusTipo.EXITO, "Activa") else StatusBadge(StatusTipo.NEUTRO, "Sin señal")
}
