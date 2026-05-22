package com.dangodiary.ui.edit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.dangodiary.DangoDiaryApp
import com.dangodiary.R
import com.dangodiary.data.Photo
import com.dangodiary.ui.common.CuisinePickerField
import com.dangodiary.ui.common.DatePickerField
import com.dangodiary.ui.common.RatingStars
import com.dangodiary.ui.common.RestaurantNameField
import sh.calvin.reorderable.ReorderableColumn
import java.io.File

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
        // Wait for the entry load (or the new-entry currency seed) before rendering — avoids
        // a one-frame flash of empty fields when opening an existing entry. Matches the same
        // pattern in SettingsScreen.
        if (s.loading) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // -----------------------------------------------------------------
            // Restaurant Information — what & where & when
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
                // Don't fire autocomplete for the saved name when editing — only after the user
                // changes it. Empty on new entries (so first-time typing still gets suggestions).
                suppressSuggestionsFor = s.initialName,
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
            // Dishes — one or more dishes per entry
            // -----------------------------------------------------------------
            SectionHeader(stringResource(R.string.edit_section_dishes))

            ReorderableColumn(
                list = s.dishes,
                onSettle = { from, to -> vm.moveDish(from, to) },
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) { index, draft, _ ->
                DishRow(
                    draft = draft,
                    showRemove = s.dishes.size > 1,
                    onNameChange = { vm.setDishName(index, it) },
                    onPriceChange = { vm.setDishPrice(index, it) },
                    onRemove = { vm.removeDish(index) },
                    dragHandleModifier = Modifier.draggableHandle(),
                    dragHandleDescription = stringResource(R.string.edit_reorder_dish),
                )
            }

            TextButton(
                onClick = vm::addDish,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.padding(start = 4.dp))
                Text(stringResource(R.string.edit_add_dish))
            }

            // -----------------------------------------------------------------
            // Company & Notes
            // -----------------------------------------------------------------
            SectionHeader(stringResource(R.string.edit_section_company))

            OutlinedTextField(
                value = s.companions,
                onValueChange = vm::setCompanions,
                label = { Text(stringResource(R.string.edit_field_companions)) },
                placeholder = { Text(stringResource(R.string.edit_companions_placeholder)) },
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

            ReorderableColumn(
                list = s.photos,
                onSettle = { from, to -> vm.movePhoto(from, to) },
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) { _, photo, _ ->
                PhotoEditRow(
                    photo = photo,
                    onCaptionChange = { vm.setPhotoCaption(photo.path, it) },
                    onRemove = { vm.removePhoto(photo.path) },
                    dragHandleModifier = Modifier.draggableHandle(),
                    dragHandleDescription = stringResource(R.string.edit_reorder_photo),
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
 * One photo row in the edit form: thumbnail on the left, an optional caption text field on the
 * right, and a remove (×) button at the end. Captions are stored on the [Photo] and persisted
 * with the entry via [com.dangodiary.data.Photos].
 */
@Composable
private fun PhotoEditRow(
    photo: Photo,
    onCaptionChange: (String) -> Unit,
    onRemove: () -> Unit,
    dragHandleModifier: Modifier = Modifier,
    dragHandleDescription: String? = null,
) {
    // Long-press anywhere on the row toggles "edit mode" for this row, revealing the drag
    // handle and × button. Long-pressing the thumbnail is the most reliable trigger because
    // it's a large non-text-field target; the caption text field consumes its own long-press
    // (for text selection).
    var revealed by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { revealed = !revealed },
                    onTap = { if (revealed) revealed = false },
                )
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (revealed) {
            DragHandleIcon(
                modifier = dragHandleModifier,
                contentDescription = dragHandleDescription,
            )
        }
        AsyncImage(
            model = File(photo.path),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        OutlinedTextField(
            value = photo.caption,
            onValueChange = onCaptionChange,
            label = { Text(stringResource(R.string.edit_field_photo_caption)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        if (revealed) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.edit_remove_photo),
                )
            }
        }
    }
}

/**
 * One dish row: name on the left, price on the right, with an optional remove (×) button when
 * there's more than one dish in the list.
 */
@Composable
private fun DishRow(
    draft: DishDraft,
    showRemove: Boolean,
    onNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onRemove: () -> Unit,
    dragHandleModifier: Modifier = Modifier,
    dragHandleDescription: String? = null,
) {
    // Long-press toggles "edit mode" for this row, revealing the drag handle and × button.
    // Text-field children consume their own long-press gestures (for text selection), so the
    // long-press on the row only catches the gaps between fields. That's enough for an
    // affordance once the user knows the gesture exists.
    var revealed by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { revealed = !revealed },
                    onTap = { if (revealed) revealed = false },
                )
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (revealed) {
            DragHandleIcon(
                modifier = dragHandleModifier,
                contentDescription = dragHandleDescription,
            )
        }
        OutlinedTextField(
            value = draft.name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.edit_field_dish)) },
            placeholder = { Text(stringResource(R.string.edit_dish_placeholder)) },
            singleLine = true,
            modifier = Modifier.weight(1.5f),
        )
        // Price field: weight 1.1 (rather than 1) so the floated "Dish Price" label fits on
        // one line on phone widths — the previous narrower allocation made the label wrap to
        // two lines, which in turn forced the whole field to grow taller than the dish-name
        // sibling and elongated the row downward.
        OutlinedTextField(
            value = draft.priceText,
            onValueChange = onPriceChange,
            label = { Text(stringResource(R.string.edit_field_price)) },
            placeholder = { Text(stringResource(R.string.edit_price_placeholder)) },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
            ),
            isError = draft.priceError,
            supportingText = if (draft.priceError) {
                { Text(stringResource(R.string.edit_price_invalid)) }
            } else null,
            singleLine = true,
            modifier = Modifier.weight(1.1f),
        )
        if (showRemove && revealed) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.edit_remove_dish),
                )
            }
        }
    }
}

/**
 * Rating field rendered as a disabled [OutlinedTextField] with the stars overlaid in its
 * content area. The text field provides the exact same border + notched floating-label visual
 * treatment as every other field on the form, with zero pixel-tweaking on our side. The stars
 * overlay sits in the field's content area and uses [Arrangement.SpaceBetween] to fill the row.
 *
 * The disabled colours are overridden so the field reads as active. The same "disabled but
 * styled-active" trick is used in [com.dangodiary.ui.common.DatePickerField] and
 * [com.dangodiary.ui.common.CuisinePickerField].
 */
@Composable
private fun RatingFieldBox(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    isError: Boolean,
    errorText: String,
    label: String,
) {
    // Error text is rendered outside the Box so the field's height stays constant whether or
    // not the error is showing. That way the stars overlay's Alignment.Center always centers
    // on the field's content area and never drifts with supportingText height.
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                // A single space keeps the field "non-empty" so the floating label stays up.
                // The text colour is transparent so the space never renders.
                value = " ",
                onValueChange = {},
                readOnly = true,
                enabled = false,
                isError = isError,
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color.Transparent,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    // Mirror error variants so isError flips border + label colours too.
                    errorTextColor = Color.Transparent,
                ),
            )
            // Stars overlay the field's content area. Horizontal padding of 12 dp matches
            // OutlinedTextField's internal content insets so the outermost stars line up with
            // the field's left/right edges. The 6 dp top padding biases the row downward —
            // Box's Alignment.Center landed visually a touch high because the floated label
            // shifts the field's effective content centre below its geometric centre.
            RatingStars(
                rating = rating,
                onRatingChange = onRatingChange,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
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

/** Leading drag handle for a reorderable row. The caller wires the
 *  [sh.calvin.reorderable.ReorderableCollectionItemScope.draggableHandle] modifier into
 *  [modifier] from the ReorderableColumn scope; this composable just renders the icon at a
 *  fixed size with vertical padding to align with the OutlinedTextField content row. */
@Composable
private fun DragHandleIcon(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Icon(
        imageVector = Icons.Filled.DragHandle,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(top = 16.dp, bottom = 0.dp)
            .size(24.dp),
    )
}
