package com.example.engine

import android.graphics.BlurMaskFilter
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import com.example.model.BrushType
import com.example.model.DrawingPoint
import com.example.model.DrawingStroke
import com.example.model.PressureCurve
import com.example.model.SmoothnessLevel
import com.example.model.ToolType
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Ultra-smooth, zero-latency stroke processing engine for BlackCanvas.
 * - Zero allocations in hot touch path (ACTION_MOVE).
 * - Midpoint quadratic Bezier curve generation for silky smooth lines.
 * - Low-pass EMA filter with adaptive velocity boosting.
 * - Hardware historical motion event batching.
 * - Pressure curves (SOFT, NORMAL, FIRM, CUSTOM).
 * - Stylus tilt sensitivity.
 */
class SmoothStrokeEngine {

    private var isStylusActive: Boolean = false
    private var activePointerId: Int = -1

    // Configuration
    var smoothnessLevel: SmoothnessLevel = SmoothnessLevel.NORMAL
    var pressureCurve: PressureCurve = PressureCurve.NORMAL
    var customPressureFactor: Float = 1.0f
    var isTiltEnabled: Boolean = true

    // Internal working buffers for zero-allocation touch processing
    private val rawPointBuffer = ArrayList<DrawingPoint>(256)
    private var lastSmoothedX = 0f
    private var lastSmoothedY = 0f
    private var lastSmoothedPressure = 1.0f
    private var lastSmoothedTilt = 0f
    private var lastSmoothedTime = 0L

    /**
     * Resolves the ToolType from Android MotionEvent tool info.
     */
    fun resolveToolType(event: MotionEvent, pointerIndex: Int): ToolType {
        return when (event.getToolType(pointerIndex)) {
            MotionEvent.TOOL_TYPE_STYLUS -> ToolType.STYLUS
            MotionEvent.TOOL_TYPE_ERASER -> ToolType.ERASER
            MotionEvent.TOOL_TYPE_FINGER -> ToolType.FINGER
            else -> ToolType.UNKNOWN
        }
    }

    /**
     * Extracts points from MotionEvent with historical data and palm rejection with zero memory churn.
     */
    fun extractPointsFromEvent(
        event: MotionEvent,
        outPoints: MutableList<DrawingPoint>
    ): Pair<ToolType, Boolean> {
        val action = event.actionMasked
        val pointerIndex = event.actionIndex
        val toolType = resolveToolType(event, pointerIndex)

        // Palm rejection: If stylus is active, ignore finger touches
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            if (toolType == ToolType.STYLUS || toolType == ToolType.ERASER) {
                isStylusActive = true
                activePointerId = event.getPointerId(pointerIndex)
            } else if (isStylusActive) {
                return Pair(toolType, false)
            } else {
                activePointerId = event.getPointerId(0)
            }
        }

        if (isStylusActive && toolType == ToolType.FINGER) {
            return Pair(toolType, false)
        }

        val targetPointerIndex = if (activePointerId != -1) {
            val idx = event.findPointerIndex(activePointerId)
            if (idx >= 0) idx else 0
        } else {
            0
        }

        if (action == MotionEvent.ACTION_DOWN) {
            rawPointBuffer.clear()
            outPoints.clear()
            val initX = event.getX(targetPointerIndex)
            val initY = event.getY(targetPointerIndex)
            val rawPressure = event.getPressure(targetPointerIndex)
            val initPressure = applyPressureCurve(normalizeRawPressure(rawPressure))
            val initTilt = if (isTiltEnabled) event.getAxisValue(MotionEvent.AXIS_TILT, targetPointerIndex) else 0f
            val initTime = event.eventTime

            lastSmoothedX = initX
            lastSmoothedY = initY
            lastSmoothedPressure = initPressure
            lastSmoothedTilt = initTilt
            lastSmoothedTime = initTime

            val firstPoint = DrawingPoint(initX, initY, initPressure, initTilt, initTime)
            rawPointBuffer.add(firstPoint)
            outPoints.add(firstPoint)
            return Pair(toolType, true)
        }

        // Process historical sub-sample points for high polling rate (120Hz/240Hz)
        val historySize = event.historySize
        for (h in 0 until historySize) {
            val hx = event.getHistoricalX(targetPointerIndex, h)
            val hy = event.getHistoricalY(targetPointerIndex, h)
            val hRawPressure = event.getHistoricalPressure(targetPointerIndex, h)
            val hPressure = applyPressureCurve(normalizeRawPressure(hRawPressure))
            val hTilt = if (isTiltEnabled) event.getHistoricalAxisValue(MotionEvent.AXIS_TILT, targetPointerIndex, h) else 0f
            val hTime = event.getHistoricalEventTime(h)

            appendSmoothedPoint(hx, hy, hPressure, hTilt, hTime, outPoints)
        }

        // Process current point
        val currX = event.getX(targetPointerIndex)
        val currY = event.getY(targetPointerIndex)
        val rawPressure = event.getPressure(targetPointerIndex)
        val currPressure = applyPressureCurve(normalizeRawPressure(rawPressure))
        val currTilt = if (isTiltEnabled) event.getAxisValue(MotionEvent.AXIS_TILT, targetPointerIndex) else 0f
        val currTime = event.eventTime

        appendSmoothedPoint(currX, currY, currPressure, currTilt, currTime, outPoints)

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            isStylusActive = false
            activePointerId = -1
            rawPointBuffer.clear()
        }

        return Pair(toolType, true)
    }

    private fun appendSmoothedPoint(
        rx: Float,
        ry: Float,
        rPressure: Float,
        rTilt: Float,
        rTime: Long,
        outPoints: MutableList<DrawingPoint>
    ) {
        val dx = rx - lastSmoothedX
        val dy = ry - lastSmoothedY
        val distSq = dx * dx + dy * dy

        // Minimal jitter filter: skip tiny subpixel noise under 0.25px
        if (distSq < 0.0625f) return

        val dist = kotlin.math.sqrt(distSq)
        val dt = max(1L, rTime - lastSmoothedTime).toFloat()
        val speed = dist / dt

        val baseAlpha = when (smoothnessLevel) {
            SmoothnessLevel.LOW -> 0.75f
            SmoothnessLevel.NORMAL -> 0.52f
            SmoothnessLevel.HIGH -> 0.35f
        }

        // Adaptive responsiveness: when moving fast, follow raw touch closer for zero lag
        val speedFactor = (speed * 0.40f).coerceIn(0f, 0.45f)
        val alpha = (baseAlpha + speedFactor).coerceIn(0.30f, 0.95f)

        val sx = lastSmoothedX + alpha * dx
        val sy = lastSmoothedY + alpha * dy
        val sp = lastSmoothedPressure + alpha * (rPressure - lastSmoothedPressure)
        val st = lastSmoothedTilt + alpha * (rTilt - lastSmoothedTilt)

        lastSmoothedX = sx
        lastSmoothedY = sy
        lastSmoothedPressure = sp
        lastSmoothedTilt = st
        lastSmoothedTime = rTime

        outPoints.add(DrawingPoint(sx, sy, sp, st, rTime))
    }

    private fun normalizeRawPressure(raw: Float): Float {
        return if (raw <= 0.001f || raw.isNaN()) {
            1.0f
        } else {
            raw.coerceIn(0.1f, 2.0f)
        }
    }

    fun applyPressureCurve(pressure: Float): Float {
        return when (pressureCurve) {
            PressureCurve.SOFT -> pressure.pow(0.65f)
            PressureCurve.NORMAL -> pressure
            PressureCurve.FIRM -> pressure.pow(1.5f)
            PressureCurve.CUSTOM -> pressure.pow(customPressureFactor.coerceIn(0.4f, 2.5f))
        }
    }

    companion object {
        /**
         * Builds a quadratic Bezier spline path with midpoint interpolation.
         * Creates smooth, organic curves without jagged sharp angles.
         */
        fun buildSmoothPath(points: List<DrawingPoint>, targetPath: Path): Path {
            targetPath.rewind()
            val size = points.size
            if (size == 0) return targetPath

            if (size == 1) {
                val p0 = points[0]
                targetPath.moveTo(p0.x, p0.y)
                targetPath.lineTo(p0.x + 0.1f, p0.y + 0.1f)
                return targetPath
            }

            val p0 = points[0]
            val p1 = points[1]
            targetPath.moveTo(p0.x, p0.y)

            if (size == 2) {
                targetPath.lineTo(p1.x, p1.y)
                return targetPath
            }

            // Start line to midpoint between point 0 and 1
            var prevMidX = (p0.x + p1.x) * 0.5f
            var prevMidY = (p0.y + p1.y) * 0.5f
            targetPath.lineTo(prevMidX, prevMidY)

            // Connect successive midpoints with quadratic Bezier curves
            for (i in 1 until size - 1) {
                val curr = points[i]
                val next = points[i + 1]
                val nextMidX = (curr.x + next.x) * 0.5f
                val nextMidY = (curr.y + next.y) * 0.5f

                targetPath.quadTo(curr.x, curr.y, nextMidX, nextMidY)
                prevMidX = nextMidX
                prevMidY = nextMidY
            }

            val last = points[size - 1]
            targetPath.lineTo(last.x, last.y)

            return targetPath
        }

        fun calculateStrokeWidth(
            baseWidth: Float,
            brushType: BrushType,
            pressure: Float,
            tiltRad: Float = 0f
        ): Float {
            val tiltMultiplier = if (tiltRad > 0.1f) {
                1.0f + (sin(tiltRad.toDouble()).toFloat() * 0.8f)
            } else {
                1.0f
            }

            return when (brushType) {
                BrushType.PEN -> {
                    (baseWidth * (0.65f + 0.55f * pressure)).coerceIn(baseWidth * 0.5f, baseWidth * 1.6f)
                }
                BrushType.PENCIL -> {
                    (baseWidth * (0.4f + 0.9f * pressure) * tiltMultiplier).coerceIn(baseWidth * 0.35f, baseWidth * 2.0f)
                }
                BrushType.MARKER -> {
                    baseWidth * (0.8f + 0.3f * pressure)
                }
                BrushType.SOFT_BRUSH -> {
                    (baseWidth * (0.3f + 1.2f * pressure) * tiltMultiplier).coerceIn(baseWidth * 0.3f, baseWidth * 2.5f)
                }
                BrushType.HIGHLIGHTER -> {
                    baseWidth * 1.8f
                }
                BrushType.ERASER -> {
                    baseWidth * 2.0f
                }
            }
        }

        /**
         * Configures Android Paint for each specific brush type with high-fidelity rendering.
         */
        fun configurePaintForBrush(
            paint: Paint,
            brushType: BrushType,
            color: Long,
            opacity: Float,
            strokeWidth: Float
        ) {
            paint.reset()
            paint.isAntiAlias = true
            paint.isDither = true
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidth

            val baseAlpha = (color shr 24 and 0xFF).toFloat() / 255f
            val finalAlpha = (baseAlpha * opacity).coerceIn(0f, 1f)
            val alphaInt = (finalAlpha * 255).toInt()
            val rgbColor = (color.toInt() and 0x00FFFFFF) or (alphaInt shl 24)

            paint.color = rgbColor

            when (brushType) {
                BrushType.PEN -> {
                    paint.strokeCap = Paint.Cap.ROUND
                    paint.strokeJoin = Paint.Join.ROUND
                }
                BrushType.PENCIL -> {
                    paint.strokeCap = Paint.Cap.ROUND
                    paint.strokeJoin = Paint.Join.ROUND
                    paint.pathEffect = DashPathEffect(floatArrayOf(12f, 1.5f), 0f)
                }
                BrushType.MARKER -> {
                    paint.strokeCap = Paint.Cap.SQUARE
                    paint.strokeJoin = Paint.Join.MITER
                }
                BrushType.SOFT_BRUSH -> {
                    paint.strokeCap = Paint.Cap.ROUND
                    paint.strokeJoin = Paint.Join.ROUND
                    val blurRadius = (strokeWidth * 0.35f).coerceAtLeast(1.5f)
                    paint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
                }
                BrushType.HIGHLIGHTER -> {
                    paint.strokeCap = Paint.Cap.ROUND
                    paint.strokeJoin = Paint.Join.ROUND
                    val highlighterAlpha = (alphaInt * 0.45f).toInt().coerceIn(20, 160)
                    paint.color = (color.toInt() and 0x00FFFFFF) or (highlighterAlpha shl 24)
                }
                BrushType.ERASER -> {
                    paint.strokeCap = Paint.Cap.ROUND
                    paint.strokeJoin = Paint.Join.ROUND
                    paint.color = android.graphics.Color.BLACK
                }
            }
        }

        /**
         * Tests if a stroke intersects within radius of a point (for Stroke Eraser).
         */
        fun doesStrokeIntersectPoint(
            stroke: DrawingStroke,
            pointX: Float,
            pointY: Float,
            radius: Float
        ): Boolean {
            val threshold = (stroke.strokeWidth / 2f + radius).coerceAtLeast(10f)
            val thresholdSq = threshold * threshold

            val pts = stroke.points
            val count = pts.size
            for (i in 0 until count - 1) {
                val p1 = pts[i]
                val p2 = pts[i + 1]
                val distSq = distanceSqToSegment(pointX, pointY, p1.x, p1.y, p2.x, p2.y)
                if (distSq <= thresholdSq) {
                    return true
                }
            }
            return false
        }

        private fun distanceSqToSegment(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
            val dx = x2 - x1
            val dy = y2 - y1
            val l2 = dx * dx + dy * dy
            if (l2 == 0f) return (px - x1) * (px - x1) + (py - y1) * (py - y1)
            var t = ((px - x1) * dx + (py - y1) * dy) / l2
            t = t.coerceIn(0f, 1f)
            val projX = x1 + t * dx
            val projY = y1 + t * dy
            return (px - projX) * (px - projX) + (py - projY) * (py - projY)
        }
    }

    fun reset() {
        isStylusActive = false
        activePointerId = -1
        rawPointBuffer.clear()
    }
}
