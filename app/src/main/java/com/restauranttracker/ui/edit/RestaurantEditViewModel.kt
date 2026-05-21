package com.restauranttracker.ui.edit

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.restauranttracker.RestaurantApp
import com.restauranttracker.data.PhotoPaths
import com.restauranttracker.data.Restaurant
import com.restauranttracker.data.RestaurantDao
import com.restauranttracker.util.PhotoStorage
import com.restauranttracker.util.parsePriceInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Currency
import java.util.Locale

data class EditState(
    val id: Long? = null,
    val name: String = "",
    val visitedOn: Long = LocalDate.now().toEpochDay(),
    val rating: Int = 0,
    val cuisine: String? = null,
    val notes: String = "",
    val companions: String = "",
    val priceText: String = "",
    val currencyCode: String = Currency.getInstance(Locale.getDefault()).currencyCode,
    val addressText: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoPaths: List<String> = emptyList(),
    val nameError: String? = null,
    val ratingError: String? = null,
    val priceError: String? = null,
    val loading: Boolean = true,
)

class RestaurantEditViewModel(
    private val id: Long?,
    private val dao: RestaurantDao,
    private val photoStorage: PhotoStorage,
) : ViewModel() {

    private val _state = MutableStateFlow(EditState(id = id))
    val state: StateFlow<EditState> = _state.asStateFlow()

    // Paths added during this edit session that must be cleaned up if the user cancels
    // without saving. Existing photos on the row are not in here.
    private val addedPaths = mutableListOf<String>()

    init {
        if (id != null) {
            viewModelScope.launch {
                // Load once. We deliberately do not keep observing — once the user opens the
                // form, they own the field values until they save or cancel.
                val restaurant = dao.observeById(id).filterNotNull().first()
                loadFrom(restaurant)
            }
        } else {
            _state.update { it.copy(loading = false) }
        }
    }

    private fun loadFrom(r: Restaurant) {
        _state.update {
            it.copy(
                id = r.id,
                name = r.name,
                visitedOn = r.visitedOn,
                rating = r.rating,
                cuisine = r.cuisine,
                notes = r.notes,
                companions = r.companions,
                priceText = r.dishPriceCents?.let { cents ->
                    val divisor = pow10(Currency.getInstance(r.currencyCode).defaultFractionDigits)
                    (cents.toDouble() / divisor).toString()
                } ?: "",
                currencyCode = r.currencyCode,
                addressText = r.addressText.orEmpty(),
                latitude = r.latitude,
                longitude = r.longitude,
                photoPaths = PhotoPaths.decode(r.photoPathsJson),
                loading = false,
            )
        }
    }

    fun setName(v: String) = _state.update { it.copy(name = v, nameError = null) }
    fun setDate(v: Long) = _state.update { it.copy(visitedOn = v) }
    fun setRating(v: Int) = _state.update { it.copy(rating = v, ratingError = null) }
    fun setCuisine(v: String?) = _state.update { it.copy(cuisine = v) }
    fun setNotes(v: String) = _state.update { it.copy(notes = v) }
    fun setCompanions(v: String) = _state.update { it.copy(companions = v) }
    fun setPriceText(v: String) = _state.update { it.copy(priceText = v, priceError = null) }
    fun setCurrencyCode(v: String) = _state.update { it.copy(currencyCode = v.uppercase()) }
    /** Set address and coordinates together when the user picks an autocomplete result. */
    fun setPlace(address: String, lat: Double, lng: Double) = _state.update {
        it.copy(addressText = address, latitude = lat, longitude = lng)
    }

    fun clearLocation() = _state.update {
        it.copy(addressText = "", latitude = null, longitude = null)
    }

    fun addPhotoPath(path: String) {
        addedPaths += path
        _state.update { it.copy(photoPaths = it.photoPaths + path) }
    }

    fun importPhotoFromUri(uri: Uri) {
        viewModelScope.launch {
            val path = photoStorage.importFrom(uri) ?: return@launch
            addPhotoPath(path)
        }
    }

    fun removePhoto(path: String) {
        // If the user added it in this session and then removed it, delete the file too.
        if (path in addedPaths) {
            addedPaths -= path
            photoStorage.delete(listOf(path))
        }
        _state.update { it.copy(photoPaths = it.photoPaths - path) }
    }

    fun cancel() {
        photoStorage.delete(addedPaths)
        addedPaths.clear()
    }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        val name = s.name.trim()
        val nameErr = if (name.isEmpty()) "edit_name_required" else null
        val ratingErr = if (s.rating !in 1..10) "edit_rating_required" else null
        val priceCents: Long? = if (s.priceText.isBlank()) null
        else parsePriceInput(s.priceText, s.currencyCode)
        val priceErr = if (s.priceText.isNotBlank() && priceCents == null) "edit_price_invalid" else null

        if (nameErr != null || ratingErr != null || priceErr != null) {
            _state.update {
                it.copy(nameError = nameErr, ratingError = ratingErr, priceError = priceErr)
            }
            return
        }

        val toSave = Restaurant(
            id = s.id ?: 0,
            name = name,
            visitedOn = s.visitedOn,
            rating = s.rating,
            cuisine = s.cuisine,
            notes = s.notes.trim(),
            companions = s.companions.trim(),
            dishPriceCents = priceCents,
            currencyCode = s.currencyCode,
            addressText = s.addressText.trim().ifEmpty { null },
            latitude = s.latitude,
            longitude = s.longitude,
            photoPathsJson = PhotoPaths.encode(s.photoPaths),
        )

        viewModelScope.launch {
            if (s.id == null) dao.insert(toSave) else dao.update(toSave)
            addedPaths.clear()
            onDone()
        }
    }

    companion object {
        fun factory(app: RestaurantApp, id: Long?): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RestaurantEditViewModel(
                        id = id,
                        dao = app.database.restaurantDao(),
                        photoStorage = app.photoStorage,
                    ) as T
            }
    }
}

private fun pow10(n: Int): Double {
    var r = 1.0
    repeat(n) { r *= 10 }
    return r
}
