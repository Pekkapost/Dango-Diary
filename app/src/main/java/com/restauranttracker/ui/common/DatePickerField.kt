package com.restauranttracker.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.restauranttracker.util.formatDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    epochDay: Long,
    onDateChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { open = true },
    ) {
        // A read-only field is the cleanest visual; we forward all clicks via the Box wrapper
        // because the TextField swallows them otherwise.
        OutlinedTextField(
            value = formatDate(epochDay),
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (open) {
        // Material3's DatePicker treats selectedDateMillis as UTC midnight of the chosen day,
        // so we must use UTC on both sides — converting via the system zone shifts the date
        // for users west of UTC.
        val initialMillis = LocalDate.ofEpochDay(epochDay)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        val picked = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        onDateChange(picked.toEpochDay())
                    }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}
