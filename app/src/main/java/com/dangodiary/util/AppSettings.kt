package com.dangodiary.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * App-wide user preferences backed by a Preferences DataStore at
 * `<filesDir>/datastore/dango_diary_settings.preferences_pb`.
 *
 * Currently exposes one knob — the default currency code applied to new entries. Older entries
 * keep whatever currency they were saved with, so flipping this setting never rewrites history.
 *
 * Single-instance pattern enforced by [Context.appSettingsDataStore]; the
 * [DangoDiaryApp][com.dangodiary.DangoDiaryApp] holds the wrapper.
 */
class AppSettings(private val dataStore: DataStore<Preferences>) {

    val defaultCurrency: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_CURRENCY] ?: FALLBACK_CURRENCY
    }

    /** The theme the user picked, stored by [com.dangodiary.ui.theme.ThemeOption.name]. The
     *  view layer maps the name back to the enum and to a [androidx.compose.material3.ColorScheme]. */
    val themeName: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_THEME] ?: FALLBACK_THEME
    }

    suspend fun setDefaultCurrency(code: String) {
        dataStore.edit { it[KEY_DEFAULT_CURRENCY] = code.trim().uppercase() }
    }

    suspend fun setTheme(name: String) {
        dataStore.edit { it[KEY_THEME] = name }
    }

    companion object {
        const val FALLBACK_CURRENCY = "USD"
        const val FALLBACK_THEME = "SYSTEM"
        private val KEY_DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        private val KEY_THEME = stringPreferencesKey("theme")
    }
}

// Extension on Context so the DataStore is process-singleton-by-construction; calling it twice
// against the same Context returns the same instance.
private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dango_diary_settings",
)

fun buildAppSettings(context: Context): AppSettings =
    AppSettings(context.applicationContext.appSettingsDataStore)
