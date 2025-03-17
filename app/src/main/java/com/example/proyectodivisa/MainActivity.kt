package com.example.proyectodivisa

import ExchangeRateChart
import com.example.proyectodivisa.ui.ExchangeRateViewModel
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import kotlinx.coroutines.withContext
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

                // Estado para la moneda seleccionada
                var compareCurrency by remember { mutableStateOf("USD") }

                // Función para manejar los datos cargados
                val onDataLoaded: (List<ExchangeRate>) -> Unit = { rates ->
                    viewModel.updateRates(rates) // Actualizar el ViewModel con los nuevos datos
                }

                // Función para manejar la selección de moneda
                val onCurrencySelected: (String) -> Unit = { currency ->
                    compareCurrency = currency
                }

                // Función para manejar la selección de rango de fechas
                val onDateRangeSelected: (Long, Long) -> Unit = { start, end ->
                    // Aquí puedes manejar el rango de fechas si es necesario
                }

                // Mostrar la lista de tipos de cambio
                ExchangeRateList(
                    exchangeRates = exchangeRates,
                    compareCurrency = compareCurrency,
                    onDataLoaded = onDataLoaded,
                    onCurrencySelected = onCurrencySelected,
                    onDateRangeSelected = onDateRangeSelected
                )
            }
        }
    }
}

@Composable
fun ExchangeRateList(
    exchangeRates: List<ExchangeRate>,
    compareCurrency: String, // Moneda para comparar
    onDataLoaded: (List<ExchangeRate>) -> Unit, // Callback para datos cargados
    onCurrencySelected: (String) -> Unit, // Callback para selección de moneda
    onDateRangeSelected: (Long, Long) -> Unit // Callback para selección de rango de fechas
) {
    val context = LocalContext.current
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }

    Column(modifier = Modifier.padding(16.dp)) {
        // Selector de moneda
        TextField(
            value = compareCurrency,
            onValueChange = { onCurrencySelected(it) }, // Actualizar la moneda seleccionada
            label = { Text("Moneda") },
            modifier = Modifier.fillMaxWidth()
        )

        // Selectores de fecha
        Button(
            onClick = { showDatePicker(context) { year, month, day ->
                val calendar = Calendar.getInstance()
                calendar.set(year, month, day)
                startDate = calendar.timeInMillis
            } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Seleccionar fecha de inicio")
        }

        Button(
            onClick = { showDatePicker(context) { year, month, day ->
                val calendar = Calendar.getInstance()
                calendar.set(year, month, day)
                endDate = calendar.timeInMillis
            } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Seleccionar fecha de fin")
        }

        // Botón para consultar el ContentProvider
        Button(
            onClick = {
                if (startDate != null && endDate != null) {
                    (context as? MainActivity)?.consultarContentProvider(
                        startDate!!,
                        endDate!!,
                        compareCurrency,
                        compareCurrency, // Moneda base y moneda a comparar
                        onDataLoaded = { data ->
                            // Convertir los datos a ExchangeRate si es necesario
                            val rates = data.flatMap { (currency, rates) ->
                                rates.map { (timestamp, rate) ->
                                    ExchangeRate(currency, rate, timestamp)
                                }
                            }
                            onDataLoaded(rates) // Pasar los datos al callback
                        }
                    )
                } else {
                    Log.d("ContentProvider", "Selecciona ambas fechas")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Consultar ContentProvider")
        }

        // Mostrar el gráfico
        if (exchangeRates.isNotEmpty()) {
            ExchangeRateChart(exchangeRates)
        } else {
            Text("No hay datos para mostrar", modifier = Modifier.padding(vertical = 16.dp))
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
private fun MainActivity.consultarContentProvider(
    startTime: Long,
    endTime: Long,
    baseCurrency: String,
    compareCurrency: String,
    onDataLoaded: (Map<String, List<Pair<Long, Double>>>) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // Consultar la moneda base
            val uriBase = Uri.parse("content://com.example.proyectodivisa.provider/exchange_rates/$baseCurrency")
            val cursorBase = contentResolver.query(uriBase, null, null, arrayOf(startTime.toString(), endTime.toString()), null)

            // Consultar la moneda a comparar
            val uriCompare = Uri.parse("content://com.example.proyectodivisa.provider/exchange_rates/$compareCurrency")
            val cursorCompare = contentResolver.query(uriCompare, null, null, arrayOf(startTime.toString(), endTime.toString()), null)

            val data = mutableMapOf<String, List<Pair<Long, Double>>>()

            // Procesar los datos de la moneda base
            val baseData = mutableListOf<Pair<Long, Double>>()
            cursorBase?.use {
                val currencyIndex = it.getColumnIndex("currency")
                val rateIndex = it.getColumnIndex("rate")
                val timestampIndex = it.getColumnIndex("timestamp")

                while (it.moveToNext()) {
                    val currency = if (currencyIndex >= 0) it.getString(currencyIndex) else "Desconocido"
                    val rate = if (rateIndex >= 0) it.getDouble(rateIndex) else 0.0
                    val timestamp = if (timestampIndex >= 0) it.getLong(timestampIndex) else 0L

                    baseData.add(timestamp to rate)
                    Log.d("ContentProvider", "Moneda base: $currency, Tipo de cambio: $rate, Fecha: ${formatTimestamp(timestamp)}")
                }
            }
            data[baseCurrency] = baseData

            // Procesar los datos de la moneda a comparar
            val compareData = mutableListOf<Pair<Long, Double>>()
            cursorCompare?.use {
                val currencyIndex = it.getColumnIndex("currency")
                val rateIndex = it.getColumnIndex("rate")
                val timestampIndex = it.getColumnIndex("timestamp")

                while (it.moveToNext()) {
                    val currency = if (currencyIndex >= 0) it.getString(currencyIndex) else "Desconocido"
                    val rate = if (rateIndex >= 0) it.getDouble(rateIndex) else 0.0
                    val timestamp = if (timestampIndex >= 0) it.getLong(timestampIndex) else 0L

                    compareData.add(timestamp to rate)
                    Log.d("ContentProvider", "Moneda a comparar: $currency, Tipo de cambio: $rate, Fecha: ${formatTimestamp(timestamp)}")
                }
            }
            data[compareCurrency] = compareData

            // Pasar los datos al callback
            withContext(Dispatchers.Main) {
                onDataLoaded(data)
            }
        } catch (e: Exception) {
            Log.e("ContentProvider", "Error al consultar la base de datos: ${e.message}")
        }
    }
}