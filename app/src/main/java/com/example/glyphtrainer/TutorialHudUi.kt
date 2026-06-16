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
            textSize = 14f
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
            textSize = 34f
            gravity = Gravity.CENTER
            includeFontPadding = false
        }
    }

    fun styleLabel(label: TextView, colors: AppThemeColors) {
        label.setTextColor(colors.text)
        label.background = TechHudDrawable(
            fillColor = Color.argb(95, 0, 0, 0),
            strokeColor = colors.accentSoft,
            strokeWidth = 1.5f,
            cornerCut = BUTTON_CORNER_CUT
        )
    }

    fun styleButton(button: TextView, colors: AppThemeColors) {
        button.setTextColor(colors.text)
        button.background = TechHudDrawable(
            fillColor = colors.buttonBackground,
            strokeColor = colors.outline,
            strokeWidth = 3f,
            cornerCut = BUTTON_CORNER_CUT
        )
    }

    fun stylePanel(panel: TextView, colors: AppThemeColors) {
        panel.background = panelBackground(colors)
    }

    fun panelBackground(colors: AppThemeColors): Drawable {
        return TechHudDrawable(
            fillColor = colors.panelBackground,
            strokeColor = colors.outline,
            strokeWidth = 4f,
            cornerCut = PANEL_CORNER_CUT,
            drawCornerAccents = true
        )
    }

    fun stylePointer(pointer: TextView, colors: AppThemeColors) {
        pointer.setTextColor(colors.accent)
        pointer.setShadowLayer(18f, 0f, 0f, colors.accentSoft)
    }

    fun styleIndicator(dot: TextView, colors: AppThemeColors, active: Boolean) {
        dot.setTextColor(if (active) colors.accent else Color.argb(155, 150, 160, 170))
        dot.setShadowLayer(if (active) 18f else 0f, 0f, 0f, colors.accentSoft)
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
            alpha = 90
            this.strokeWidth = this@TechHudDrawable.strokeWidth + 5f
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
            val accent = 44f

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
}
