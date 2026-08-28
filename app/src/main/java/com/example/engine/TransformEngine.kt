package com.example.engine

import com.example.model.AlignmentType
import com.example.model.DrawingPoint
import com.example.model.DrawingStroke
import com.example.model.MirrorMode
import com.example.model.SelectionBounds
import com.example.model.SymmetryMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object TransformEngine {

    /**
     * Calculates the bounding box enclosing a collection of vector strokes.
     */
    fun computeBounds(strokes: List<DrawingStroke>): SelectionBounds? {
        if (strokes.isEmpty()) return null
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var hasPoints = false

        for (stroke in strokes) {
            for (p in stroke.points) {
                hasPoints = true
                if (p.x < minX) minX = p.x
                if (p.x > maxX) maxX = p.x
                if (p.y < minY) minY = p.y
                if (p.y > maxY) maxY = p.y
            }
        }

        if (!hasPoints) return null
        // Add small padding for single dots/lines
        if (maxX - minX < 2f) { minX -= 10f; maxX += 10f }
        if (maxY - minY < 2f) { minY -= 10f; maxY += 10f }

        return SelectionBounds(minX, minY, maxX, maxY)
    }

    /**
     * Translates strokes by dx, dy.
     */
    fun translateStrokes(strokes: List<DrawingStroke>, dx: Float, dy: Float): List<DrawingStroke> {
        return strokes.map { stroke ->
            stroke.copy(
                points = stroke.points.map { p ->
                    p.copy(x = p.x + dx, y = p.y + dy)
                }
            )
        }
    }

    /**
     * Scales strokes relative to a pivot point.
     */
    fun scaleStrokes(strokes: List<DrawingStroke>, pivotX: Float, pivotY: Float, scaleX: Float, scaleY: Float): List<DrawingStroke> {
        return strokes.map { stroke ->
            stroke.copy(
                strokeWidth = (stroke.strokeWidth * ((scaleX + scaleY) / 2f).coerceAtLeast(0.1f)).coerceIn(1f, 120f),
                points = stroke.points.map { p ->
                    p.copy(
                        x = pivotX + (p.x - pivotX) * scaleX,
                        y = pivotY + (p.y - pivotY) * scaleY
                    )
                }
            )
        }
    }

    /**
     * Rotates strokes around a pivot point by degrees.
     */
    fun rotateStrokes(strokes: List<DrawingStroke>, pivotX: Float, pivotY: Float, degrees: Float): List<DrawingStroke> {
        val rad = Math.toRadians(degrees.toDouble()).toFloat()
        val cosT = cos(rad)
        val sinT = sin(rad)

        return strokes.map { stroke ->
            stroke.copy(
                points = stroke.points.map { p ->
                    val relX = p.x - pivotX
                    val relY = p.y - pivotY
                    p.copy(
                        x = pivotX + (relX * cosT - relY * sinT),
                        y = pivotY + (relX * sinT + relY * cosT)
                    )
                }
            )
        }
    }

    /**
     * Aligns selected strokes to bounding container or alignment anchor.
     */
    fun alignStrokes(strokes: List<DrawingStroke>, alignmentType: AlignmentType): List<DrawingStroke> {
        val overallBounds = computeBounds(strokes) ?: return strokes
        return strokes.map { stroke ->
            val strokeBounds = computeBounds(listOf(stroke)) ?: return@map stroke
            val dx = when (alignmentType) {
                AlignmentType.LEFT -> overallBounds.minX - strokeBounds.minX
                AlignmentType.CENTER_HORIZONTAL -> overallBounds.centerX - strokeBounds.centerX
                AlignmentType.RIGHT -> overallBounds.maxX - strokeBounds.maxX
                else -> 0f
            }
            val dy = when (alignmentType) {
                AlignmentType.TOP -> overallBounds.minY - strokeBounds.minY
                AlignmentType.CENTER_VERTICAL -> overallBounds.centerY - strokeBounds.centerY
                AlignmentType.BOTTOM -> overallBounds.maxY - strokeBounds.maxY
                else -> 0f
            }
            if (dx == 0f && dy == 0f) stroke
            else stroke.copy(points = stroke.points.map { p -> p.copy(x = p.x + dx, y = p.y + dy) })
        }
    }

    /**
     * Generates mirrored/symmetric vector strokes based on active mirror and symmetry settings.
     */
    fun generateSymmetricStrokes(
        baseStroke: DrawingStroke,
        canvasCenterX: Float,
        canvasCenterY: Float,
        mirrorMode: MirrorMode,
        symmetryMode: SymmetryMode
    ): List<DrawingStroke> {
        val results = mutableListOf(baseStroke)

        // 1. Mirror Mode
        when (mirrorMode) {
            MirrorMode.NONE -> {}
            MirrorMode.HORIZONTAL -> {
                results.add(
                    baseStroke.copy(
                        id = System.nanoTime() + 1,
                        points = baseStroke.points.map { p ->
                            p.copy(x = 2 * canvasCenterX - p.x)
                        }
                    )
                )
            }
            MirrorMode.VERTICAL -> {
                results.add(
                    baseStroke.copy(
                        id = System.nanoTime() + 2,
                        points = baseStroke.points.map { p ->
                            p.copy(y = 2 * canvasCenterY - p.y)
                        }
                    )
                )
            }
            MirrorMode.BOTH -> {
                results.add(
                    baseStroke.copy(
                        id = System.nanoTime() + 1,
                        points = baseStroke.points.map { p -> p.copy(x = 2 * canvasCenterX - p.x) }
                    )
                )
                results.add(
                    baseStroke.copy(
                        id = System.nanoTime() + 2,
                        points = baseStroke.points.map { p -> p.copy(y = 2 * canvasCenterY - p.y) }
                    )
                )
                results.add(
                    baseStroke.copy(
                        id = System.nanoTime() + 3,
                        points = baseStroke.points.map { p -> p.copy(x = 2 * canvasCenterX - p.x, y = 2 * canvasCenterY - p.y) }
                    )
                )
            }
        }

        // 2. Rotational Symmetry Mode
        val foldCount = when (symmetryMode) {
            SymmetryMode.NONE -> 1
            SymmetryMode.TWO_WAY -> 2
            SymmetryMode.FOUR_WAY -> 4
            SymmetryMode.SIX_WAY -> 6
            SymmetryMode.EIGHT_WAY -> 8
        }

        if (foldCount > 1) {
            val angleStep = 360f / foldCount
            for (f in 1 until foldCount) {
                val rotDeg = angleStep * f
                val rad = Math.toRadians(rotDeg.toDouble()).toFloat()
                val cosT = cos(rad)
                val sinT = sin(rad)

                results.add(
                    baseStroke.copy(
                        id = System.nanoTime() + 10 + f,
                        points = baseStroke.points.map { p ->
                            val relX = p.x - canvasCenterX
                            val relY = p.y - canvasCenterY
                            p.copy(
                                x = canvasCenterX + (relX * cosT - relY * sinT),
                                y = canvasCenterY + (relX * sinT + relY * cosT)
                            )
                        }
                    )
                )
            }
        }

        return results
    }
}
