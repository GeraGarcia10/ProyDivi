package com.example.proyectodivisa

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.proyectodivisa.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prueba temporal para verificar la conexión a la API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Obtener los tipos de cambio desde la API
                val response = RetrofitClient.instance.getExchangeRates()
                Log.d("API_TEST", "Response: ${response.conversion_rates}")
            } catch (e: Exception) {
                Log.e("API_TEST", "Error: ${e.message}")
            }
        }

        // Configuración de la interfaz de usuario (opcional por ahora)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                // Aquí puedes añadir componentes de Compose en el futuro
            }
        }
    }
}
