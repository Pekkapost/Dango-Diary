package com.dangodiary.ui.edit

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dangodiary.DangoDiaryApp
import com.dangodiary.data.Entry
import com.dangodiary.data.EntryDao
import com.dangodiary.data.PhotoPaths
import com.dangodiary.util.AppSettings
import com.dangodiary.util.PhotoStorage
import com.dangodiary.util.centsToEditableString
import com.dangodiary.util.parsePriceInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class EditState(
    val id: Long? = null,
    val name: String = "",
    val visitedOn: Long = LocalDate.now().toEpochDay(),
    val rating: Int = 0,
    val cuisine: String? = null,
    val meal: String = "",
    val notes: String = "",
    val companions: String = "",
    val priceText: String = "",
    // Defaults to AppSettings.FALLBACK_CURRENCY until the new-entry init coroutine reads the
    // user's actual preference; existing entries overwrite this with their saved currency.
    val currencyCode: String = AppSettings.FALLBACK_CURRENCY,
    val addressText: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoPaths: List<String> = emptyList(),
    val nameError: Boolean = false,
    val ratingError: Boolean = false,
    val priceError: Boolean = false,
    val loading: Boolean = true,
)

class EntryEditViewModel(
    private val id: Long?,
    private val dao: EntryDao,
    private val photoStorage: PhotoStorage,
    private val settings: AppSettings,
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
                val entry = dao.observeById(id).filterNotNull().first()
                loadFrom(entry)
            }
        } else {
            viewModelScope.launch {
                // New entry: seed the currency from the user's app-wide preference. Existing
                // entries keep their own saved code (via loadFrom), so changing the setting
                // never rewrites history.
                val currency = settings.defaultCurrency.first()
                _state.update { it.copy(currencyCode = currency, loading = false) }
            }
        }
    }

    private fun loadFrom(entry: Entry) {
        _state.update {
            it.copy(
                id = entry.id,
                name = entry.name,
                visitedOn = entry.visitedOn,
                rating = entry.rating,
                cuisine = entry.cuisine,
                meal = entry.meal,
                notes = entry.notes,
                companions = entry.companions,
                priceText = entry.dishPriceCents
                    ?.let { cents -> centsToEditableString(cents, entry.currencyCode) }
                    .orEmpty(),
                currencyCode = entry.currencyCode,
                addressText = entry.addressText.orEmpty(),
                latitude = entry.latitude,
                longitude = entry.longitude,
                photoPaths = PhotoPaths.decode(entry.photoPathsJson),
                loading = false,
            )
        }
    }

    fun setName(v: String) = _state.update { it.copy(name = v, nameError = false) }
    fun setDate(v: Long) = _state.update { it.copy(visitedOn = v) }
    fun setRating(v: Int) = _state.update { it.copy(rating = v, ratingError = false) }
    fun setCuisine(v: String?) = _state.update { it.copy(cuisine = v) }
    fun setMeal(v: String) = _state.update { it.copy(meal = v) }
    fun setNotes(v: String) = _state.update { it.copy(notes = v) }
    fun setCompanions(v: String) = _state.update { it.copy(companions = v) }
    fun setPriceText(v: String) = _state.update { it.copy(priceText = v, priceError = false) }

    /** Manual edits to the address field. Coordinates are intentionally preserved — if the
     *  user is tweaking the address after picking a place, they're usually fixing a typo, not
     *  invalidating the location. */
    fun setAddressText(v: String) = _state.update { it.copy(addressText = v) }

    /** Apply a place the user picked from name-field autocomplete: replaces name, address, and
     *  coordinates atomically. Clears the name error if it was set. */
    fun applyPickedRestaurant(name: String, address: String, lat: Double, lng: Double) =
        _state.update {
            it.copy(
                name = name,
                nameError = false,
                addressText = address,
                latitude = lat,
                longitude = lng,
            )
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
        val nameErr = name.isEmpty()
        val ratingErr = s.rating !in 1..10
        val priceCents: Long? = if (s.priceText.isBlank()) null
        else parsePriceInput(s.priceText, s.currencyCode)
        val priceErr = s.priceText.isNotBlank() && priceCents == null

        if (nameErr || ratingErr || priceErr) {
            _state.update {
                it.copy(nameError = nameErr, ratingError = ratingErr, priceError = priceErr)
            }
            return
        }

        val toSave = Entry(
            id = s.id ?: 0,
            name = name,
            visitedOn = s.visitedOn,
            rating = s.rating,
            cuisine = s.cuisine,
            meal = s.meal.trim(),
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
        fun factory(app: DangoDiaryApp, id: Long?): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    EntryEditViewModel(
                        id = id,
                        dao = app.database.entryDao(),
                        photoStorage = app.photoStorage,
                        settings = app.appSettings,
                    ) as T
            }
    }
}
