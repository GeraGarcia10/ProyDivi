package com.example.proyectodivisa.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.proyectodivisa.api.RetrofitClient
import com.example.proyectodivisa.database.AppDatabase
import com.example.proyectodivisa.database.ExchangeRate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Obtener la instancia de la base de datos
            val database = AppDatabase.getDatabase(applicationContext)

            // Obtener los tipos de cambio desde la API
            val response = RetrofitClient.instance.getExchangeRates()
            val rates = response.conversion_rates.map { (currency, rate) ->
                ExchangeRate(currency, rate)
            }

            // Guardar los datos en la base de datos
            database.exchangeRateDao().insertAll(rates)

            // Indicar que el trabajo se completó con éxito
            Result.success()
        } catch (e: Exception) {
            // Indicar que el trabajo falló
            Result.failure()
        }
    }
}