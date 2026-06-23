package pt.vicktor.glyphon

import android.content.SharedPreferences
import android.graphics.Color
import androidx.core.content.edit

enum class AppColorTheme {
    GREEN,
    BLUE,
    STANDARD
}

data class AppThemeColors(
    val accent: Int,
    val accentSoft: Int,
    val outline: Int,
    val panelBackground: Int,
    val buttonBackground: Int,
    val text: Int
)

object AppThemeConfig {
    const val PREFERENCES_NAME = "glyph_trainer_state"
    const val PREF_COLOR_THEME = "app_color_theme"

    val DEFAULT_THEME = AppColorTheme.STANDARD

    fun colors(theme: AppColorTheme): AppThemeColors {
        return when (theme) {
            AppColorTheme.GREEN -> AppThemeColors(
                accent = Color.rgb(0, 255, 140),
                accentSoft = Color.argb(150, 0, 255, 140),
                outline = Color.argb(230, 0, 255, 140),
                panelBackground = Color.argb(225, 4, 18, 14),
                buttonBackground = Color.argb(185, 10, 55, 42),
                text = Color.WHITE
            )
            AppColorTheme.BLUE -> AppThemeColors(
                accent = Color.rgb(0, 210, 255),
                accentSoft = Color.argb(150, 0, 210, 255),
                outline = Color.argb(230, 0, 210, 255),
                panelBackground = Color.argb(225, 5, 14, 28),
                buttonBackground = Color.argb(185, 8, 40, 70),
                text = Color.WHITE
            )
            AppColorTheme.STANDARD -> AppThemeColors(
                accent = Color.rgb(220, 225, 230),
                accentSoft = Color.argb(135, 220, 225, 230),
                outline = Color.argb(215, 220, 225, 230),
                panelBackground = Color.argb(225, 18, 18, 18),
                buttonBackground = Color.argb(175, 55, 55, 55),
                text = Color.WHITE
            )
        }
    }

    fun restoreTheme(preferences: SharedPreferences): AppColorTheme {
        val stored = preferences.getString(PREF_COLOR_THEME, DEFAULT_THEME.name)
        return AppColorTheme.entries.firstOrNull { it.name == stored } ?: DEFAULT_THEME
    }

    fun saveTheme(preferences: SharedPreferences, theme: AppColorTheme) {
        preferences.edit {
            putString(PREF_COLOR_THEME, theme.name)
        }
    }

    fun nextTheme(theme: AppColorTheme): AppColorTheme {
        return when (theme) {
            AppColorTheme.STANDARD -> AppColorTheme.GREEN
            AppColorTheme.GREEN -> AppColorTheme.BLUE
            AppColorTheme.BLUE -> AppColorTheme.STANDARD
        }
    }

    fun shortLabel(theme: AppColorTheme): String {
        return when (theme) {
            AppColorTheme.STANDARD -> "STD"
            AppColorTheme.GREEN -> "GREEN"
            AppColorTheme.BLUE -> "BLUE"
        }
    }

    fun isFuturePremiumTheme(theme: AppColorTheme): Boolean {
        // Future premium policy only: STANDARD for standard users; GREEN/BLUE for premium.
        // No restriction is enforced in this phase.
        return theme != AppColorTheme.STANDARD
    }
}
