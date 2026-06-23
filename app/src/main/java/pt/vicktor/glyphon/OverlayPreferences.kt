package pt.vicktor.glyphon

import android.content.Context
import androidx.core.content.edit

internal data class OverlayPreferenceState(
    val firstLaunchTutorialPending: Boolean,
    val showTutorialOnLaunch: Boolean,
    val colorTheme: AppColorTheme,
    val glyphLimit: Int,
    val horizontalScale: Float,
    val verticalScale: Float,
    val autoCaptureEnabled: Boolean,
    val floatingGroupX: Int,
    val floatingGroupY: Int,
    val overlayOpacityPercent: Int,
    val showGlyphs: Boolean
)

internal class OverlayPreferences(context: Context) {

    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun restore(
        defaultFloatingGroupX: Int,
        defaultFloatingGroupY: Int
    ): OverlayPreferenceState {
        val firstLaunchTutorialPending = !preferences.getBoolean(
            PREF_TUTORIAL_INITIALIZED,
            false
        )
        val showTutorialOnLaunch = preferences.getBoolean(
            PREF_SHOW_TUTORIAL_ON_LAUNCH,
            DEFAULT_SHOW_TUTORIAL_ON_LAUNCH
        )
        val colorTheme = AppThemeConfig.restoreTheme(preferences)
        val glyphLimit = preferences.getInt(PREF_GLYPH_LIMIT, DEFAULT_GLYPH_LIMIT)
            .coerceIn(3, 5)
        val horizontalScale = preferences.getFloat(PREF_HORIZONTAL_SCALE, DEFAULT_SCALE)
            .coerceIn(0.5f, 1.8f)
        val verticalScale = preferences.getFloat(PREF_VERTICAL_SCALE, DEFAULT_SCALE)
            .coerceIn(0.5f, 1.8f)
        val autoCaptureEnabled = preferences.getBoolean(
            PREF_AUTO_CAPTURE,
            DEFAULT_AUTO_CAPTURE
        )
        val resetFloatingPosition = preferences.getInt(
            PREF_FLOATING_POSITION_LAYOUT_VERSION,
            0
        ) < FLOATING_POSITION_LAYOUT_VERSION
        val floatingGroupX = if (resetFloatingPosition) {
            defaultFloatingGroupX
        } else {
            preferences.getInt(PREF_FLOATING_GROUP_X, defaultFloatingGroupX)
        }
        val floatingGroupY = if (resetFloatingPosition) {
            defaultFloatingGroupY
        } else {
            preferences.getInt(PREF_FLOATING_GROUP_Y, defaultFloatingGroupY)
        }
        val overlayOpacityPercent = preferences.getInt(
            PREF_OVERLAY_OPACITY_PERCENT,
            DEFAULT_OVERLAY_OPACITY_PERCENT
        ).takeIf { it == 100 || it == 80 || it == 60 }
            ?: DEFAULT_OVERLAY_OPACITY_PERCENT
        val showGlyphs = preferences.getBoolean(PREF_SHOW_GLYPHS, DEFAULT_SHOW_GLYPHS)

        preferences.edit {
            if (resetFloatingPosition) {
                putInt(PREF_FLOATING_GROUP_X, floatingGroupX)
                putInt(PREF_FLOATING_GROUP_Y, floatingGroupY)
                putInt(PREF_FLOATING_POSITION_LAYOUT_VERSION, FLOATING_POSITION_LAYOUT_VERSION)
            }
            if (firstLaunchTutorialPending) {
                putBoolean(PREF_TUTORIAL_INITIALIZED, true)
                putBoolean(PREF_SHOW_TUTORIAL_ON_LAUNCH, DEFAULT_SHOW_TUTORIAL_ON_LAUNCH)
            }
            if (!preferences.contains(PREF_PREMIUM_ENABLED)) {
                putBoolean(PREF_PREMIUM_ENABLED, DEFAULT_PREMIUM_ENABLED)
            }
            if (!preferences.contains(PREF_AUTO_CAPTURE_USES)) {
                putInt(PREF_AUTO_CAPTURE_USES, 0)
            }
            if (!preferences.contains(PREF_FLOATING_DRAG_USES)) {
                putInt(PREF_FLOATING_DRAG_USES, 0)
            }
            if (!preferences.contains(AppThemeConfig.PREF_COLOR_THEME)) {
                putString(AppThemeConfig.PREF_COLOR_THEME, AppThemeConfig.DEFAULT_THEME.name)
            }
        }

        return OverlayPreferenceState(
            firstLaunchTutorialPending = firstLaunchTutorialPending,
            showTutorialOnLaunch = showTutorialOnLaunch,
            colorTheme = colorTheme,
            glyphLimit = glyphLimit,
            horizontalScale = horizontalScale,
            verticalScale = verticalScale,
            autoCaptureEnabled = autoCaptureEnabled,
            floatingGroupX = floatingGroupX,
            floatingGroupY = floatingGroupY,
            overlayOpacityPercent = overlayOpacityPercent,
            showGlyphs = showGlyphs
        )
    }

    fun saveGlyphLimit(glyphLimit: Int) {
        preferences.edit { putInt(PREF_GLYPH_LIMIT, glyphLimit) }
    }

    fun saveGlyphScales(horizontalScale: Float, verticalScale: Float) {
        preferences.edit {
            putFloat(PREF_HORIZONTAL_SCALE, horizontalScale)
            putFloat(PREF_VERTICAL_SCALE, verticalScale)
        }
    }

    fun saveAutoCaptureMode(autoCaptureEnabled: Boolean) {
        preferences.edit { putBoolean(PREF_AUTO_CAPTURE, autoCaptureEnabled) }
    }

    fun saveColorTheme(colorTheme: AppColorTheme) {
        AppThemeConfig.saveTheme(preferences, colorTheme)
    }

    fun saveFloatingGroupPosition(x: Int, y: Int) {
        preferences.edit {
            putInt(PREF_FLOATING_GROUP_X, x)
            putInt(PREF_FLOATING_GROUP_Y, y)
        }
    }

    fun saveOverlayOpacity(overlayOpacityPercent: Int) {
        preferences.edit { putInt(PREF_OVERLAY_OPACITY_PERCENT, overlayOpacityPercent) }
    }

    fun saveShowGlyphs(showGlyphs: Boolean) {
        preferences.edit { putBoolean(PREF_SHOW_GLYPHS, showGlyphs) }
    }

    companion object {
        const val PREFERENCES_NAME = "glyph_trainer_state"
        const val PREF_SHOW_TUTORIAL_ON_LAUNCH = "show_tutorial_on_launch"
        const val DEFAULT_GLYPH_LIMIT = 5
        const val DEFAULT_SCALE = 1f
        const val DEFAULT_AUTO_CAPTURE = false
        const val DEFAULT_OVERLAY_OPACITY_PERCENT = 100
        const val DEFAULT_SHOW_GLYPHS = true
        const val DEFAULT_SHOW_TUTORIAL_ON_LAUNCH = false

        private const val PREF_GLYPH_LIMIT = "glyph_limit"
        private const val PREF_HORIZONTAL_SCALE = "horizontal_scale"
        private const val PREF_VERTICAL_SCALE = "vertical_scale"
        private const val PREF_AUTO_CAPTURE = "auto_capture"
        private const val PREF_FLOATING_GROUP_X = "floating_group_x"
        private const val PREF_FLOATING_GROUP_Y = "floating_group_y"
        private const val PREF_FLOATING_POSITION_LAYOUT_VERSION = "floating_position_layout_version"
        private const val PREF_OVERLAY_OPACITY_PERCENT = "overlay_opacity_percent"
        private const val PREF_SHOW_GLYPHS = "show_glyphs"
        private const val PREF_TUTORIAL_INITIALIZED = "tutorial_initialized"
        private const val PREF_PREMIUM_ENABLED = "premium_enabled"
        private const val PREF_AUTO_CAPTURE_USES = "auto_capture_uses"
        private const val PREF_FLOATING_DRAG_USES = "floating_drag_uses"
        private const val DEFAULT_PREMIUM_ENABLED = false
        private const val FLOATING_POSITION_LAYOUT_VERSION = 1
    }
}
