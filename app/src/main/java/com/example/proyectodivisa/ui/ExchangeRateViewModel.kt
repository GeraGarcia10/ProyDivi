package com.example.proyectodivisa.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.proyectodivisa.database.AppDatabase
import com.example.proyectodivisa.database.ExchangeRate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ExchangeRateViewModel(private val database: AppDatabase) : ViewModel() {

    // Estado que almacena la lista de tipos de cambio
    private val _exchangeRates = MutableStateFlow<List<ExchangeRate>>(emptyList())
    val exchangeRates: StateFlow<List<ExchangeRate>> = _exchangeRates

    init {
        // Cargar los datos de la base de datos al iniciar el ViewModel
        loadExchangeRates()
    }

    private fun loadExchangeRates() {
        viewModelScope.launch { // Usar viewModelScope para corrutinas
            // Obtener los datos de la base de datos
            val rates = database.exchangeRateDao().getAll()
            _exchangeRates.value = rates
        }
    }

    // Fábrica para crear el ViewModel
    class Factory(private val database: AppDatabase) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ExchangeRateViewModel(database) as T
        }
    }
}