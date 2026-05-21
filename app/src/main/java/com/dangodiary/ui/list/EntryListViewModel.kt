package com.dangodiary.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dangodiary.DangoDiaryApp
import com.dangodiary.data.PhotoPaths
import com.dangodiary.data.Entry
import com.dangodiary.data.EntryDao
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

class EntryListViewModel(
    private val dao: EntryDao,
) : ViewModel() {

    private val _filters = MutableStateFlow(ListFilters())
    val filters: StateFlow<ListFilters> = _filters.asStateFlow()

    val items: StateFlow<List<Entry>> =
        combine(dao.observeAll(), _filters) { all, f -> applyFilters(all, f) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(q: String) = _filters.update { it.copy(query = q) }
    fun setSort(s: Sort) = _filters.update { it.copy(sort = s) }
    fun setMinRating(r: Int) = _filters.update { it.copy(minRating = r) }
    fun setOnlyWithPhoto(v: Boolean) = _filters.update { it.copy(onlyWithPhoto = v) }
    fun clearFilters() = _filters.update { ListFilters() }

    companion object {
        fun factory(app: DangoDiaryApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    EntryListViewModel(app.database.entryDao()) as T
            }
    }
}

private fun applyFilters(all: List<Entry>, f: ListFilters): List<Entry> {
    val q = f.query.trim().lowercase()
    val filtered = all.filter { entry ->
        if (q.isNotEmpty()) {
            val haystack = (entry.name + " " + entry.notes + " " + entry.companions + " " +
                (entry.addressText ?: "")).lowercase()
            if (!haystack.contains(q)) return@filter false
        }
        if (entry.rating < f.minRating * 2) return@filter false
        if (f.onlyWithPhoto && PhotoPaths.decode(entry.photoPathsJson).isEmpty()) return@filter false
        true
    }
    return when (f.sort) {
        Sort.RECENT -> filtered.sortedByDescending { it.visitedOn }
        Sort.RATING -> filtered.sortedByDescending { it.rating }
        Sort.NAME -> filtered.sortedBy { it.name.lowercase() }
        Sort.PRICE -> filtered.sortedBy { it.dishPriceCents ?: Long.MAX_VALUE }
    }
}
