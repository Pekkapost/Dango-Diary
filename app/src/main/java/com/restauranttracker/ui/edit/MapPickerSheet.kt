package com.restauranttracker.ui.edit

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.restauranttracker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerSheet(
    initial: LatLng?,
    onDismiss: () -> Unit,
    onConfirm: (LatLng) -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val default = initial ?: LatLng(0.0, 0.0)
    var picked by remember { mutableStateOf<LatLng?>(initial) }

    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(default, if (initial != null) 15f else 2f)
    }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) scope.launchCenterOnCurrent(ctx, cameraState, onPicked = { picked = it })
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.map_picker_title))

            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                cameraPositionState = cameraState,
                onMapClick = { latLng -> picked = latLng },
            ) {
                picked?.let { Marker(state = MarkerState(position = it)) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        ctx, Manifest.permission.ACCESS_FINE_LOCATION,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        scope.launchCenterOnCurrent(ctx, cameraState, onPicked = { picked = it })
                    } else {
                        locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }) {
                    Text(stringResource(R.string.map_picker_use_location))
                }

                OutlinedButton(onClick = onDismiss) {
                    Text(stringResource(R.string.map_picker_cancel))
                }

                Button(
                    onClick = { picked?.let(onConfirm) },
                    enabled = picked != null,
                ) {
                    Text(stringResource(R.string.map_picker_confirm))
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun CoroutineScope.launchCenterOnCurrent(
    ctx: android.content.Context,
    cameraState: com.google.maps.android.compose.CameraPositionState,
    onPicked: (LatLng) -> Unit,
) {
    launch {
        val fused = LocationServices.getFusedLocationProviderClient(ctx)
        val location = runCatching { fused.lastLocation.await() }.getOrNull()
        if (location != null) {
            val pos = LatLng(location.latitude, location.longitude)
            cameraState.position = CameraPosition.fromLatLngZoom(pos, 15f)
            onPicked(pos)
        }
    }
}
