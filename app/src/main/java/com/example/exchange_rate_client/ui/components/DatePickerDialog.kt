package com.example.exchange_rate_client.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.util.Calendar
import java.util.TimeZone


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDatePickerDialog(
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedDateMillis = datePickerState.selectedDateMillis ?: 0L
                    val adjustedDateMillis = adjustDateToLocalTimezone(selectedDateMillis)
                    onDateSelected(adjustedDateMillis)
                    onDismiss()
                }
            ) {
                Text("Seleccionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}


private fun adjustDateToLocalTimezone(utcDate: Long): Long {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendar.timeInMillis = utcDate

    // Obtener solo la fecha (sin hora) en UTC
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    // Crear un nuevo Calendar en la zona horaria local con la fecha obtenida
    val localCalendar = Calendar.getInstance()
    localCalendar.set(year, month, day, 0, 0, 0) // Establecer la hora a 00:00:00 en la zona horaria local
    localCalendar.set(Calendar.MILLISECOND, 0)

    return localCalendar.timeInMillis
}
