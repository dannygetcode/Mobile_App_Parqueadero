package com.parqueadero.appparqueadero.ui.register

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.parqueadero.appparqueadero.data.network.RetrofitClient
import kotlinx.coroutines.launch

@Composable
fun CreatePinScreen(navController: NavController, telefono: String, codigo: String) {
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Crear tu PIN", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Usuario: $telefono")

        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text("PIN (4 dígitos)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        errorMsg?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                if (pin.length != 4) {
                    errorMsg = "El PIN debe tener exactamente 4 dígitos"
                    return@Button
                }

                scope.launch {
                    try {
                        val response = RetrofitClient.api.validarCodigoYPin(telefono, codigo, pin)
                        if (response.isSuccessful) {
                            navController.navigate("login/$telefono")
                        } else {
                            errorMsg = "Error al crear el PIN"
                        }

                    } catch (e: Exception) {
                        errorMsg = "Error: ${e.localizedMessage}"
                        e.printStackTrace()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
        ) {
            Text("Crear")
        }
    }
}
