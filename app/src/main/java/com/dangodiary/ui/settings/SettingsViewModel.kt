package com.dangodiary.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dangodiary.DangoDiaryApp
import com.dangodiary.ui.theme.ThemeOption
import com.dangodiary.util.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsState(
    val defaultCurrency: String = AppSettings.FALLBACK_CURRENCY,
    val draftDefaultCurrency: String = AppSettings.FALLBACK_CURRENCY,
    val theme: ThemeOption = ThemeOption.PURPLE,
    val hideTotalPrice: Boolean = false,
    val loading: Boolean = true,
    val saved: Boolean = false,
) {
    val hasUnsavedChanges: Boolean
        get() = draftDefaultCurrency.trim().uppercase() != defaultCurrency
}

class SettingsViewModel(private val settings: AppSettings) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val currency = settings.defaultCurrency.first()
            val theme = ThemeOption.fromName(settings.themeName.first())
            val hideTotalPrice = settings.hideTotalPrice.first()
            _state.update {
                it.copy(
                    defaultCurrency = currency,
                    draftDefaultCurrency = currency,
                    theme = theme,
                    hideTotalPrice = hideTotalPrice,
                    loading = false,
                )
            }
        }
    }

    fun setDraftCurrency(v: String) = _state.update {
        it.copy(draftDefaultCurrency = v.uppercase(), saved = false)
    }

    fun save() {
        val draft = _state.value.draftDefaultCurrency.trim().uppercase()
        if (draft.isEmpty()) return
        viewModelScope.launch {
            settings.setDefaultCurrency(draft)
            _state.update {
                it.copy(defaultCurrency = draft, draftDefaultCurrency = draft, saved = true)
            }
        }
    }

    /** Theme writes through immediately — there's no "draft theme", since the choice is one
     *  tap and the user wants to see the result right away. */
    fun setTheme(option: ThemeOption) {
        _state.update { it.copy(theme = option) }
        viewModelScope.launch { settings.setTheme(option.name) }
    }

    /** Toggle is a one-tap setting too; persist immediately so list/detail re-render. */
    fun setHideTotalPrice(v: Boolean) {
        _state.update { it.copy(hideTotalPrice = v) }
        viewModelScope.launch { settings.setHideTotalPrice(v) }
    }

    companion object {
        fun factory(app: DangoDiaryApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(app.appSettings) as T
            }
    }
}
