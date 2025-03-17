package com.example.proyectodivisa.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import com.example.proyectodivisa.database.AppDatabase

class ExchangeRateProvider : ContentProvider() {

    private lateinit var database: AppDatabase

    // URI Matcher para identificar las solicitudes
    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(AUTHORITY, "exchange_rates", EXCHANGE_RATES)
        addURI(AUTHORITY, "exchange_rates/*", EXCHANGE_RATE_BY_CURRENCY)
    }

    companion object {
        const val AUTHORITY = "com.example.proyectodivisa.provider"
        const val EXCHANGE_RATES = 1
        const val EXCHANGE_RATE_BY_CURRENCY = 2
    }

    override fun onCreate(): Boolean {
        database = AppDatabase.getDatabase(context!!)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        return when (uriMatcher.match(uri)) {
            EXCHANGE_RATES -> {
                // Consulta para obtener todos los tipos de cambio
                database.exchangeRateDao().getAllCursor()
            }
            EXCHANGE_RATE_BY_CURRENCY -> {
                // Consulta para obtener el tipo de cambio de una moneda específica
                val currency = uri.lastPathSegment ?: throw IllegalArgumentException("Moneda no especificada")

                // Verifica si se proporcionaron argumentos para el rango de fechas
                if (selectionArgs != null && selectionArgs.size >= 2) {
                    try {
                        // Obtén las fechas de inicio y fin desde selectionArgs
                        val startTime = selectionArgs[0].toLong()
                        val endTime = selectionArgs[1].toLong()

                        // Realiza la consulta con el rango de fechas
                        database.exchangeRateDao().getExchangeRateByCurrencyAndDateRangeCursor(currency, startTime, endTime)
                    } catch (e: NumberFormatException) {
                        throw IllegalArgumentException("Formato de fecha inválido")
                    }
                } else {
                    // Consulta sin rango de fechas
                    database.exchangeRateDao().getExchangeRateByCurrencyCursor(currency)
                }
            }
            else -> throw IllegalArgumentException("URI desconocida: $uri")
        }
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            EXCHANGE_RATES -> "vnd.android.cursor.dir/vnd.com.example.proyectodivisa.exchange_rates"
            EXCHANGE_RATE_BY_CURRENCY -> "vnd.android.cursor.item/vnd.com.example.proyectodivisa.exchange_rates"
            else -> throw IllegalArgumentException("URI desconocida: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException("No se permite la inserción en este ContentProvider")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        throw UnsupportedOperationException("No se permite la eliminación en este ContentProvider")
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        throw UnsupportedOperationException("No se permite la actualización en este ContentProvider")
    }
}