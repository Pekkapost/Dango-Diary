package com.dangodiary.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dangodiary.DangoDiaryApp
import com.dangodiary.util.AppSettings

/**
 * Root theme wrapper. Reads the user's [ThemeOption] preference reactively from
 * [AppSettings.themeName] so a change made in the Settings screen re-themes the whole app
 * immediately — no restart, no re-navigation.
 *
 * Dark mode follows the system. [ThemeOption.SYSTEM] uses the default Material 3 schemes; the
 * other options carry their own hand-tuned light + dark colour pairs in [ThemeOption].
 *
 * Material You dynamic colour is intentionally dropped: it overrides the user's chosen theme
 * with the wallpaper-derived scheme on Android 12+, which defeats the purpose of an in-app
 * theme picker. If we want it back, expose it as a separate setting.
 */
@Composable
fun DangoDiaryTheme(content: @Composable () -> Unit) {
    val app = LocalContext.current.applicationContext as DangoDiaryApp
    val themeName by app.appSettings.themeName
        .collectAsStateWithLifecycle(initialValue = AppSettings.FALLBACK_THEME)
    val option = ThemeOption.fromName(themeName)
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = colorSchemeFor(option, dark),
        content = content,
    )
}
