package com.dangodiary.ui.list

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dangodiary.DangoDiaryApp
import com.dangodiary.data.CuisineCatalog
import com.dangodiary.data.CuisineGroup
import com.dangodiary.data.Entry
import com.dangodiary.data.EntryDao
import com.dangodiary.data.Photos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class Sort { RECENT, RATING, NAME, DISTANCE }

/**
 * Optional cuisine filter. A specific cuisine matches that id exactly; a group matches every
 * cuisine in the supertype (e.g. all "Cafés"). null = no filter.
 */
sealed class CuisineFilter {
    data class Specific(val cuisineId: String) : CuisineFilter()
    data class Group(val group: CuisineGroup) : CuisineFilter()
}

data class ListFilters(
    val query: String = "",
    val sort: Sort = Sort.RECENT,
    /** Half-stars (0..10). Filter passes if entry.rating >= minRatingHalfStars. */
    val minRatingHalfStars: Int = 0,
    val onlyWithPhoto: Boolean = false,
    val cuisine: CuisineFilter? = null,
) {
    val hasAny: Boolean
        get() = query.isNotBlank() ||
            sort != Sort.RECENT ||
            minRatingHalfStars > 0 ||
            onlyWithPhoto ||
            cuisine != null
}

class EntryListViewModel(
    private val dao: EntryDao,
) : ViewModel() {

    private val _filters = MutableStateFlow(ListFilters())
    val filters: StateFlow<ListFilters> = _filters.asStateFlow()

    /** Last-known device location, set by [setCurrentLocation] when the user picks the
     *  Distance sort and permission has been granted. Null = unknown; distance sort silently
     *  falls back to the recency order until a location arrives. */
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    val items: StateFlow<List<Entry>> =
        combine(dao.observeAll(), _filters, _currentLocation) { all, f, loc ->
            applyFilters(all, f, loc)
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(q: String) = _filters.update { it.copy(query = q) }
    fun setSort(s: Sort) = _filters.update { it.copy(sort = s) }
    fun setMinRatingHalfStars(r: Int) = _filters.update {
        it.copy(minRatingHalfStars = r.coerceIn(0, 10))
    }
    fun setOnlyWithPhoto(v: Boolean) = _filters.update { it.copy(onlyWithPhoto = v) }
    fun setCuisineFilter(f: CuisineFilter?) = _filters.update { it.copy(cuisine = f) }
    fun setCurrentLocation(location: Location?) {
        _currentLocation.value = location
    }
    fun clearFilters() = _filters.update {
        // Preserve the current sort; "filters" and "sort" are conceptually distinct in the UI
        // (sort menu vs. filter sheet) even though they share this state object.
        ListFilters(sort = it.sort)
    }

    companion object {
        fun factory(app: DangoDiaryApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    EntryListViewModel(app.database.entryDao()) as T
            }
    }
}

private fun applyFilters(all: List<Entry>, f: ListFilters, here: Location?): List<Entry> {
    val q = f.query.trim().lowercase()
    val filtered = all.filter { entry ->
        if (q.isNotEmpty()) {
            val haystack = (entry.name + " " + entry.notes + " " + entry.companions + " " +
                (entry.addressText ?: "")).lowercase()
            if (!haystack.contains(q)) return@filter false
        }
        if (entry.rating < f.minRatingHalfStars) return@filter false
        if (f.onlyWithPhoto && Photos.decode(entry.photoPathsJson).isEmpty()) return@filter false
        when (val cf = f.cuisine) {
            null -> {}
            is CuisineFilter.Specific -> if (entry.cuisine != cf.cuisineId) return@filter false
            is CuisineFilter.Group ->
                if (CuisineCatalog.groupFor(entry.cuisine) != cf.group) return@filter false
        }
        true
    }
    return when (f.sort) {
        Sort.RECENT -> filtered.sortedByDescending { it.visitedOn }
        Sort.RATING -> filtered.sortedByDescending { it.rating }
        Sort.NAME -> filtered.sortedBy { it.name.lowercase() }
        Sort.DISTANCE -> {
            if (here == null) {
                // No location yet — fall back to recency so the list isn't blank or scrambled
                // while we wait for the location fix.
                filtered.sortedByDescending { it.visitedOn }
            } else {
                filtered.sortedBy { entry ->
                    val lat = entry.latitude
                    val lng = entry.longitude
                    if (lat == null || lng == null) {
                        // Entries without coordinates sink to the bottom of the distance sort.
                        Float.MAX_VALUE
                    } else {
                        val out = FloatArray(1)
                        Location.distanceBetween(here.latitude, here.longitude, lat, lng, out)
                        out[0]
                    }
                }
            }
        }
    }
}
