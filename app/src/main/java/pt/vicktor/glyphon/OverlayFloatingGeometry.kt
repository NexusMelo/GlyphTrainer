package pt.vicktor.glyphon

internal object OverlayFloatingGeometry {

    const val BUTTON_SIZE = 256
    const val CONTENT_BUTTON_SIZE = 224
    const val BUTTON_MARGIN = 24
    const val BUTTON_TOP = 180
    const val MODE_WIDTH = 256
    const val MODE_HEIGHT = 95
    const val SECONDARY_WIDTH = 256
    const val SECONDARY_HEIGHT = 95
    const val MODE_CONTENT_HEIGHT = SECONDARY_HEIGHT
    const val MODE_GAP = 16
    const val CONFIG_GAP = 16
    const val CLOSE_SIZE = 96
    const val CLOSE_OVERLAP = 72

    private const val MODE_MANUAL_WIDTH = SECONDARY_WIDTH
    private const val MODE_AUTO_WIDTH = SECONDARY_WIDTH
    private const val CONFIG_FLYOUT_STEP_X = 200
    private const val CONFIG_FLYOUT_STEP_Y = 64
    private const val CONFIG_SUBMENU_VERTICAL_GAP = 16

    data class Position(val x: Int, val y: Int)

    data class Layout(
        val button: Position,
        val close: Position,
        val mode: Position,
        val modeWidth: Int,
        val config: Position,
        val theme: Position,
        val opacity: Position,
        val show: Position
    )

    fun layout(
        groupX: Int,
        groupY: Int,
        autoCaptureEnabled: Boolean,
        screenWidth: Int
    ): Layout {
        val modeWidth = if (autoCaptureEnabled) MODE_AUTO_WIDTH else MODE_MANUAL_WIDTH
        val configX = groupX + (MODE_WIDTH - SECONDARY_WIDTH) / 2
        val configY = groupY + BUTTON_SIZE + MODE_GAP + MODE_HEIGHT + CONFIG_GAP
        val themeX = configX - configFlyoutStepX(screenWidth)
        val themeY = configY + CONFIG_FLYOUT_STEP_Y
        val opacityY = themeY + SECONDARY_HEIGHT + CONFIG_SUBMENU_VERTICAL_GAP
        val showY = opacityY + SECONDARY_HEIGHT + CONFIG_SUBMENU_VERTICAL_GAP

        return Layout(
            button = Position(
                groupX + (MODE_WIDTH - CONTENT_BUTTON_SIZE) / 2,
                groupY + (BUTTON_SIZE - CONTENT_BUTTON_SIZE) / 2
            ),
            close = Position(groupX, groupY - CLOSE_OVERLAP),
            mode = Position(
                groupX + (MODE_WIDTH - modeWidth) / 2,
                groupY + BUTTON_SIZE + MODE_GAP +
                        (MODE_HEIGHT - MODE_CONTENT_HEIGHT) / 2
            ),
            modeWidth = modeWidth,
            config = Position(configX, configY),
            theme = Position(themeX, themeY),
            opacity = Position(themeX, opacityY),
            show = Position(themeX, showY)
        )
    }

    fun constrainGroupPosition(
        requestedX: Int,
        requestedY: Int,
        screenWidth: Int,
        screenHeight: Int,
        topInset: Int,
        bottomInset: Int,
        configExpanded: Boolean
    ): Position {
        val maxX = (screenWidth - MODE_WIDTH).coerceAtLeast(0)
        val minX = if (configExpanded) configFlyoutStepX(screenWidth) else 0
        val minY = topInset + CLOSE_OVERLAP
        val maxY = (screenHeight - bottomInset - groupHeight(configExpanded, screenWidth))
            .coerceAtLeast(minY)

        return Position(
            requestedX.coerceIn(minX, maxX),
            requestedY.coerceIn(minY, maxY)
        )
    }

    fun defaultGroupPosition(
        screenWidth: Int,
        screenHeight: Int,
        topInset: Int,
        bottomInset: Int
    ): Position {
        val x = (screenWidth - MODE_WIDTH - BUTTON_MARGIN).coerceAtLeast(0)
        val availableHeight = (screenHeight - topInset - bottomInset).coerceAtLeast(0)
        val visibleGroupHeight = CLOSE_OVERLAP + collapsedGroupHeight()
        val visibleTop = topInset + ((availableHeight - visibleGroupHeight) / 2).coerceAtLeast(0)

        return Position(x, visibleTop + CLOSE_OVERLAP)
    }

    private fun configFlyoutStepX(screenWidth: Int): Int {
        val availableOffset = (screenWidth - MODE_WIDTH).coerceAtLeast(0)
        return CONFIG_FLYOUT_STEP_X.coerceAtMost(availableOffset)
    }

    private fun collapsedGroupHeight(): Int {
        return BUTTON_SIZE + MODE_GAP + MODE_HEIGHT + CONFIG_GAP + SECONDARY_HEIGHT
    }

    private fun groupHeight(configExpanded: Boolean, screenWidth: Int): Int {
        if (!configExpanded) return collapsedGroupHeight()

        val expandedLayout = layout(
            groupX = 0,
            groupY = 0,
            autoCaptureEnabled = false,
            screenWidth = screenWidth
        )
        return expandedLayout.show.y + SECONDARY_HEIGHT
    }
}
