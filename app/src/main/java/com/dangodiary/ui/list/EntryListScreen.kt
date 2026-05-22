package com.dangodiary.ui.list

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.dangodiary.R
import com.dangodiary.DangoDiaryApp
import com.dangodiary.data.CuisineCatalog
import com.dangodiary.data.CuisineGroup
import com.dangodiary.data.Dishes
import com.dangodiary.data.Entry
import com.dangodiary.data.Photos
import com.dangodiary.ui.common.LegacyBadge
import com.dangodiary.ui.common.RatingStars
import com.dangodiary.util.extractCity
import com.dangodiary.util.formatDate
import com.dangodiary.util.formatPrice
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryListScreen(
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as DangoDiaryApp
    val vm: EntryListViewModel = viewModel(factory = EntryListViewModel.factory(app))
    val items by vm.items.collectAsStateWithLifecycle()
    val filters by vm.filters.collectAsStateWithLifecycle()
    val hideTotalPrice by app.appSettings.hideTotalPrice
        .collectAsStateWithLifecycle(initialValue = false)

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val locationDeniedMessage = stringResource(R.string.list_sort_distance_no_location)

    // Permission launcher for the coarse-location prompt. On grant, fetch last known location;
    // on deny, surface a snackbar telling the user distance sort needs the permission.
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            vm.setCurrentLocation(fetchLastKnownLocation(ctx))
            vm.setSort(Sort.DISTANCE)
        } else {
            coroutineScope.launch { snackbarHostState.showSnackbar(locationDeniedMessage) }
        }
    }

    // Helper invoked by the Sort menu when the user picks Distance. Either fetches the location
    // directly (permission already granted) or prompts for it. On denial, snackbar + don't
    // flip the sort.
    val onPickDistanceSort: () -> Unit = onPickDistanceSort@{
        val granted = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            vm.setCurrentLocation(fetchLastKnownLocation(ctx))
            vm.setSort(Sort.DISTANCE)
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.list_title)) },
                actions = {
                    ListToolbarActions(
                        filters = filters,
                        vm = vm,
                        onOpenSettings = onOpenSettings,
                        onPickDistanceSort = onPickDistanceSort,
                    )
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.action_add)) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = onAdd,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        // First-launch empty state: no entries AND no active filters/query. Render as a
        // standalone centred Box on the full viewport (skip the search bar + filter chips,
        // which are useless when there's nothing to search), so the text sits in the visual
        // middle of the app rather than below the search bar's bottom edge.
        val trulyEmpty = items.isEmpty() && !filters.hasAny
        if (trulyEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.list_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(32.dp),
                )
            }
            return@Scaffold
        }

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
                // Filter or query excludes everything. Keep the search bar + chips visible
                // (the user needs them to clear/adjust the filter); centring is within the
                // remaining space below them, which is the correct framing here.
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.list_no_matches),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
                    items(items, key = { it.id }) { entry ->
                        EntryRow(
                            entry = entry,
                            hideTotalPrice = hideTotalPrice,
                            onClick = { onOpen(entry.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListToolbarActions(
    filters: ListFilters,
    vm: EntryListViewModel,
    onOpenSettings: () -> Unit,
    onPickDistanceSort: () -> Unit,
) {
    var sortOpen by remember { mutableStateOf(false) }
    var filterSheetOpen by remember { mutableStateOf(false) }

    IconButton(onClick = { sortOpen = true }) {
        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.list_sort))
    }
    DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.list_sort_recent)) },
            onClick = { vm.setSort(Sort.RECENT); sortOpen = false },
            trailingIcon = { if (filters.sort == Sort.RECENT) Text("✓") },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.list_sort_rating)) },
            onClick = { vm.setSort(Sort.RATING); sortOpen = false },
            trailingIcon = { if (filters.sort == Sort.RATING) Text("✓") },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.list_sort_name)) },
            onClick = { vm.setSort(Sort.NAME); sortOpen = false },
            trailingIcon = { if (filters.sort == Sort.NAME) Text("✓") },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.list_sort_distance)) },
            onClick = { sortOpen = false; onPickDistanceSort() },
            trailingIcon = { if (filters.sort == Sort.DISTANCE) Text("✓") },
        )
    }

    IconButton(onClick = { filterSheetOpen = true }) {
        Icon(Icons.Filled.Tune, contentDescription = stringResource(R.string.list_filter))
    }

    IconButton(onClick = onOpenSettings) {
        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
    }

    if (filterSheetOpen) {
        FilterBottomSheet(
            filters = filters,
            vm = vm,
            onDismiss = { filterSheetOpen = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    filters: ListFilters,
    vm: EntryListViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.list_filter_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            // ----- Rating slider --------------------------------------------------------
            val wholeStars = filters.minRatingHalfStars / 2
            Text(
                text = if (filters.minRatingHalfStars == 0) {
                    stringResource(R.string.list_filter_min_rating_any)
                } else {
                    stringResource(R.string.list_filter_min_rating_label, wholeStars)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = wholeStars.toFloat(),
                onValueChange = { vm.setMinRatingHalfStars((it.toInt()) * 2) },
                valueRange = 0f..5f,
                steps = 4,
            )

            HorizontalDivider()

            // ----- Cuisine picker -------------------------------------------------------
            Text(
                text = stringResource(R.string.list_filter_cuisine_label),
                style = MaterialTheme.typography.bodyMedium,
            )
            CuisineFilterPicker(
                current = filters.cuisine,
                onSelect = { vm.setCuisineFilter(it) },
            )

            HorizontalDivider()

            // ----- Has-photo toggle -----------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { vm.setOnlyWithPhoto(!filters.onlyWithPhoto) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.list_filter_has_photo),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = filters.onlyWithPhoto,
                    onCheckedChange = vm::setOnlyWithPhoto,
                )
            }

            // ----- Action row -----------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { vm.clearFilters() },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.list_filter_clear)) }

                androidx.compose.material3.Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.list_filter_done)) }
            }
        }
    }
}

/**
 * Cuisine filter picker. Renders as a tap-to-open OutlinedTextField (like CuisinePickerField)
 * with a DropdownMenu containing: an "Any cuisine" reset item, then each [CuisineGroup] as a
 * tappable "<group> (any)" header followed by every cuisine in that group as a regular item.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CuisineFilterPicker(
    current: CuisineFilter?,
    onSelect: (CuisineFilter?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val display = when (current) {
        null -> stringResource(R.string.list_filter_cuisine_any)
        is CuisineFilter.Specific -> CuisineCatalog.labelFor(current.cuisineId)
            ?: stringResource(R.string.list_filter_cuisine_any)
        is CuisineFilter.Group -> stringResource(
            R.string.list_filter_cuisine_group_any, current.group.label,
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = true },
        ) {
            OutlinedTextField(
                value = display,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                trailingIcon = {
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.list_filter_cuisine_any)) },
                onClick = { onSelect(null); open = false },
                trailingIcon = { if (current == null) Text("✓") },
            )
            CuisineCatalog.grouped.forEach { (group, cuisines) ->
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.list_filter_cuisine_group_any, group.label),
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    onClick = { onSelect(CuisineFilter.Group(group)); open = false },
                    trailingIcon = {
                        if (current is CuisineFilter.Group && current.group == group) Text("✓")
                    },
                )
                cuisines.forEach { c ->
                    DropdownMenuItem(
                        text = { Text("    ${c.label}") },
                        onClick = { onSelect(CuisineFilter.Specific(c.id)); open = false },
                        trailingIcon = {
                            if (current is CuisineFilter.Specific && current.cuisineId == c.id) {
                                Text("✓")
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveFilterChips(filters: ListFilters, vm: EntryListViewModel) {
    val anyChips = filters.minRatingHalfStars > 0 || filters.onlyWithPhoto || filters.cuisine != null
    if (!anyChips) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        if (filters.minRatingHalfStars > 0) {
            FilterChip(
                selected = true,
                onClick = { vm.setMinRatingHalfStars(0) },
                label = {
                    Text(stringResource(R.string.list_filter_min_rating_label, filters.minRatingHalfStars / 2))
                },
            )
        }
        when (val cf = filters.cuisine) {
            null -> {}
            is CuisineFilter.Specific -> FilterChip(
                selected = true,
                onClick = { vm.setCuisineFilter(null) },
                label = {
                    Text(stringResource(
                        R.string.list_filter_chip_cuisine,
                        CuisineCatalog.labelFor(cf.cuisineId) ?: "",
                    ))
                },
            )
            is CuisineFilter.Group -> FilterChip(
                selected = true,
                onClick = { vm.setCuisineFilter(null) },
                label = {
                    Text(stringResource(R.string.list_filter_chip_cuisine, cf.group.label))
                },
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

/**
 * Best-effort last-known location via [LocationManager]. Tries each enabled provider in turn —
 * NETWORK first (low-power, indoor-friendly), then PASSIVE (already cached from any other app),
 * then GPS as a last resort. Returns null if no provider has a cached fix or permission was
 * revoked between the check and the call. Distance sort silently falls back to recency in that
 * case (see [applyFilters]).
 */
private fun fetchLastKnownLocation(ctx: Context): android.location.Location? {
    val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = listOf(
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER,
        LocationManager.GPS_PROVIDER,
    ).filter { lm.isProviderEnabled(it) }
    return providers.firstNotNullOfOrNull { provider ->
        runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
    }
}

@Composable
private fun EntryRow(entry: Entry, hideTotalPrice: Boolean, onClick: () -> Unit) {
    val photos = remember(entry.photoPathsJson) {
        Photos.paths(entry.photoPathsJson)
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
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        val subtitle = listOfNotNull(
                            CuisineCatalog.labelFor(entry.cuisine),
                            extractCity(entry.addressText),
                        ).joinToString("  ·  ")
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatDate(entry.visitedOn),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (entry.isLegacy) {
                            LegacyBadge(modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RatingStars(rating = entry.rating, modifier = Modifier.weight(1f))
                    val totalCents = remember(entry.dishesJson) {
                        Dishes.decode(entry.dishesJson).mapNotNull { it.priceCents }
                            .takeIf { it.isNotEmpty() }
                            ?.sum()
                    }
                    if (totalCents != null && !hideTotalPrice) {
                        Text(
                            text = formatPrice(totalCents, entry.currencyCode),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
