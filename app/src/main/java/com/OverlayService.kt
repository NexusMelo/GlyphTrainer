package com

import android.app.AppOpsManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.IBinder
import android.provider.Settings
import android.view.WindowManager
import android.widget.ImageView
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.graphics.Color
import android.view.Gravity
import androidx.annotation.StringRes
import com.example.glyphtrainer.AppMode
import com.example.glyphtrainer.DrawView
import com.example.glyphtrainer.R

class OverlayService : Service(),


    DrawView.OverlayListener {

    private companion object {
        const val CAPTURE_START_DELAY_MS = 140L
        const val GLYPH_DISPLAY_DELAY_MS = 3_000L
    }

    private lateinit var wm: WindowManager
    private lateinit var drawView: DrawView


    private lateinit var closeBtn: ImageView
    private lateinit var startBtn: ImageView
    private lateinit var modeBtn: ImageView
    private lateinit var resetBtn: ImageView
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

    private val buttonSize = 130
    private val gap = 20
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
        }
    }
    private val overlayPermissionListener = AppOpsManager.OnOpChangedListener { _, packageName ->
        if (packageName == this.packageName && !canUseOverlay()) {
            mainHandler.post { stopSelf() }
        }
    }

    private var glyphLimit = 5
    private var capturing = false
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

        createDrawLayer()
        if (creationFailed) return

        createButtons()
        if (creationFailed) return

        disableCapture()
        updateStartButton(false)
        updateModeButton()
        updateProgramButtons()
        registerOverlayPermissionListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!canUseOverlay() || !isOverlayReady()) {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    // =====================================================
    // DRAW LAYER
    // =====================================================

    private fun createDrawLayer(){

        drawView = DrawView(this, this)

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

        closeBtn = makeButton(0x88FF4444.toInt()){ stopSelf() }

        startBtn = makeButton(0x66006600){
            if (enableCapture()) {
                updateStartButton(true)
            }
        }

        modeBtn = makeButton(0x550088FF){
            cancelGlyphDisplay()
            disableCapture()

            glyphLimit = if(glyphLimit == 5) 4 else 5

            drawView.setGlyphLimit(glyphLimit)

            updateStartButton(false)
            updateModeButton()
        }

        resetBtn = makeButton(0x88FFFF00.toInt()){
            cancelGlyphDisplay()
            disableCapture()
            drawView.resetGlyphs()
            val active = enableCapture()
            updateStartButton(active)
        }
        zoomHXPlus = makeMenuButton(R.string.adjust_horizontal_increase) {
            drawView.adjustHorizontal(1f)
        }

        zoomHXMinus = makeMenuButton(R.string.adjust_horizontal_decrease) {
            drawView.adjustHorizontal(-1f)
        }

        zoomVPlus = makeMenuButton(R.string.adjust_vertical_increase) {
            drawView.adjustVertical(1f)
        }

        zoomVMinus = makeMenuButton(R.string.adjust_vertical_decrease) {
            drawView.adjustVertical(-1f)
        }
    }


    private fun makeButton(color:Int, action:()->Unit): ImageView {

        val v = ImageView(this)
        v.setBackgroundColor(color)
        v.setOnClickListener{ action() }

        val params = WindowManager.LayoutParams(
            buttonSize,buttonSize,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

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

    // =====================================================
    // POSITIONING
    // =====================================================

    override fun onAreaUpdated(area: RectF) {

        val baseY = (area.top - buttonSize - gap).toInt()
        val screenWidth = drawView.width

        closeParams.x = (area.right + gap).toInt()
        closeParams.y = baseY

        startParams.x = closeParams.x
        startParams.y = baseY - buttonSize - 20

        modeParams.x = closeParams.x
        modeParams.y = startParams.y - buttonSize - 20

        resetParams.x = (screenWidth/2)-(buttonSize/2)
        resetParams.y = baseY + buttonSize + 20

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

        cancelGlyphDisplay()
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
        mainHandler.removeCallbacks(showGlyphSequenceRunnable)
        mainHandler.postDelayed(showGlyphSequenceRunnable, GLYPH_DISPLAY_DELAY_MS)
    }

    private fun cancelGlyphDisplay() {
        mainHandler.removeCallbacks(showGlyphSequenceRunnable)
    }

    // =====================================================
    // UI STATES
    // =====================================================

    private fun updateStartButton(active:Boolean){
        if(active)
            startBtn.setBackgroundColor(0xCC00FF00.toInt())
        else
            startBtn.setBackgroundColor(0x66006600)
    }

    private fun updateModeButton(){
        if(glyphLimit==5)
            modeBtn.setBackgroundColor(0xFF0066FF.toInt())
        else
            modeBtn.setBackgroundColor(0xFF00BBFF.toInt())
    }
    private fun updateProgramButtons() {

        val visible = AppMode.currentMode == AppMode.Mode.PROGRAM
        val visibility = if (visible) TextView.VISIBLE else TextView.GONE

        zoomHXPlus.visibility = visibility
        zoomHXMinus.visibility = visibility
        zoomVPlus.visibility = visibility
        zoomVMinus.visibility = visibility
    }

    // =====================================================
    // DESTROY
    // =====================================================

    override fun onDestroy() {
        mainHandler.removeCallbacks(startCaptureRunnable)
        cancelGlyphDisplay()
        unregisterOverlayPermissionListener()

        if (::drawView.isInitialized) removeOverlayView(drawView)
        if (::closeBtn.isInitialized) removeOverlayView(closeBtn)
        if (::startBtn.isInitialized) removeOverlayView(startBtn)
        if (::modeBtn.isInitialized) removeOverlayView(modeBtn)
        if (::resetBtn.isInitialized) removeOverlayView(resetBtn)
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
        if (!view.isAttachedToWindow) {
            stopSelf()
            return
        }

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
