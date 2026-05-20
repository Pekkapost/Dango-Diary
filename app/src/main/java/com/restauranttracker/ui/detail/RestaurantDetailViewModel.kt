package com.restauranttracker.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.restauranttracker.RestaurantApp
import com.restauranttracker.data.PhotoPaths
import com.restauranttracker.data.Restaurant
import com.restauranttracker.data.RestaurantDao
import com.restauranttracker.util.PhotoStorage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RestaurantDetailViewModel(
    private val id: Long,
    private val dao: RestaurantDao,
    private val photoStorage: PhotoStorage,
) : ViewModel() {

    val restaurant: StateFlow<Restaurant?> =
        dao.observeById(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(onDone: () -> Unit) {
        val current = restaurant.value ?: return
        viewModelScope.launch {
            photoStorage.delete(PhotoPaths.decode(current.photoPathsJson))
            dao.delete(current)
            onDone()
        }
    }

    companion object {
        fun factory(app: RestaurantApp, id: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RestaurantDetailViewModel(
                        id = id,
                        dao = app.database.restaurantDao(),
                        photoStorage = app.photoStorage,
                    ) as T
            }
    }
}
