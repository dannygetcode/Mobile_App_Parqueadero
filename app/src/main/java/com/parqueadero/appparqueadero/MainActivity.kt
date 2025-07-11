package com.parqueadero.appparqueadero

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.parqueadero.appparqueadero.ui.AppNavGraph
import com.parqueadero.appparqueadero.ui.theme.AppParqueaderoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppParqueaderoTheme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }
        }

    }
}
