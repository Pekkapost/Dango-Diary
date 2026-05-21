package com.dangodiary.ui.common

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode

private const val TAG = "AddressAutocomplete"

/** Snapshot of a place the user picked from autocomplete. */
data class PickedPlace(
    val address: String,
    val latitude: Double,
    val longitude: Double,
)

/**
 * Address picker backed by Google Places autocomplete. Tapping the field launches the official
 * Places autocomplete activity; on success we receive a [PickedPlace] with the formatted address
 * and coordinates. If the Places SDK has not been initialised (e.g. missing API key) the field
 * becomes a no-op tap — surface that to the user via [supportingText].
 */
@Composable
fun AddressAutocompleteField(
    label: String,
    address: String,
    onPlacePicked: (PickedPlace) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search address",
    supportingText: String? = null,
) {
    val ctx = LocalContext.current
    val placesReady = Places.isInitialized()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@rememberLauncherForActivityResult
            val place = runCatching { Autocomplete.getPlaceFromIntent(data) }.getOrNull()
            if (place != null) {
                val addr = place.formattedAddress
                    ?: place.displayName
                    ?: return@rememberLauncherForActivityResult
                val ll = place.location ?: return@rememberLauncherForActivityResult
                onPlacePicked(PickedPlace(addr, ll.latitude, ll.longitude))
            }
        } else if (result.data != null) {
            // RESULT_CANCELED with data = autocomplete error (e.g. quota / key issue).
            // Plain cancel has no data and is silent.
            val status = Autocomplete.getStatusFromIntent(result.data!!)
            Log.w(TAG, "Places autocomplete failed: ${status.statusMessage}")
        }
    }

    fun launch() {
        if (!placesReady) return
        val intent = Autocomplete.IntentBuilder(
            AutocompleteActivityMode.FULLSCREEN,
            listOf(
                Place.Field.ID,
                Place.Field.DISPLAY_NAME,
                Place.Field.FORMATTED_ADDRESS,
                Place.Field.LOCATION,
            ),
        ).build(ctx)
        launcher.launch(intent)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = placesReady) { launch() },
    ) {
        OutlinedTextField(
            value = address,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            supportingText = supportingText?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            // Match the look of an active outlined input despite enabled = false (so the outer
            // Box catches the click). See DatePickerField for the same pattern.
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}
