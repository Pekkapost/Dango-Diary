package com.restauranttracker.ui.edit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.LatLng
import com.restauranttracker.R
import com.restauranttracker.RestaurantApp
import com.restauranttracker.ui.common.DatePickerField
import com.restauranttracker.ui.common.PhotoGrid
import com.restauranttracker.ui.common.RatingStars

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantEditScreen(
    restaurantId: Long?,
    onDone: () -> Unit,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as RestaurantApp
    val vm: RestaurantEditViewModel = viewModel(
        factory = RestaurantEditViewModel.factory(app, restaurantId),
        key = "edit-${restaurantId ?: "new"}",
    )
    val s by vm.state.collectAsStateWithLifecycle()
    var showMapPicker by remember { mutableStateOf(false) }
    var photoMenuOpen by remember { mutableStateOf(false) }
    var pendingCameraPath by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val path = pendingCameraPath
        pendingCameraPath = null
        if (success && path != null) vm.addPhotoPath(path)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) vm.importPhotoFromUri(uri)
    }

    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val (uri, path) = app.photoStorage.newCameraTargetUri()
            pendingCameraPath = path
            cameraLauncher.launch(uri)
        }
    }

    // If the user navigates away without saving, drop any photos they imported.
    DisposableEffect(Unit) {
        onDispose { vm.cancel() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (restaurantId == null) stringResource(R.string.edit_title_new)
                        else stringResource(R.string.edit_title_edit)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = s.name,
                onValueChange = vm::setName,
                label = { Text(stringResource(R.string.edit_field_name)) },
                isError = s.nameError != null,
                supportingText = {
                    if (s.nameError != null) Text(stringResource(R.string.edit_name_required))
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            DatePickerField(
                label = stringResource(R.string.edit_field_date),
                epochDay = s.visitedOn,
                onDateChange = vm::setDate,
            )

            Column {
                Text(stringResource(R.string.edit_field_rating))
                RatingStars(rating = s.rating, onRatingChange = vm::setRating)
                if (s.ratingError != null) {
                    Text(
                        stringResource(R.string.edit_rating_required),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    )
                }
            }

            OutlinedTextField(
                value = s.companions,
                onValueChange = vm::setCompanions,
                label = { Text(stringResource(R.string.edit_field_companions)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = s.priceText,
                    onValueChange = vm::setPriceText,
                    label = { Text(stringResource(R.string.edit_field_price)) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                    ),
                    isError = s.priceError != null,
                    supportingText = {
                        if (s.priceError != null) Text(stringResource(R.string.edit_price_invalid))
                    },
                    singleLine = true,
                    modifier = Modifier.weight(2f),
                )
                OutlinedTextField(
                    value = s.currencyCode,
                    onValueChange = vm::setCurrencyCode,
                    label = { Text(stringResource(R.string.edit_field_currency)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = s.addressText,
                onValueChange = vm::setAddress,
                label = { Text(stringResource(R.string.edit_field_address)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showMapPicker = true }) {
                    Icon(Icons.Filled.Map, contentDescription = null)
                    Text("  " + stringResource(R.string.edit_pick_on_map))
                }
                if (s.latitude != null && s.longitude != null) {
                    OutlinedButton(onClick = { vm.setPin(null, null) }) {
                        Text(stringResource(R.string.edit_clear_pin))
                    }
                }
            }
            if (s.latitude != null && s.longitude != null) {
                Text("Pin: ${"%.5f".format(s.latitude)}, ${"%.5f".format(s.longitude)}")
            }

            OutlinedTextField(
                value = s.notes,
                onValueChange = vm::setNotes,
                label = { Text(stringResource(R.string.edit_field_notes)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            )

            Row {
                OutlinedButton(onClick = { photoMenuOpen = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("  " + stringResource(R.string.edit_add_photo))
                }
                DropdownMenu(
                    expanded = photoMenuOpen,
                    onDismissRequest = { photoMenuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit_take_photo)) },
                        leadingIcon = { Icon(Icons.Filled.CameraAlt, contentDescription = null) },
                        onClick = {
                            photoMenuOpen = false
                            cameraPermission.launch(android.Manifest.permission.CAMERA)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit_choose_photo)) },
                        leadingIcon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                        onClick = {
                            photoMenuOpen = false
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    )
                }
            }

            if (s.photoPaths.isNotEmpty()) {
                PhotoGrid(
                    paths = s.photoPaths,
                    onRemove = vm::removePhoto,
                    modifier = Modifier.height(360.dp),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDone,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.edit_cancel)) }

                Button(
                    onClick = { vm.save(onDone = onDone) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.edit_save)) }
            }
        }
    }

    if (showMapPicker) {
        MapPickerSheet(
            initial = s.latitude?.let { lat ->
                s.longitude?.let { lng -> LatLng(lat, lng) }
            },
            onDismiss = { showMapPicker = false },
            onConfirm = { latLng ->
                vm.setPin(latLng.latitude, latLng.longitude)
                showMapPicker = false
            },
        )
    }
}
