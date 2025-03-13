package com.example.proyectodivisa.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// Interfaz que define las solicitudes a la API
interface ApiService {
    @GET("v6/5077eec3a7e06a6f424efe91/latest/USD") // aqui va la clave de la API
    suspend fun getExchangeRates(): ExchangeRateResponse
}

// Clase que representa la respuesta de la API
data class ExchangeRateResponse(
    val result: String,
    val conversion_rates: Map<String, Double>
)

// Objeto que proporciona una instancia de Retrofit
object RetrofitClient {
    private const val BASE_URL = "https://v6.exchangerate-api.com/"

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}