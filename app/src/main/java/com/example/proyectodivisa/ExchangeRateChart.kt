import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.proyectodivisa.database.ExchangeRate
import com.madrapps.plot.line.DataPoint
import com.madrapps.plot.line.LineGraph
import com.madrapps.plot.line.LinePlot

@Composable
fun ExchangeRateChart(exchangeRates: List<ExchangeRate>) {
    // Convertir los datos de ExchangeRate a DataPoint
    val dataPoints = exchangeRates.mapIndexed { index, rate ->
        DataPoint(index.toFloat(), rate.rate.toFloat())
    }

    // Crear una línea para el gráfico
    val line = LinePlot.Line(
        dataPoints, // Lista de puntos de datos
        LinePlot.Connection(color = androidx.compose.ui.graphics.Color.Blue), // Color de la línea
        LinePlot.Intersection(color = androidx.compose.ui.graphics.Color.Red), // Color de los puntos
        LinePlot.Highlight(color = androidx.compose.ui.graphics.Color.Green) // Color de resaltado
    )

    // Mostrar el gráfico de líneas
    LineGraph(
        plot = LinePlot(listOf(line)), // Lista de líneas
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp) // Tamaño del gráfico
    )
}