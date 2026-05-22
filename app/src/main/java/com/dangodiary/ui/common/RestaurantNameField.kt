package com.dangodiary.ui.common

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import kotlinx.coroutines.delay

private const val TAG = "RestaurantNameField"
private const val MIN_QUERY_LEN = 2
private const val DEBOUNCE_MS = 300L

/** A name + address + coordinates snapshot from a picked autocomplete suggestion. */
data class RestaurantPick(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
)

/**
 * Name field with inline Google Places autocomplete.
 *
 * As the user types the restaurant name, the field debounces for [DEBOUNCE_MS] then queries
 * Places for matching predictions and renders them as a dropdown. Picking a suggestion fetches
 * the place's formatted address + coordinates and fires [onPlacePicked], which the screen uses
 * to fill the name *and* address fields atomically. The user can also keep typing to override
 * the suggestion; manual entry is always allowed.
 *
 * Uses [AutocompleteSessionToken] so all queries leading up to a single pick are billed as one
 * session (query + 1 details fetch). A fresh token is minted after every pick.
 *
 * If the Places SDK has not been initialised (e.g. missing API key), the field degrades to a
 * plain editable [OutlinedTextField] with no suggestions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantNameField(
    name: String,
    onNameChange: (String) -> Unit,
    onPlacePicked: (RestaurantPick) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
) {
    val ctx = LocalContext.current
    val placesReady = Places.isInitialized()
    val placesClient = remember(placesReady) {
        if (placesReady) Places.createClient(ctx) else null
    }

    var sessionToken by remember { mutableStateOf(AutocompleteSessionToken.newInstance()) }
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    // Snapshot of the last name the user explicitly picked from a suggestion. Used to suppress
    // the dropdown after a pick — without it, the LaunchedEffect would re-query with the just-
    // picked name and the menu would pop back open immediately.
    var lastPickedName by remember { mutableStateOf("") }

    LaunchedEffect(name) {
        if (!placesReady || placesClient == null) return@LaunchedEffect
        if (name.length < MIN_QUERY_LEN || name == lastPickedName) {
            predictions = emptyList()
            expanded = false
            return@LaunchedEffect
        }
        delay(DEBOUNCE_MS)
        val req = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(sessionToken)
            .setQuery(name)
            .build()
        placesClient.findAutocompletePredictions(req)
            .addOnSuccessListener { resp ->
                predictions = resp.autocompletePredictions
                expanded = predictions.isNotEmpty()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Autocomplete query failed for '$name'", e)
                predictions = emptyList()
                expanded = false
            }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { /* expansion is driven by predictions, not by the user tapping */ },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(label) },
            isError = isError,
            supportingText = supportingText,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable),
        )
        if (expanded && predictions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                predictions.forEach { prediction ->
                    val primary = prediction.getPrimaryText(null).toString()
                    val secondary = prediction.getSecondaryText(null).toString()
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(primary, style = MaterialTheme.typography.bodyLarge)
                                if (secondary.isNotBlank()) {
                                    Text(
                                        secondary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        },
                        onClick = {
                            expanded = false
                            val client = placesClient ?: return@DropdownMenuItem
                            val fetchReq = FetchPlaceRequest.builder(
                                prediction.placeId,
                                listOf(Place.Field.FORMATTED_ADDRESS, Place.Field.LOCATION),
                            ).setSessionToken(sessionToken).build()
                            client.fetchPlace(fetchReq)
                                .addOnSuccessListener { resp ->
                                    val place = resp.place
                                    val addr = place.formattedAddress
                                    val ll = place.location
                                    lastPickedName = primary
                                    if (addr != null && ll != null) {
                                        onPlacePicked(RestaurantPick(primary, addr, ll.latitude, ll.longitude))
                                    } else {
                                        // Place returned no address/location — still adopt the
                                        // name so the field reflects the user's selection.
                                        onNameChange(primary)
                                    }
                                    // Mint a new session for the next query/pick cycle.
                                    sessionToken = AutocompleteSessionToken.newInstance()
                                }
                                .addOnFailureListener { e ->
                                    Log.w(TAG, "Place fetch failed for '$primary'", e)
                                    onNameChange(primary)
                                }
                        },
                    )
                }
            }
        }
    }
}
