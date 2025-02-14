package com.example.proyectodivisa.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDao {
    @Insert
    suspend fun insertAll(rates: List<ExchangeRate>)

    @Query("SELECT * FROM exchange_rates")
    fun getAll(): Flow<List<ExchangeRate>> // Usamos Flow para observar cambios
}