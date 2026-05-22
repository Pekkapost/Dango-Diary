package com.dangodiary.ui.edit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dangodiary.DangoDiaryApp
import com.dangodiary.R
import com.dangodiary.ui.common.CuisinePickerField
import com.dangodiary.ui.common.DatePickerField
import com.dangodiary.ui.common.PhotoGrid
import com.dangodiary.ui.common.RatingStars
import com.dangodiary.ui.common.RestaurantNameField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditScreen(
    entryId: Long?,
    onDone: () -> Unit,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as DangoDiaryApp
    val vm: EntryEditViewModel = viewModel(
        factory = EntryEditViewModel.factory(app, entryId),
        key = "edit-${entryId ?: "new"}",
    )
    val s by vm.state.collectAsStateWithLifecycle()
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
                        if (entryId == null) stringResource(R.string.edit_title_new)
                        else stringResource(R.string.edit_title_edit)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.edit_cancel),
                        )
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
            // -----------------------------------------------------------------
            // Restaurant — what & where & when
            // -----------------------------------------------------------------
            SectionHeader(stringResource(R.string.edit_section_restaurant), first = true)

            RestaurantNameField(
                name = s.name,
                onNameChange = vm::setName,
                onPlacePicked = { pick ->
                    vm.applyPickedRestaurant(pick.name, pick.address, pick.latitude, pick.longitude)
                },
                label = stringResource(R.string.edit_field_name),
                isError = s.nameError,
                supportingText = if (s.nameError) {
                    { Text(stringResource(R.string.edit_name_required)) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CuisinePickerField(
                    label = stringResource(R.string.edit_field_cuisine),
                    selectedId = s.cuisine,
                    onSelect = vm::setCuisine,
                    placeholder = stringResource(R.string.edit_cuisine_placeholder),
                    clearLabel = stringResource(R.string.edit_cuisine_clear),
                    modifier = Modifier.weight(1f),
                )
                DatePickerField(
                    label = stringResource(R.string.edit_field_date),
                    epochDay = s.visitedOn,
                    onDateChange = vm::setDate,
                    modifier = Modifier.weight(1f),
                )
            }

            RatingFieldBox(
                rating = s.rating,
                onRatingChange = vm::setRating,
                isError = s.ratingError,
                errorText = stringResource(R.string.edit_rating_required),
                label = stringResource(R.string.edit_field_rating),
            )

            OutlinedTextField(
                value = s.addressText,
                onValueChange = vm::setAddressText,
                label = { Text(stringResource(R.string.edit_field_address)) },
                placeholder = { Text(stringResource(R.string.edit_address_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // -----------------------------------------------------------------
            // Meal — what you ordered + what it cost
            // -----------------------------------------------------------------
            SectionHeader(stringResource(R.string.edit_section_meal))

            OutlinedTextField(
                value = s.meal,
                onValueChange = vm::setMeal,
                label = { Text(stringResource(R.string.edit_field_meal)) },
                placeholder = { Text(stringResource(R.string.edit_meal_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = s.priceText,
                onValueChange = vm::setPriceText,
                label = { Text(stringResource(R.string.edit_field_price)) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                ),
                isError = s.priceError,
                supportingText = if (s.priceError) {
                    { Text(stringResource(R.string.edit_price_invalid)) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // -----------------------------------------------------------------
            // Company & notes
            // -----------------------------------------------------------------
            SectionHeader(stringResource(R.string.edit_section_company))

            OutlinedTextField(
                value = s.companions,
                onValueChange = vm::setCompanions,
                label = { Text(stringResource(R.string.edit_field_companions)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = s.notes,
                onValueChange = vm::setNotes,
                label = { Text(stringResource(R.string.edit_field_notes)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            )

            // -----------------------------------------------------------------
            // Photos
            // -----------------------------------------------------------------
            SectionHeader(stringResource(R.string.edit_section_photos))

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
}

/** Section heading. Adds extra top spacing for visual separation between sections, except on
 *  the first one (where the scaffold already provides the top padding). */
@Composable
private fun SectionHeader(title: String, first: Boolean = false) {
    if (!first) Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

/**
 * Rating field rendered as an outlined box with a notched floating label, mirroring the
 * [OutlinedTextField] visual treatment so it reads as the same kind of element as the
 * surrounding fields. The five stars distribute across the box width via [Arrangement.SpaceBetween].
 *
 * Drawn from scratch (border + offset label with surface background) rather than borrowing a
 * disabled OutlinedTextField, so we can put arbitrary tap-aware content inside the field area
 * without fighting the text field's internal layout.
 */
@Composable
private fun RatingFieldBox(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    isError: Boolean,
    errorText: String,
    label: String,
) {
    val borderColor =
        if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
    val labelColor =
        if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = MaterialTheme.shapes.extraSmall
    val surface = MaterialTheme.colorScheme.surface

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(width = 1.dp, color = borderColor, shape = shape),
        ) {
            RatingStars(
                rating = rating,
                onRatingChange = onRatingChange,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            )
            // Floating label notching the top border. The surface-coloured background masks the
            // border line behind it, producing the standard OutlinedTextField notch effect. The
            // -7 dp Y offset lifts the label's centerline onto the border itself.
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = labelColor,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp)
                    .offset(y = (-7).dp)
                    .background(surface)
                    .padding(horizontal = 4.dp),
            )
        }
        if (isError) {
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}
