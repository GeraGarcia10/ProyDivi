package com.example.exchange_rate_client.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.widget.LinearLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExchangeRateChart(
    data: List<Pair<Long, Double>>,
    context: Context
) {
    if (data.isEmpty()) {
        Text("No hay datos para mostrar")
        return
    }

    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                configureChart(this, data)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .padding(16.dp),
        update = { chart ->
            configureChart(chart, data)
        }
    )
}

private fun configureChart(
    chart: LineChart,
    data: List<Pair<Long, Double>>
) {
    chart.clear() // Limpiar el gráfico antes de agregar nuevos datos
    if (data.isEmpty()) return

    // Convertir los datos al formato que espera MPAndroidChart
    val entries = data.mapIndexed { index, (date, rate) ->
        Entry(index.toFloat(), rate.toFloat())
    }

    // Calcular el rango del eje Y
    val minRate = data.minOf { it.second }.toFloat()
    val maxRate = data.maxOf { it.second }.toFloat()

    // Configurar el eje X
    chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
    chart.xAxis.valueFormatter = object : ValueFormatter() {
        @SuppressLint("ConstantLocale")
        private val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

        override fun getFormattedValue(value: Float): String {
            val date = data[value.toInt()].first
            return dateFormat.format(Date(date))
        }
    }

    chart.xAxis.setLabelCount(5, true)
    chart.xAxis.textSize = 10f
    chart.xAxis.setDrawLabels(true)

    // Configurar el eje Y
    chart.axisLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
    chart.axisLeft.textSize = 10f
    chart.axisLeft.setLabelCount(10, true)
    chart.axisLeft.setDrawLabels(true)
    chart.axisRight.isEnabled = false

    // Establecer el rango del eje Y
    chart.axisLeft.axisMinimum = minRate - 0.01f
    chart.axisLeft.axisMaximum = maxRate + 0.01f

    // Formatear los valores del eje Y
    chart.axisLeft.setValueFormatter(object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return String.format(Locale.getDefault(), "%.6f", value)
        }
    })

    // Crear el conjunto de datos
    val dataSet = LineDataSet(entries, "Tasa de cambio").apply {
        color = android.graphics.Color.BLUE
        valueTextColor = android.graphics.Color.BLACK
        lineWidth = 2f
    }

    // Agregar los datos al gráfico
    chart.data = LineData(dataSet)
    chart.invalidate() // Refrescar el gráfico
}