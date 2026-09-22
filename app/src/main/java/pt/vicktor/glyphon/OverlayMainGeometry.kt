package pt.vicktor.glyphon

internal object OverlayMainGeometry {
    const val MAX_PREVIEW_SLOTS = 5
    const val MAIN_CONTROL_COUNT = 5
    const val MAIN_CONTROL_SIZE = 96
    const val MAIN_CONTROL_GAP = 24

    private const val GLYPH_SIZE_DIVISOR = 6f
    private const val PREVIEW_BASE_Y = 120f
    private const val PREVIEW_VERTICAL_OFFSET = 60f
    private const val PREVIEW_TOP_GLYPH_FACTOR = 0.55f
    private const val PREVIEW_SPACING_FACTOR = 1.2f
    private const val FIVE_PREVIEW_START_X = 40f
    private const val PREVIEW_TO_CONTROLS_GAP = 7f
    private const val CONTROLS_TO_CAPTURE_GAP = 10.4f
    private const val CAPTURE_HORIZONTAL_MARGIN = 60f
    private const val CAPTURE_HEIGHT_FRACTION = 0.48f
    private const val CAPTURE_EXTRA_HEIGHT = 120f
    private const val CAPTURE_CONTENT_INSET = 60f
    private const val INDICATOR_BOTTOM_GAP = 70f

    data class Bounds(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    )

    data class Layout(
        val controlsTop: Int,
        val controlsBottom: Int,
        val capture: Bounds,
        val content: Bounds,
        val indicatorY: Float
    )

    fun calculate(width: Int, layoutHeight: Int): Layout {
        val controlsTop = (previewBottom(width) + PREVIEW_TO_CONTROLS_GAP).toInt()
        val controlsBottom = controlsTop + MAIN_CONTROL_SIZE
        val captureTop = controlsBottom + CONTROLS_TO_CAPTURE_GAP
        val captureHeight = layoutHeight * CAPTURE_HEIGHT_FRACTION + CAPTURE_EXTRA_HEIGHT
        val captureBottom = captureTop + captureHeight
        val capture = Bounds(
            left = CAPTURE_HORIZONTAL_MARGIN,
            top = captureTop,
            right = width - CAPTURE_HORIZONTAL_MARGIN,
            bottom = captureBottom
        )
        val content = Bounds(
            left = capture.left + CAPTURE_CONTENT_INSET,
            top = capture.top + CAPTURE_CONTENT_INSET,
            right = capture.right - CAPTURE_CONTENT_INSET,
            bottom = capture.bottom - CAPTURE_CONTENT_INSET
        )

        return Layout(
            controlsTop = controlsTop,
            controlsBottom = controlsBottom,
            capture = capture,
            content = content,
            indicatorY = capture.bottom + INDICATOR_BOTTOM_GAP
        )
    }

    fun previewBounds(width: Int, slotCount: Int, slot: Int): Bounds {
        val glyphSize = glyphSize(width)
        val spacing = glyphSize * PREVIEW_SPACING_FACTOR
        val groupWidth = glyphSize + spacing * (slotCount - 1)
        val startX = if (slotCount == MAX_PREVIEW_SLOTS) {
            FIVE_PREVIEW_START_X
        } else {
            (width - groupWidth) / 2f
        }
        val left = startX + slot * spacing
        val top = previewTop(width)
        return Bounds(left, top, left + glyphSize, top + glyphSize)
    }

    private fun glyphSize(width: Int) = width / GLYPH_SIZE_DIVISOR

    private fun previewTop(width: Int): Float {
        return PREVIEW_BASE_Y + PREVIEW_VERTICAL_OFFSET +
            glyphSize(width) * PREVIEW_TOP_GLYPH_FACTOR
    }

    private fun previewBottom(width: Int): Float {
        return previewTop(width) + glyphSize(width)
    }
}
