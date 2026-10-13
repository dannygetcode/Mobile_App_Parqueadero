package com.parqueadero.appparqueadero.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.parqueadero.appparqueadero.data.session.EstadoSesion
import com.parqueadero.appparqueadero.data.session.MotivoCierre
import com.parqueadero.appparqueadero.data.session.SessionManager
import com.parqueadero.appparqueadero.ui.auth.BienvenidaScreen
import com.parqueadero.appparqueadero.ui.auth.activar.ActivarScreen
import com.parqueadero.appparqueadero.ui.auth.login.LoginScreen

object Rutas {
    // Grafo de acceso
    const val BIENVENIDA = "bienvenida"
    const val ACTIVAR = "activar"
    const val LOGIN = "login"

    // Grafo principal
    const val INICIO = "inicio"
    const val PAGOS = "pagos"
    const val ACTIVIDAD = "actividad"
    const val PERFIL = "perfil"
    const val CAMARAS = "camaras"
}

/**
 * Raíz: el estado de la sesión decide qué grafo existe. Al iniciar sesión el grafo de acceso se descarta
 * completo (no queda en la pila) y cualquier 401 o cierre de sesión descarta el principal.
 */
@Composable
fun CupoRaiz(sesion: SessionManager) {
    val estado by sesion.estado.collectAsStateWithLifecycle()
    when (val e = estado) {
        EstadoSesion.Cargando -> Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        EstadoSesion.Activa -> GrafoPrincipal()
        is EstadoSesion.Inactiva -> GrafoAcceso(e.motivo)
    }
}

@Composable
private fun GrafoAcceso(motivo: MotivoCierre?) {
    val nav = rememberNavController()
    // Quien ya tuvo sesión en este proceso vuelve directo al login.
    val inicio = if (motivo != null) Rutas.LOGIN else Rutas.BIENVENIDA

    NavHost(navController = nav, startDestination = inicio) {
        composable(Rutas.BIENVENIDA) {
            BienvenidaScreen(
                onActivarCuenta = { nav.navigate(Rutas.ACTIVAR) { launchSingleTop = true } },
                onIniciarSesion = { nav.navigate(Rutas.LOGIN) { launchSingleTop = true } },
            )
        }
        composable(Rutas.ACTIVAR) {
            ActivarScreen(
                onSalir = { nav.popBackStack() },
                onIrALogin = {
                    nav.navigate(Rutas.LOGIN) {
                        popUpTo(Rutas.ACTIVAR) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Rutas.LOGIN) {
            LoginScreen(
                motivoCierre = motivo,
                onAtras = if (nav.previousBackStackEntry != null) ({ nav.popBackStack() }) else null,
                onActivarCuenta = { nav.navigate(Rutas.ACTIVAR) { launchSingleTop = true } },
            )
        }
    }
}

private data class Pestana(
    val ruta: String,
    val etiqueta: String,
    val icono: ImageVector,
    val iconoSeleccionado: ImageVector,
)

private val PESTANAS = listOf(
    Pestana(Rutas.INICIO, "Inicio", Icons.Outlined.Home, Icons.Rounded.Home),
    Pestana(Rutas.PAGOS, "Pagos", Icons.AutoMirrored.Rounded.ReceiptLong, Icons.AutoMirrored.Rounded.ReceiptLong),
    Pestana(Rutas.ACTIVIDAD, "Actividad", Icons.Outlined.Insights, Icons.Rounded.Insights),
    Pestana(Rutas.PERFIL, "Perfil", Icons.Outlined.Person, Icons.Rounded.Person),
)

@Composable
private fun GrafoPrincipal() {
    val nav = rememberNavController()
    val entrada by nav.currentBackStackEntryAsState()
    val destino = entrada?.destination
    val enPestana = PESTANAS.any { p -> destino?.hierarchy?.any { it.route == p.ruta } == true }

    Scaffold(
        bottomBar = {
            if (enPestana) {
                NavigationBar {
                    PESTANAS.forEach { p ->
                        val seleccionada = destino?.hierarchy?.any { it.route == p.ruta } == true
                        NavigationBarItem(
                            selected = seleccionada,
                            onClick = { nav.irAPestana(p.ruta) },
                            icon = { Icon(if (seleccionada) p.iconoSeleccionado else p.icono, contentDescription = null) },
                            label = { Text(p.etiqueta) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Rutas.INICIO,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable(Rutas.INICIO) {
                InicioProvisional(onAbrirCamaras = { nav.navigate(Rutas.CAMARAS) { launchSingleTop = true } })
            }
            composable(Rutas.PAGOS) { PantallaProvisional("Pagos", "Aquí subirás tus comprobantes y verás tu historial.") }
            composable(Rutas.ACTIVIDAD) { PantallaProvisional("Actividad", "Aquí verás tus visitas y pagos del año.") }
            composable(Rutas.PERFIL) { PerfilProvisional() }
            composable(Rutas.CAMARAS) {
                PantallaProvisional("Cámaras", "Vista simulada de las cámaras del parqueadero.", onAtras = { nav.popBackStack() })
            }
        }
    }
}

/** Cambia de pestaña sin duplicar pantallas: conserva y restaura el estado de cada una. */
private fun NavHostController.irAPestana(ruta: String) {
    navigate(ruta) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
