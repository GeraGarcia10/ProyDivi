package com.example.exchange_rate_client

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.exchange_rate_client.data.CurrencyData
import com.example.exchange_rate_client.ui.components.CurrencyComboBox
import com.example.exchange_rate_client.ui.components.ExchangeRateChart
import com.example.exchange_rate_client.ui.components.ShowDatePickerDialog
import com.example.exchange_rate_client.ui.theme.ExchangerateclientTheme
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExchangerateclientTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    ExchangeRateClientApp()
                }
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeRateClientApp() {
    val context = LocalContext.current
    var rate by remember { mutableStateOf("MXN") }
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var result by remember { mutableStateOf("") }
    var chartData = remember { mutableStateListOf<Pair<Long, Double>>() }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Formatear las fechas para mostrarlas en los TextField
    val startDateFormatted = startDate?.let { formatDate(it) } ?: ""
    val endDateFormatted = endDate?.let { formatDate(it) } ?: ""

    // Usar la lista de divisas desde CurrencyData
    val currencies = CurrencyData.currencies

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cliente de Tasas de Cambio") }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Combo box para seleccionar la divisa
                CurrencyComboBox(
                    selectedCurrency = rate,
                    onCurrencySelected = { rate = it },
                    currencies = currencies,
                    modifier = Modifier.fillMaxWidth()
                )


                // Botón para seleccionar la fecha inicial
                OutlinedTextField(
                    value = startDateFormatted,
                    onValueChange = { },
                    label = { Text("Fecha de inicio") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showStartDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha inicial")
                        }
                    }
                )

                // Botón para seleccionar la fecha final
                OutlinedTextField(
                    value = endDateFormatted,
                    onValueChange = { },
                    label = { Text("Fecha de fin") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showEndDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha final")
                        }
                    }
                )

                Button(
                    onClick = {
                        if (startDate != null && endDate != null) {
                            val startDateWithTime = setTimeToDate(startDate!!, 0, 0)
                            val endDateWithTime = setTimeToDate(endDate!!, 23, 59)

                            chartData.clear()
                            val newData = queryExchangeRates(context, startDateWithTime, endDateWithTime, rate)
                            chartData.addAll(newData)

                            Log.d("ExchangeRateClientApp", "New data: $newData")
                            Log.d("ExchangeRateClientApp", "Chart data: $chartData")

                            result = if (chartData.isEmpty()) {
                                "No se encontraron datos para la divisa $rate"
                            } else {
                                "Datos obtenidos correctamente"
                            }
                        } else {
                            result = "Por favor, selecciona ambas fechas"
                        }
                    },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Obtener tasas de cambio")
            }

                // Mostrar la gráfica
                ExchangeRateChart(
                    data = chartData,
                    context = context
                )

                // Mostrar el DatePicker para la fecha inicial
                if (showStartDatePicker) {
                    ShowDatePickerDialog(
                        onDateSelected = { selectedDate ->
                            startDate = selectedDate
                            showStartDatePicker = false
                        },
                        onDismiss = { showStartDatePicker = false }
                    )
                }

                // Mostrar el DatePicker para la fecha final
                if (showEndDatePicker) {
                    ShowDatePickerDialog(
                        onDateSelected = { selectedDate ->
                            endDate = selectedDate
                            showEndDatePicker = false
                        },
                        onDismiss = { showEndDatePicker = false }
                    )
                }

            }
        }
    )
}

// Función para formatear una fecha (timestamp a String)
private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    dateFormat.timeZone = TimeZone.getDefault() // Usar la zona horaria local
    return dateFormat.format(Date(timestamp))
}


private fun formatDateWithTime(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    dateFormat.timeZone = TimeZone.getDefault() // Usar la zona horaria local
    return dateFormat.format(Date(timestamp))
}


private fun setTimeToDate(date: Long, hour: Int, minute: Int): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = date
    calendar.set(Calendar.HOUR_OF_DAY, hour)
    calendar.set(Calendar.MINUTE, minute)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

@SuppressLint("Range")
private fun queryExchangeRates(
    context: Context,
    startDate: Long,
    endDate: Long,
    currencyCode: String
): List<Pair<Long, Double>> {

    val uri = Uri.parse("content://com.example.exchangeRate.providers/exchange_rates_by_date_range")
        .buildUpon()
        .appendQueryParameter("currencyCode", currencyCode) // Agrega el código de la divisa como parámetro
        .build()

    Log.d("QueryExchangeRates", "Querying exchange rates for currency: $currencyCode, Start: ${formatDateWithTime(startDate)}, End: ${formatDateWithTime(endDate)}")

    // Realiza la consulta al ContentProvider
    val cursor = context.contentResolver.query(
        uri,
        null,
        null,
        arrayOf(startDate.toString(), endDate.toString()),
        null
    )

    return cursor?.use {
        val results = mutableListOf<Pair<Long, Double>>()
        Log.d("QueryExchangeRates", "Cursor count: ${it.count}")
        while (it.moveToNext()) {
            val rate = it.getDouble(it.getColumnIndex("rate"))
            val lastUpdateUnix = it.getLong(it.getColumnIndex("last_update_unix"))

            Log.d("QueryExchangeRates", "Fetched row - last_update_unix: ${formatDateWithTime(lastUpdateUnix)}, rate: $rate")
            results.add(lastUpdateUnix to rate)
        }
        Log.d("QueryExchangeRates", "Total results fetched: ${results.size}")
        results
    } ?: run {
        Log.w("QueryExchangeRates", "Cursor is null, returning empty list")
        emptyList()
    }
}

