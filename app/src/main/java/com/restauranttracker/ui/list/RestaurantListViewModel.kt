package com.restauranttracker.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.restauranttracker.RestaurantApp
import com.restauranttracker.data.PhotoPaths
import com.restauranttracker.data.Restaurant
import com.restauranttracker.data.RestaurantDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class Sort { RECENT, RATING, NAME, PRICE }

data class ListFilters(
    val query: String = "",
    val sort: Sort = Sort.RECENT,
    val minRating: Int = 0,
    val onlyWithPhoto: Boolean = false,
) {
    val hasAny: Boolean
        get() = query.isNotBlank() || sort != Sort.RECENT || minRating > 0 || onlyWithPhoto
}

class RestaurantListViewModel(
    private val dao: RestaurantDao,
) : ViewModel() {

    private val _filters = MutableStateFlow(ListFilters())
    val filters: StateFlow<ListFilters> = _filters.asStateFlow()

    val items: StateFlow<List<Restaurant>> =
        combine(dao.observeAll(), _filters) { all, f -> applyFilters(all, f) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(q: String) = _filters.update { it.copy(query = q) }
    fun setSort(s: Sort) = _filters.update { it.copy(sort = s) }
    fun setMinRating(r: Int) = _filters.update { it.copy(minRating = r) }
    fun setOnlyWithPhoto(v: Boolean) = _filters.update { it.copy(onlyWithPhoto = v) }
    fun clearFilters() = _filters.update { ListFilters() }

    companion object {
        fun factory(app: RestaurantApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RestaurantListViewModel(app.database.restaurantDao()) as T
            }
    }
}

private fun applyFilters(all: List<Restaurant>, f: ListFilters): List<Restaurant> {
    val q = f.query.trim().lowercase()
    val filtered = all.filter { r ->
        if (q.isNotEmpty()) {
            val haystack = (r.name + " " + r.notes + " " + r.companions + " " +
                (r.addressText ?: "")).lowercase()
            if (!haystack.contains(q)) return@filter false
        }
        if (r.rating < f.minRating) return@filter false
        if (f.onlyWithPhoto && PhotoPaths.decode(r.photoPathsJson).isEmpty()) return@filter false
        true
    }
    return when (f.sort) {
        Sort.RECENT -> filtered.sortedByDescending { it.visitedOn }
        Sort.RATING -> filtered.sortedByDescending { it.rating }
        Sort.NAME -> filtered.sortedBy { it.name.lowercase() }
        Sort.PRICE -> filtered.sortedBy { it.dishPriceCents ?: Long.MAX_VALUE }
    }
}
