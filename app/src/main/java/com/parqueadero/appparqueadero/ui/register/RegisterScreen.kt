package com.parqueadero.appparqueadero.ui.register

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.parqueadero.appparqueadero.data.model.UsuarioRegistroDTO
import com.parqueadero.appparqueadero.data.network.RetrofitClient
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(navController: NavController) {
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var placa by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
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
        Text("Registro", fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Primer Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = apellido, onValueChange = { apellido = it }, label = { Text("Primer Apellido") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = placa, onValueChange = { placa = it }, label = { Text("Placa del Vehículo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = telefono, onValueChange = { telefono = it }, label = { Text("Número de Teléfono") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(16.dp))

        errorMsg?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        if (success) {
            Text(text = "Registro exitoso. Espera el código del administrador.", color = MaterialTheme.colorScheme.primary)
        }

        Button(
            onClick = {
                if (nombre.isBlank() || apellido.isBlank() || placa.isBlank() || telefono.isBlank()) {
                    errorMsg = "Todos los campos son obligatorios"
                    return@Button
                }

                errorMsg = null
                scope.launch {
                    try {
                        val dto = UsuarioRegistroDTO(
                            nombre = nombre,
                            apellido = apellido,
                            placa = placa,
                            telefono = telefono
                        )
                        RetrofitClient.api.registrarUsuario(dto)
                        success = true
                        // Navega a verificación
                        navController.navigate("verification/${telefono}")
                    } catch (e: Exception) {
                        errorMsg = "Error al registrar: ${e.localizedMessage}"
                        e.printStackTrace()
                    }
                }
            }
,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
        ) {
            Text("Registrarse")
        }
    }
}
