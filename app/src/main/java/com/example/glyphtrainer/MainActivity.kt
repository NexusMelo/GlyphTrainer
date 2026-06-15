package com.example.glyphtrainer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import androidx.core.view.WindowCompat
import androidx.core.graphics.withTranslation
import com.OverlayService

class MainActivity : Activity() {
    private val OVERLAY_REQ_CODE = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        checkOverlayPermission()
    }

    private fun checkOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            startOverlay()
        } else {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.fromParts("package", packageName, null)
            startActivityForResult(intent, OVERLAY_REQ_CODE)
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == OVERLAY_REQ_CODE) {
            if (Settings.canDrawOverlays(this)) startOverlay()
            else finish()
        }
    }

    private fun startOverlay() {
        startService(Intent(this, OverlayService::class.java))
        finish()
    }
}

// =====================================================
// ===================== DRAW VIEW ======================
// =====================================================

class DrawView : View {

    private val isProgramMode
        get() = AppMode.currentMode == AppMode.Mode.PROGRAM

    private val nodePaint = Paint().apply {
        color = Color.argb(200, 120, 255, 180)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    // posições normalizadas (0..1) dentro da drawArea
    private val glyphNodes = listOf(

        // (1,1)
        PointF(0.50f, 0.05f),

        // (2,1) (2,2)  ← mais próximos das bordas
        PointF(0.03f, 0.24f),
        PointF(0.97f, 0.24f),

        // (3,1) (3,2)
        PointF(0.32f, 0.37f),
        PointF(0.68f, 0.37f),

        // (4,1)
        PointF(0.50f, 0.50f),

        // (5,1) (5,2)
        PointF(0.32f, 0.63f),
        PointF(0.68f, 0.63f),

        // (6,1) (6,2)  ← mais próximos das bordas
        PointF(0.03f, 0.76f),
        PointF(0.97f, 0.76f),

        // (7,1)
        PointF(0.50f, 0.95f)
    )
    private var horizontalScale = 1f
    private var verticalScale = 1f
    private val visualArea = RectF()
    private val glyphArea = RectF()
    private fun drawNodes(canvas: Canvas){

        val radius = glyphArea.width() * 0.035f

        for(node in glyphNodes){

            val cx = 0.5f
            val cy = 0.5f

            val scaledX = cx + (node.x - cx) * horizontalScale
            val scaledY = cy + (node.y - cy) * verticalScale

            val x = visualArea.left + scaledX * visualArea.width()
            val y = visualArea.top + scaledY * visualArea.height()

            canvas.drawCircle(x, y, radius, nodePaint)
        }
    }

    interface OverlayListener {
        fun onAreaUpdated(area: RectF)
        fun onCaptureFinished()
    }

    private var listener: OverlayListener? = null

    constructor(context: Context) : super(context) { init() }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) { init() }
    constructor(context: Context, listener: OverlayListener) : super(context) {
        this.listener = listener
        init()
    }

    private fun init() {
        setBackgroundColor(Color.TRANSPARENT)
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun isOpaque() = false

    private var touchCount = 0
    private var maxTouches = 5
    private var captureEnabled = false
    private var gestureActive = false

    // 🔥 anti toque acidental
    private val MIN_GESTURE_DISTANCE = 40f
    private var gestureDistance = 0f

    private val MAX_SLOTS = 5
    private val drawArea = RectF()

    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int){
        val margin = 120f

        drawArea.set(
            margin,
            h*0.30f,
            w-margin,
            h*0.78f
        )
        val expand = 60f  // tamanho extra do quadrado verde

        visualArea.set(
            drawArea.left - expand,
            drawArea.top - expand,
            drawArea.right + expand,
            drawArea.bottom + expand
        )
        setWillNotDraw(false)
        // área interna com proporção fixa para o glyph
        val glyphAspect = 1.6f   // altura / largura

        var gWidth = drawArea.width()
        var gHeight = gWidth * glyphAspect

        if (gHeight > drawArea.height()) {
            gHeight = drawArea.height()
            gWidth = gHeight / glyphAspect
        }

        val gx = drawArea.centerX() - gWidth / 2f
        val gy = drawArea.centerY() - gHeight / 2f

        glyphArea.set(
            gx,
            gy,
            gx + gWidth,
            gy + gHeight
        )

        // ajustar espessuras conforme tamanho real
        val base = glyphArea.width()

        paintCore.strokeWidth = base * 0.035f
        paintGlowMid.strokeWidth = base * 0.055f
        paintGlowOuter.strokeWidth = base * 0.08f

        listener?.onAreaUpdated(drawArea)
    }

    // ---------- PAINTS ----------

    private val borderPaint = Paint().apply {
        color = Color.argb(220,0,255,120)
        strokeWidth = 5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.YELLOW
        textSize = 60f
        isAntiAlias = true
    }
    private val slotBorderPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val paintCore = Paint().apply {
        color = Color.rgb(140,255,200)
        strokeWidth = 42f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    private val paintGlowMid = Paint().apply {
        color = Color.rgb(0,255,120)
        strokeWidth = 64f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        maskFilter = BlurMaskFilter(70f, BlurMaskFilter.Blur.NORMAL)
        alpha = 180
    }

    private val paintGlowOuter = Paint().apply {
        color = Color.rgb(0,255,120)
        strokeWidth = 95f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        maskFilter = BlurMaskFilter(130f, BlurMaskFilter.Blur.NORMAL)
        alpha = 90
    }

    private var currentPath = Path()
    private val saved = MutableList<Path?>(MAX_SLOTS){ null }

    private var lastX = 0f
    private var lastY = 0f

    // =====================================================

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {

            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            canvas.drawRect(visualArea, borderPaint)
            drawNodes(canvas)

        val glyphSize = width / 6f
        val spacing = glyphSize * 1.2f
        val startX = 40f

        for (slot in 0 until MAX_SLOTS) {

            val x = startX + slot * spacing
            val y = 120f
            val boxY = y + glyphSize * 0.55f

            // desenhar contorno exatamente na mesma zona do desenho
            canvas.drawRect(
                x,
                boxY,
                x + glyphSize,
                boxY + glyphSize,
                slotBorderPaint
            )

            val path = saved[slot] ?: continue

            canvas.withTranslation(x, y) {
                scale(glyphSize / width, glyphSize / width)

                drawPath(path, paintGlowOuter)
                drawPath(path, paintGlowMid)
                drawPath(path, paintCore)
            }
        }

        if(captureEnabled){
            canvas.drawPath(currentPath, paintGlowOuter)
            canvas.drawPath(currentPath, paintGlowMid)
            canvas.drawPath(currentPath, paintCore)
        }

        canvas.drawText(
            "$touchCount / $maxTouches",
            drawArea.centerX()-40f,
            drawArea.bottom+90f,
            textPaint
        )
        if (isProgramMode) {

            val programPaint = Paint().apply {
                color = Color.WHITE
                textSize = 60f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            canvas.drawText(
                context.getString(R.string.program_mode_label),
                width / 2f,
                drawArea.top - 40f,
                programPaint
            )
        }
    }

    // =====================================================
    // TOUCH SYSTEM
    // =====================================================

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {

        // modo programação não desenha nem captura
        if (AppMode.currentMode != AppMode.Mode.PLAY)
            return true

        if(!captureEnabled) return true
        if(event.pointerCount > 1) return true

        when(event.actionMasked){

            MotionEvent.ACTION_DOWN -> {

                if(!visualArea.contains(event.x,event.y)) return true
                if(gestureActive) return true

                gestureActive = true
                gestureDistance = 0f

                currentPath = Path()
                currentPath.moveTo(event.x,event.y)

                lastX = event.x
                lastY = event.y
            }

            MotionEvent.ACTION_MOVE -> {

                if(!gestureActive) return true

                // 🔥 saiu da zona → termina logo
                if(!visualArea.contains(event.x,event.y) && gestureDistance > MIN_GESTURE_DISTANCE / 2){
                    finishGesture()
                    return true
                }

                val dx = event.x - lastX
                val dy = event.y - lastY
                gestureDistance += hypot(dx,dy)

                val midX = (event.x + lastX)/2
                val midY = (event.y + lastY)/2

                currentPath.quadTo(lastX,lastY,midX,midY)

                lastX = event.x
                lastY = event.y
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                finishGesture()
            }
        }

        invalidate()
        return true
    }

    // =====================================================

    private fun finishGesture() {

        if (!gestureActive) return

        // 🔥 desligar imediatamente para evitar reentrada
        gestureActive = false

        // ignora gestos pequenos
        if (gestureDistance < MIN_GESTURE_DISTANCE) {
            gestureDistance = 0f
            currentPath = Path()
            invalidate()
            return
        }

        if (touchCount < maxTouches) {
            saved[touchCount] = Path(currentPath)
            touchCount++
        }

        // limpeza SEMPRE executada
        gestureDistance = 0f
        currentPath = Path()

        if (touchCount >= maxTouches) {
            captureEnabled = false
            listener?.onCaptureFinished()
        }

        invalidate()
    }

    fun startCapture() {

        if (touchCount >= maxTouches) {
            resetGlyphs()
        }

        // reset completo do estado do gesto
        gestureActive = false
        currentPath = Path()

        captureEnabled = true
        invalidate()
    }

    fun stopCapture() {

        // força limpeza total do estado interno
        captureEnabled = false
        gestureActive = false
        gestureDistance = 0f

        currentPath = Path()

        invalidate()
    }

    fun setGlyphLimit(limit:Int){
        maxTouches = limit
        resetGlyphs()
    }

    fun resetGlyphs(){
        for(i in saved.indices) saved[i]=null
        touchCount=0
        captureEnabled=false
        gestureActive=false
        currentPath=Path()
        invalidate()
    }
    fun adjustHorizontal(direction: Float) {
        horizontalScale += direction * 0.05f
        horizontalScale = horizontalScale.coerceIn(0.5f, 1.8f)
        invalidate()
    }

    fun adjustVertical(direction: Float) {
        verticalScale += direction * 0.05f
        verticalScale = verticalScale.coerceIn(0.5f, 1.8f)
        invalidate()
    }
}
