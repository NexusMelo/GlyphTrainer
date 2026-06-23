package pt.vicktor.glyphon

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat

internal class OverlayControlFactory(private val context: Context) {

    fun createMenuButton(@StringRes textRes: Int): TextView {
        return TextView(context).apply {
            setText(textRes)
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(30, 20, 30, 20)
            setBackgroundColor(0xAA000000.toInt())
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
    }

    fun createIconButton(@DrawableRes iconRes: Int, iconColor: Int): TextView {
        return TextView(context).apply {
            styleMainControlButton(this, iconColor, 34f)
            setVectorIcon(this, iconRes, iconColor)
            visibility = View.INVISIBLE
        }
    }

    fun styleFloatingRoundButton(
        button: TextView,
        textSize: Float,
        elevationValue: Float,
        fillColor: Int,
        strokeWidth: Int,
        strokeColor: Int
    ) {
        button.apply {
            this.textSize = textSize
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            includeFontPadding = false
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(fillColor)
                setStroke(strokeWidth, strokeColor)
            }
            elevation = elevationValue
        }
    }

    fun styleFloatingPillText(button: TextView) {
        button.apply {
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            includeFontPadding = false
        }
    }

    fun styleSquareControl(
        button: TextView,
        contentColor: Int,
        theme: AppColorTheme
    ) {
        styleOutlinedControl(
            button,
            contentColor,
            SQUARE_CONTROL_STROKE_WIDTH,
            SQUARE_CONTROL_CORNER_RADIUS,
            theme
        )
    }

    fun styleRectangularControl(
        button: TextView,
        contentColor: Int,
        theme: AppColorTheme
    ) {
        styleOutlinedControl(
            button,
            contentColor,
            RECTANGULAR_CONTROL_STROKE_WIDTH,
            RECTANGULAR_CONTROL_CORNER_RADIUS,
            theme
        )
    }

    fun setVectorIcon(button: TextView, @DrawableRes iconRes: Int, tintColor: Int) {
        button.text = null
        val icon = ContextCompat.getDrawable(context, iconRes)?.mutate()
        button.compoundDrawableTintList = ColorStateList.valueOf(tintColor)
        button.setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null)
    }

    fun applyReferenceButtonBackground(button: TextView, @DrawableRes drawableRes: Int) {
        button.text = null
        button.compoundDrawableTintList = null
        button.setCompoundDrawables(null, null, null, null)
        button.background = ContextCompat.getDrawable(context, drawableRes)
    }

    private fun styleMainControlButton(button: TextView, textColor: Int, textSize: Float) {
        button.setTextColor(textColor)
        button.textSize = textSize
        button.gravity = Gravity.CENTER
        button.includeFontPadding = false
        button.setPadding(0, 0, 0, 0)
        button.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun styleOutlinedControl(
        button: TextView,
        contentColor: Int,
        strokeWidth: Int,
        cornerRadius: Float,
        theme: AppColorTheme
    ) {
        button.setTextColor(contentColor)
        button.compoundDrawableTintList = ColorStateList.valueOf(contentColor)
        button.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            this.cornerRadius = cornerRadius
            setColor(Color.TRANSPARENT)
            setStroke(strokeWidth, AppThemeConfig.colors(theme).outline)
        }
    }

    private companion object {
        const val SQUARE_CONTROL_STROKE_WIDTH = 3
        const val RECTANGULAR_CONTROL_STROKE_WIDTH = 4
        const val SQUARE_CONTROL_CORNER_RADIUS = 18f
        const val RECTANGULAR_CONTROL_CORNER_RADIUS = 18f
    }
}
