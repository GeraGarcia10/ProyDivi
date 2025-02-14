package com.example.proyectodivisa.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExchangeRateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) // Actualiza si ya existe
    suspend fun insertAll(rates: List<ExchangeRate>)

    @Query("SELECT * FROM exchange_rates")
    suspend fun getAll(): List<ExchangeRate>
}