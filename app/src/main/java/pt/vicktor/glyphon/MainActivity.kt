package pt.vicktor.glyphon

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
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

class MainActivity : Activity() {
    private val OVERLAY_REQ_CODE = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        val hasOverlayPermission = Settings.canDrawOverlays(this)
        if (!hasOverlayPermission) {
            setTheme(R.style.Theme_GlyphTrainer_Permission)
        }

        super.onCreate(savedInstanceState)

        if (intent?.action == Intent.ACTION_MAIN) {
            AppMode.currentMode = AppMode.Mode.PLAY
        }

        if (hasOverlayPermission) {
            startOverlay()
        } else {
            showOverlayPermissionExplanation()
        }
    }

    private fun showOverlayPermissionExplanation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.overlay_permission_title)
            .setMessage(R.string.overlay_permission_message)
            .setPositiveButton(R.string.action_continue) { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivityForResult(intent, OVERLAY_REQ_CODE)
            }
            .setNegativeButton(R.string.action_cancel) { _, _ -> finish() }
            .setCancelable(false)
            .show()
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
    private val captureAreaBounds = RectF()
    private val glyphArea = RectF()
    private val previewBoxBounds = RectF()
    private val pathTransform = Matrix()
    private val renderPath = Path()
    private fun drawNodes(canvas: Canvas){

        val radius = glyphArea.width() * 0.035f

        for(node in glyphNodes){

            val cx = 0.5f
            val cy = 0.5f

            val scaledX = cx + (node.x - cx) * horizontalScale
            val scaledY = cy + (node.y - cy) * verticalScale

            val x = captureAreaBounds.left + scaledX * captureAreaBounds.width()
            val y = captureAreaBounds.top + scaledY * captureAreaBounds.height()

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
    private var completedSequenceVisible = false
    private var goVisible = false
    private var replayGlyphIndex: Int? = null

    // 🔥 anti toque acidental
    private val MIN_GESTURE_DISTANCE = 40f
    private var gestureDistance = 0f

    private val MAX_SLOTS = 5
    private val MAIN_OVERLAY_VERTICAL_OFFSET = 100f
    private val GLYPH_BOX_VERTICAL_OFFSET = 60f
    private val STATIC_OVERLAY_COMPENSATION = 60f
    private val drawArea = RectF()
    private var stableLayoutHeight = 0

    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int){
        val margin = 120f
        if (stableLayoutHeight == 0) {
            stableLayoutHeight = h
        }
        val layoutHeight = minOf(h, stableLayoutHeight)

        drawArea.set(
            margin,
            layoutHeight*0.30f + MAIN_OVERLAY_VERTICAL_OFFSET,
            w-margin,
            layoutHeight*0.78f + MAIN_OVERLAY_VERTICAL_OFFSET
        )
        val expand = 60f  // tamanho extra do quadrado verde

        captureAreaBounds.set(
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
        color = AppThemeConfig.colors(AppThemeConfig.DEFAULT_THEME).outline
        strokeWidth = 5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.YELLOW
        textSize = 60f
        isAntiAlias = true
    }
    private val goPaint = Paint().apply {
        color = Color.YELLOW
        textSize = 360f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private val programPaint = Paint().apply {
        color = Color.WHITE
        textSize = 60f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private val slotBorderPaint = Paint().apply {
        color = AppThemeConfig.colors(AppThemeConfig.DEFAULT_THEME).accent
        style = Paint.Style.STROKE
        strokeWidth = 5.2f
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
            canvas.drawRect(captureAreaBounds, borderPaint)

        replayGlyphIndex
            ?.let(saved::getOrNull)
            ?.let { path ->
                drawSavedPath(canvas, path, captureAreaBounds)
            }

        val glyphSize = width / 6f
        val spacing = glyphSize * 1.2f
        val groupWidth = glyphSize + spacing * (maxTouches - 1)
        val startX = if (maxTouches == MAX_SLOTS) 40f else (width - groupWidth) / 2f

        for (slot in 0 until maxTouches) {

            val x = startX + slot * spacing
            val y = 120f + GLYPH_BOX_VERTICAL_OFFSET
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

            previewBoxBounds.set(x, boxY, x + glyphSize, boxY + glyphSize)
            drawSavedPath(canvas, path, previewBoxBounds)
        }

        if(captureEnabled){
            canvas.drawPath(currentPath, paintGlowOuter)
            canvas.drawPath(currentPath, paintGlowMid)
            canvas.drawPath(currentPath, paintCore)
        }

        canvas.drawText(
            "$touchCount / $maxTouches",
            drawArea.centerX()-40f,
            drawArea.bottom+90f-STATIC_OVERLAY_COMPENSATION,
            textPaint
        )
        if (goVisible) {
            canvas.drawText(
                "GO",
                drawArea.centerX(),
                drawArea.centerY()-STATIC_OVERLAY_COMPENSATION,
                goPaint
            )
        }
        if (isProgramMode) {
            canvas.drawText(
                context.getString(R.string.program_mode_label),
                width / 2f,
                drawArea.top - 40f,
                programPaint
            )
        }
    }

    private fun drawSavedPath(canvas: Canvas, normalizedPath: Path, target: RectF) {
        if (captureAreaBounds.isEmpty || target.isEmpty) return

        pathTransform.reset()
        pathTransform.setScale(target.width(), target.height())
        pathTransform.postTranslate(target.left, target.top)
        renderPath.reset()
        normalizedPath.transform(pathTransform, renderPath)

        val strokeScale = target.width() / captureAreaBounds.width()
        drawPathWithStrokeScale(canvas, renderPath, strokeScale)
    }

    private fun drawPathWithStrokeScale(canvas: Canvas, path: Path, strokeScale: Float) {
        val coreStroke = paintCore.strokeWidth
        val midStroke = paintGlowMid.strokeWidth
        val outerStroke = paintGlowOuter.strokeWidth

        paintCore.strokeWidth = coreStroke * strokeScale
        paintGlowMid.strokeWidth = midStroke * strokeScale
        paintGlowOuter.strokeWidth = outerStroke * strokeScale

        canvas.drawPath(path, paintGlowOuter)
        canvas.drawPath(path, paintGlowMid)
        canvas.drawPath(path, paintCore)

        paintCore.strokeWidth = coreStroke
        paintGlowMid.strokeWidth = midStroke
        paintGlowOuter.strokeWidth = outerStroke
    }

    private fun normalizedCurrentPath(): Path {
        pathTransform.reset()
        pathTransform.setTranslate(-captureAreaBounds.left, -captureAreaBounds.top)
        pathTransform.postScale(
            1f / captureAreaBounds.width(),
            1f / captureAreaBounds.height()
        )

        return Path(currentPath).apply {
            transform(pathTransform)
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

                if(!captureAreaBounds.contains(event.x,event.y)) return true
                if(gestureActive) return true

                gestureActive = true
                gestureDistance = 0f

                currentPath.reset()
                currentPath.moveTo(event.x,event.y)

                lastX = event.x
                lastY = event.y
            }

            MotionEvent.ACTION_MOVE -> {

                if(!gestureActive) return true

                // 🔥 saiu da zona → termina logo
                if(!captureAreaBounds.contains(event.x,event.y) && gestureDistance > MIN_GESTURE_DISTANCE / 2){
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
            currentPath.reset()
            invalidate()
            return
        }

        if (touchCount < maxTouches) {
            saved[touchCount] = normalizedCurrentPath()
            touchCount++
        }

        // limpeza SEMPRE executada
        gestureDistance = 0f
        currentPath.reset()

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
        completedSequenceVisible = false
        goVisible = false
        replayGlyphIndex = null
        gestureActive = false
        currentPath.reset()

        captureEnabled = true
        invalidate()
    }

    fun stopCapture() {

        // força limpeza total do estado interno
        captureEnabled = false
        gestureActive = false
        gestureDistance = 0f

        currentPath.reset()

        invalidate()
    }

    fun setGlyphLimit(limit:Int){
        maxTouches = limit
        resetGlyphs()
    }

    fun setGlyphScales(horizontal: Float, vertical: Float) {
        horizontalScale = horizontal.coerceIn(0.5f, 1.8f)
        verticalScale = vertical.coerceIn(0.5f, 1.8f)
        invalidate()
    }

    fun setAppColorTheme(theme: AppColorTheme) {
        val colors = AppThemeConfig.colors(theme)
        borderPaint.color = colors.outline
        slotBorderPaint.color = colors.accent
        invalidate()
    }

    fun showCompletedSequence() {
        if (touchCount < maxTouches) return

        completedSequenceVisible = true
        invalidate()
    }

    fun showGoMessage() {
        if (touchCount < maxTouches) return

        goVisible = true
        invalidate()
    }

    fun hideCompletedSequence() {
        if (!completedSequenceVisible && !goVisible) return

        completedSequenceVisible = false
        goVisible = false
        invalidate()
    }

    fun showReplayGlyph(index: Int) {
        if (touchCount < maxTouches || saved.getOrNull(index) == null) return

        goVisible = false
        replayGlyphIndex = index
        invalidate()
    }

    fun clearReplayGlyph() {
        if (replayGlyphIndex == null) return

        replayGlyphIndex = null
        invalidate()
    }

    fun clearGoMessage() {
        if (!goVisible) return

        goVisible = false
        invalidate()
    }

    fun resetGlyphs(){
        for(i in saved.indices) saved[i]=null
        touchCount=0
        captureEnabled=false
        gestureActive=false
        completedSequenceVisible=false
        goVisible=false
        replayGlyphIndex=null
        currentPath.reset()
        invalidate()
    }
    fun adjustHorizontal(direction: Float): Float {
        horizontalScale += direction * 0.05f
        horizontalScale = horizontalScale.coerceIn(0.5f, 1.8f)
        invalidate()
        return horizontalScale
    }

    fun adjustVertical(direction: Float): Float {
        verticalScale += direction * 0.05f
        verticalScale = verticalScale.coerceIn(0.5f, 1.8f)
        invalidate()
        return verticalScale
    }
}
