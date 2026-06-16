package com.example.glyphtrainer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.widget.TextView
import androidx.annotation.StringRes

object TutorialHudUi {
    private const val PANEL_CORNER_CUT = 30f
    private const val BUTTON_CORNER_CUT = 14f

    fun makeControlLabel(context: android.content.Context, @StringRes textRes: Int): TextView {
        return TextView(context).apply {
            setText(textRes)
            textSize = 12.5f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            includeFontPadding = false
            letterSpacing = 0.08f
        }
    }

    fun makeControlButton(context: android.content.Context): TextView {
        return TextView(context).apply {
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            includeFontPadding = false
            letterSpacing = 0.08f
            elevation = 8f
        }
    }

    fun makeTutorialButton(
        context: android.content.Context,
        @StringRes textRes: Int,
        textSize: Float
    ): TextView {
        return TextView(context).apply {
            setText(textRes)
            this.textSize = textSize
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            includeFontPadding = false
            elevation = 8f
        }
    }

    fun makeIndicatorDot(context: android.content.Context): TextView {
        return TextView(context).apply {
            text = "•"
            textSize = 24f
            gravity = Gravity.CENTER
            includeFontPadding = false
        }
    }

    fun styleLabel(label: TextView, colors: AppThemeColors) {
        label.setTextColor(colors.text)
        label.background = null
    }

    fun styleButton(button: TextView, colors: AppThemeColors) {
        button.setTextColor(colors.text)
        button.background = SoftHudDrawable(fillColor = Color.argb(70, 255, 255, 255))
    }

    fun styleIconButton(button: TextView, colors: AppThemeColors) {
        button.setTextColor(Color.argb(230, 245, 250, 255))
        button.background = null
        button.setShadowLayer(6f, 0f, 0f, colors.accentSoft)
    }

    fun styleSwitch(button: TextView, colors: AppThemeColors, enabled: Boolean) {
        button.setTextColor(colors.text)
        val activeColor = if (enabled) Color.rgb(0, 190, 95) else Color.rgb(185, 42, 42)
        button.background = SwitchHudDrawable(
            fillColor = Color.argb(190, Color.red(activeColor), Color.green(activeColor), Color.blue(activeColor)),
            strokeColor = Color.argb(230, Color.red(activeColor), Color.green(activeColor), Color.blue(activeColor)),
            knobColor = Color.WHITE,
            knobOnRight = enabled
        )
    }

    fun styleSelector(button: TextView, colors: AppThemeColors, theme: AppColorTheme) {
        button.setTextColor(colors.text)
        val selectorColor = when (theme) {
            AppColorTheme.STANDARD -> Color.rgb(95, 100, 108)
            AppColorTheme.GREEN -> Color.rgb(0, 170, 95)
            AppColorTheme.BLUE -> Color.rgb(0, 135, 210)
        }
        button.background = TechHudDrawable(
            fillColor = Color.argb(
                190,
                Color.red(selectorColor),
                Color.green(selectorColor),
                Color.blue(selectorColor)
            ),
            strokeColor = Color.argb(
                230,
                Color.red(selectorColor),
                Color.green(selectorColor),
                Color.blue(selectorColor)
            ),
            strokeWidth = 3f,
            cornerCut = 26f
        )
    }

    fun stylePanel(panel: TextView, colors: AppThemeColors) {
        panel.background = panelBackground(colors)
    }

    fun panelBackground(colors: AppThemeColors): Drawable {
        return TechHudDrawable(
            fillColor = colors.panelBackground,
            strokeColor = colors.outline,
            strokeWidth = 2.5f,
            cornerCut = PANEL_CORNER_CUT,
            drawCornerAccents = true
        )
    }

    fun stylePointer(pointer: TextView, colors: AppThemeColors) {
        pointer.setTextColor(colors.accent)
        pointer.setShadowLayer(18f, 0f, 0f, colors.accentSoft)
    }

    fun styleIndicator(dot: TextView, colors: AppThemeColors, active: Boolean) {
        dot.setTextColor(if (active) colors.accent else Color.argb(105, 150, 160, 170))
        dot.setShadowLayer(if (active) 8f else 0f, 0f, 0f, colors.accentSoft)
    }

    private class TechHudDrawable(
        private val fillColor: Int,
        private val strokeColor: Int,
        private val strokeWidth: Float,
        private val cornerCut: Float,
        private val drawCornerAccents: Boolean = false
    ) : Drawable() {
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = fillColor
        }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = strokeColor
            this.strokeWidth = this@TechHudDrawable.strokeWidth
        }
        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = strokeColor
            alpha = 45
            this.strokeWidth = this@TechHudDrawable.strokeWidth + 2f
        }
        private val path = Path()

        override fun draw(canvas: Canvas) {
            val bounds = bounds
            buildPath(bounds)
            canvas.drawPath(path, fillPaint)
            canvas.drawPath(path, glowPaint)
            canvas.drawPath(path, strokePaint)

            if (drawCornerAccents) {
                drawAccents(canvas, bounds)
            }
        }

        private fun buildPath(bounds: Rect) {
            val left = bounds.left + strokeWidth
            val top = bounds.top + strokeWidth
            val right = bounds.right - strokeWidth
            val bottom = bounds.bottom - strokeWidth
            val cut = cornerCut.coerceAtMost((right - left) / 4f).coerceAtMost((bottom - top) / 4f)

            path.reset()
            path.moveTo(left + cut, top)
            path.lineTo(right - cut, top)
            path.lineTo(right, top + cut)
            path.lineTo(right, bottom - cut)
            path.lineTo(right - cut, bottom)
            path.lineTo(left + cut, bottom)
            path.lineTo(left, bottom - cut)
            path.lineTo(left, top + cut)
            path.close()
        }

        private fun drawAccents(canvas: Canvas, bounds: Rect) {
            val left = bounds.left + strokeWidth + 12f
            val top = bounds.top + strokeWidth + 12f
            val right = bounds.right - strokeWidth - 12f
            val bottom = bounds.bottom - strokeWidth - 12f
            val accent = 28f

            canvas.drawLine(left, top + accent, left, top, strokePaint)
            canvas.drawLine(left, top, left + accent, top, strokePaint)
            canvas.drawLine(right - accent, top, right, top, strokePaint)
            canvas.drawLine(right, top, right, top + accent, strokePaint)
            canvas.drawLine(left, bottom - accent, left, bottom, strokePaint)
            canvas.drawLine(left, bottom, left + accent, bottom, strokePaint)
            canvas.drawLine(right - accent, bottom, right, bottom, strokePaint)
            canvas.drawLine(right, bottom, right, bottom - accent, strokePaint)
        }

        override fun setAlpha(alpha: Int) {
            fillPaint.alpha = alpha
            strokePaint.alpha = alpha
            glowPaint.alpha = (alpha * 0.35f).toInt()
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            fillPaint.colorFilter = colorFilter
            strokePaint.colorFilter = colorFilter
            glowPaint.colorFilter = colorFilter
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    private class SoftHudDrawable(
        private val fillColor: Int
    ) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = fillColor
        }

        override fun draw(canvas: Canvas) {
            val left = bounds.left.toFloat()
            val top = bounds.top.toFloat()
            val right = bounds.right.toFloat()
            val bottom = bounds.bottom.toFloat()
            val radius = (bottom - top) / 4f
            canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint)
        }

        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            paint.colorFilter = colorFilter
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    private class SwitchHudDrawable(
        private val fillColor: Int,
        private val strokeColor: Int,
        private val knobColor: Int,
        private val knobOnRight: Boolean
    ) : Drawable() {
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = fillColor
        }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = strokeColor
            strokeWidth = 3f
        }
        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = strokeColor
            alpha = 85
            strokeWidth = 8f
        }
        private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = knobColor
        }

        override fun draw(canvas: Canvas) {
            val left = bounds.left + 3f
            val top = bounds.top + 3f
            val right = bounds.right - 3f
            val bottom = bounds.bottom - 3f
            val radius = (bottom - top) / 2f

            canvas.drawRoundRect(left, top, right, bottom, radius, radius, fillPaint)
            canvas.drawRoundRect(left, top, right, bottom, radius, radius, glowPaint)
            canvas.drawRoundRect(left, top, right, bottom, radius, radius, strokePaint)

            val knobRadius = radius * 0.58f
            val knobX = if (knobOnRight) {
                right - radius
            } else {
                left + radius
            }
            val knobY = (top + bottom) / 2f
            canvas.drawCircle(knobX, knobY, knobRadius, knobPaint)
        }

        override fun setAlpha(alpha: Int) {
            fillPaint.alpha = alpha
            strokePaint.alpha = alpha
            glowPaint.alpha = (alpha * 0.35f).toInt()
            knobPaint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            fillPaint.colorFilter = colorFilter
            strokePaint.colorFilter = colorFilter
            glowPaint.colorFilter = colorFilter
            knobPaint.colorFilter = colorFilter
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}
