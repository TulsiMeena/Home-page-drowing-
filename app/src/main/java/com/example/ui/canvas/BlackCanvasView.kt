package com.example.ui.canvas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import com.example.engine.ShapeRecognizer
import com.example.engine.SmoothStrokeEngine
import com.example.engine.TransformEngine
import com.example.model.BrushType
import com.example.model.CanvasBackgroundColor
import com.example.model.DrawingLayer
import com.example.model.DrawingPoint
import com.example.model.DrawingStroke
import com.example.model.EraserMode
import com.example.model.GridMode
import com.example.model.GuideLine
import com.example.model.MainToolMode
import com.example.model.MirrorMode
import com.example.model.PressureCurve
import com.example.model.SelectionBounds
import com.example.model.SelectionMode
import com.example.model.ShapeProperties
import com.example.model.ShapeType
import com.example.model.SmoothnessLevel
import com.example.model.SymmetryMode
import com.example.model.ToolType
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Ultra-low latency, zero-lag GPU-accelerated drawing canvas view for BlackCanvas.
 * - Cached hardware paths for committed strokes (zero path re-building on render).
 * - Real-time quadratic Bezier active stroke drawing.
 * - Hardware frame synchronization & high polling rate motion event processing.
 */
class BlackCanvasView(context: Context) : View(context), Choreographer.FrameCallback {

    private val strokeEngine = SmoothStrokeEngine()
    private val activePoints = ArrayList<DrawingPoint>(256)
    private val pathBuffer = Path()

    // Path cache for zero-allocation rendering of completed strokes
    private val strokePathCache = HashMap<Long, Path>()

    // Layers & Settings
    private var layersList: List<DrawingLayer> = emptyList()
    private var activeLayerId: Long = 0
    private var mainToolMode: MainToolMode = MainToolMode.DRAW
    private var selectionMode: SelectionMode = SelectionMode.RECTANGLE
    private var selectedStrokeIds: Set<Long> = emptySet()

    private var currentBrush: BrushType = BrushType.PEN
    private var currentColor: Long = 0xFFFFFFFF
    private var currentWidth: Float = 4.0f
    private var currentOpacity: Float = 1.0f
    private var currentEraserMode: EraserMode = EraserMode.PIXEL
    private var isStraightLineMode: Boolean = false

    // Shapes Tool State
    private var selectedShapeType: ShapeType = ShapeType.RECTANGLE
    private var currentShapeProperties: ShapeProperties = ShapeProperties()
    private var shapeStartX: Float = 0f
    private var shapeStartY: Float = 0f
    private var shapeEndX: Float = 0f
    private var shapeEndY: Float = 0f
    private var isDraggingShape: Boolean = false

    // Selection Drag & Transform State
    private var selectStartX: Float = 0f
    private var selectStartY: Float = 0f
    private var selectCurrentX: Float = 0f
    private var selectCurrentY: Float = 0f
    private var isSelectingArea: Boolean = false
    private val lassoPoints = ArrayList<DrawingPoint>(128)

    private var isDraggingSelection: Boolean = false
    private var lastSelectionDragX: Float = 0f
    private var lastSelectionDragY: Float = 0f

    // Grid, Guides, Symmetry & Background
    private var gridMode: GridMode = GridMode.OFF
    private var guidesList: List<GuideLine> = emptyList()
    private var mirrorMode: MirrorMode = MirrorMode.NONE
    private var symmetryMode: SymmetryMode = SymmetryMode.NONE
    private var canvasBg: CanvasBackgroundColor = CanvasBackgroundColor.TRANSPARENT
    private var backgroundOpacity: Float = 0.0f
    private var customBgColor: Long = 0xFF12141CL

    // Viewport Navigation Matrix
    private val canvasMatrix = Matrix()
    private val inverseMatrix = Matrix()
    private val touchPointBuffer = FloatArray(2)

    private var viewportScale = 1.0f
    private var viewportTransX = 0.0f
    private var viewportTransY = 0.0f
    private var viewportRotation = 0.0f

    // Two-finger gesture tracking
    private var isNavigating = false
    private var prevSpan = 0f
    private var prevMidX = 0f
    private var prevMidY = 0f
    private var prevAngle = 0f

    // Eraser preview position
    private var eraserPreviewX = -1f
    private var eraserPreviewY = -1f

    // Frame metrics
    private var lastFrameTimeNanos: Long = 0
    private var frameCount: Int = 0
    private var fpsCalculationTime: Long = 0
    var onFpsCalculated: ((Int) -> Unit)? = null

    // Callbacks to ViewModel
    var onStrokeStartListener: ((List<DrawingPoint>, ToolType) -> Unit)? = null
    var onStrokeEndListener: ((List<DrawingPoint>, Float, Float) -> Unit)? = null
    var onStrokeCancelListener: (() -> Unit)? = null
    var onInputDroppedListener: (() -> Unit)? = null
    var onStylusButtonToggleListener: (() -> Unit)? = null
    var onSelectionCompleted: ((Set<Long>) -> Unit)? = null
    var onSelectionMoved: ((Float, Float) -> Unit)? = null
    var onShapeCommitted: ((Float, Float, Float, Float) -> Unit)? = null
    var onFillTapped: ((Float, Float) -> Unit)? = null

    // Specialized Paints
    private val strokePaint = Paint()
    private val activeStrokePaint = Paint()
    private val fillPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = Color.argb(40, 255, 255, 255)
    }

    private val guidePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.argb(160, 0, 229, 255)
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }

    private val symmetryPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.argb(100, 255, 64, 129)
        pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
    }

    private val selectionBoxPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.argb(220, 0, 229, 255)
        pathEffect = DashPathEffect(floatArrayOf(10f, 6f), 0f)
    }

    private val selectionHandlePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.argb(255, 0, 229, 255)
    }

    private val eraserPreviewPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.argb(180, 255, 255, 255)
    }

    private val linePreviewPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.argb(200, 0, 229, 255)
    }

    init {
        setBackgroundColor(Color.TRANSPARENT)
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Choreographer.getInstance().removeFrameCallback(this)
        strokeEngine.reset()
        strokePathCache.clear()
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (lastFrameTimeNanos > 0) {
            frameCount++
            val deltaMs = (frameTimeNanos - fpsCalculationTime) / 1_000_000
            if (deltaMs >= 1000) {
                val fps = (frameCount * 1000 / deltaMs).toInt().coerceIn(1, 144)
                onFpsCalculated?.invoke(fps)
                frameCount = 0
                fpsCalculationTime = frameTimeNanos
            }
        } else {
            fpsCalculationTime = frameTimeNanos
        }
        lastFrameTimeNanos = frameTimeNanos
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun updateLayers(layers: List<DrawingLayer>, activeId: Long) {
        this.layersList = layers
        this.activeLayerId = activeId

        // Clean up cached paths for deleted strokes
        val currentIds = HashSet<Long>()
        for (l in layers) {
            for (s in l.strokes) {
                currentIds.add(s.id)
            }
        }
        strokePathCache.keys.retainAll(currentIds)

        invalidate()
    }

    fun updateToolMode(mode: MainToolMode, selectionMode: SelectionMode, selectedIds: Set<Long>) {
        this.mainToolMode = mode
        this.selectionMode = selectionMode
        this.selectedStrokeIds = selectedIds
        invalidate()
    }

    fun updateShapeTool(type: ShapeType, properties: ShapeProperties) {
        this.selectedShapeType = type
        this.currentShapeProperties = properties
    }

    fun updateGridAndGuides(
        grid: GridMode,
        guides: List<GuideLine>,
        mirror: MirrorMode,
        symmetry: SymmetryMode,
        bg: CanvasBackgroundColor,
        bgOpacity: Float = 0.0f,
        customBg: Long = 0xFF12141CL
    ) {
        this.gridMode = grid
        this.guidesList = guides
        this.mirrorMode = mirror
        this.symmetryMode = symmetry
        this.canvasBg = bg
        this.backgroundOpacity = bgOpacity
        this.customBgColor = customBg
        invalidate()
    }

    fun updateBrushConfig(
        brush: BrushType,
        color: Long,
        width: Float,
        opacity: Float,
        eraserMode: EraserMode,
        straightLineMode: Boolean
    ) {
        this.currentBrush = brush
        this.currentColor = color
        this.currentWidth = width
        this.currentOpacity = opacity
        this.currentEraserMode = eraserMode
        this.isStraightLineMode = straightLineMode
        invalidate()
    }

    fun updateEngineSettings(
        smoothness: SmoothnessLevel,
        pressureCurve: PressureCurve,
        customFactor: Float,
        tiltEnabled: Boolean
    ) {
        strokeEngine.smoothnessLevel = smoothness
        strokeEngine.pressureCurve = pressureCurve
        strokeEngine.customPressureFactor = customFactor
        strokeEngine.isTiltEnabled = tiltEnabled
    }

    fun resetView() {
        viewportScale = 1.0f
        viewportTransX = 0.0f
        viewportTransY = 0.0f
        viewportRotation = 0.0f
        updateMatrix()
        invalidate()
    }

    fun smartCenterArtwork() {
        val visibleStrokes = layersList.filter { it.isVisible }.flatMap { it.strokes }
        val bounds = TransformEngine.computeBounds(visibleStrokes) ?: run {
            resetView()
            return
        }

        val canvasW = width.toFloat()
        val canvasH = height.toFloat()
        val padding = 80f

        val scaleX = (canvasW - padding * 2) / bounds.width.coerceAtLeast(10f)
        val scaleY = (canvasH - padding * 2) / bounds.height.coerceAtLeast(10f)
        viewportScale = min(scaleX, scaleY).coerceIn(0.6f, 3.0f)
        viewportRotation = 0f
        viewportTransX = (canvasW / 2f) - bounds.centerX
        viewportTransY = (canvasH / 2f) - bounds.centerY
        updateMatrix()
        invalidate()
    }

    private fun updateMatrix() {
        canvasMatrix.reset()
        val cx = width / 2f
        val cy = height / 2f
        canvasMatrix.postTranslate(-cx, -cy)
        canvasMatrix.postRotate(viewportRotation)
        canvasMatrix.postScale(viewportScale, viewportScale)
        canvasMatrix.postTranslate(cx + viewportTransX, cy + viewportTransY)
        canvasMatrix.invert(inverseMatrix)
    }

    private fun mapScreenToWorld(screenX: Float, screenY: Float): Pair<Float, Float> {
        touchPointBuffer[0] = screenX
        touchPointBuffer[1] = screenY
        inverseMatrix.mapPoints(touchPointBuffer)
        return Pair(touchPointBuffer[0], touchPointBuffer[1])
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateMatrix()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.buttonState == MotionEvent.BUTTON_STYLUS_PRIMARY ||
            event.buttonState == MotionEvent.BUTTON_SECONDARY
        ) {
            onStylusButtonToggleListener?.invoke()
        }

        // Two-Finger Gesture for Viewport Navigation (Pinch Zoom / Pan / Rotate)
        if (event.pointerCount >= 2) {
            if (!isNavigating) {
                if (activePoints.isNotEmpty()) {
                    onStrokeCancelListener?.invoke()
                    activePoints.clear()
                }
                isSelectingArea = false
                isDraggingShape = false
                isDraggingSelection = false
                isNavigating = true
            }
            handleTwoFingerGesture(event)
            invalidate()
            return true
        } else if (isNavigating && event.actionMasked == MotionEvent.ACTION_UP) {
            isNavigating = false
            return true
        } else if (isNavigating && event.pointerCount < 2) {
            isNavigating = false
        }

        val (wx, wy) = mapScreenToWorld(event.x, event.y)

        // 1. FILL TOOL MODE
        if (mainToolMode == MainToolMode.FILL) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                onFillTapped?.invoke(wx, wy)
            }
            return true
        }

        // 2. SHAPES TOOL MODE
        if (mainToolMode == MainToolMode.SHAPES) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    shapeStartX = wx
                    shapeStartY = wy
                    shapeEndX = wx
                    shapeEndY = wy
                    isDraggingShape = true
                    invalidate()
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDraggingShape) {
                        shapeEndX = wx
                        shapeEndY = wy
                        invalidate()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (isDraggingShape) {
                        isDraggingShape = false
                        onShapeCommitted?.invoke(shapeStartX, shapeStartY, wx, wy)
                        invalidate()
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    isDraggingShape = false
                    invalidate()
                }
            }
            return true
        }

        // 3. SELECTION TOOL MODE
        if (mainToolMode == MainToolMode.SELECT) {
            val activeLayer = layersList.find { it.id == activeLayerId }
            val selectedStrokes = activeLayer?.strokes?.filter { selectedStrokeIds.contains(it.id) } ?: emptyList()
            val bounds = TransformEngine.computeBounds(selectedStrokes)

            // If touching inside existing selection bounds -> drag to move
            if (event.actionMasked == MotionEvent.ACTION_DOWN && bounds != null &&
                wx in bounds.minX..bounds.maxX && wy in bounds.minY..bounds.maxY
            ) {
                isDraggingSelection = true
                lastSelectionDragX = wx
                lastSelectionDragY = wy
                return true
            }

            if (isDraggingSelection) {
                if (event.actionMasked == MotionEvent.ACTION_MOVE) {
                    val dx = wx - lastSelectionDragX
                    val dy = wy - lastSelectionDragY
                    onSelectionMoved?.invoke(dx, dy)
                    lastSelectionDragX = wx
                    lastSelectionDragY = wy
                    invalidate()
                } else if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                    isDraggingSelection = false
                }
                return true
            }

            // Otherwise, area selection (Rectangle or Lasso)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    selectStartX = wx
                    selectStartY = wy
                    selectCurrentX = wx
                    selectCurrentY = wy
                    isSelectingArea = true
                    lassoPoints.clear()
                    lassoPoints.add(DrawingPoint(wx, wy))
                    invalidate()
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isSelectingArea) {
                        selectCurrentX = wx
                        selectCurrentY = wy
                        lassoPoints.add(DrawingPoint(wx, wy))
                        invalidate()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (isSelectingArea) {
                        isSelectingArea = false
                        completeAreaSelection(activeLayer)
                        lassoPoints.clear()
                        invalidate()
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    isSelectingArea = false
                    lassoPoints.clear()
                    invalidate()
                }
            }
            return true
        }

        // 4. ZERO-LATENCY FREEHAND DRAWING MODE
        val (toolType, accepted) = strokeEngine.extractPointsFromEvent(event, activePoints)
        if (!accepted) {
            onInputDroppedListener?.invoke()
            return true
        }

        if (currentBrush == BrushType.ERASER || toolType == ToolType.ERASER) {
            val lastP = activePoints.lastOrNull()
            if (lastP != null) {
                val (ewx, ewy) = mapScreenToWorld(lastP.x, lastP.y)
                eraserPreviewX = ewx
                eraserPreviewY = ewy
            }
        } else {
            eraserPreviewX = -1f
            eraserPreviewY = -1f
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val worldPoints = activePoints.map { p ->
                    val (pwx, pwy) = mapScreenToWorld(p.x, p.y)
                    p.copy(x = pwx, y = pwy)
                }
                onStrokeStartListener?.invoke(worldPoints, toolType)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                // Direct GPU invalidate - no Compose StateFlow hopping during active touch movement
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val worldPoints = activePoints.map { p ->
                    val (pwx, pwy) = mapScreenToWorld(p.x, p.y)
                    p.copy(x = pwx, y = pwy)
                }
                val cx = width / 2f
                val cy = height / 2f
                val (wcx, wcy) = mapScreenToWorld(cx, cy)
                onStrokeEndListener?.invoke(worldPoints, wcx, wcy)
                activePoints.clear()
                eraserPreviewX = -1f
                eraserPreviewY = -1f
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                onStrokeCancelListener?.invoke()
                activePoints.clear()
                eraserPreviewX = -1f
                eraserPreviewY = -1f
                invalidate()
            }
        }
        return true
    }

    private fun completeAreaSelection(activeLayer: DrawingLayer?) {
        if (activeLayer == null) return
        val matchedIds = mutableSetOf<Long>()

        if (selectionMode == SelectionMode.RECTANGLE) {
            val minX = min(selectStartX, selectCurrentX)
            val maxX = max(selectStartX, selectCurrentX)
            val minY = min(selectStartY, selectCurrentY)
            val maxY = max(selectStartY, selectCurrentY)

            for (stroke in activeLayer.strokes) {
                if (stroke.points.any { p -> p.x in minX..maxX && p.y in minY..maxY }) {
                    matchedIds.add(stroke.id)
                }
            }
        } else if (selectionMode == SelectionMode.LASSO && lassoPoints.size > 2) {
            for (stroke in activeLayer.strokes) {
                if (stroke.points.any { p -> ShapeRecognizer.isPointInsideClosedPolygon(p.x, p.y, lassoPoints) }) {
                    matchedIds.add(stroke.id)
                }
            }
        } else {
            // Single stroke / Multi stroke tap
            for (stroke in activeLayer.strokes.reversed()) {
                if (SmoothStrokeEngine.doesStrokeIntersectPoint(stroke, selectStartX, selectStartY, radius = 24f)) {
                    matchedIds.add(stroke.id)
                    break
                }
            }
        }

        onSelectionCompleted?.invoke(matchedIds)
    }

    private fun handleTwoFingerGesture(event: MotionEvent) {
        val x0 = event.getX(0)
        val y0 = event.getY(0)
        val x1 = event.getX(1)
        val y1 = event.getY(1)

        val span = hypot(x1 - x0, y1 - y0)
        val midX = (x0 + x1) / 2f
        val midY = (y0 + y1) / 2f
        val angle = Math.toDegrees(atan2((y1 - y0).toDouble(), (x1 - x0).toDouble())).toFloat()

        if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN || prevSpan == 0f) {
            prevSpan = span
            prevMidX = midX
            prevMidY = midY
            prevAngle = angle
            return
        }

        if (prevSpan > 0f) {
            val scaleFactor = span / prevSpan
            viewportScale = (viewportScale * scaleFactor).coerceIn(0.4f, 8.0f)

            val dx = midX - prevMidX
            val dy = midY - prevMidY
            viewportTransX += dx
            viewportTransY += dy

            val dAngle = angle - prevAngle
            if (Math.abs(dAngle) < 30f) {
                viewportRotation += dAngle
            }
            updateMatrix()
        }

        prevSpan = span
        prevMidX = midX
        prevMidY = midY
        prevAngle = angle
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Draw Canvas Background with smooth opacity support
        if (canvasBg == CanvasBackgroundColor.TRANSPARENT || backgroundOpacity <= 0.001f) {
            canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
        } else {
            val baseColor = when (canvasBg) {
                CanvasBackgroundColor.TRANSPARENT -> 0x00000000
                CanvasBackgroundColor.BLACK -> 0xFF000000.toInt()
                CanvasBackgroundColor.DARK_GRAY -> Color.rgb(30, 32, 40)
                CanvasBackgroundColor.DARK_BLUE -> Color.rgb(13, 27, 42)
                CanvasBackgroundColor.DARK_PURPLE -> Color.rgb(27, 10, 42)
                CanvasBackgroundColor.WHITE -> Color.WHITE
                CanvasBackgroundColor.CUSTOM -> customBgColor.toInt()
            }
            val alpha = (backgroundOpacity * 255).toInt().coerceIn(0, 255)
            val finalColor = (baseColor and 0x00FFFFFF) or (alpha shl 24)
            canvas.drawColor(finalColor)
        }

        canvas.save()
        canvas.concat(canvasMatrix)

        // 2. Visual Grid
        drawGrid(canvas)

        // 3. Symmetry & Mirror Guide Planes
        drawSymmetryPlanes(canvas)

        // 4. User Guide Lines
        drawGuides(canvas)

        // 5. Render Layers & Strokes from cached Path objects (Zero GC & fast GPU dispatch)
        for (layer in layersList) {
            if (!layer.isVisible) continue

            for (stroke in layer.strokes) {
                if (stroke.points.size < 2) continue

                // Retrieve or build cached Path for this completed stroke
                val cachedPath = strokePathCache.getOrPut(stroke.id) {
                    val p = Path()
                    SmoothStrokeEngine.buildSmoothPath(stroke.points, p)
                    p
                }

                // Fill if present
                if (stroke.isClosed && stroke.fillColor != 0x00000000L && stroke.fillOpacity > 0f) {
                    val fillAlpha = ((stroke.fillColor shr 24 and 0xFF) * stroke.fillOpacity * layer.opacity / 255f).toInt().coerceIn(0, 255)
                    fillPaint.color = (stroke.fillColor.toInt() and 0x00FFFFFF) or (fillAlpha shl 24)
                    canvas.drawPath(cachedPath, fillPaint)
                }

                // Stroke
                SmoothStrokeEngine.configurePaintForBrush(
                    paint = strokePaint,
                    brushType = stroke.brushType,
                    color = stroke.color,
                    opacity = stroke.opacity * layer.opacity,
                    strokeWidth = stroke.strokeWidth
                )
                canvas.drawPath(cachedPath, strokePaint)
            }
        }

        // 6. Draw In-Flight Active Stroke (Drawing Mode) with live smooth spline
        if (mainToolMode == MainToolMode.DRAW) {
            drawActiveStroke(canvas)
        }

        // 7. Shape Drag Live Preview
        if (mainToolMode == MainToolMode.SHAPES && isDraggingShape) {
            drawShapePreview(canvas)
        }

        // 8. Selection Bounding Box & Handles
        drawSelectionOverlay(canvas)

        // 9. Eraser Preview
        if (eraserPreviewX >= 0f && eraserPreviewY >= 0f) {
            val previewRadius = (currentWidth * 2.0f) / 2f
            canvas.drawCircle(eraserPreviewX, eraserPreviewY, previewRadius, eraserPreviewPaint)
        }

        canvas.restore()
    }

    private fun drawGrid(canvas: Canvas) {
        if (gridMode == GridMode.OFF) return
        val step = if (gridMode == GridMode.SMALL) 24f else 48f
        val extent = 4000f

        var x = -extent
        while (x <= extent) {
            canvas.drawLine(x, -extent, x, extent, gridPaint)
            x += step
        }
        var y = -extent
        while (y <= extent) {
            canvas.drawLine(-extent, y, extent, y, gridPaint)
            y += step
        }
    }

    private fun drawSymmetryPlanes(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val (wcx, wcy) = mapScreenToWorld(cx, cy)
        val extent = 4000f

        if (mirrorMode == MirrorMode.HORIZONTAL || mirrorMode == MirrorMode.BOTH) {
            canvas.drawLine(wcx, -extent, wcx, extent, symmetryPaint)
        }
        if (mirrorMode == MirrorMode.VERTICAL || mirrorMode == MirrorMode.BOTH) {
            canvas.drawLine(-extent, wcy, extent, wcy, symmetryPaint)
        }
        if (symmetryMode != SymmetryMode.NONE) {
            val folds = when (symmetryMode) {
                SymmetryMode.TWO_WAY -> 2
                SymmetryMode.FOUR_WAY -> 4
                SymmetryMode.SIX_WAY -> 6
                SymmetryMode.EIGHT_WAY -> 8
                else -> 1
            }
            val angleStep = 360f / folds
            for (f in 0 until folds) {
                val rad = Math.toRadians((angleStep * f).toDouble()).toFloat()
                canvas.drawLine(wcx, wcy, wcx + extent * kotlin.math.cos(rad), wcy + extent * kotlin.math.sin(rad), symmetryPaint)
            }
        }
    }

    private fun drawGuides(canvas: Canvas) {
        val extent = 4000f
        for (g in guidesList) {
            if (g.isHorizontal) {
                canvas.drawLine(-extent, g.position, extent, g.position, guidePaint)
            } else {
                canvas.drawLine(g.position, -extent, g.position, extent, guidePaint)
            }
        }
    }

    private fun drawActiveStroke(canvas: Canvas) {
        val pointCount = activePoints.size
        if (pointCount >= 2) {
            val worldPoints = activePoints.map { p ->
                val (pwx, pwy) = mapScreenToWorld(p.x, p.y)
                p.copy(x = pwx, y = pwy)
            }

            val isEraser = currentBrush == BrushType.ERASER
            val latestPressure = worldPoints.lastOrNull()?.pressure ?: 1.0f
            val latestTilt = worldPoints.lastOrNull()?.tiltRad ?: 0.0f
            val effectiveWidth = SmoothStrokeEngine.calculateStrokeWidth(
                currentWidth,
                currentBrush,
                latestPressure,
                latestTilt
            )

            SmoothStrokeEngine.configurePaintForBrush(
                paint = activeStrokePaint,
                brushType = currentBrush,
                color = if (isEraser) 0xFF000000 else currentColor,
                opacity = if (isEraser) 1.0f else currentOpacity,
                strokeWidth = effectiveWidth
            )

            if (isStraightLineMode) {
                val p0 = worldPoints.first()
                val p1 = worldPoints.last()
                linePreviewPaint.strokeWidth = effectiveWidth
                canvas.drawLine(p0.x, p0.y, p1.x, p1.y, linePreviewPaint)
            } else {
                SmoothStrokeEngine.buildSmoothPath(worldPoints, pathBuffer)
                canvas.drawPath(pathBuffer, activeStrokePaint)
            }
        }
    }

    private fun drawShapePreview(canvas: Canvas) {
        val previewPoints = ShapeRecognizer.generateShapePoints(
            shapeType = selectedShapeType,
            x1 = shapeStartX,
            y1 = shapeStartY,
            x2 = shapeEndX,
            y2 = shapeEndY,
            properties = currentShapeProperties
        )
        if (previewPoints.size >= 2) {
            SmoothStrokeEngine.buildSmoothPath(previewPoints, pathBuffer)

            if (currentShapeProperties.fillColor != 0x00000000L && currentShapeProperties.fillOpacity > 0f) {
                fillPaint.color = currentShapeProperties.fillColor.toInt()
                canvas.drawPath(pathBuffer, fillPaint)
            }

            linePreviewPaint.strokeWidth = currentShapeProperties.strokeWidth
            linePreviewPaint.color = currentShapeProperties.strokeColor.toInt()
            canvas.drawPath(pathBuffer, linePreviewPaint)
        }
    }

    private fun drawSelectionOverlay(canvas: Canvas) {
        val activeLayer = layersList.find { it.id == activeLayerId }
        val selectedStrokes = activeLayer?.strokes?.filter { selectedStrokeIds.contains(it.id) } ?: emptyList()
        val bounds = TransformEngine.computeBounds(selectedStrokes)

        // Draw selection box around selected strokes
        if (bounds != null && selectedStrokes.isNotEmpty()) {
            canvas.drawRect(bounds.minX, bounds.minY, bounds.maxX, bounds.maxY, selectionBoxPaint)

            // Draw corner handles
            val handleRadius = 6f
            canvas.drawCircle(bounds.minX, bounds.minY, handleRadius, selectionHandlePaint)
            canvas.drawCircle(bounds.maxX, bounds.minY, handleRadius, selectionHandlePaint)
            canvas.drawCircle(bounds.minX, bounds.maxY, handleRadius, selectionHandlePaint)
            canvas.drawCircle(bounds.maxX, bounds.maxY, handleRadius, selectionHandlePaint)
            // Rotation handle on top center
            canvas.drawCircle(bounds.centerX, bounds.minY - 20f, handleRadius, selectionHandlePaint)
            canvas.drawLine(bounds.centerX, bounds.minY, bounds.centerX, bounds.minY - 20f, selectionBoxPaint)
        }

        // Draw active area selection preview (Marquee or Lasso)
        if (isSelectingArea) {
            if (selectionMode == SelectionMode.RECTANGLE) {
                val minX = min(selectStartX, selectCurrentX)
                val maxX = max(selectStartX, selectCurrentX)
                val minY = min(selectStartY, selectCurrentY)
                val maxY = max(selectStartY, selectCurrentY)
                canvas.drawRect(minX, minY, maxX, maxY, selectionBoxPaint)
            } else if (selectionMode == SelectionMode.LASSO && lassoPoints.size > 1) {
                SmoothStrokeEngine.buildSmoothPath(lassoPoints, pathBuffer)
                canvas.drawPath(pathBuffer, selectionBoxPaint)
            }
        }
    }
}
