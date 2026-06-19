package com

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.Service
import android.content.res.ColorStateList
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.view.Gravity
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.example.glyphtrainer.AppColorTheme
import com.example.glyphtrainer.AppThemeConfig
import com.example.glyphtrainer.AppThemeColors
import com.example.glyphtrainer.AppMode
import com.example.glyphtrainer.DrawView
import com.example.glyphtrainer.R
import com.example.glyphtrainer.TutorialHudUi
import kotlin.math.abs

class OverlayService : Service(),


    DrawView.OverlayListener {

    private companion object {
        const val PREFERENCES_NAME = "glyph_trainer_state"
        const val LOG_TAG = "GlyphTrainerOverlay"
        const val PREF_GLYPH_LIMIT = "glyph_limit"
        const val PREF_HORIZONTAL_SCALE = "horizontal_scale"
        const val PREF_VERTICAL_SCALE = "vertical_scale"
        const val PREF_AUTO_CAPTURE = "auto_capture"
        const val PREF_FLOATING_GROUP_X = "floating_group_x"
        const val PREF_FLOATING_GROUP_Y = "floating_group_y"
        const val PREF_OVERLAY_MINIMIZED = "overlay_minimized"
        const val PREF_OVERLAY_OPACITY_PERCENT = "overlay_opacity_percent"
        const val PREF_TUTORIAL_INITIALIZED = "tutorial_initialized"
        const val PREF_SHOW_TUTORIAL_ON_LAUNCH = "show_tutorial_on_launch"
        const val PREF_PREMIUM_ENABLED = "premium_enabled"
        const val PREF_AUTO_CAPTURE_USES = "auto_capture_uses"
        const val PREF_FLOATING_DRAG_USES = "floating_drag_uses"
        const val TUTORIAL_ENABLED = false
        const val DEFAULT_GLYPH_LIMIT = 5
        const val DEFAULT_SCALE = 1f
        const val DEFAULT_AUTO_CAPTURE = false
        const val DEFAULT_OVERLAY_MINIMIZED = false
        const val DEFAULT_OVERLAY_OPACITY_PERCENT = 100
        const val DEFAULT_SHOW_TUTORIAL_ON_LAUNCH = false
        const val DEFAULT_PREMIUM_ENABLED = false
        const val FLOATING_BUTTON_SIZE = 256
        const val FLOATING_CONTENT_BUTTON_SIZE = 224
        const val FLOATING_BUTTON_MARGIN = 24
        const val FLOATING_BUTTON_TOP = 180
        const val FLOATING_MODE_WIDTH = 256
        const val FLOATING_MODE_HEIGHT = 95
        const val FLOATING_SECONDARY_WIDTH = 256
        const val FLOATING_SECONDARY_HEIGHT = 95
        const val FLOATING_MODE_MANUAL_WIDTH = FLOATING_SECONDARY_WIDTH
        const val FLOATING_MODE_AUTO_WIDTH = FLOATING_SECONDARY_WIDTH
        const val FLOATING_MODE_CONTENT_HEIGHT = FLOATING_SECONDARY_HEIGHT
        const val FLOATING_MODE_GAP = 16
        const val FLOATING_SKIN_GAP = 16
        const val FLOATING_CLOSE_SIZE = 96
        const val FLOATING_CLOSE_OVERLAP = 72
        const val CAPTURE_START_DELAY_MS = 140L
        const val GLYPH_DISPLAY_DELAY_MS = 2_000L
        const val REPLAY_START_DELAY_MS = 1_000L
        const val REPLAY_GLYPH_DURATION_MS = 1_750L
        const val REPLAY_GLYPH_GAP_MS = 0L
        const val REPLAY_PREPARE_DELAY_MS = GLYPH_DISPLAY_DELAY_MS - REPLAY_START_DELAY_MS
        const val TUTORIAL_BUTTON_HEIGHT = 64
        const val TUTORIAL_BUTTON_MARGIN = 24
        const val TUTORIAL_CONTROL_WIDTH = 160
        const val THEME_CONTROL_WIDTH = 188
        const val THEME_CONTENT_WIDTH = FLOATING_SECONDARY_WIDTH
        const val THEME_CONTENT_HEIGHT = FLOATING_SECONDARY_HEIGHT
        const val OPACITY_CONTENT_WIDTH = FLOATING_SECONDARY_WIDTH
        const val OPACITY_CONTENT_HEIGHT = FLOATING_SECONDARY_HEIGHT
        const val FLOATING_OPACITY_GAP = 16
        const val FLOATING_GROUP_HEIGHT =
            FLOATING_BUTTON_SIZE + FLOATING_MODE_GAP + FLOATING_MODE_HEIGHT +
                    FLOATING_SKIN_GAP + THEME_CONTENT_HEIGHT +
                    FLOATING_OPACITY_GAP + OPACITY_CONTENT_HEIGHT
        const val TOP_CONTROL_GAP = 24
        const val TUTORIAL_CARD_WIDTH = 560
        const val TUTORIAL_CARD_HEIGHT = 300
        const val TUTORIAL_CARD_MARGIN = 24
        const val TUTORIAL_POINTER_SIZE = 44
    }

    private lateinit var wm: WindowManager
    private lateinit var drawView: DrawView


    private lateinit var closeBtn: TextView
    private lateinit var startBtn: TextView
    private lateinit var modeBtn: TextView
    private lateinit var resetBtn: TextView
    private lateinit var floatingBtn: TextView
    private lateinit var floatingModeBtn: TextView
    private lateinit var floatingCloseBtn: TextView
    private lateinit var tutorialToggleBtn: TextView
    private lateinit var themeBtn: TextView
    private lateinit var opacityBtn: TextView
    private lateinit var tutorialLayer: FrameLayout
    private lateinit var tutorialPointer: TextView
    private lateinit var tutorialCard: LinearLayout
    private lateinit var tutorialCloseBtn: TextView
    private lateinit var tutorialBody: TextView
    private lateinit var tutorialBackBtn: TextView
    private lateinit var tutorialNextBtn: TextView
    private val tutorialIndicators = mutableListOf<TextView>()
    private lateinit var zoomHXPlus: TextView
    private lateinit var zoomHXMinus: TextView
    private lateinit var zoomVPlus: TextView
    private lateinit var zoomVMinus: TextView

    private lateinit var zoomHXPlusParams: WindowManager.LayoutParams
    private lateinit var zoomHXMinusParams: WindowManager.LayoutParams
    private lateinit var zoomVPlusParams: WindowManager.LayoutParams
    private lateinit var zoomVMinusParams: WindowManager.LayoutParams

    private lateinit var drawParams: WindowManager.LayoutParams
    private lateinit var closeParams: WindowManager.LayoutParams
    private lateinit var startParams: WindowManager.LayoutParams
    private lateinit var modeParams: WindowManager.LayoutParams
    private lateinit var resetParams: WindowManager.LayoutParams
    private lateinit var floatingParams: WindowManager.LayoutParams
    private lateinit var floatingModeParams: WindowManager.LayoutParams
    private lateinit var floatingCloseParams: WindowManager.LayoutParams
    private lateinit var tutorialToggleParams: WindowManager.LayoutParams
    private lateinit var themeParams: WindowManager.LayoutParams
    private lateinit var opacityParams: WindowManager.LayoutParams
    private lateinit var tutorialLayerParams: WindowManager.LayoutParams

    private val drawArea = RectF()
    private val buttonSize = 96
    private val gap = 24
    private val floatingTouchSlop by lazy {
        ViewConfiguration.get(this).scaledTouchSlop
    }
    private var fixedControlsY: Int? = null
    private var floatingGroupX = FLOATING_BUTTON_MARGIN
    private var floatingGroupY = FLOATING_BUTTON_TOP
    private var floatingDragStartRawX = 0f
    private var floatingDragStartRawY = 0f
    private var floatingDragStartGroupX = 0
    private var floatingDragStartGroupY = 0
    private var floatingDragging = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val startCaptureRunnable = Runnable {
        if (capturing && canUseOverlay() && isPlayMode() && isOverlayReady()) {
            drawView.startCapture()
        } else {
            disableCapture()
        }
    }
    private val showGlyphSequenceRunnable = Runnable {
        if (!capturing && canUseOverlay() && isPlayMode() && isOverlayReady()) {
            drawView.showCompletedSequence()
            startReplay()
        }
    }
    private val replayStepRunnable = object : Runnable {
        override fun run() {
            if (capturing || !canUseOverlay() || !isPlayMode() || !isOverlayReady()) {
                cancelReplay()
                return
            }

            if (replayGlyphVisible) {
                drawView.clearReplayGlyph()
                replayGlyphVisible = false

                if (replayIndex >= glyphLimit) {
                    minimizeOverlay()
                    return
                }

                mainHandler.postDelayed(this, REPLAY_GLYPH_GAP_MS)
                return
            }

            if (replayIndex >= glyphLimit) return

            drawView.showReplayGlyph(replayIndex)
            replayIndex++
            replayGlyphVisible = true
            mainHandler.postDelayed(this, REPLAY_GLYPH_DURATION_MS)
        }
    }
    private val overlayPermissionListener = AppOpsManager.OnOpChangedListener { _, packageName ->
        if (packageName == this.packageName && !canUseOverlay()) {
            mainHandler.post { stopSelf() }
        }
    }

    private var glyphLimit = DEFAULT_GLYPH_LIMIT
    private var horizontalScale = DEFAULT_SCALE
    private var verticalScale = DEFAULT_SCALE
    private var autoCaptureEnabled = DEFAULT_AUTO_CAPTURE
    private var showTutorialOnLaunch = DEFAULT_SHOW_TUTORIAL_ON_LAUNCH
    private var currentColorTheme = AppThemeConfig.DEFAULT_THEME
    private var overlayOpacityPercent = DEFAULT_OVERLAY_OPACITY_PERCENT
    private var firstLaunchTutorialPending = false
    private var capturing = false
    private var replayIndex = 0
    private var replayGlyphVisible = false
    private var tutorialStepIndex = 0
    private var overlayMinimized = false
    private var restoreMinimizedOnCreate = DEFAULT_OVERLAY_MINIMIZED
    private var creationFailed = false
    private var permissionListenerRegistered = false

    private data class TutorialStep(
        val bodyRes: Int,
        val target: TutorialTarget
    )

    private enum class TutorialTarget {
        CAPTURE_AREA,
        MODE_BUTTON,
        START_BUTTON,
        RESET_BUTTON,
        CLOSE_BUTTON,
        GO_MESSAGE,
        FLOATING_BUTTON,
        FLOATING_MODE
    }

    private val tutorialSteps = listOf(
        TutorialStep(
            R.string.tutorial_step_1_body,
            TutorialTarget.CAPTURE_AREA
        ),
        TutorialStep(
            R.string.tutorial_step_2_body,
            TutorialTarget.MODE_BUTTON
        ),
        TutorialStep(
            R.string.tutorial_step_3_body,
            TutorialTarget.START_BUTTON
        ),
        TutorialStep(
            R.string.tutorial_step_4_body,
            TutorialTarget.RESET_BUTTON
        ),
        TutorialStep(
            R.string.tutorial_step_5_body,
            TutorialTarget.CLOSE_BUTTON
        ),
        TutorialStep(
            R.string.tutorial_step_6_body,
            TutorialTarget.GO_MESSAGE
        ),
        TutorialStep(
            R.string.tutorial_step_7_body,
            TutorialTarget.FLOATING_BUTTON
        ),
        TutorialStep(
            R.string.tutorial_step_8_body,
            TutorialTarget.FLOATING_MODE
        )
    )

    override fun onBind(intent: Intent?): IBinder? = null

    // =====================================================
    // CREATE
    // =====================================================

    override fun onCreate() {
        super.onCreate()

        if (!canUseOverlay()) {
            stopSelf()
            return
        }

        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        restorePreferences()

        createDrawLayer()
        if (creationFailed) return

        createButtons()
        if (creationFailed) return

        createFloatingControls()
        if (creationFailed) return

        createThemeControl()
        if (creationFailed) return

        createOpacityControl()
        if (creationFailed) return

        if (TUTORIAL_ENABLED) {
            createTutorialControls()
            if (creationFailed) return
        }

        disableCapture()
        updateStartButton(false)
        updateModeButton()
        updateProgramButtons()
        updateTutorialToggleButton()
        applyOverlayOpacity()
        registerOverlayPermissionListener()
        applyRestoredOverlayState()

        if (TUTORIAL_ENABLED && (firstLaunchTutorialPending || showTutorialOnLaunch)) {
            mainHandler.postDelayed({ showTutorial() }, 250L)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!canUseOverlay() || creationFailed) {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    // =====================================================
    // DRAW LAYER
    // =====================================================

    private fun createDrawLayer(){

        drawView = DrawView(this, this)
        drawView.setGlyphLimit(glyphLimit)
        drawView.setGlyphScales(horizontalScale, verticalScale)
        drawView.setAppColorTheme(currentColorTheme)

        drawParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // começa sem capturar toques
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )

        addOverlayView(drawView, drawParams)
    }

    // =====================================================
    // BUTTONS
    // =====================================================
    private fun makeMenuButton(@StringRes textRes: Int, action:()->Unit): TextView {

        val v = TextView(this)
        v.setText(textRes)
        v.setTextColor(Color.WHITE)
        v.textSize = 14f
        v.setPadding(30,20,30,20)
        v.setBackgroundColor(0xAA000000.toInt())
        v.gravity = Gravity.CENTER
        v.setOnClickListener { action() }
        v.visibility = TextView.GONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        addOverlayView(v, params)

        when {
            !::zoomHXPlusParams.isInitialized -> zoomHXPlusParams = params
            !::zoomHXMinusParams.isInitialized -> zoomHXMinusParams = params
            !::zoomVPlusParams.isInitialized -> zoomVPlusParams = params
            else -> zoomVMinusParams = params
        }

        return v
    }

    private fun createButtons(){

        closeBtn = makeIconButton(R.drawable.ic_close, Color.RED){ stopSelf() }
        applyReferenceButtonBackground(closeBtn, R.drawable.btn_close_reference)

        startBtn = makeIconButton(R.drawable.ic_play, Color.WHITE){
            if (enableCapture()) {
                updateStartButton(true)
            }
        }
        applyReferencePlayButton(false)

        modeBtn = makeIconButton(glyphLimitIcon(), Color.CYAN){
            cancelSequencePresentation()
            disableCapture()

            glyphLimit = if (glyphLimit == 5) 3 else glyphLimit + 1
            saveGlyphLimit()

            drawView.setGlyphLimit(glyphLimit)

            updateStartButton(false)
            updateModeButton()
        }
        updateModeButton()

        resetBtn = makeIconButton(R.drawable.ic_reset, Color.YELLOW){
            cancelSequencePresentation()
            disableCapture()
            drawView.resetGlyphs()
            val active = enableCapture()
            updateStartButton(active)
        }
        applyReferenceButtonBackground(resetBtn, R.drawable.btn_reset_reference)
        zoomHXPlus = makeMenuButton(R.string.adjust_horizontal_increase) {
            horizontalScale = drawView.adjustHorizontal(1f)
            saveGlyphScales()
        }

        zoomHXMinus = makeMenuButton(R.string.adjust_horizontal_decrease) {
            horizontalScale = drawView.adjustHorizontal(-1f)
            saveGlyphScales()
        }

        zoomVPlus = makeMenuButton(R.string.adjust_vertical_increase) {
            verticalScale = drawView.adjustVertical(1f)
            saveGlyphScales()
        }

        zoomVMinus = makeMenuButton(R.string.adjust_vertical_decrease) {
            verticalScale = drawView.adjustVertical(-1f)
            saveGlyphScales()
        }

        zoomVMinus.post {
            if (!drawArea.isEmpty) {
                positionOverlayControls(drawArea)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingControls() {
        floatingBtn = TextView(this).apply {
            styleFloatingRoundButton(
                textSize = 42f,
                elevationValue = 8f,
                fillColor = Color.argb(210, 0, 75, 95),
                strokeWidth = 4,
                strokeColor = Color.CYAN
            )
            visibility = View.GONE
            setOnClickListener { restoreOverlay(autoCaptureEnabled) }
            setOnTouchListener { view, event -> handleFloatingDrag(view, event) }
        }

        floatingParams = WindowManager.LayoutParams(
            FLOATING_CONTENT_BUTTON_SIZE,
            FLOATING_CONTENT_BUTTON_SIZE,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = floatingButtonX()
            y = floatingButtonY()
        }

        addOverlayView(floatingBtn, floatingParams)

        floatingCloseBtn = TextView(this).apply {
            styleFloatingRoundButton(
                textSize = 24f,
                elevationValue = 10f,
                fillColor = Color.argb(220, 150, 20, 20),
                strokeWidth = 3,
                strokeColor = Color.RED
            )
            applyReferenceButtonBackground(this, R.drawable.btn_floating_close_reference)
            visibility = View.GONE
            setOnClickListener { stopSelf() }
            setOnTouchListener { view, event -> handleFloatingDrag(view, event) }
        }

        floatingCloseParams = WindowManager.LayoutParams(
            FLOATING_CLOSE_SIZE,
            FLOATING_CLOSE_SIZE,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = floatingGroupX
            y = floatingGroupY - FLOATING_CLOSE_OVERLAP
        }

        addOverlayView(floatingCloseBtn, floatingCloseParams)

        floatingModeBtn = TextView(this).apply {
            styleFloatingPillText()
            visibility = View.GONE
            setOnClickListener {
                autoCaptureEnabled = !autoCaptureEnabled
                saveAutoCaptureMode()
                updateFloatingModeButton()
            }
            setOnTouchListener { view, event -> handleFloatingDrag(view, event) }
        }

        floatingModeParams = WindowManager.LayoutParams(
            floatingModeWidth(),
            FLOATING_MODE_CONTENT_HEIGHT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = floatingModeX()
            y = floatingModeY()
        }

        addOverlayView(floatingModeBtn, floatingModeParams)
        applyFloatingGroupPosition(floatingGroupX, floatingGroupY)
        updateFloatingButton()
        updateFloatingModeButton()
    }

    private fun TextView.styleFloatingRoundButton(
        textSize: Float,
        elevationValue: Float,
        fillColor: Int,
        strokeWidth: Int,
        strokeColor: Int
    ) {
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

    private fun TextView.styleFloatingPillText() {
        textSize = 15f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        includeFontPadding = false
    }

    private fun floatingButtonX(): Int {
        return floatingGroupX + (FLOATING_MODE_WIDTH - FLOATING_CONTENT_BUTTON_SIZE) / 2
    }

    private fun floatingButtonY(): Int {
        return floatingGroupY + (FLOATING_BUTTON_SIZE - FLOATING_CONTENT_BUTTON_SIZE) / 2
    }

    private fun floatingModeWidth(): Int {
        return if (autoCaptureEnabled) FLOATING_MODE_AUTO_WIDTH else FLOATING_MODE_MANUAL_WIDTH
    }

    private fun floatingModeX(): Int {
        return floatingGroupX + (FLOATING_MODE_WIDTH - floatingModeWidth()) / 2
    }

    private fun floatingModeY(): Int {
        return floatingGroupY + FLOATING_BUTTON_SIZE + FLOATING_MODE_GAP +
                (FLOATING_MODE_HEIGHT - FLOATING_MODE_CONTENT_HEIGHT) / 2
    }

    private fun floatingSkinX(): Int {
        return floatingGroupX + (FLOATING_MODE_WIDTH - THEME_CONTENT_WIDTH) / 2
    }

    private fun floatingSkinY(): Int {
        return floatingGroupY + FLOATING_BUTTON_SIZE + FLOATING_MODE_GAP +
                FLOATING_MODE_HEIGHT + FLOATING_SKIN_GAP
    }

    private fun floatingOpacityX(): Int {
        return floatingGroupX + (FLOATING_MODE_WIDTH - OPACITY_CONTENT_WIDTH) / 2
    }

    private fun floatingOpacityY(): Int {
        return floatingSkinY() + THEME_CONTENT_HEIGHT + FLOATING_OPACITY_GAP
    }

    private fun createTutorialControls() {
        tutorialToggleBtn = TutorialHudUi.makeControlButton(this).apply {
            setOnClickListener {
                showTutorialOnLaunch = !showTutorialOnLaunch
                saveTutorialLaunchPreference()
                updateTutorialToggleButton()
                if (showTutorialOnLaunch) {
                    showTutorial()
                } else {
                    hideTutorial()
                }
            }
        }
        tutorialToggleParams = createHudControlParams(
            TUTORIAL_CONTROL_WIDTH,
            TUTORIAL_BUTTON_HEIGHT,
            TUTORIAL_BUTTON_MARGIN,
            TUTORIAL_BUTTON_MARGIN
        )

        tutorialLayer = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(80, 0, 0, 0))
            visibility = View.GONE
            isClickable = true
        }

        tutorialLayerParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        tutorialPointer = TextView(this).apply {
            textSize = 34f
            setTextColor(Color.YELLOW)
            gravity = Gravity.CENTER
            includeFontPadding = false
        }
        tutorialLayer.addView(
            tutorialPointer,
            FrameLayout.LayoutParams(TUTORIAL_POINTER_SIZE, TUTORIAL_POINTER_SIZE)
        )

        tutorialCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 22, 28, 24)
            background = GradientDrawable().apply {
                cornerRadius = 24f
                setColor(Color.argb(220, 15, 15, 15))
                setStroke(2, Color.argb(200, 255, 255, 255))
            }
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }

        tutorialCloseBtn = TutorialHudUi.makeTutorialButton(this, R.string.tutorial_close, 30f).apply {
            setOnClickListener { hideTutorial() }
        }
        header.addView(
            tutorialCloseBtn,
            LinearLayout.LayoutParams(68, 60)
        )
        tutorialCard.addView(
            header,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        tutorialBody = TextView(this).apply {
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            includeFontPadding = true
            setLineSpacing(4f, 1.0f)
            setPadding(0, 16, 0, 18)
        }
        tutorialCard.addView(
            tutorialBody,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        tutorialBackBtn = TutorialHudUi.makeTutorialButton(this, R.string.tutorial_back, 36f).apply {
            setOnClickListener {
                if (tutorialStepIndex > 0) {
                    showTutorial(tutorialStepIndex - 1)
                }
            }
        }
        nav.addView(tutorialBackBtn, LinearLayout.LayoutParams(118, 70))

        val indicators = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        tutorialIndicators.clear()
        repeat(tutorialSteps.size) {
            val dot = TutorialHudUi.makeIndicatorDot(this@OverlayService)
            tutorialIndicators.add(dot)
            indicators.addView(dot, LinearLayout.LayoutParams(28, 46))
        }
        nav.addView(indicators, LinearLayout.LayoutParams(0, 70, 1f))

        tutorialNextBtn = TutorialHudUi.makeTutorialButton(this, R.string.tutorial_next, 36f).apply {
            setOnClickListener {
                if (tutorialStepIndex < tutorialSteps.lastIndex) {
                    showTutorial(tutorialStepIndex + 1)
                }
            }
        }
        nav.addView(tutorialNextBtn, LinearLayout.LayoutParams(118, 70))

        tutorialCard.addView(
            nav,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        tutorialLayer.addView(
            tutorialCard,
            FrameLayout.LayoutParams(TUTORIAL_CARD_WIDTH, TUTORIAL_CARD_HEIGHT)
        )

        addOverlayView(tutorialLayer, tutorialLayerParams)
        addOverlayView(tutorialToggleBtn, tutorialToggleParams)

        applyCurrentTheme()
    }

    private fun createThemeControl() {
        themeBtn = TutorialHudUi.makeControlButton(this).apply {
            visibility = View.GONE
            setOnClickListener {
                currentColorTheme = AppThemeConfig.nextTheme(currentColorTheme)
                saveColorTheme()
                applyCurrentTheme()
            }
            setOnTouchListener { view, event -> handleFloatingDrag(view, event) }
        }
        themeParams = createThemeControlParams()
        addOverlayView(themeBtn, themeParams)

        applyCurrentTheme()
    }

    private fun createOpacityControl() {
        opacityBtn = TutorialHudUi.makeControlButton(this).apply {
            visibility = View.GONE
            setOnClickListener {
                overlayOpacityPercent = nextOverlayOpacityPercent()
                saveOverlayOpacity()
                applyOverlayOpacity()
                updateOpacityButton()
            }
            setOnTouchListener { view, event -> handleFloatingDrag(view, event) }
        }
        opacityParams = createOpacityControlParams()
        addOverlayView(opacityBtn, opacityParams)
        updateOpacityButton()
    }

    private fun createThemeControlParams(): WindowManager.LayoutParams {
        return createHudControlParams(
            THEME_CONTENT_WIDTH,
            THEME_CONTENT_HEIGHT,
            floatingSkinX(),
            floatingSkinY()
        ).apply {
            gravity = Gravity.TOP or Gravity.END
        }
    }

    private fun createOpacityControlParams(): WindowManager.LayoutParams {
        return createHudControlParams(
            OPACITY_CONTENT_WIDTH,
            OPACITY_CONTENT_HEIGHT,
            floatingOpacityX(),
            floatingOpacityY()
        ).apply {
            gravity = Gravity.TOP or Gravity.END
        }
    }

    private fun createHudControlParams(
        width: Int,
        height: Int,
        xPos: Int,
        yPos: Int,
        touchable: Boolean = true
    ): WindowManager.LayoutParams {
        val flags = if (touchable) {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }

        return WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = xPos
            y = yPos
        }
    }

    private fun handleFloatingDrag(view: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                floatingDragStartRawX = event.rawX
                floatingDragStartRawY = event.rawY
                floatingDragStartGroupX = floatingGroupX
                floatingDragStartGroupY = floatingGroupY
                floatingDragging = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - floatingDragStartRawX
                val deltaY = event.rawY - floatingDragStartRawY

                if (!floatingDragging &&
                    (abs(deltaX) > floatingTouchSlop || abs(deltaY) > floatingTouchSlop)
                ) {
                    floatingDragging = true
                }

                if (floatingDragging) {
                    applyFloatingGroupPosition(
                        floatingDragStartGroupX - deltaX.toInt(),
                        floatingDragStartGroupY + deltaY.toInt()
                    )
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (floatingDragging) {
                    saveFloatingGroupPosition()
                } else {
                    view.performClick()
                }
                floatingDragging = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                floatingDragging = false
                return true
            }
        }

        return false
    }

    private fun applyFloatingGroupPosition(requestedX: Int, requestedY: Int) {
        val screenWidth = drawView.width.takeIf { it > 0 }
            ?: resources.displayMetrics.widthPixels
        val screenHeight = drawView.height.takeIf { it > 0 }
            ?: resources.displayMetrics.heightPixels
        val topInset = getSystemBarSize("status_bar_height")
        val bottomInset = getSystemBarSize("navigation_bar_height")
        val maxX = (screenWidth - FLOATING_MODE_WIDTH).coerceAtLeast(0)
        val minY = topInset + FLOATING_CLOSE_OVERLAP
        val maxY = (screenHeight - bottomInset - FLOATING_GROUP_HEIGHT)
            .coerceAtLeast(minY)

        floatingGroupX = requestedX.coerceIn(0, maxX)
        floatingGroupY = requestedY.coerceIn(minY, maxY)

        if (::floatingParams.isInitialized) {
            floatingParams.x = floatingButtonX()
            floatingParams.y = floatingButtonY()
            updateOverlayView(floatingBtn, floatingParams)
        }
        if (::floatingCloseParams.isInitialized) {
            floatingCloseParams.x = floatingGroupX
            floatingCloseParams.y = floatingGroupY - FLOATING_CLOSE_OVERLAP
            updateOverlayView(floatingCloseBtn, floatingCloseParams)
        }
        if (::floatingModeParams.isInitialized) {
            floatingModeParams.width = floatingModeWidth()
            floatingModeParams.height = FLOATING_MODE_CONTENT_HEIGHT
            floatingModeParams.x = floatingModeX()
            floatingModeParams.y = floatingModeY()
            updateOverlayView(floatingModeBtn, floatingModeParams)
        }
        if (::themeParams.isInitialized) {
            themeParams.x = floatingSkinX()
            themeParams.y = floatingSkinY()
            updateOverlayView(themeBtn, themeParams)
        }
        if (::opacityParams.isInitialized) {
            opacityParams.x = floatingOpacityX()
            opacityParams.y = floatingOpacityY()
            updateOverlayView(opacityBtn, opacityParams)
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun getSystemBarSize(resourceName: String): Int {
        val resourceId = resources.getIdentifier(resourceName, "dimen", "android")
        return if (resourceId != 0) resources.getDimensionPixelSize(resourceId) else 0
    }


    private fun makeButton(
        @StringRes textRes: Int,
        textColor: Int,
        textSize: Float = 34f,
        action:()->Unit
    ): TextView {

        val v = TextView(this)
        v.setText(textRes)
        v.styleMainControlButton(textColor, textSize)
        v.setOnClickListener{ action() }

        val params = WindowManager.LayoutParams(
            buttonSize,buttonSize,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START

        addOverlayView(v, params)

        when{
            !::closeParams.isInitialized -> closeParams=params
            !::startParams.isInitialized -> startParams=params
            !::modeParams.isInitialized -> modeParams=params
            !::resetParams.isInitialized -> resetParams=params
            !::zoomHXPlusParams.isInitialized -> zoomHXPlusParams=params
            !::zoomHXMinusParams.isInitialized -> zoomHXMinusParams=params
            !::zoomVPlusParams.isInitialized -> zoomVPlusParams=params
            else -> zoomVMinusParams=params
        }

        return v
    }

    private fun makeIconButton(
        @DrawableRes iconRes: Int,
        iconColor: Int,
        action:()->Unit
    ): TextView {

        val v = TextView(this)
        v.styleMainControlButton(iconColor, 34f)
        v.setVectorIcon(iconRes, iconColor)
        v.setOnClickListener{ action() }

        val params = WindowManager.LayoutParams(
            buttonSize,buttonSize,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START

        addOverlayView(v, params)

        when{
            !::closeParams.isInitialized -> closeParams=params
            !::startParams.isInitialized -> startParams=params
            !::modeParams.isInitialized -> modeParams=params
            !::resetParams.isInitialized -> resetParams=params
            !::zoomHXPlusParams.isInitialized -> zoomHXPlusParams=params
            !::zoomHXMinusParams.isInitialized -> zoomHXMinusParams=params
            !::zoomVPlusParams.isInitialized -> zoomVPlusParams=params
            else -> zoomVMinusParams=params
        }

        return v
    }

    private fun TextView.styleMainControlButton(textColor: Int, textSize: Float) {
        setTextColor(textColor)
        this.textSize = textSize
        gravity = Gravity.CENTER
        includeFontPadding = false
        setPadding(0, 0, 0, 0)
        setBackgroundColor(Color.TRANSPARENT)
    }

    private fun TextView.setVectorIcon(@DrawableRes iconRes: Int, tintColor: Int) {
        text = null
        val icon = ContextCompat.getDrawable(this@OverlayService, iconRes)?.mutate()
        compoundDrawableTintList = ColorStateList.valueOf(tintColor)
        setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null)
    }

    // =====================================================
    // POSITIONING
    // =====================================================

    override fun onAreaUpdated(area: RectF) {
        drawArea.set(area)

        if (!::zoomVMinus.isInitialized || !zoomVMinus.isAttachedToWindow) return

        positionOverlayControls(drawArea)
    }

    private fun positionOverlayControls(area: RectF) {
        val screenWidth = drawView.width
        val controlsWidth = buttonSize * 4 + gap * 3
        val controlsStartX = (screenWidth - controlsWidth) / 2
        val controlsY = fixedControlsY ?: (
            area.top - buttonSize - 70f
        ).toInt().also {
            fixedControlsY = it
        }

        modeParams.x = controlsStartX
        modeParams.y = controlsY
        startParams.x = controlsStartX + buttonSize + gap
        startParams.y = controlsY
        resetParams.x = controlsStartX + (buttonSize + gap) * 2
        resetParams.y = controlsY
        closeParams.x = controlsStartX + (buttonSize + gap) * 3
        closeParams.y = controlsY

        updateOverlayView(closeBtn, closeParams)
        updateOverlayView(startBtn, startParams)
        updateOverlayView(modeBtn, modeParams)
        updateOverlayView(resetBtn, resetParams)
        val centerY = (drawView.height / 2) - 200
        val spacing = 220
        val startX = (drawView.width / 2) - (spacing * 2)

        zoomHXMinusParams.x = startX-spacing*2
        zoomHXMinusParams.y = centerY

        zoomHXPlusParams.x = startX - spacing
        zoomHXPlusParams.y = centerY

        zoomVMinusParams.x = startX
        zoomVMinusParams.y = centerY

        zoomVPlusParams.x = startX + spacing
        zoomVPlusParams.y = centerY

        updateOverlayView(zoomHXMinus, zoomHXMinusParams)
        updateOverlayView(zoomHXPlus, zoomHXPlusParams)
        updateOverlayView(zoomVMinus, zoomVMinusParams)
        updateOverlayView(zoomVPlus, zoomVPlusParams)
    }

    // =====================================================
    // CAPTURE CONTROL (CORRIGIDO)
    // =====================================================

    private fun enableCapture(): Boolean {

        if (capturing) return true
        if (!canUseOverlay() || !isPlayMode() || !isOverlayReady()) return false

        cancelSequencePresentation()
        drawView.hideCompletedSequence()
        capturing = true

        drawParams.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

        updateOverlayView(drawView, drawParams)

        // pequeno delay para estabilizar input
        mainHandler.removeCallbacks(startCaptureRunnable)
        mainHandler.postDelayed(startCaptureRunnable, CAPTURE_START_DELAY_MS)
        return true
    }

    private fun disableCapture(){

        capturing = false
        mainHandler.removeCallbacks(startCaptureRunnable)

        if (!::drawView.isInitialized || !::drawParams.isInitialized) return

        drawParams.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

        updateOverlayView(drawView, drawParams)

        drawView.stopCapture()
    }

    override fun onCaptureFinished() {
        disableCapture()
        updateStartButton(false)
        cancelSequencePresentation()
        drawView.showGoMessage()
        mainHandler.postDelayed(showGlyphSequenceRunnable, REPLAY_PREPARE_DELAY_MS)
    }

    private fun startReplay() {
        cancelReplay(clearGoMessage = false)
        mainHandler.postDelayed(replayStepRunnable, REPLAY_START_DELAY_MS)
    }

    private fun cancelSequencePresentation() {
        mainHandler.removeCallbacks(showGlyphSequenceRunnable)
        cancelReplay()
    }

    private fun cancelReplay(clearGoMessage: Boolean = true) {
        mainHandler.removeCallbacks(replayStepRunnable)
        replayIndex = 0
        replayGlyphVisible = false

        if (::drawView.isInitialized) {
            drawView.clearReplayGlyph()
            if (clearGoMessage) {
                drawView.clearGoMessage()
            }
        }
    }

    private fun applyRestoredOverlayState() {
        if (restoreMinimizedOnCreate) {
            minimizeOverlay()
        } else {
            saveOverlayMinimizedState()
        }
    }

    private fun minimizeOverlay() {
        if (overlayMinimized || !::drawView.isInitialized) return

        overlayMinimized = true
        saveOverlayMinimizedState()
        cancelSequencePresentation()
        disableCapture()
        drawView.resetGlyphs()
        drawView.visibility = View.GONE
        setMainControlsVisibility(View.GONE)
        hideTutorial()
        if (::tutorialToggleBtn.isInitialized) {
            tutorialToggleBtn.visibility = View.GONE
        }
        if (::themeBtn.isInitialized) {
            applyFloatingGroupPosition(floatingGroupX, floatingGroupY)
            updateThemeButton()
            themeBtn.visibility = View.VISIBLE
        }
        if (::opacityBtn.isInitialized) {
            applyFloatingGroupPosition(floatingGroupX, floatingGroupY)
            updateOpacityButton()
            opacityBtn.visibility = View.VISIBLE
        }
        updateProgramButtons()

        if (::floatingBtn.isInitialized) {
            applyFloatingGroupPosition(floatingGroupX, floatingGroupY)
            updateFloatingButton()
            floatingBtn.visibility = View.VISIBLE
        }
        if (::floatingModeBtn.isInitialized) {
            updateFloatingModeButton()
            floatingModeBtn.visibility = View.VISIBLE
        }
        if (::floatingCloseBtn.isInitialized) {
            floatingCloseBtn.visibility = View.VISIBLE
        }
    }

    private fun restoreOverlay(startCaptureAutomatically: Boolean = false) {
        if (!canUseOverlay() || creationFailed || !::drawView.isInitialized) return

        cancelSequencePresentation()
        disableCapture()
        drawView.setGlyphLimit(glyphLimit)
        drawView.setGlyphScales(horizontalScale, verticalScale)
        drawView.visibility = View.VISIBLE
        setMainControlsVisibility(View.VISIBLE)
        if (::tutorialToggleBtn.isInitialized) {
            tutorialToggleBtn.visibility = View.VISIBLE
        }
        if (::themeBtn.isInitialized) {
            themeBtn.visibility = View.GONE
        }
        if (::opacityBtn.isInitialized) {
            opacityBtn.visibility = View.GONE
        }
        overlayMinimized = false
        saveOverlayMinimizedState()
        updateStartButton(false)
        updateModeButton()
        updateProgramButtons()

        if (::floatingBtn.isInitialized) {
            floatingBtn.visibility = View.GONE
        }
        if (::floatingModeBtn.isInitialized) {
            floatingModeBtn.visibility = View.GONE
        }
        if (::floatingCloseBtn.isInitialized) {
            floatingCloseBtn.visibility = View.GONE
        }

        if (startCaptureAutomatically) {
            updateStartButton(enableCapture())
        }
    }

    private fun setMainControlsVisibility(visibility: Int) {
        closeBtn.visibility = visibility
        startBtn.visibility = visibility
        modeBtn.visibility = visibility
        resetBtn.visibility = visibility
    }

    private fun nextOverlayOpacityPercent(): Int {
        return when (overlayOpacityPercent) {
            100 -> 80
            80 -> 60
            else -> 100
        }
    }

    private fun applyOverlayOpacity() {
        val overlayAlpha = overlayOpacityPercent / 100f
        if (::drawView.isInitialized) {
            drawView.alpha = overlayAlpha
        }
        if (::closeBtn.isInitialized) {
            closeBtn.alpha = overlayAlpha
        }
        if (::startBtn.isInitialized) {
            startBtn.alpha = overlayAlpha
        }
        if (::modeBtn.isInitialized) {
            modeBtn.alpha = overlayAlpha
        }
        if (::resetBtn.isInitialized) {
            resetBtn.alpha = overlayAlpha
        }
    }

    // =====================================================
    // UI STATES
    // =====================================================

    private fun updateStartButton(active:Boolean){
        applyReferencePlayButton(active)
    }

    private fun applyReferencePlayButton(active: Boolean) {
        applyReferenceButtonBackground(
            startBtn,
            if (active) {
                R.drawable.btn_play_reference_active
            } else {
                R.drawable.btn_play_reference_inactive
            }
        )
    }

    private fun applyReferenceButtonBackground(
        button: TextView,
        @DrawableRes drawableRes: Int
    ) {
        button.text = null
        button.compoundDrawableTintList = null
        button.setCompoundDrawables(null, null, null, null)
        button.background = ContextCompat.getDrawable(this, drawableRes)
    }

    private fun updateModeButton(){
        applyReferenceButtonBackground(modeBtn, glyphLimitMainButton())
        updateFloatingButton()
    }

    private fun updateFloatingButton() {
        if (!::floatingBtn.isInitialized) return

        applyReferenceButtonBackground(floatingBtn, glyphLimitFloatingButton())
    }

    private fun updateFloatingModeButton() {
        if (!::floatingModeBtn.isInitialized) return

        if (::floatingModeParams.isInitialized) {
            floatingModeParams.width = floatingModeWidth()
            floatingModeParams.height = FLOATING_MODE_CONTENT_HEIGHT
            floatingModeParams.x = floatingModeX()
            floatingModeParams.y = floatingModeY()
            updateOverlayView(floatingModeBtn, floatingModeParams)
        }
        applyReferenceButtonBackground(
            floatingModeBtn,
            if (autoCaptureEnabled) {
                R.drawable.btn_auto_reference
            } else {
                R.drawable.btn_manual_reference
            }
        )
    }

    @DrawableRes
    private fun glyphLimitIcon(): Int {
        return when (glyphLimit) {
            3 -> R.drawable.ic_digit_3
            4 -> R.drawable.ic_digit_4
            else -> R.drawable.ic_digit_5
        }
    }

    @DrawableRes
    private fun glyphLimitMainButton(): Int {
        return when (currentColorTheme) {
            AppColorTheme.STANDARD -> when (glyphLimit) {
                3 -> R.drawable.btn_mode_3_reference
                4 -> R.drawable.btn_mode_4_reference
                else -> R.drawable.btn_mode_5_reference
            }
            AppColorTheme.GREEN -> when (glyphLimit) {
                3 -> R.drawable.btn_mode_3_green_reference
                4 -> R.drawable.btn_mode_4_green_reference
                else -> R.drawable.btn_mode_5_green_reference
            }
            AppColorTheme.BLUE -> when (glyphLimit) {
                3 -> R.drawable.btn_mode_3_blue_reference
                4 -> R.drawable.btn_mode_4_blue_reference
                else -> R.drawable.btn_mode_5_blue_reference
            }
        }
    }

    @DrawableRes
    private fun glyphLimitFloatingButton(): Int {
        return when (currentColorTheme) {
            AppColorTheme.STANDARD -> when (glyphLimit) {
                3 -> R.drawable.btn_floating_mode_3_reference
                4 -> R.drawable.btn_floating_mode_4_reference
                else -> R.drawable.btn_floating_mode_5_reference
            }
            AppColorTheme.GREEN -> when (glyphLimit) {
                3 -> R.drawable.btn_floating_mode_3_green_reference
                4 -> R.drawable.btn_floating_mode_4_green_reference
                else -> R.drawable.btn_floating_mode_5_green_reference
            }
            AppColorTheme.BLUE -> when (glyphLimit) {
                3 -> R.drawable.btn_floating_mode_3_blue_reference
                4 -> R.drawable.btn_floating_mode_4_blue_reference
                else -> R.drawable.btn_floating_mode_5_blue_reference
            }
        }
    }

    private fun updateTutorialToggleButton() {
        if (!::tutorialToggleBtn.isInitialized) return

        tutorialToggleBtn.setText(R.string.tutorial_label)
        TutorialHudUi.styleSwitch(
            tutorialToggleBtn,
            AppThemeConfig.colors(currentColorTheme),
            showTutorialOnLaunch
        )
    }

    private fun updateThemeButton() {
        if (!::themeBtn.isInitialized) return

        applyReferenceButtonBackground(
            themeBtn,
            when (currentColorTheme) {
                AppColorTheme.STANDARD -> R.drawable.btn_theme_reference_standard
                AppColorTheme.GREEN -> R.drawable.btn_theme_reference_green
                AppColorTheme.BLUE -> R.drawable.btn_theme_reference_blue
            }
        )
    }

    private fun updateOpacityButton() {
        if (!::opacityBtn.isInitialized) return

        applyReferenceButtonBackground(opacityBtn, overlayOpacityButton())
    }

    @DrawableRes
    private fun overlayOpacityButton(): Int {
        return when (overlayOpacityPercent) {
            80 -> R.drawable.btn_opacity_80_reference
            60 -> R.drawable.btn_opacity_60_reference
            else -> R.drawable.btn_opacity_100_reference
        }
    }

    private fun applyCurrentTheme() {
        val colors = AppThemeConfig.colors(currentColorTheme)

        if (::drawView.isInitialized) {
            drawView.setAppColorTheme(currentColorTheme)
        }
        if (::tutorialLayer.isInitialized) {
            tutorialLayer.setBackgroundColor(Color.argb(86, 0, 0, 0))
        }
        if (::tutorialCard.isInitialized) {
            tutorialCard.background = TutorialHudUi.panelBackground(colors)
        }
        if (::tutorialPointer.isInitialized) {
            TutorialHudUi.stylePointer(tutorialPointer, colors)
        }
        if (::tutorialBody.isInitialized) {
            tutorialBody.setTextColor(colors.text)
        }
        if (::tutorialCloseBtn.isInitialized) {
            TutorialHudUi.styleIconButton(tutorialCloseBtn, colors)
        }
        if (::tutorialBackBtn.isInitialized) {
            TutorialHudUi.styleButton(tutorialBackBtn, colors)
        }
        if (::tutorialNextBtn.isInitialized) {
            TutorialHudUi.styleButton(tutorialNextBtn, colors)
        }
        if (::tutorialToggleBtn.isInitialized) {
            updateTutorialToggleButton()
        }
        if (::themeBtn.isInitialized) {
            updateThemeButton()
        }
        if (::modeBtn.isInitialized) {
            updateModeButton()
        }
        if (::floatingBtn.isInitialized) {
            updateFloatingButton()
        }
        updateTutorialIndicators(colors)
    }

    private fun updateTutorialIndicators(colors: AppThemeColors) {
        tutorialIndicators.forEachIndexed { index, dot ->
            TutorialHudUi.styleIndicator(dot, colors, index == tutorialStepIndex)
        }
    }

    private fun showTutorial(stepIndex: Int = 0) {
        if (!::tutorialLayer.isInitialized || tutorialSteps.isEmpty()) return

        tutorialStepIndex = stepIndex.coerceIn(0, tutorialSteps.lastIndex)
        val step = tutorialSteps[tutorialStepIndex]

        tutorialBody.setText(step.bodyRes)
        tutorialBackBtn.visibility = if (tutorialStepIndex == 0) View.INVISIBLE else View.VISIBLE
        tutorialNextBtn.visibility =
            if (tutorialStepIndex == tutorialSteps.lastIndex) View.INVISIBLE else View.VISIBLE
        updateTutorialIndicators(AppThemeConfig.colors(currentColorTheme))

        positionTutorialStep(step.target)
        tutorialLayer.visibility = View.VISIBLE
    }

    private fun hideTutorial() {
        if (::tutorialLayer.isInitialized) {
            tutorialLayer.visibility = View.GONE
        }
    }

    private fun positionTutorialStep(target: TutorialTarget) {
        val targetRect = getTutorialTargetRect(target)
        val screenWidth = drawView.width.takeIf { it > 0 }
            ?: resources.displayMetrics.widthPixels
        val screenHeight = drawView.height.takeIf { it > 0 }
            ?: resources.displayMetrics.heightPixels
        val cardWidth = TUTORIAL_CARD_WIDTH.coerceAtMost(screenWidth - TUTORIAL_CARD_MARGIN * 2)
        val cardHeight = TUTORIAL_CARD_HEIGHT
        val cardAbove = targetRect.centerY() > screenHeight / 2f
        val rawCardX = (targetRect.centerX() - cardWidth / 2f).toInt()
        val rawCardY = if (cardAbove) {
            (targetRect.top - cardHeight - TUTORIAL_CARD_MARGIN).toInt()
        } else {
            (targetRect.bottom + TUTORIAL_CARD_MARGIN).toInt()
        }
        val cardX = rawCardX.coerceIn(
            TUTORIAL_CARD_MARGIN,
            (screenWidth - cardWidth - TUTORIAL_CARD_MARGIN).coerceAtLeast(TUTORIAL_CARD_MARGIN)
        )
        val cardY = rawCardY.coerceIn(
            TUTORIAL_CARD_MARGIN,
            (screenHeight - cardHeight - TUTORIAL_CARD_MARGIN).coerceAtLeast(TUTORIAL_CARD_MARGIN)
        )

        (tutorialCard.layoutParams as FrameLayout.LayoutParams).apply {
            width = cardWidth
            height = cardHeight
            leftMargin = cardX
            topMargin = cardY
        }
        tutorialCard.requestLayout()

        tutorialPointer.setText(
            if (cardAbove) {
                R.string.tutorial_pointer_down
            } else {
                R.string.tutorial_pointer_up
            }
        )
        val pointerX = (targetRect.centerX() - TUTORIAL_POINTER_SIZE / 2f).toInt()
            .coerceIn(0, (screenWidth - TUTORIAL_POINTER_SIZE).coerceAtLeast(0))
        val pointerY = if (cardAbove) {
            cardY + cardHeight
        } else {
            cardY - TUTORIAL_POINTER_SIZE
        }.coerceIn(0, (screenHeight - TUTORIAL_POINTER_SIZE).coerceAtLeast(0))

        (tutorialPointer.layoutParams as FrameLayout.LayoutParams).apply {
            leftMargin = pointerX
            topMargin = pointerY
        }
        tutorialPointer.requestLayout()
    }

    private fun getTutorialTargetRect(target: TutorialTarget): RectF {
        val fallbackWidth = resources.displayMetrics.widthPixels.toFloat()
        val fallbackHeight = resources.displayMetrics.heightPixels.toFloat()
        val screenWidth = drawView.width.takeIf { it > 0 }?.toFloat() ?: fallbackWidth

        return when (target) {
            TutorialTarget.CAPTURE_AREA -> {
                if (!drawArea.isEmpty) {
                    RectF(drawArea)
                } else {
                    RectF(
                        screenWidth * 0.25f,
                        fallbackHeight * 0.32f,
                        screenWidth * 0.75f,
                        fallbackHeight * 0.70f
                    )
                }
            }
            TutorialTarget.MODE_BUTTON -> paramsRect(modeParams, false)
            TutorialTarget.START_BUTTON -> paramsRect(startParams, false)
            TutorialTarget.RESET_BUTTON -> paramsRect(resetParams, false)
            TutorialTarget.CLOSE_BUTTON -> paramsRect(closeParams, false)
            TutorialTarget.GO_MESSAGE -> RectF(
                drawArea.centerX() - 90f,
                drawArea.centerY() - 90f,
                drawArea.centerX() + 90f,
                drawArea.centerY() + 90f
            )
            TutorialTarget.FLOATING_BUTTON -> paramsRect(floatingParams, true)
            TutorialTarget.FLOATING_MODE -> paramsRect(floatingModeParams, true)
        }
    }

    private fun paramsRect(
        params: WindowManager.LayoutParams,
        gravityEnd: Boolean
    ): RectF {
        val width = params.width.takeIf { it > 0 } ?: 1
        val height = params.height.takeIf { it > 0 } ?: 1
        val screenWidth = drawView.width.takeIf { it > 0 }
            ?: resources.displayMetrics.widthPixels
        val left = if (gravityEnd) screenWidth - params.x - width else params.x
        val top = params.y

        return RectF(
            left.toFloat(),
            top.toFloat(),
            (left + width).toFloat(),
            (top + height).toFloat()
        )
    }

    private fun updateProgramButtons() {

        val visible = !overlayMinimized && AppMode.currentMode == AppMode.Mode.PROGRAM
        val visibility = if (visible) TextView.VISIBLE else TextView.GONE

        zoomHXPlus.visibility = visibility
        zoomHXMinus.visibility = visibility
        zoomVPlus.visibility = visibility
        zoomVMinus.visibility = visibility
    }

    private fun restorePreferences() {
        val preferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        firstLaunchTutorialPending = !preferences.getBoolean(
            PREF_TUTORIAL_INITIALIZED,
            false
        )
        showTutorialOnLaunch = preferences.getBoolean(
            PREF_SHOW_TUTORIAL_ON_LAUNCH,
            DEFAULT_SHOW_TUTORIAL_ON_LAUNCH
        )
        currentColorTheme = AppThemeConfig.restoreTheme(preferences)
        glyphLimit = preferences.getInt(PREF_GLYPH_LIMIT, DEFAULT_GLYPH_LIMIT)
            .coerceIn(3, 5)
        horizontalScale = preferences.getFloat(PREF_HORIZONTAL_SCALE, DEFAULT_SCALE)
            .coerceIn(0.5f, 1.8f)
        verticalScale = preferences.getFloat(PREF_VERTICAL_SCALE, DEFAULT_SCALE)
            .coerceIn(0.5f, 1.8f)
        autoCaptureEnabled = preferences.getBoolean(
            PREF_AUTO_CAPTURE,
            DEFAULT_AUTO_CAPTURE
        )
        floatingGroupX = preferences.getInt(
            PREF_FLOATING_GROUP_X,
            FLOATING_BUTTON_MARGIN
        )
        floatingGroupY = preferences.getInt(
            PREF_FLOATING_GROUP_Y,
            FLOATING_BUTTON_TOP
        )
        restoreMinimizedOnCreate = preferences.getBoolean(
            PREF_OVERLAY_MINIMIZED,
            DEFAULT_OVERLAY_MINIMIZED
        )
        overlayOpacityPercent = preferences.getInt(
            PREF_OVERLAY_OPACITY_PERCENT,
            DEFAULT_OVERLAY_OPACITY_PERCENT
        ).takeIf { it == 100 || it == 80 || it == 60 }
            ?: DEFAULT_OVERLAY_OPACITY_PERCENT
        overlayMinimized = false

        preferences.edit {
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
    }

    private fun saveGlyphLimit() {
        getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit {
            putInt(PREF_GLYPH_LIMIT, glyphLimit)
        }
    }

    private fun saveGlyphScales() {
        getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit {
            putFloat(PREF_HORIZONTAL_SCALE, horizontalScale)
            putFloat(PREF_VERTICAL_SCALE, verticalScale)
        }
    }

    private fun saveAutoCaptureMode() {
        getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(PREF_AUTO_CAPTURE, autoCaptureEnabled)
        }
    }

    private fun saveTutorialLaunchPreference() {
        getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(PREF_SHOW_TUTORIAL_ON_LAUNCH, showTutorialOnLaunch)
        }
    }

    private fun saveColorTheme() {
        AppThemeConfig.saveTheme(
            getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
            currentColorTheme
        )
    }

    private fun saveFloatingGroupPosition() {
        getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit {
            putInt(PREF_FLOATING_GROUP_X, floatingGroupX)
            putInt(PREF_FLOATING_GROUP_Y, floatingGroupY)
        }
    }

    private fun saveOverlayMinimizedState() {
        getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(PREF_OVERLAY_MINIMIZED, overlayMinimized)
        }
    }

    private fun saveOverlayOpacity() {
        getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit {
            putInt(PREF_OVERLAY_OPACITY_PERCENT, overlayOpacityPercent)
        }
    }

    // =====================================================
    // DESTROY
    // =====================================================

    override fun onDestroy() {
        mainHandler.removeCallbacks(startCaptureRunnable)
        cancelSequencePresentation()
        unregisterOverlayPermissionListener()

        if (::drawView.isInitialized) removeOverlayView(drawView)
        if (::closeBtn.isInitialized) removeOverlayView(closeBtn)
        if (::startBtn.isInitialized) removeOverlayView(startBtn)
        if (::modeBtn.isInitialized) removeOverlayView(modeBtn)
        if (::resetBtn.isInitialized) removeOverlayView(resetBtn)
        if (::floatingBtn.isInitialized) removeOverlayView(floatingBtn)
        if (::floatingModeBtn.isInitialized) removeOverlayView(floatingModeBtn)
        if (::floatingCloseBtn.isInitialized) removeOverlayView(floatingCloseBtn)
        if (::tutorialToggleBtn.isInitialized) removeOverlayView(tutorialToggleBtn)
        if (::themeBtn.isInitialized) removeOverlayView(themeBtn)
        if (::opacityBtn.isInitialized) removeOverlayView(opacityBtn)
        if (::tutorialLayer.isInitialized) removeOverlayView(tutorialLayer)
        if (::zoomHXPlus.isInitialized) removeOverlayView(zoomHXPlus)
        if (::zoomHXMinus.isInitialized) removeOverlayView(zoomHXMinus)
        if (::zoomVPlus.isInitialized) removeOverlayView(zoomVPlus)
        if (::zoomVMinus.isInitialized) removeOverlayView(zoomVMinus)

        super.onDestroy()
    }

    private fun addOverlayView(view: View, params: WindowManager.LayoutParams) {
        try {
            wm.addView(view, params)
        } catch (exception: WindowManager.BadTokenException) {
            Log.e(LOG_TAG, "WindowManager.addView failed", exception)
            creationFailed = true
            stopSelf()
        } catch (exception: IllegalArgumentException) {
            Log.e(LOG_TAG, "WindowManager.addView failed", exception)
            creationFailed = true
            stopSelf()
        } catch (exception: SecurityException) {
            Log.e(LOG_TAG, "WindowManager.addView failed", exception)
            creationFailed = true
            stopSelf()
        }
    }

    private fun updateOverlayView(view: View, params: WindowManager.LayoutParams) {
        if (!view.isAttachedToWindow) return

        try {
            wm.updateViewLayout(view, params)
        } catch (exception: IllegalArgumentException) {
            Log.e(LOG_TAG, "WindowManager.updateViewLayout failed", exception)
            stopSelf()
        } catch (exception: SecurityException) {
            Log.e(LOG_TAG, "WindowManager.updateViewLayout failed", exception)
            stopSelf()
        }
    }

    private fun removeOverlayView(view: View) {
        if (!view.isAttachedToWindow) return

        try {
            wm.removeView(view)
        } catch (exception: IllegalArgumentException) {
            Log.w(LOG_TAG, "WindowManager.removeView failed", exception)
            // The view may already have been removed by WindowManager.
        } catch (exception: SecurityException) {
            Log.w(LOG_TAG, "WindowManager.removeView failed", exception)
            // Overlay permission may have been revoked during teardown.
        }
    }

    private fun isPlayMode() = AppMode.currentMode == AppMode.Mode.PLAY

    private fun canUseOverlay() = Settings.canDrawOverlays(this)

    private fun isOverlayReady(): Boolean {
        return ::drawView.isInitialized && drawView.isAttachedToWindow &&
                ::closeBtn.isInitialized && closeBtn.isAttachedToWindow &&
                ::startBtn.isInitialized && startBtn.isAttachedToWindow &&
                ::modeBtn.isInitialized && modeBtn.isAttachedToWindow &&
                ::resetBtn.isInitialized && resetBtn.isAttachedToWindow &&
                ::zoomHXPlus.isInitialized && zoomHXPlus.isAttachedToWindow &&
                ::zoomHXMinus.isInitialized && zoomHXMinus.isAttachedToWindow &&
                ::zoomVPlus.isInitialized && zoomVPlus.isAttachedToWindow &&
                ::zoomVMinus.isInitialized && zoomVMinus.isAttachedToWindow
    }

    private fun registerOverlayPermissionListener() {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        try {
            appOps.startWatchingMode(
                AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                packageName,
                overlayPermissionListener
            )
            permissionListenerRegistered = true
        } catch (_: SecurityException) {
            stopSelf()
        }
    }

    private fun unregisterOverlayPermissionListener() {
        if (!permissionListenerRegistered) return

        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        try {
            appOps.stopWatchingMode(overlayPermissionListener)
        } catch (_: SecurityException) {
            // Permission state may already be unavailable during teardown.
        }
        permissionListenerRegistered = false
    }
}
