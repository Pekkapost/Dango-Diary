package com.restauranttracker.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.restauranttracker.R
import com.restauranttracker.RestaurantApp
import com.restauranttracker.data.PhotoPaths
import com.restauranttracker.data.Restaurant
import com.restauranttracker.ui.common.RatingStars
import com.restauranttracker.util.formatDate
import com.restauranttracker.util.formatPrice
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantListScreen(
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    val app = LocalContext.current.applicationContext as RestaurantApp
    val vm: RestaurantListViewModel = viewModel(factory = RestaurantListViewModel.factory(app))
    val items by vm.items.collectAsStateWithLifecycle()
    val filters by vm.filters.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.list_title)) },
                actions = { ListToolbarActions(filters, vm) },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.action_add)) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = onAdd,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = filters.query,
                onValueChange = vm::setQuery,
                placeholder = { Text(stringResource(R.string.list_search_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            ActiveFilterChips(filters, vm)

            if (items.isEmpty()) {
                val emptyMessage =
                    if (filters.query.isNotBlank() || filters.hasAny) R.string.list_no_matches
                    else R.string.list_empty
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(emptyMessage),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(32.dp),
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items, key = { it.id }) { restaurant ->
                        RestaurantRow(restaurant, onClick = { onOpen(restaurant.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ListToolbarActions(
    filters: ListFilters,
    vm: RestaurantListViewModel,
) {
    var sortOpen by remember { mutableStateOf(false) }
    var filterOpen by remember { mutableStateOf(false) }

    IconButton(onClick = { sortOpen = true }) {
        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.list_sort))
    }
    DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.list_sort_recent)) },
            onClick = { vm.setSort(Sort.RECENT); sortOpen = false },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.list_sort_rating)) },
            onClick = { vm.setSort(Sort.RATING); sortOpen = false },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.list_sort_name)) },
            onClick = { vm.setSort(Sort.NAME); sortOpen = false },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.list_sort_price)) },
            onClick = { vm.setSort(Sort.PRICE); sortOpen = false },
        )
    }

    IconButton(onClick = { filterOpen = true }) {
        Icon(Icons.Filled.Tune, contentDescription = stringResource(R.string.list_filter))
    }
    DropdownMenu(expanded = filterOpen, onDismissRequest = { filterOpen = false }) {
        (0..5).forEach { stars ->
            DropdownMenuItem(
                text = {
                    Text(
                        if (stars == 0) "Any rating"
                        else stringResource(R.string.list_filter_min_rating, stars)
                    )
                },
                onClick = { vm.setMinRating(stars); filterOpen = false },
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.list_filter_has_photo)) },
            onClick = { vm.setOnlyWithPhoto(!filters.onlyWithPhoto); filterOpen = false },
            trailingIcon = {
                if (filters.onlyWithPhoto) Text("✓")
            },
        )
        if (filters.hasAny) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.list_filter_clear)) },
                onClick = { vm.clearFilters(); filterOpen = false },
            )
        }
    }
}

@Composable
private fun ActiveFilterChips(filters: ListFilters, vm: RestaurantListViewModel) {
    if (filters.minRating == 0 && !filters.onlyWithPhoto) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        if (filters.minRating > 0) {
            FilterChip(
                selected = true,
                onClick = { vm.setMinRating(0) },
                label = { Text(stringResource(R.string.list_filter_min_rating, filters.minRating)) },
            )
        }
        if (filters.onlyWithPhoto) {
            FilterChip(
                selected = true,
                onClick = { vm.setOnlyWithPhoto(false) },
                label = { Text(stringResource(R.string.list_filter_has_photo)) },
            )
        }
    }
}

@Composable
private fun RestaurantRow(restaurant: Restaurant, onClick: () -> Unit) {
    val photos = remember(restaurant.photoPathsJson) {
        PhotoPaths.decode(restaurant.photoPathsJson)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val first = photos.firstOrNull()
            if (first != null) {
                AsyncImage(
                    model = File(first),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = restaurant.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!restaurant.addressText.isNullOrBlank()) {
                    Text(
                        text = restaurant.addressText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = formatDate(restaurant.visitedOn),
                    style = MaterialTheme.typography.bodySmall,
                )
                RatingStars(rating = restaurant.rating)
                if (restaurant.dishPriceCents != null) {
                    Text(
                        text = formatPrice(restaurant.dishPriceCents, restaurant.currencyCode),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
