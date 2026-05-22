package com.dangodiary.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Built-in app themes. Persisted via [com.dangodiary.util.AppSettings.themeName] by enum name.
 *
 * Declaration order is the order shown in the Settings screen. Each option carries a
 * representative [swatch] (rendered as a coloured dot beside the radio row) and a hand-picked
 * light + dark [ColorScheme] pair.
 */
enum class ThemeOption(
    val displayName: String,
    val swatch: Color,
    val isAppDefault: Boolean = false,
) {
    PURPLE("Purple", Color(0xFF6A1B9A), isAppDefault = true),
    BROWN("Brown", Color(0xFF8D6E63)),
    BLUE("Blue", Color(0xFF1976D2)),
    RED("Red", Color(0xFFD32F2F)),
    PINK("Pink", Color(0xFFFDA2F5));

    companion object {
        /** Unknown / unrecognised names (legacy "SYSTEM", "GREEN", etc.) fall back to the
         *  app default, [PURPLE]. */
        fun fromName(name: String?): ThemeOption =
            entries.firstOrNull { it.name == name } ?: PURPLE
    }
}

// ---- Purple (app default) ------------------------------------------------------------------

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

// ---- Brown ---------------------------------------------------------------------------------
// primaryContainer is the FAB background in M3 — bumped from a peach (#FFDBC8) to a clear
// medium brown (#A1887F, Material Brown 300) so the "Add restaurant" FAB actually reads as
// brown rather than washed-out cream. onPrimaryContainer stays dark for label contrast.

private val BrownLight = lightColorScheme(
    primary = Color(0xFF6D4C41),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA1887F),
    onPrimaryContainer = Color(0xFF2A1708),
    secondary = Color(0xFF77574A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBC8),
    onSecondaryContainer = Color(0xFF2C160B),
    tertiary = Color(0xFF655F31),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFF8F5),
    surface = Color(0xFFFFF8F5),
    surfaceVariant = Color(0xFFF3DDD2),
    onSurfaceVariant = Color(0xFF52433C),
)

private val BrownDark = darkColorScheme(
    primary = Color(0xFFFFB694),
    onPrimary = Color(0xFF4F2A19),
    primaryContainer = Color(0xFF8D6E63),
    onPrimaryContainer = Color(0xFFFFE9DC),
    secondary = Color(0xFFE7BEAA),
    onSecondary = Color(0xFF442A1F),
    background = Color(0xFF1A120E),
    surface = Color(0xFF1A120E),
    surfaceVariant = Color(0xFF52433C),
    onSurfaceVariant = Color(0xFFD7C2B7),
)

// ---- Blue ----------------------------------------------------------------------------------

private val BlueLight = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F7),
    onSecondaryContainer = Color(0xFF101C2B),
    tertiary = Color(0xFF6B5778),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF8FAFE),
    surface = Color(0xFFF8FAFE),
    surfaceVariant = Color(0xFFDFE3EB),
    onSurfaceVariant = Color(0xFF43474E),
)

private val BlueDark = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003259),
    primaryContainer = Color(0xFF00497F),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    background = Color(0xFF101418),
    surface = Color(0xFF101418),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7CF),
)

// ---- Red -----------------------------------------------------------------------------------

private val RedLight = lightColorScheme(
    primary = Color(0xFFD32F2F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD5),
    onPrimaryContainer = Color(0xFF410001),
    secondary = Color(0xFF775653),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD5),
    onSecondaryContainer = Color(0xFF2C1513),
    tertiary = Color(0xFF715B2E),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFBFA),
    surface = Color(0xFFFFFBFA),
    surfaceVariant = Color(0xFFF5DDD9),
    onSurfaceVariant = Color(0xFF534340),
)

private val RedDark = darkColorScheme(
    primary = Color(0xFFFFB4A9),
    onPrimary = Color(0xFF690003),
    primaryContainer = Color(0xFF930006),
    onPrimaryContainer = Color(0xFFFFDAD5),
    secondary = Color(0xFFE7BDB7),
    onSecondary = Color(0xFF442927),
    background = Color(0xFF1A110F),
    surface = Color(0xFF1A110F),
    surfaceVariant = Color(0xFF534340),
    onSurfaceVariant = Color(0xFFD8C2BE),
)

// ---- Pink ----------------------------------------------------------------------------------
// Primary is the user's canonical pink #FDA2F5 — a light pastel, so onPrimary stays dark.

private val PinkLight = lightColorScheme(
    primary = Color(0xFFFDA2F5),
    onPrimary = Color(0xFF2A0029),
    primaryContainer = Color(0xFFFFD7FB),
    onPrimaryContainer = Color(0xFF40003D),
    secondary = Color(0xFF8B5A86),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD8F2),
    onSecondaryContainer = Color(0xFF36082F),
    tertiary = Color(0xFFB44A6F),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFF8FB),
    surface = Color(0xFFFFF8FB),
    surfaceVariant = Color(0xFFF3DDEC),
    onSurfaceVariant = Color(0xFF51434C),
)

private val PinkDark = darkColorScheme(
    primary = Color(0xFFFDA2F5),
    onPrimary = Color(0xFF4A0046),
    primaryContainer = Color(0xFF6B1F65),
    onPrimaryContainer = Color(0xFFFFD7FB),
    secondary = Color(0xFFF7B3E8),
    onSecondary = Color(0xFF4B0746),
    background = Color(0xFF1E1119),
    surface = Color(0xFF1E1119),
    surfaceVariant = Color(0xFF51434C),
    onSurfaceVariant = Color(0xFFD5C2CE),
)

/** Returns the [ColorScheme] for the given theme + dark-mode preference. */
fun colorSchemeFor(option: ThemeOption, dark: Boolean): ColorScheme = when (option) {
    ThemeOption.PURPLE -> if (dark) PurpleDark else PurpleLight
    ThemeOption.BROWN -> if (dark) BrownDark else BrownLight
    ThemeOption.BLUE -> if (dark) BlueDark else BlueLight
    ThemeOption.RED -> if (dark) RedDark else RedLight
    ThemeOption.PINK -> if (dark) PinkDark else PinkLight
}
