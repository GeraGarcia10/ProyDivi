package com.example.proyectodivisa.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exchange_rates")
data class ExchangeRate(
    @PrimaryKey val currency: String, // Moneda (ej: USD, EUR, GBP)
    val rate: Double, // Tipo de cambio
    val timestamp: Long = System.currentTimeMillis() // Fecha y hora de la sincronización
)