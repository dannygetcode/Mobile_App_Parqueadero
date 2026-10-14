package com.parqueadero.appparqueadero

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.parqueadero.appparqueadero.data.session.EstadoSesion
import com.parqueadero.appparqueadero.data.session.MotivoCierre
import com.parqueadero.appparqueadero.ui.navigation.CupoRaiz
import com.parqueadero.appparqueadero.ui.theme.CupoTheme
import com.parqueadero.appparqueadero.ui.theme.ThemeMode
import kotlinx.coroutines.launch

/** FragmentActivity (no ComponentActivity) porque BiometricPrompt la necesita. */
class MainActivity : FragmentActivity() {

    private val container by lazy { (application as CupoApp).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Comprobantes, pagos y accesos no deben verse en la miniatura de recientes ni en capturas.
        // En debug se permite para poder probar y reportar errores.
        if (!BuildConfig.DEBUG) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
        enableEdgeToEdge()
        // Sin sesión no deben quedar fotos de comprobantes en la caché.
        lifecycleScope.launch {
            container.sesion.estado.collect { estado ->
                if (estado is EstadoSesion.Inactiva) container.fuenteImagen.limpiarTemporales()
            }
        }
        setContent {
            val tema by container.preferencias.tema.collectAsStateWithLifecycle(initialValue = ThemeMode.SISTEMA)
            CupoTheme(themeMode = tema) {
                CupoRaiz(container.sesion, container.preferencias)
            }
        }
    }

    override fun onDestroy() {
        // Al cerrar la app (atrás desde la raíz o quitarla de recientes) hay que volver a entrar con PIN o huella.
        // Un cambio de configuración (rotar, tema) no termina la actividad y conserva la sesión.
        if (isFinishing) container.sesion.cerrar(MotivoCierre.APP_CERRADA)
        super.onDestroy()
    }
}
