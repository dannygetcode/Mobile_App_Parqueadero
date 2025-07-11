package com.parqueadero.appparqueadero.ui.login

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.parqueadero.appparqueadero.data.network.RetrofitClient
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController, telefonoInicial: String = "") {
    val scope = rememberCoroutineScope()
    var telefono by remember { mutableStateOf(telefonoInicial) }
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Iniciar Sesión", fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = telefono,
            onValueChange = { telefono = it },
            label = { Text("Número de Teléfono") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text("PIN (4 dígitos)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMsg != null) {
            Text(
                text = errorMsg!!,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp)
            )
        }
        Button(
            onClick = {
                if (telefono.length < 7 || pin.length != 4) {
                    errorMsg = "Revisa que el número y el PIN sean válidos"
                } else {
                    errorMsg = null
                    scope.launch {
                        val response = RetrofitClient.api.login(telefono, pin)
                        if (response.isSuccessful) {
                            val token = response.body()?.get("token") ?: ""
                            if (token.isNotBlank()) {
                                context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                                    .edit()
                                    .putString("jwt_token", token)
                                    .apply()
                                navController.navigate("home")
                            } else {
                                errorMsg = "Token vacío"
                            }
                        } else {
                            errorMsg = "Credenciales inválidas"
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Entrar")
        }

    }
}
