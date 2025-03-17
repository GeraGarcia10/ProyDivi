//package com.example.proyectodivisa
//
//import android.graphics.Color
//import android.widget.LinearLayout
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.viewinterop.AndroidView
//import com.github.mikephil.charting.components.XAxis
//import com.github.mikephil.charting.data.Entry
//import com.github.mikephil.charting.data.LineData
//import com.github.mikephil.charting.data.LineDataSet
//import com.github.mikephil.charting.formatter.ValueFormatter
//import com.patrykandpatrick.vico.compose.style.ChartStyle
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//
//@Composable
//fun LineChartView(data: Map<String, List<Pair<Long, Double>>>) {
//    AndroidView(
//        factory = { context ->
//            ChartStyle.LineChart(context).apply {
//                layoutParams = LinearLayout.LayoutParams(
//                    LinearLayout.LayoutParams.MATCH_PARENT,
//                    LinearLayout.LayoutParams.MATCH_PARENT
//                )
//                description.isEnabled = false // Deshabilitar la descripción
//                setTouchEnabled(true) // Habilitar interacción con el gráfico
//                setDrawGridBackground(false) // Deshabilitar el fondo de la cuadrícula
//                isDragEnabled = true // Habilitar arrastre
//                setScaleEnabled(true) // Habilitar zoom
//                setPinchZoom(true) // Habilitar zoom con pellizco
//            }
//        },
//        update = { lineChart ->
//            // Limpiar el gráfico antes de agregar nuevos datos
//            lineChart.clear()
//
//            // Convertir los datos a entradas de MPAndroidChart
//            val entriesList = data.map { (currency, rates) ->
//                rates.mapIndexed { index, (timestamp, rate) ->
//                    Entry(index.toFloat(), rate.toFloat())
//                }
//            }
//
//            // Crear un LineDataSet para cada moneda
//            val lineDataSets = entriesList.mapIndexed { index, entries ->
//                LineDataSet(entries, "Moneda ${index + 1}").apply {
//                    color = when (index) {
//                        0 -> Color.BLUE
//                        1 -> Color.RED
//                        else -> Color.GREEN
//                    } // Colores para cada línea
//                    setCircleColor(Color.BLACK) // Color de los puntos
//                    lineWidth = 2f // Grosor de la línea
//                    setDrawValues(false) // No mostrar valores encima de los puntos
//                }
//            }
//
//            // Configurar el eje X
//            lineChart.xAxis.apply {
//                position = XAxis.XAxisPosition.BOTTOM
//                valueFormatter = object : ValueFormatter() {
//                    private val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
//
//                    override fun getFormattedValue(value: Float): String {
//                        val timestamp = data.values.flatten()[value.toInt()].first
//                        return dateFormat.format(Date(timestamp))
//                    }
//                }
//                setDrawGridLines(false) // Deshabilitar líneas de la cuadrícula
//                granularity = 1f // Mostrar todas las etiquetas
//            }
//
//            // Configurar el eje Y
//            lineChart.axisLeft.apply {
//                setDrawGridLines(true) // Habilitar líneas de la cuadrícula
//                granularity = 0.1f // Intervalo entre valores
//            }
//
//            // Deshabilitar el eje Y derecho
//            lineChart.axisRight.isEnabled = false
//
//            // Agregar los datos al gráfico
//            lineChart.data = LineData(lineDataSets)
//            lineChart.invalidate() // Refrescar el gráfico
//        }
//    )
//}