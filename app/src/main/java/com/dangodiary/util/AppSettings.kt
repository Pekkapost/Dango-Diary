package com.dangodiary.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * App-wide user preferences backed by a Preferences DataStore at
 * `<filesDir>/datastore/dango_diary_settings.preferences_pb`.
 *
 * Three knobs: default currency for new entries, theme preset, and hide-total-price toggle.
 * Settings are read reactively via the Flow properties and re-render the UI when changed;
 * the default-currency is only seeded onto *new* entries (existing entries keep whatever
 * currency they were saved with).
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

    /** When true, the list-row total and the detail subtitle's total-price segment are hidden.
     *  Per-dish prices in the detail's Dishes section are still shown. */
    val hideTotalPrice: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_HIDE_TOTAL_PRICE] ?: false
    }

    suspend fun setDefaultCurrency(code: String) {
        dataStore.edit { it[KEY_DEFAULT_CURRENCY] = code.trim().uppercase() }
    }

    suspend fun setTheme(name: String) {
        dataStore.edit { it[KEY_THEME] = name }
    }

    suspend fun setHideTotalPrice(v: Boolean) {
        dataStore.edit { it[KEY_HIDE_TOTAL_PRICE] = v }
    }

    companion object {
        const val FALLBACK_CURRENCY = "USD"
        // Pink is the app default — see ThemeOption.PINK. Users who explicitly pick something
        // else have their choice persisted; new installs and unrecognised stored values land
        // here.
        const val FALLBACK_THEME = "PINK"
        private val KEY_DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        private val KEY_THEME = stringPreferencesKey("theme")
        private val KEY_HIDE_TOTAL_PRICE = booleanPreferencesKey("hide_total_price")
    }
}

// Extension on Context so the DataStore is process-singleton-by-construction; calling it twice
// against the same Context returns the same instance.
private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dango_diary_settings",
)

fun buildAppSettings(context: Context): AppSettings =
    AppSettings(context.applicationContext.appSettingsDataStore)
