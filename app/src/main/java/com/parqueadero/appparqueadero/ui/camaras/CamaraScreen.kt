package com.parqueadero.appparqueadero.ui.camaras

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.parqueadero.appparqueadero.data.model.CamaraDTO
import com.parqueadero.appparqueadero.data.network.RetrofitClient
import kotlinx.coroutines.launch

@Composable
fun CamaraScreen() {
    val scope = rememberCoroutineScope()
    var camaras by remember { mutableStateOf<List<CamaraDTO>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val api = RetrofitClient.api
                camaras = api.getCamaras().sortedWith(
                    compareByDescending<CamaraDTO> { it.activa }.thenBy { it.id }
                )
            } catch (e: Exception) {
                error = "Error al cargar cámaras: ${e.localizedMessage}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Cámaras en Tiempo Real",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .padding(top = 65.dp, bottom = 16.dp)
                .align(Alignment.CenterHorizontally)
        )

        if (error != null) {
            Text(text = error!!, color = MaterialTheme.colorScheme.error)
            return@Column
        }

        // Mostramos dos recuadros fijos, aunque solo una cámara esté activa
        repeat(2) { index ->
            val camara = camaras.getOrNull(index)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .padding(top = 24.dp, bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = camara?.nombre ?: "Cámara ${index + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .padding(top = 24.dp, bottom = 16.dp)
                            .align(Alignment.CenterHorizontally)
                    )

                    if (camara != null && camara.activa) {
                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
                                    webViewClient = WebViewClient()
                                    settings.apply {
                                        javaScriptEnabled = true
                                        loadWithOverviewMode = true
                                        useWideViewPort = true // evita zoom excesivo
                                    }
                                    loadUrl(camara.url)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color(0xFF2C2C2C)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cámara no disponible",
                                color = Color.White
                            )
                        }
                    }
                }
            }

        }
    }
}
