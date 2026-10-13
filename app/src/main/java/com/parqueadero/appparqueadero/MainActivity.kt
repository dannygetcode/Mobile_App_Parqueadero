package com.parqueadero.appparqueadero

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parqueadero.appparqueadero.ui.navigation.CupoRaiz
import com.parqueadero.appparqueadero.ui.theme.CupoTheme
import com.parqueadero.appparqueadero.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CupoApp).container
        setContent {
            val tema by container.preferencias.tema.collectAsStateWithLifecycle(initialValue = ThemeMode.SISTEMA)
            CupoTheme(themeMode = tema) {
                CupoRaiz(container.sesion)
            }
        }
    }
}
