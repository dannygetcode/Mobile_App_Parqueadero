package com.parqueadero.appparqueadero.ui.payments

import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.parqueadero.appparqueadero.data.api.crearPartesPago
import com.parqueadero.appparqueadero.data.network.RetrofitClient
import kotlinx.coroutines.launch
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PaymentScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var placa by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val userId = context.getSharedPreferences("auth", 0).getLong("user_id", -1L)
    val start = remember { mutableStateOf(LocalDate.now()) }
    val end = remember { mutableStateOf(LocalDate.now().plusMonths(1)) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        imageUri = it
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = placa,
            onValueChange = { placa = it.uppercase() },
            label = { Text("Ingresa Placa") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { /* ver historial */ }) {
                Text("Ver Historial de Pagos")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clickable { launcher.launch("image/*") }
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri == null) {
                Text("Toca para subir imagen")
            } else {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    try {
                        val partes = crearPartesPago(
                            context = context,
                            userId = userId,
                            placa = placa,
                            start = start.value,
                            end = end.value,
                            imageUri = imageUri
                        )

                        val response = RetrofitClient.api.subirPago(
                            userId = partes.userId,
                            placa = partes.placa,
                            start = partes.start,
                            end = partes.end,
                            image = partes.imagen
                        )

                        if (response.isSuccessful) {
                            val pago = response.body()
                            // Aquí podrías mostrar el valor y la fecha OCR
                            // println(pago?.amount)
                            // println(pago?.paymentDate)
                            // o mostrar Snackbar:
                            // SnackbarHostState().showSnackbar(...) solo funciona dentro de Scaffold
                            // Mostrar mensaje
                            Toast.makeText(context, "Pago registrado con éxito", Toast.LENGTH_SHORT).show()

                            // Limpiar campos
                            placa = ""
                            imageUri = null
                            start.value = LocalDate.now()
                            end.value = LocalDate.now().plusMonths(1)
                        } else {
                            println("Error HTTP: ${response.code()}")
                        }
                    } catch (e: Exception) {
                        println("Error de red: ${e.message}")
                    }
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Enviar Pago")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(onClick = { navController.navigate("home") }) {
            Text("Volver al Home")
        }
    }
}
