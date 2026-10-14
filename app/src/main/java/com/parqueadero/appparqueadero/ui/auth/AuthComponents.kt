package com.parqueadero.appparqueadero.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.parqueadero.appparqueadero.ui.theme.CupoFonts
import com.parqueadero.appparqueadero.ui.theme.Radius
import com.parqueadero.appparqueadero.ui.theme.Spacing

/** Marca: la señal "P" de parqueadero. */
@Composable
fun CupoMarca(modifier: Modifier = Modifier, tamano: Dp = 56.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(tamano)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(Radius.md))
            .clearAndSetSemantics { contentDescription = "Cupo" },
    ) {
        Text(
            "P",
            color = MaterialTheme.colorScheme.onPrimary,
            fontFamily = CupoFonts.SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            // Logotipo: tamaño ligado al cuadro (dp), no al tamaño de texto del sistema, para que la P no se desborde.
            fontSize = with(LocalDensity.current) { (tamano * 0.6f).toSp() },
        )
    }
}

/** Estructura de las pantallas de acceso: barra superior opcional, contenido con scroll y acción al pie. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScaffold(
    titulo: String,
    subtitulo: String?,
    onAtras: (() -> Unit)?,
    pie: @Composable ColumnScope.() -> Unit,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    if (onAtras != null) {
                        IconButton(onClick = onAtras) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg),
            ) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                if (subtitulo != null) {
                    Text(
                        subtitulo,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(Spacing.xs))
                contenido()
                Spacer(Modifier.height(Spacing.md))
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                content = pie,
            )
        }
    }
}

/** Botón principal con indicador de carga; mantiene el alto mientras carga. */
@Composable
fun BotonPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cargando: Boolean = false,
    habilitado: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = habilitado && !cargando,
        shape = RoundedCornerShape(Radius.md),
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp)
            .heightIn(min = 52.dp),
    ) {
        if (cargando) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(20.dp)
                    .semantics { contentDescription = "Cargando" },
            )
        } else {
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}

enum class TipoAviso { ERROR, INFO }

/** Mensaje en línea que TalkBack anuncia al aparecer. */
@Composable
fun AvisoEnLinea(texto: String, tipo: TipoAviso, modifier: Modifier = Modifier) {
    val (fondo, contenido) = when (tipo) {
        TipoAviso.ERROR -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        TipoAviso.INFO -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        color = fondo,
        contentColor = contenido,
        shape = RoundedCornerShape(Radius.md),
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(Spacing.md),
        ) {
            Icon(
                if (tipo == TipoAviso.ERROR) Icons.Rounded.ErrorOutline else Icons.Rounded.Info,
                contentDescription = null,
            )
            Text(texto, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
