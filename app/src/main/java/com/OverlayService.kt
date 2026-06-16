package com

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.provider.Settings
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.graphics.Color
import android.view.Gravity
import androidx.annotation.StringRes
import androidx.core.content.edit
import com.example.glyphtrainer.AppMode
import com.example.glyphtrainer.DrawView
import com.example.glyphtrainer.R
import kotlin.math.abs

class OverlayService : Service(),


    DrawView.OverlayListener {

    private companion object {
        const val PREFERENCES_NAME = "glyph_trainer_state"
        const val PREF_GLYPH_LIMIT = "glyph_limit"
        const val PREF_HORIZONTAL_SCALE = "horizontal_scale"
        const val PREF_VERTICAL_SCALE = "vertical_scale"
        const val PREF_AUTO_CAPTURE = "auto_capture"
        const val PREF_FLOATING_GROUP_X = "floating_group_x"
        const val PREF_FLOATING_GROUP_Y = "floating_group_y"
        const val DEFAULT_GLYPH_LIMIT = 5
        const val DEFAULT_SCALE = 1f
        const val DEFAULT_AUTO_CAPTURE = false
        const val FLOATING_BUTTON_SIZE = 132
        const val FLOATING_BUTTON_MARGIN = 24
        const val FLOATING_BUTTON_TOP = 180
        const val FLOATING_MODE_WIDTH = 176
        const val FLOATING_MODE_HEIGHT = 64
        const val FLOATING_MODE_GAP = 16
        const val FLOATING_CLOSE_SIZE = 48
        const val FLOATING_CLOSE_OVERLAP = FLOATING_CLOSE_SIZE / 2
        const val FLOATING_GROUP_HEIGHT =
            FLOATING_BUTTON_SIZE + FLOATING_MODE_GAP + FLOATING_MODE_HEIGHT
        const val CAPTURE_START_DELAY_MS = 140L
        const val GLYPH_DISPLAY_DELAY_MS = 2_000L
        const val REPLAY_START_DELAY_MS = 1_000L
        const val REPLAY_GLYPH_DURATION_MS = 1_750L
        const val REPLAY_GLYPH_GAP_MS = 0L
        const val REPLAY_PREPARE_DELAY_MS = GLYPH_DISPLAY_DELAY_MS - REPLAY_START_DELAY_MS
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

    private class BaselineShiftSpan(
        private val shiftPx: Int
    ) : MetricAffectingSpan() {
        override fun updateDrawState(textPaint: TextPaint) {
            textPaint.baselineShift += shiftPx
        }

        override fun updateMeasureState(textPaint: TextPaint) {
            textPaint.baselineShift += shiftPx
        }
    }

    private var glyphLimit = DEFAULT_GLYPH_LIMIT
    private var horizontalScale = DEFAULT_SCALE
    private var verticalScale = DEFAULT_SCALE
    private var autoCaptureEnabled = DEFAULT_AUTO_CAPTURE
    private var capturing = false
    private var replayIndex = 0
    private var replayGlyphVisible = false
    private var overlayMinimized = false
    private var creationFailed = false
    private var permissionListenerRegistered = false

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

        disableCapture()
        updateStartButton(false)
        updateModeButton()
        updateProgramButtons()
        registerOverlayPermissionListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!canUseOverlay() || creationFailed) {
            stopSelf()
        } else if (overlayMinimized) {
            restoreOverlay()
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

        closeBtn = makeButton(R.string.overlay_close, Color.RED){ stopSelf() }
        shiftButtonSymbol(closeBtn, R.string.overlay_close, -8)

        startBtn = makeButton(R.string.overlay_start, Color.WHITE){
            if (enableCapture()) {
                updateStartButton(true)
            }
        }
        shiftButtonSymbol(startBtn, R.string.overlay_start, -27)

        modeBtn = makeButton(R.string.overlay_glyph_limit, Color.CYAN, 30f){
            cancelSequencePresentation()
            disableCapture()

            glyphLimit = if (glyphLimit == 5) 3 else glyphLimit + 1
            saveGlyphLimit()

            drawView.setGlyphLimit(glyphLimit)

            updateStartButton(false)
            updateModeButton()
        }

        resetBtn = makeButton(R.string.overlay_reset, Color.YELLOW){
            cancelSequencePresentation()
            disableCapture()
            drawView.resetGlyphs()
            val active = enableCapture()
            updateStartButton(active)
        }
        shiftButtonSymbol(resetBtn, R.string.overlay_reset, -30)
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
            textSize = 42f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            includeFontPadding = false
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(210, 0, 75, 95))
                setStroke(4, Color.CYAN)
            }
            elevation = 8f
            visibility = View.GONE
            setOnClickListener { restoreOverlay(autoCaptureEnabled) }
            setOnTouchListener { view, event -> handleFloatingDrag(view, event) }
        }

        floatingParams = WindowManager.LayoutParams(
            FLOATING_BUTTON_SIZE,
            FLOATING_BUTTON_SIZE,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = floatingGroupX + (FLOATING_MODE_WIDTH - FLOATING_BUTTON_SIZE) / 2
            y = floatingGroupY
        }

        addOverlayView(floatingBtn, floatingParams)

        floatingCloseBtn = TextView(this).apply {
            setText(R.string.overlay_close)
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            includeFontPadding = false
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(220, 150, 20, 20))
                setStroke(3, Color.RED)
            }
            elevation = 10f
            visibility = View.GONE
            setOnClickListener { stopSelf() }
            setOnTouchListener { view, event -> handleFloatingDrag(view, event) }
        }
        shiftButtonSymbol(floatingCloseBtn, R.string.overlay_close, -12)

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
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            includeFontPadding = false
            visibility = View.GONE
            setOnClickListener {
                autoCaptureEnabled = !autoCaptureEnabled
                saveAutoCaptureMode()
                updateFloatingModeButton()
            }
            setOnTouchListener { view, event -> handleFloatingDrag(view, event) }
        }

        floatingModeParams = WindowManager.LayoutParams(
            FLOATING_MODE_WIDTH,
            FLOATING_MODE_HEIGHT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = floatingGroupX
            y = floatingGroupY + FLOATING_BUTTON_SIZE + FLOATING_MODE_GAP
        }

        addOverlayView(floatingModeBtn, floatingModeParams)
        applyFloatingGroupPosition(floatingGroupX, floatingGroupY)
        updateFloatingButton()
        updateFloatingModeButton()
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
            floatingParams.x =
                floatingGroupX + (FLOATING_MODE_WIDTH - FLOATING_BUTTON_SIZE) / 2
            floatingParams.y = floatingGroupY
            updateOverlayView(floatingBtn, floatingParams)
        }
        if (::floatingCloseParams.isInitialized) {
            floatingCloseParams.x = floatingGroupX
            floatingCloseParams.y = floatingGroupY - FLOATING_CLOSE_OVERLAP
            updateOverlayView(floatingCloseBtn, floatingCloseParams)
        }
        if (::floatingModeParams.isInitialized) {
            floatingModeParams.x = floatingGroupX
            floatingModeParams.y =
                floatingGroupY + FLOATING_BUTTON_SIZE + FLOATING_MODE_GAP
            updateOverlayView(floatingModeBtn, floatingModeParams)
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
        v.setTextColor(textColor)
        v.textSize = textSize
        v.gravity = Gravity.CENTER
        v.includeFontPadding = false
        v.setPadding(0, 0, 0, 0)
        v.setBackgroundColor(Color.TRANSPARENT)
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

    private fun shiftButtonSymbol(
        button: TextView,
        @StringRes textRes: Int,
        baselineShiftPx: Int
    ) {
        val symbol = SpannableString(getText(textRes))
        symbol.setSpan(
            BaselineShiftSpan(baselineShiftPx),
            0,
            symbol.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        button.text = symbol
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
        val controlsY = fixedControlsY ?: (area.bottom + 110f).toInt().also {
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

    private fun minimizeOverlay() {
        if (overlayMinimized || !::drawView.isInitialized) return

        overlayMinimized = true
        cancelSequencePresentation()
        disableCapture()
        drawView.resetGlyphs()
        drawView.visibility = View.GONE
        setMainControlsVisibility(View.GONE)
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
        overlayMinimized = false
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

    // =====================================================
    // UI STATES
    // =====================================================

    private fun updateStartButton(active:Boolean){
        if(active)
            startBtn.setTextColor(Color.GREEN)
        else
            startBtn.setTextColor(Color.WHITE)
    }

    private fun updateModeButton(){
        modeBtn.setText(
            when (glyphLimit) {
                3 -> R.string.overlay_glyph_limit_3
                4 -> R.string.overlay_glyph_limit_4
                else -> R.string.overlay_glyph_limit
            }
        )
        modeBtn.setTextColor(Color.CYAN)
        updateFloatingButton()
    }

    private fun updateFloatingButton() {
        if (!::floatingBtn.isInitialized) return

        floatingBtn.setText(
            when (glyphLimit) {
                3 -> R.string.overlay_glyph_limit_3
                4 -> R.string.overlay_glyph_limit_4
                else -> R.string.overlay_glyph_limit
            }
        )
    }

    private fun updateFloatingModeButton() {
        if (!::floatingModeBtn.isInitialized) return

        floatingModeBtn.setText(
            if (autoCaptureEnabled) {
                R.string.overlay_mode_auto
            } else {
                R.string.overlay_mode_manual
            }
        )
        floatingModeBtn.background = GradientDrawable().apply {
            cornerRadius = FLOATING_MODE_HEIGHT / 2f
            setColor(
                if (autoCaptureEnabled) {
                    Color.argb(210, 0, 110, 70)
                } else {
                    Color.argb(190, 45, 45, 45)
                }
            )
            setStroke(
                3,
                if (autoCaptureEnabled) Color.GREEN else Color.LTGRAY
            )
        }
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

    private fun saveFloatingGroupPosition() {
        getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit {
            putInt(PREF_FLOATING_GROUP_X, floatingGroupX)
            putInt(PREF_FLOATING_GROUP_Y, floatingGroupY)
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
        if (::zoomHXPlus.isInitialized) removeOverlayView(zoomHXPlus)
        if (::zoomHXMinus.isInitialized) removeOverlayView(zoomHXMinus)
        if (::zoomVPlus.isInitialized) removeOverlayView(zoomVPlus)
        if (::zoomVMinus.isInitialized) removeOverlayView(zoomVMinus)

        super.onDestroy()
    }

    private fun addOverlayView(view: View, params: WindowManager.LayoutParams) {
        try {
            wm.addView(view, params)
        } catch (_: WindowManager.BadTokenException) {
            creationFailed = true
            stopSelf()
        } catch (_: IllegalArgumentException) {
            creationFailed = true
            stopSelf()
        } catch (_: SecurityException) {
            creationFailed = true
            stopSelf()
        }
    }

    private fun updateOverlayView(view: View, params: WindowManager.LayoutParams) {
        if (!view.isAttachedToWindow) return

        try {
            wm.updateViewLayout(view, params)
        } catch (_: IllegalArgumentException) {
            stopSelf()
        } catch (_: SecurityException) {
            stopSelf()
        }
    }

    private fun removeOverlayView(view: View) {
        if (!view.isAttachedToWindow) return

        try {
            wm.removeView(view)
        } catch (_: IllegalArgumentException) {
            // The view may already have been removed by WindowManager.
        } catch (_: SecurityException) {
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
