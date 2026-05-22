package com.dangodiary.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.dangodiary.R
import com.dangodiary.DangoDiaryApp
import com.dangodiary.data.CuisineCatalog
import com.dangodiary.data.Dishes
import com.dangodiary.data.Entry
import com.dangodiary.data.Photo
import com.dangodiary.data.Photos
import com.dangodiary.ui.common.PhotoGrid
import com.dangodiary.ui.common.RatingStars
import com.dangodiary.util.formatDate
import com.dangodiary.util.formatPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(
    entryId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as DangoDiaryApp
    val vm: EntryDetailViewModel = viewModel(
        factory = EntryDetailViewModel.factory(app, entryId),
        key = "detail-$entryId",
    )
    val entry by vm.entry.collectAsStateWithLifecycle()
    val hideTotalPrice by app.appSettings.hideTotalPrice
        .collectAsStateWithLifecycle(initialValue = false)
    var confirmDelete by remember { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        entry?.name ?: stringResource(R.string.detail_title),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.detail_action_edit))
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.detail_action_delete))
                    }
                    Box {
                        IconButton(onClick = { overflowOpen = true }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.detail_action_more),
                            )
                        }
                        DropdownMenu(
                            expanded = overflowOpen,
                            onDismissRequest = { overflowOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.detail_action_yelp)) },
                                onClick = {
                                    overflowOpen = false
                                    val name = entry?.name ?: return@DropdownMenuItem
                                    val loc = entry?.addressText.orEmpty()
                                    openYelpSearch(ctx, name, loc)
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        val current = entry
        if (current == null) {
            // Either still loading or the row was deleted from under us. Either way: nothing to render.
            return@Scaffold
        }
        DetailBody(
            entry = current,
            hideTotalPrice = hideTotalPrice,
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.detail_delete_confirm_title)) },
            text = { Text(stringResource(R.string.detail_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.delete(onDone = onBack)
                }) { Text(stringResource(R.string.detail_delete_confirm_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.detail_delete_confirm_cancel))
                }
            },
        )
    }
}

@Composable
private fun DetailBody(
    entry: Entry,
    hideTotalPrice: Boolean,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val photos = remember(entry.photoPathsJson) {
        Photos.decode(entry.photoPathsJson)
    }
    val dishes = remember(entry.dishesJson) {
        Dishes.decode(entry.dishesJson)
    }
    val totalCents: Long? = remember(dishes) {
        val priced = dishes.mapNotNull { it.priceCents }
        if (priced.isEmpty()) null else priced.sum()
    }
    val lat = entry.latitude
    val lng = entry.longitude
    val hasLocation = !entry.addressText.isNullOrBlank() || (lat != null && lng != null)
    val subtitle = buildList {
        CuisineCatalog.labelFor(entry.cuisine)?.let { add(it) }
        add(formatDate(entry.visitedOn))
        if (totalCents != null && !hideTotalPrice) {
            add(formatPrice(totalCents, entry.currencyCode))
        }
    }.joinToString("  ·  ")

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        // The name is rendered in the top app bar (bold); no need to repeat it here.
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RatingStars(rating = entry.rating)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (hasLocation) {
            HorizontalDivider()
            DetailSection(title = stringResource(R.string.detail_section_location)) {
                var mapExpanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!entry.addressText.isNullOrBlank()) {
                        Text(
                            entry.addressText,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    if (lat != null && lng != null) {
                        IconButton(onClick = { mapExpanded = !mapExpanded }) {
                            Icon(
                                imageVector = if (mapExpanded) Icons.Filled.ExpandLess
                                else Icons.Filled.ExpandMore,
                                contentDescription = stringResource(
                                    if (mapExpanded) R.string.detail_hide_map
                                    else R.string.detail_show_map
                                ),
                            )
                        }
                        IconButton(
                            onClick = {
                                val uri = "geo:$lat,$lng?q=$lat,$lng(${Uri.encode(entry.name)})".toUri()
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            },
                        ) {
                            Icon(
                                Icons.Filled.Map,
                                contentDescription = stringResource(R.string.detail_open_in_maps),
                            )
                        }
                    }
                }
                if (mapExpanded && lat != null && lng != null) {
                    val cameraState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 15f)
                    }
                    GoogleMap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        cameraPositionState = cameraState,
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            scrollGesturesEnabled = false,
                            tiltGesturesEnabled = false,
                            rotationGesturesEnabled = false,
                            zoomGesturesEnabled = false,
                        ),
                    ) {
                        Marker(state = MarkerState(position = LatLng(lat, lng)))
                    }
                }
            }
        }

        if (dishes.isNotEmpty()) {
            HorizontalDivider()
            DetailSection(title = stringResource(R.string.detail_section_dishes)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    dishes.forEach { dish ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = dish.name.ifBlank {
                                    stringResource(R.string.detail_dish_unnamed)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            if (dish.priceCents != null) {
                                Text(
                                    text = formatPrice(dish.priceCents, entry.currencyCode),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (entry.companions.isNotBlank()) {
            HorizontalDivider()
            DetailSection(title = stringResource(R.string.detail_section_with)) {
                Text(entry.companions, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (entry.notes.isNotBlank()) {
            HorizontalDivider()
            DetailSection(title = stringResource(R.string.detail_section_notes)) {
                Text(entry.notes, style = MaterialTheme.typography.bodyMedium)
            }
        }

        var enlargedPhoto by remember { mutableStateOf<Photo?>(null) }
        if (photos.isNotEmpty()) {
            HorizontalDivider()
            PhotoGrid(
                paths = photos.map { it.path },
                captionFor = { path -> photos.firstOrNull { it.path == path }?.caption.orEmpty() },
                onClick = { path -> enlargedPhoto = photos.firstOrNull { it.path == path } },
                // Match the horizontal/vertical padding the other DetailSections use, so the
                // grid doesn't sit flush against the divider or screen edges.
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 12.dp)
                    .height(280.dp),
            )
        }
        enlargedPhoto?.let { photo ->
            EnlargedPhotoDialog(photo = photo, onDismiss = { enlargedPhoto = null })
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
        )
        content()
    }
}

/**
 * Open a Yelp search for the given restaurant. Tries the Yelp Android app first via the
 * `yelp:///search` deep-link scheme; falls back to the web URL (which the browser will
 * handle) when the app isn't installed. Uses a try/catch on [ActivityNotFoundException]
 * instead of `resolveActivity` so we don't need an Android 11+ `<queries>` manifest entry.
 */
private fun openYelpSearch(ctx: android.content.Context, name: String, location: String) {
    val appUri = buildYelpAppUri(name, location)
    val webUri = buildYelpWebUrl(name, location).toUri()
    val appIntent = Intent(Intent.ACTION_VIEW, appUri)
    try {
        ctx.startActivity(appIntent)
    } catch (_: android.content.ActivityNotFoundException) {
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, webUri)) }
    }
}

/** Yelp app deep-link: `yelp:///search?terms=<name>&location=<addr>`. Three slashes —
 *  empty host between the scheme and the path. */
private fun buildYelpAppUri(name: String, location: String): android.net.Uri {
    val terms = Uri.encode(name.trim())
    val locParam = location.trim().takeIf { it.isNotEmpty() }
        ?.let { "&location=${Uri.encode(it)}" }
        .orEmpty()
    return "yelp:///search?terms=$terms$locParam".toUri()
}

/** Browser-fallback Yelp search URL. Includes the address as `find_loc` when present so
 *  Yelp narrows the search to the right area; without it the result depends on Yelp's best
 *  guess from the device's IP. */
private fun buildYelpWebUrl(name: String, location: String): String {
    val desc = Uri.encode(name.trim())
    val locParam = location.trim().takeIf { it.isNotEmpty() }
        ?.let { "&find_loc=${Uri.encode(it)}" }
        .orEmpty()
    return "https://www.yelp.com/search?find_desc=$desc$locParam"
}

/**
 * Full-screen modal showing a single photo enlarged for easier viewing. The image fits the
 * dialog (no cropping) and the caption sits below in white-on-dark. Tap anywhere to dismiss;
 * the system back button also closes via the Dialog's onDismissRequest.
 */
@Composable
private fun EnlargedPhotoDialog(photo: Photo, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val interaction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                // Tap anywhere on the backdrop (including over the image — no ripple) closes.
                .clickable(interactionSource = interaction, indication = null) { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp),
            ) {
                AsyncImage(
                    model = File(photo.path),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (photo.caption.isNotBlank()) {
                    Text(
                        text = photo.caption,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }
    }
}
