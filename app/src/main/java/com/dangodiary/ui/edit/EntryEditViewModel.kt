package com.dangodiary.ui.edit

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dangodiary.DangoDiaryApp
import com.dangodiary.data.Dish
import com.dangodiary.data.Dishes
import com.dangodiary.data.Entry
import com.dangodiary.data.EntryDao
import com.dangodiary.data.Photo
import com.dangodiary.data.Photos
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
import java.util.UUID

/** One row in the dish list as the user is editing it. The price is held as the raw text the
 *  user typed; we only parse it at save time so partial input ("12.") doesn't blow up.
 *
 *  [id] is a per-row UUID used as a stable Compose key by the reorderable list — without it,
 *  two equal drafts (e.g. two blank rows) would collide. It's session-only; not persisted. */
data class DishDraft(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val priceText: String = "",
    val priceError: Boolean = false,
)

data class EditState(
    val id: Long? = null,
    val name: String = "",
    /** The restaurant name as loaded from the DB (or "" for a new entry). Used by the name
     *  field to suppress autocomplete suggestions for the unedited loaded value — typing into
     *  the field for an existing entry shouldn't immediately blast the user with alternatives
     *  for what they already saved. Any user edit makes [name] diverge from this and unlocks
     *  the dropdown. */
    val initialName: String = "",
    val visitedOn: Long = LocalDate.now().toEpochDay(),
    val rating: Int = 0,
    val cuisine: String? = null,
    val dishes: List<DishDraft> = listOf(DishDraft()),
    val notes: String = "",
    val companions: String = "",
    // Defaults to AppSettings.FALLBACK_CURRENCY until the new-entry init coroutine reads the
    // user's actual preference; existing entries overwrite this with their saved currency.
    val currencyCode: String = AppSettings.FALLBACK_CURRENCY,
    val addressText: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photos: List<Photo> = emptyList(),
    val isLegacy: Boolean = false,
    val nameError: Boolean = false,
    val ratingError: Boolean = false,
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
        val savedDishes = Dishes.decode(entry.dishesJson).map { dish ->
            DishDraft(
                name = dish.name,
                priceText = dish.priceCents
                    ?.let { cents -> centsToEditableString(cents, entry.currencyCode) }
                    .orEmpty(),
            )
        }
        // Fall back to a single empty dish if the saved list is empty so the UI always renders
        // at least one row. Pre-v3 entries that escaped the migration backfill end up here too.
        val dishes = savedDishes.ifEmpty { listOf(DishDraft()) }

        _state.update {
            it.copy(
                id = entry.id,
                name = entry.name,
                initialName = entry.name,
                visitedOn = entry.visitedOn,
                rating = entry.rating,
                cuisine = entry.cuisine,
                dishes = dishes,
                notes = entry.notes,
                companions = entry.companions,
                currencyCode = entry.currencyCode,
                addressText = entry.addressText.orEmpty(),
                latitude = entry.latitude,
                longitude = entry.longitude,
                photos = Photos.decode(entry.photoPathsJson),
                isLegacy = entry.isLegacy,
                loading = false,
            )
        }
    }

    fun setName(v: String) = _state.update { it.copy(name = v, nameError = false) }
    fun setDate(v: Long) = _state.update { it.copy(visitedOn = v) }
    fun setRating(v: Int) = _state.update { it.copy(rating = v, ratingError = false) }
    fun setCuisine(v: String?) = _state.update { it.copy(cuisine = v) }
    fun setNotes(v: String) = _state.update { it.copy(notes = v) }
    fun setCompanions(v: String) = _state.update { it.copy(companions = v) }
    fun setLegacy(v: Boolean) = _state.update { it.copy(isLegacy = v) }

    fun setDishName(index: Int, v: String) = updateDish(index) { it.copy(name = v) }
    fun setDishPrice(index: Int, v: String) = updateDish(index) {
        it.copy(priceText = v, priceError = false)
    }

    fun addDish() = _state.update { it.copy(dishes = it.dishes + DishDraft()) }

    fun moveDish(fromIndex: Int, toIndex: Int) = _state.update { st ->
        st.copy(dishes = st.dishes.moved(fromIndex, toIndex))
    }

    fun removeDish(index: Int) = _state.update {
        // Always keep at least one row so the form has somewhere to type. Clearing the last
        // remaining dish is equivalent to leaving it blank.
        val next = it.dishes.toMutableList()
        if (next.size <= 1) {
            next[0] = DishDraft()
        } else if (index in next.indices) {
            next.removeAt(index)
        }
        it.copy(dishes = next)
    }

    private fun updateDish(index: Int, transform: (DishDraft) -> DishDraft) = _state.update {
        if (index !in it.dishes.indices) return@update it
        val next = it.dishes.toMutableList()
        next[index] = transform(next[index])
        it.copy(dishes = next)
    }

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
        _state.update { it.copy(photos = it.photos + Photo(path = path)) }
    }

    fun importPhotoFromUri(uri: Uri) {
        viewModelScope.launch {
            val path = photoStorage.importFrom(uri) ?: return@launch
            addPhotoPath(path)
        }
    }

    fun setPhotoCaption(path: String, caption: String) = _state.update { st ->
        st.copy(photos = st.photos.map { if (it.path == path) it.copy(caption = caption) else it })
    }

    fun movePhoto(fromIndex: Int, toIndex: Int) = _state.update { st ->
        st.copy(photos = st.photos.moved(fromIndex, toIndex))
    }

    fun removePhoto(path: String) {
        // If the user added it in this session and then removed it, delete the file too.
        if (path in addedPaths) {
            addedPaths -= path
            photoStorage.delete(listOf(path))
        }
        _state.update { it.copy(photos = it.photos.filterNot { it.path == path }) }
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

        // Parse every dish row that has a non-blank price; any invalid price flips its row's
        // priceError flag. Empty price text is fine (price unknown). Empty dish name + empty
        // price = row gets dropped on save.
        var anyPriceErr = false
        val parsedDishes = s.dishes.mapIndexed { i, draft ->
            if (draft.priceText.isBlank()) {
                draft.copy(priceError = false) to null
            } else {
                val cents = parsePriceInput(draft.priceText, s.currencyCode)
                if (cents == null) {
                    anyPriceErr = true
                    draft.copy(priceError = true) to null
                } else {
                    draft.copy(priceError = false) to cents
                }
            }
        }

        if (nameErr || ratingErr || anyPriceErr) {
            _state.update {
                it.copy(
                    nameError = nameErr,
                    ratingError = ratingErr,
                    dishes = parsedDishes.map { (draft, _) -> draft },
                )
            }
            return
        }

        // Build the persisted dish list: drop fully-blank rows (no name AND no price); keep
        // a row that has either. The fallback below ensures we always store at least an empty
        // list rather than dropping the column when the user added no dishes at all.
        val finalDishes = parsedDishes.mapNotNull { (draft, cents) ->
            val cleanName = draft.name.trim()
            if (cleanName.isEmpty() && cents == null) null
            else Dish(name = cleanName, priceCents = cents)
        }

        val toSave = Entry(
            id = s.id ?: 0,
            name = name,
            visitedOn = s.visitedOn,
            rating = s.rating,
            cuisine = s.cuisine,
            dishesJson = Dishes.encode(finalDishes),
            notes = s.notes.trim(),
            companions = s.companions.trim(),
            currencyCode = s.currencyCode,
            addressText = s.addressText.trim().ifEmpty { null },
            latitude = s.latitude,
            longitude = s.longitude,
            photoPathsJson = Photos.encode(s.photos.map { it.copy(caption = it.caption.trim()) }),
            isLegacy = s.isLegacy,
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

/** Returns a new list with the element at [fromIndex] moved to [toIndex]. Out-of-range or
 *  no-op moves return the receiver unchanged. */
private fun <T> List<T>.moved(fromIndex: Int, toIndex: Int): List<T> {
    if (fromIndex == toIndex) return this
    if (fromIndex !in indices || toIndex !in indices) return this
    val mutable = toMutableList()
    val item = mutable.removeAt(fromIndex)
    mutable.add(toIndex, item)
    return mutable
}
