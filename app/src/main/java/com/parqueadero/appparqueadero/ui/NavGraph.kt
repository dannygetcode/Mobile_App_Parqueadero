package com.parqueadero.appparqueadero.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.parqueadero.appparqueadero.ui.login.LoginScreen
import com.parqueadero.appparqueadero.ui.login.WelcomeScreen
import com.parqueadero.appparqueadero.ui.register.CreatePinScreen
import com.parqueadero.appparqueadero.ui.register.RegisterScreen
import com.parqueadero.appparqueadero.ui.register.VerificationScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.parqueadero.appparqueadero.ui.camaras.CamaraScreen
import com.parqueadero.appparqueadero.ui.main.HomeScreen
import com.parqueadero.appparqueadero.ui.payments.PaymentScreen


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "welcome") {
        composable("welcome") { WelcomeScreen(navController) }
        composable(
            route = "login/{telefono}",
            arguments = listOf(
                navArgument("telefono") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val telefono = backStackEntry.arguments?.getString("telefono") ?: ""
            LoginScreen(navController, telefono)
        }
        composable("login") {
            LoginScreen(navController, "")
        }


        composable("register") { RegisterScreen(navController) }
        composable("verification/{telefono}") { backStackEntry ->
            val telefono = backStackEntry.arguments?.getString("telefono") ?: ""
            VerificationScreen(navController, telefono)
        }
        composable("createPin/{telefono}/{codigo}") { backStackEntry ->
            val telefono = backStackEntry.arguments?.getString("telefono") ?: ""
            val codigo = backStackEntry.arguments?.getString("codigo") ?: ""
            CreatePinScreen(navController, telefono, codigo)
        }
        composable("home") {
            HomeScreen(navController)
        }
        composable("pagos") {
            PaymentScreen(navController)
        }

        composable("camaras") {
            CamaraScreen()
        }



    }
}
