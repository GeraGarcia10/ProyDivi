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
import com.example.proyectodivisa.database.AppDatabase
import com.example.proyectodivisa.database.ExchangeRate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtener la instancia de la base de datos
        val database = AppDatabase.getDatabase(this)

        // Prueba temporal para verificar la conexión a la API y guardar los datos
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Obtener los tipos de cambio desde la API
                val response = RetrofitClient.instance.getExchangeRates()
                Log.d("API_TEST", "Response: ${response.conversion_rates}")

                // Convertir los datos de la API a una lista de ExchangeRate
                val rates = response.conversion_rates.map { (currency, rate) ->
                    ExchangeRate(currency, rate)
                }

                // Guardar los datos en la base de datos
                database.exchangeRateDao().insertAll(rates)
                Log.d("DATABASE_TEST", "Datos guardados correctamente")
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