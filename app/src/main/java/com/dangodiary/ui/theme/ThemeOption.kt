package com.dangodiary.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Built-in app themes. Persisted via [com.dangodiary.util.AppSettings.theme] by name.
 *
 * Each option carries a hand-picked light + dark [ColorScheme] pair. SYSTEM uses the default
 * Material 3 colours (and on Android 12+, Material You dynamic colours when enabled).
 */
enum class ThemeOption(val displayName: String) {
    SYSTEM("System default"),
    PINK("Pink"),
    PURPLE("Purple"),
    GREEN("Green");

    companion object {
        fun fromName(name: String?): ThemeOption =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

private val SystemLight = lightColorScheme()
private val SystemDark = darkColorScheme()

// ---- Pink ----------------------------------------------------------------------------------
// Warm pinks anchored on #E91E63 / Material's pink 500-ish range. Secondary leans coral, tertiary
// is a soft mauve so accent buttons aren't all the same hue.

private val PinkLight = lightColorScheme(
    primary = Color(0xFFCB1F62),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF40001E),
    secondary = Color(0xFFB73E5A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9DD),
    onSecondaryContainer = Color(0xFF400015),
    tertiary = Color(0xFF8E4A6B),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFF8F8),
    surface = Color(0xFFFFF8F8),
    surfaceVariant = Color(0xFFF3DDE2),
    onSurfaceVariant = Color(0xFF514347),
)

private val PinkDark = darkColorScheme(
    primary = Color(0xFFFFB1C7),
    onPrimary = Color(0xFF650033),
    primaryContainer = Color(0xFF8E0049),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFFFB2BD),
    onSecondary = Color(0xFF65002A),
    background = Color(0xFF1F1216),
    surface = Color(0xFF1F1216),
    surfaceVariant = Color(0xFF514347),
    onSurfaceVariant = Color(0xFFD6C2C7),
)

// ---- Purple --------------------------------------------------------------------------------
// Deeper, more violet than the default M3 purple. Primary close to #6A1B9A; tertiary lavender.

private val PurpleLight = lightColorScheme(
    primary = Color(0xFF6A1B9A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDDCFF),
    onPrimaryContainer = Color(0xFF24005A),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1E192B),
    tertiary = Color(0xFF7E5260),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFDF7FF),
    surface = Color(0xFFFDF7FF),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
)

private val PurpleDark = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    background = Color(0xFF141218),
    surface = Color(0xFF141218),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
)

// ---- Green ---------------------------------------------------------------------------------
// Forest-leaning green; secondary olive, tertiary teal-ish for visual variety.

private val GreenLight = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB8F0BA),
    onPrimaryContainer = Color(0xFF002106),
    secondary = Color(0xFF52634F),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF38656A),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF7FBF1),
    surface = Color(0xFFF7FBF1),
    surfaceVariant = Color(0xFFDEE5D8),
    onSurfaceVariant = Color(0xFF424940),
)

private val GreenDark = darkColorScheme(
    primary = Color(0xFF9CD49E),
    onPrimary = Color(0xFF003910),
    primaryContainer = Color(0xFF0E5320),
    onPrimaryContainer = Color(0xFFB8F0BA),
    secondary = Color(0xFFBACBB4),
    onSecondary = Color(0xFF253423),
    background = Color(0xFF101510),
    surface = Color(0xFF101510),
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC1C9BB),
)

/** Returns the [ColorScheme] for the given theme + dark-mode preference. */
fun colorSchemeFor(option: ThemeOption, dark: Boolean): ColorScheme = when (option) {
    ThemeOption.SYSTEM -> if (dark) SystemDark else SystemLight
    ThemeOption.PINK -> if (dark) PinkDark else PinkLight
    ThemeOption.PURPLE -> if (dark) PurpleDark else PurpleLight
    ThemeOption.GREEN -> if (dark) GreenDark else GreenLight
}
