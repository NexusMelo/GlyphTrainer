package com

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.IBinder
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
        if (::drawView.isInitialized) {
            drawView.startCapture()
        }
    }

    private var glyphLimit = 5
    private var capturing = false

    override fun onBind(intent: Intent?): IBinder? = null

    // =====================================================
    // CREATE
    // =====================================================

    override fun onCreate() {
        super.onCreate()

        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        createDrawLayer()
        createButtons()

        disableCapture()
        updateStartButton(false)
        updateModeButton()
        updateProgramButtons()
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
            enableCapture()
            updateStartButton(true)
        }

        modeBtn = makeButton(0x550088FF){

            glyphLimit = if(glyphLimit == 5) 4 else 5

            drawView.setGlyphLimit(glyphLimit)

            updateModeButton()
        }

        resetBtn = makeButton(0x88FFFF00.toInt()){
            disableCapture()
            drawView.resetGlyphs()
            enableCapture()
            updateStartButton(true)
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

    private fun enableCapture(){

        if(capturing) return
        capturing = true

        drawParams.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

        updateOverlayView(drawView, drawParams)

        // pequeno delay para estabilizar input
        mainHandler.postDelayed(startCaptureRunnable, 140)
    }

    private fun disableCapture(){

        capturing = false

        drawParams.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

        updateOverlayView(drawView, drawParams)

        drawView.stopCapture()
    }

    override fun onCaptureFinished() {
        disableCapture()
        updateStartButton(false)
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
            stopSelf()
        } catch (_: IllegalArgumentException) {
            stopSelf()
        } catch (_: SecurityException) {
            stopSelf()
        }
    }

    private fun updateOverlayView(view: View, params: WindowManager.LayoutParams) {
        if (!view.isAttachedToWindow) return

        try {
            wm.updateViewLayout(view, params)
        } catch (_: IllegalArgumentException) {
            // The view may have been detached while the service was stopping.
        }
    }

    private fun removeOverlayView(view: View) {
        if (!view.isAttachedToWindow) return

        try {
            wm.removeView(view)
        } catch (_: IllegalArgumentException) {
            // The view may already have been removed by WindowManager.
        }
    }
}
