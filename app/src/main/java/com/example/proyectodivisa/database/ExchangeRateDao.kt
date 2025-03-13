package com.example.proyectodivisa.database

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExchangeRateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) // Actualiza si ya existe
    suspend fun insertAll(rates: List<ExchangeRate>)

    @Query("SELECT * FROM exchange_rates")
    suspend fun getAll(): List<ExchangeRate> // Devuelve una lista de ExchangeRate

    @Query("SELECT * FROM exchange_rates")
    fun getAllCursor(): Cursor // Devuelve un Cursor (para el ContentProvider)

    @Query("SELECT * FROM exchange_rates WHERE currency = :currency")
    fun getExchangeRateByCurrencyCursor(currency: String): Cursor

    @Query("SELECT * FROM exchange_rates WHERE currency = :currency AND timestamp BETWEEN :startTime AND :endTime")
    fun getExchangeRateByCurrencyAndDateRangeCursor(currency: String, startTime: Long, endTime: Long): Cursor
}
