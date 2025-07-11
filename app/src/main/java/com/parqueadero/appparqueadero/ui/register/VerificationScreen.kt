package com.parqueadero.appparqueadero.ui.register

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.parqueadero.appparqueadero.data.network.RetrofitClient
import kotlinx.coroutines.launch

@Composable
fun VerificationScreen(navController: NavController, telefono: String) {
    var codigo by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Código de Verificación", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = codigo,
            onValueChange = { codigo = it },
            label = { Text("Código (8 caracteres)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        errorMsg?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                if (codigo.length != 8) {
                    errorMsg = "El código debe tener exactamente 8 caracteres"
                    return@Button
                }

                scope.launch {
                    try {
                        val response = RetrofitClient.api.verificarCodigo(telefono, codigo)

                        if (response.isSuccessful) {
                            navController.navigate("createPin/$telefono/$codigo")
                        } else {
                            errorMsg = "Código incorrecto o ya vencido"
                        }
                    } catch (e: Exception) {
                        errorMsg = "Error: ${e.localizedMessage ?: "desconocido"}"
                        e.printStackTrace()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
        ) {
            Text("Enviar")
        }
    }
}

