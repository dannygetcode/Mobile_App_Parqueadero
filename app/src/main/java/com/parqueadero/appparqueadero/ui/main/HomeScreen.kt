package com.parqueadero.appparqueadero.ui.main

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.parqueadero.appparqueadero.data.model.PuertaDTO
import com.parqueadero.appparqueadero.data.network.RetrofitClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var estadoUsuario by remember { mutableStateOf<String?>(null) }
    var estadoPuerta by remember { mutableStateOf<Boolean?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val token = remember {
        context.getSharedPreferences("auth", Context.MODE_PRIVATE)
            .getString("jwt_token", "") ?: ""
    }

    suspend fun refrescarEstado() {
        isRefreshing = true
        try {
            val response = RetrofitClient.api.obtenerEstadoPuerta("Bearer $token")
            val userResponse = RetrofitClient.api.obtenerMiUsuario("Bearer $token")
            if (response.isSuccessful && userResponse.isSuccessful) {
                estadoPuerta = response.body()?.abierta
                estadoUsuario = userResponse.body()?.estado
            }
        } catch (_: Exception) {} finally {
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        refrescarEstado()
    }

    LaunchedEffect(estadoUsuario) {
        when (estadoUsuario) {
            "VENCIDO" -> snackbarHostState.showSnackbar("Tu cuenta está vencida. Realiza el pago para habilitar el acceso.")
            "SUSPENDIDO" -> snackbarHostState.showSnackbar("Tu cuenta ha sido suspendida por el administrador.")
        }
    }

    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Opciones", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                Divider()
                NavigationDrawerItem(label = { Text("Ver Perfil") }, selected = false, onClick = { /* TODO */ })
                NavigationDrawerItem(label = { Text("Configuración") }, selected = false, onClick = { /* TODO */ })
                NavigationDrawerItem(
                    label = { Text("Cerrar Sesión") }, selected = false,
                    onClick = {
                        context.getSharedPreferences("auth", Context.MODE_PRIVATE).edit().clear().apply()
                        navController.navigate("login") { popUpTo("home") { inclusive = true } }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Panel Principal") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { padding ->
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = { scope.launch { refrescarEstado() } },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(12.dp))
                    estadoUsuario?.let {
                        Text(
                            text = "Estado de cuenta: $it",
                            style = MaterialTheme.typography.titleMedium,
                            color = when (it) {
                                "ACTIVO" -> Color(0xFF2E7D32)
                                "VENCIDO" -> Color(0xFFF9A825)
                                "SUSPENDIDO" -> Color(0xFFC62828)
                                else -> Color.Gray
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    if (estadoUsuario == "ACTIVO") {
                        if (estadoPuerta != null) {
                            Box(Modifier.padding(vertical = 24.dp), Alignment.Center) {
                                CustomCircularButton(
                                    isAbierto = estadoPuerta == true,
                                    onClick = {
                                        val nuevoEstado = !(estadoPuerta ?: false)
                                        estadoPuerta = nuevoEstado
                                        scope.launch {
                                            try {
                                                RetrofitClient.api.actualizarEstadoPuerta(
                                                    "Bearer $token",
                                                    PuertaDTO(nuevoEstado)
                                                )
                                            } catch (_: Exception) {}
                                        }
                                    }
                                )
                            }
                        } else {
                            Text("Cargando estado de la puerta...", color = Color.Gray)
                        }
                    } else {
                        Spacer(Modifier.height(36.dp))
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Acceso bloqueado",
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = when (estadoUsuario) {
                                "VENCIDO" -> "Acceso bloqueado por falta de pago."
                                "SUSPENDIDO" -> "Acceso suspendido por el administrador."
                                else -> ""
                            },
                            color = Color(0xFFC62828),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp)
                    ) {
                        Button(
                            onClick = { navController.navigate("camaras") },
                            enabled = estadoUsuario == "ACTIVO"
                        ) {
                            Text("Cámaras")
                        }
                        Button(onClick = { navController.navigate("pagos") }) {
                            Text("Pagos")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomCircularButton(
    isAbierto: Boolean,
    onClick: () -> Unit
) {
    val scale = animateFloatAsState(if (isAbierto) 1.05f else 1.0f, label = "scale")
    val colorBorde = if (isAbierto) Color(0xFF4CAF50) else Color(0xFFE53935)
    val colorTexto = if (isAbierto) Color(0xFF388E3C) else Color(0xFFB71C1C)
    val icono = Icons.Default.Lock

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(180.dp)
            .scale(scale.value)
            .clip(CircleShape)
            .background(Color.White, shape = CircleShape)
            .border(width = 5.dp, color = colorBorde, shape = CircleShape)
            .clickable(onClick = onClick)
            .shadow(10.dp, shape = CircleShape)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = colorTexto,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = if (isAbierto) "Abierto" else "Cerrado",
                style = MaterialTheme.typography.titleMedium,
                color = colorTexto
            )
        }
    }
}