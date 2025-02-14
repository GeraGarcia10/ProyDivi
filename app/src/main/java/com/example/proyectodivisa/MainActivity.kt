package com.example.proyectodivisa

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.proyectodivisa.api.RetrofitClient
import com.example.proyectodivisa.database.AppDatabase
import com.example.proyectodivisa.database.ExchangeRate
import com.example.proyectodivisa.work.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

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

        // Programar la sincronización cada hora
        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            1, TimeUnit.HOURS // Intervalo de 1 hora
        ).build()

        WorkManager.getInstance(this).enqueue(syncWorkRequest)

        // Configuración de la interfaz de usuario
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                // Obtener una instancia del ViewModel
                val viewModel: ExchangeRateViewModel = viewModel(
                    factory = ExchangeRateViewModel.Factory(database)
                )

                // Observar los cambios en los datos
                val exchangeRates by viewModel.exchangeRates.collectAsState()

                // Mostrar la lista de tipos de cambio
                ExchangeRateList(exchangeRates)
            }
        }
    }
}

@Composable
fun ExchangeRateList(exchangeRates: List<ExchangeRate>) {
    LazyColumn {
        items(exchangeRates) { rate ->
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Moneda: ${rate.currency}")
                Text(text = "Tipo de cambio: ${rate.rate}")
            }
        }
    }
}

// ViewModel para manejar los datos de la interfaz de usuario
class ExchangeRateViewModel(private val database: AppDatabase) : ViewModel() {

    // Estado que almacena la lista de tipos de cambio
    private val _exchangeRates = MutableStateFlow<List<ExchangeRate>>(emptyList())
    val exchangeRates: StateFlow<List<ExchangeRate>> = _exchangeRates

    init {
        // Cargar los datos de la base de datos al iniciar el ViewModel
        loadExchangeRates()
    }

    private fun loadExchangeRates() {
        viewModelScope.launch { // Usar viewModelScope para corrutinas
            // Obtener los datos de la base de datos
            val rates = database.exchangeRateDao().getAll()
            _exchangeRates.value = rates
        }
    }

    // Fábrica para crear el ViewModel
    class Factory(private val database: AppDatabase) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ExchangeRateViewModel(database) as T
        }
    }
}