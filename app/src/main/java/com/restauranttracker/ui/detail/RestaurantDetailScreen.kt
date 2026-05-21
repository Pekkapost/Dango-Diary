package com.restauranttracker.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.restauranttracker.R
import com.restauranttracker.RestaurantApp
import com.restauranttracker.data.PhotoPaths
import com.restauranttracker.data.Restaurant
import com.restauranttracker.ui.common.PhotoGrid
import com.restauranttracker.ui.common.RatingStars
import com.restauranttracker.util.formatDate
import com.restauranttracker.util.formatPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantDetailScreen(
    restaurantId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as RestaurantApp
    val vm: RestaurantDetailViewModel = viewModel(
        factory = RestaurantDetailViewModel.factory(app, restaurantId),
        key = "detail-$restaurantId",
    )
    val r by vm.restaurant.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(r?.name ?: stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.detail_action_edit))
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.detail_action_delete))
                    }
                },
            )
        },
    ) { padding ->
        val current = r
        if (current == null) {
            // Either still loading or the row was deleted from under us. Either way: nothing to render.
            return@Scaffold
        }
        DetailBody(current, modifier = Modifier
            .fillMaxSize()
            .padding(padding))
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
private fun DetailBody(restaurant: Restaurant, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val photos = remember(restaurant.photoPathsJson) {
        PhotoPaths.decode(restaurant.photoPathsJson)
    }
    val lat = restaurant.latitude
    val lng = restaurant.longitude
    val hasLocation = !restaurant.addressText.isNullOrBlank() || (lat != null && lng != null)
    val subtitle = buildString {
        append(formatDate(restaurant.visitedOn))
        if (restaurant.dishPriceCents != null) {
            append("  ·  ")
            append(formatPrice(restaurant.dishPriceCents, restaurant.currencyCode))
        }
    }

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        if (photos.isNotEmpty()) {
            PhotoGrid(
                paths = photos,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
            )
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                restaurant.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            RatingStars(rating = restaurant.rating)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (restaurant.notes.isNotBlank()) {
            HorizontalDivider()
            DetailSection(title = stringResource(R.string.detail_section_notes)) {
                Text(restaurant.notes, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (restaurant.companions.isNotBlank()) {
            HorizontalDivider()
            DetailSection(title = stringResource(R.string.detail_section_with)) {
                Text(restaurant.companions, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (hasLocation) {
            HorizontalDivider()
            DetailSection(title = stringResource(R.string.detail_section_location)) {
                if (!restaurant.addressText.isNullOrBlank()) {
                    Text(restaurant.addressText, style = MaterialTheme.typography.bodyMedium)
                }
                if (lat != null && lng != null) {
                    val cameraState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 15f)
                    }
                    GoogleMap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
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
                    OutlinedButton(
                        onClick = {
                            val uri = "geo:$lat,$lng?q=$lat,$lng(${Uri.encode(restaurant.name)})".toUri()
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        },
                    ) {
                        Icon(Icons.Filled.Map, contentDescription = null)
                        Text("  ${stringResource(R.string.detail_open_in_maps)}")
                    }
                }
            }
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
