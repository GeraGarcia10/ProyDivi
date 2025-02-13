package com.example.proyectodivisa.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Interfaz que define las solicitudes a la API
interface ApiService {
    @GET("v6/e168862e33968d80f20e7b07/latest/USD") // Cambia YOUR_API_KEY por tu clave
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