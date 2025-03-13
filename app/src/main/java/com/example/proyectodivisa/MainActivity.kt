package com.example.proyectodivisa

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
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
    val context = LocalContext.current
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }

    Column {
        Button(onClick = {
            // Mostrar el DatePickerDialog para seleccionar la fecha de inicio
            showDatePicker(context) { year, month, day ->
                val calendar = Calendar.getInstance()
                calendar.set(year, month, day)
                startDate = calendar.timeInMillis
            }
        }) {
            Text("Seleccionar fecha de inicio")
        }

        Button(onClick = {
            // Mostrar el DatePickerDialog para seleccionar la fecha de fin
            showDatePicker(context) { year, month, day ->
                val calendar = Calendar.getInstance()
                calendar.set(year, month, day)
                endDate = calendar.timeInMillis
            }
        }) {
            Text("Seleccionar fecha de fin")
        }

        Button(onClick = {
            if (startDate != null && endDate != null) {
                // Llamar a la función para consultar el ContentProvider
                (context as? MainActivity)?.consultarContentProvider(startDate!!, endDate!!)
            } else {
                Log.d("ContentProvider", "Selecciona ambas fechas")
            }
        }) {
            Text("Consultar ContentProvider")
        }

        LazyColumn {
            items(exchangeRates) { rate ->
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Moneda: ${rate.currency}")
                    Text(text = "Tipo de cambio: ${"%.2f".format(rate.rate)}")
                    Text(text = "Última actualización: ${formatTimestamp(rate.timestamp)}")
                }
            }
        }
    }
}

// Función para mostrar el DatePickerDialog
private fun showDatePicker(context: android.content.Context, onDateSelected: (Int, Int, Int) -> Unit) {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    DatePickerDialog(context, { _, selectedYear, selectedMonth, selectedDay ->
        onDateSelected(selectedYear, selectedMonth, selectedDay)
    }, year, month, day).show()
}

// Función para formatear el timestamp
@SuppressLint("SimpleDateFormat")
fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
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

// Función para consultar el ContentProvider
private fun MainActivity.consultarContentProvider(startTime: Long, endTime: Long) {
    // Usar corrutinas para acceder a la base de datos en segundo plano
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // URI para consultar el tipo de cambio de USD
            val uri = Uri.parse("content://com.example.proyectodivisa.provider/exchange_rates/USD")

            // Realizar la consulta al ContentProvider
            val cursor = contentResolver.query(uri, null, null, arrayOf(startTime.toString(), endTime.toString()), null)

            cursor?.use {
                val currencyIndex = it.getColumnIndex("currency")
                val rateIndex = it.getColumnIndex("rate")
                val timestampIndex = it.getColumnIndex("timestamp")

                while (it.moveToNext()) {
                    val currency = if (currencyIndex >= 0) it.getString(currencyIndex) else "Desconocido"
                    val rate = if (rateIndex >= 0) it.getDouble(rateIndex) else 0.0
                    val timestamp = if (timestampIndex >= 0) it.getLong(timestampIndex) else 0L
                    Log.d("ContentProvider", "Moneda: $currency, Tipo de cambio: $rate, Fecha: ${formatTimestamp(timestamp)}")
                }
            }
        } catch (e: Exception) {
            Log.e("ContentProvider", "Error al consultar la base de datos: ${e.message}")
        }
    }
}