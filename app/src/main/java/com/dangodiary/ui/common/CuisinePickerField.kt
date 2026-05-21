package com.dangodiary.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dangodiary.data.CuisineCatalog

/**
 * Outlined picker that opens a dropdown of cuisines grouped by supertype (Restaurants / Cafés /
 * Bars). The selected value is the cuisine id from [CuisineCatalog]; null means unset.
 */
@Composable
fun CuisinePickerField(
    label: String,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select cuisine",
    clearLabel: String = "Clear",
) {
    var open by remember { mutableStateOf(false) }
    val displayText = CuisineCatalog.labelFor(selectedId) ?: ""

    Box(modifier = modifier.fillMaxWidth()) {
        // Same disabled-but-styled-active trick as DatePickerField: the field is non-interactive
        // so the surrounding Box can catch the click without the TextField intercepting it.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = true },
        ) {
            OutlinedTextField(
                value = displayText,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text(label) },
                placeholder = { Text(placeholder) },
                trailingIcon = {
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }

        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
        ) {
            if (selectedId != null) {
                DropdownMenuItem(
                    text = { Text(clearLabel) },
                    onClick = {
                        onSelect(null)
                        open = false
                    },
                )
            }
            CuisineCatalog.grouped.forEach { (group, items) ->
                Text(
                    text = group.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                items.forEach { cuisine ->
                    DropdownMenuItem(
                        text = { Text(cuisine.label) },
                        onClick = {
                            onSelect(cuisine.id)
                            open = false
                        },
                    )
                }
            }
        }
    }
}
