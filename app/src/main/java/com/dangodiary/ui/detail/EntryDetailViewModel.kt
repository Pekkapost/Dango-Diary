package com.dangodiary.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dangodiary.DangoDiaryApp
import com.dangodiary.data.PhotoPaths
import com.dangodiary.data.Entry
import com.dangodiary.data.EntryDao
import com.dangodiary.util.PhotoStorage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EntryDetailViewModel(
    private val id: Long,
    private val dao: EntryDao,
    private val photoStorage: PhotoStorage,
) : ViewModel() {

    val entry: StateFlow<Entry?> =
        dao.observeById(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(onDone: () -> Unit) {
        val current = entry.value ?: return
        viewModelScope.launch {
            photoStorage.delete(PhotoPaths.decode(current.photoPathsJson))
            dao.delete(current)
            onDone()
        }
    }

    companion object {
        fun factory(app: DangoDiaryApp, id: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    EntryDetailViewModel(
                        id = id,
                        dao = app.database.entryDao(),
                        photoStorage = app.photoStorage,
                    ) as T
            }
    }
}
