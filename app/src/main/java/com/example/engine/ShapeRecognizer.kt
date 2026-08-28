package com.example.engine

import com.example.model.DrawingPoint
import com.example.model.ShapeProperties
import com.example.model.ShapeType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class RecognizedShapeType {
    NONE,
    LINE,
    CIRCLE,
    OVAL,
    RECTANGLE,
    ROUNDED_RECTANGLE,
    TRIANGLE,
    POLYGON,
    STAR,
    ARROW
}

data class ShapeRecognitionResult(
    val type: RecognizedShapeType,
    val perfectedPoints: List<DrawingPoint>?
)

/**
 * High-performance, offline geometric shape analysis & generation engine.
 * Pure mathematics (coordinate geometry & polygon triangulation).
 */
object ShapeRecognizer {

    fun recognize(points: List<DrawingPoint>): ShapeRecognitionResult {
        if (points.size < 6) {
            return ShapeRecognitionResult(RecognizedShapeType.NONE, null)
        }

        val first = points.first()
        val last = points.last()

        var totalPathLength = 0f
        for (i in 0 until points.size - 1) {
            totalPathLength += hypot(points[i + 1].x - points[i].x, points[i + 1].y - points[i].y)
        }

        val endToEndDist = hypot(last.x - first.x, last.y - first.y)
        val avgPressure = points.map { it.pressure }.average().toFloat().coerceIn(0.2f, 1.5f)

        // 1. Line Test
        if (totalPathLength > 20f && (totalPathLength / endToEndDist) < 1.12f) {
            val linePoints = generateLinePoints(first, last, avgPressure)
            return ShapeRecognitionResult(RecognizedShapeType.LINE, linePoints)
        }

        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var sumX = 0f
        var sumY = 0f

        for (p in points) {
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
            sumX += p.x
            sumY += p.y
        }

        val boxWidth = maxX - minX
        val boxHeight = maxY - minY
        if (boxWidth < 15f || boxHeight < 15f) {
            return ShapeRecognitionResult(RecognizedShapeType.NONE, null)
        }

        val isClosed = (endToEndDist / totalPathLength) < 0.30f
        if (!isClosed) {
            return ShapeRecognitionResult(RecognizedShapeType.NONE, null)
        }

        val centerX = sumX / points.size
        val centerY = sumY / points.size

        val radii = points.map { hypot(it.x - centerX, it.y - centerY) }
        val avgRadius = radii.average().toFloat()
        val radiusVariance = radii.map { abs(it - avgRadius) }.average().toFloat() / avgRadius

        val aspectRatio = boxWidth / boxHeight

        // 2. Circle Test
        if (radiusVariance < 0.16f && aspectRatio in 0.80f..1.25f) {
            val circlePoints = generateCirclePoints(centerX, centerY, avgRadius, avgPressure)
            return ShapeRecognitionResult(RecognizedShapeType.CIRCLE, circlePoints)
        }

        // 3. Oval / Ellipse Test
        if (radiusVariance < 0.28f && (aspectRatio < 0.80f || aspectRatio > 1.25f)) {
            val ovalPoints = generateOvalPoints(centerX, centerY, boxWidth / 2f, boxHeight / 2f, avgPressure)
            return ShapeRecognitionResult(RecognizedShapeType.OVAL, ovalPoints)
        }

        // 4. Polygon Corner Detection
        val simplified = simplifyDouglasPeucker(points, epsilon = max(boxWidth, boxHeight) * 0.08f)
        val cornerCount = simplified.size - 1

        if (cornerCount == 3) {
            val trianglePoints = generatePolygonPoints(simplified, avgPressure)
            return ShapeRecognitionResult(RecognizedShapeType.TRIANGLE, trianglePoints)
        } else if (cornerCount == 4) {
            val rectPoints = generateRectPoints(minX, minY, maxX, maxY, avgPressure)
            return ShapeRecognitionResult(RecognizedShapeType.RECTANGLE, rectPoints)
        } else if (cornerCount in 5..6) {
            val polyPoints = generateRegularPolygonPoints(centerX, centerY, min(boxWidth, boxHeight) / 2f, cornerCount, avgPressure)
            return ShapeRecognitionResult(RecognizedShapeType.POLYGON, polyPoints)
        }

        return ShapeRecognitionResult(RecognizedShapeType.NONE, null)
    }

    fun generateShapePoints(
        shapeType: ShapeType,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        properties: ShapeProperties = ShapeProperties()
    ): List<DrawingPoint> {
        val minX = min(x1, x2)
        val maxX = max(x1, x2)
        val minY = min(y1, y2)
        val maxY = max(y1, y2)
        val cx = (minX + maxX) / 2f
        val cy = (minY + maxY) / 2f
        val rx = (maxX - minX) / 2f
        val ry = (maxY - minY) / 2f

        return when (shapeType) {
            ShapeType.LINE -> {
                generateLinePoints(DrawingPoint(x1, y1), DrawingPoint(x2, y2), 1.0f)
            }
            ShapeType.CIRCLE -> {
                val r = min(rx, ry)
                generateCirclePoints(cx, cy, r, 1.0f)
            }
            ShapeType.OVAL -> {
                generateOvalPoints(cx, cy, rx, ry, 1.0f)
            }
            ShapeType.RECTANGLE -> {
                generateRectPoints(minX, minY, maxX, maxY, 1.0f)
            }
            ShapeType.ROUNDED_RECTANGLE -> {
                generateRoundedRectPoints(minX, minY, maxX, maxY, properties.cornerRadius, 1.0f)
            }
            ShapeType.TRIANGLE -> {
                val pTop = DrawingPoint(cx, minY, 1.0f)
                val pRight = DrawingPoint(maxX, maxY, 1.0f)
                val pLeft = DrawingPoint(minX, maxY, 1.0f)
                interpolateSegments(listOf(pTop, pRight, pLeft, pTop))
            }
            ShapeType.POLYGON -> {
                generateRegularPolygonPoints(cx, cy, min(rx, ry), properties.polygonSides.coerceIn(3, 8), 1.0f)
            }
            ShapeType.STAR -> {
                generateStarPoints(cx, cy, min(rx, ry), min(rx, ry) * 0.45f, properties.starPoints.coerceIn(4, 8), 1.0f)
            }
            ShapeType.ARROW -> {
                generateArrowPoints(x1, y1, x2, y2, properties.arrowHeadSize)
            }
        }
    }

    private fun generateLinePoints(start: DrawingPoint, end: DrawingPoint, pressure: Float): List<DrawingPoint> {
        val steps = 8
        val result = mutableListOf<DrawingPoint>()
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            result.add(
                DrawingPoint(
                    x = start.x + t * (end.x - start.x),
                    y = start.y + t * (end.y - start.y),
                    pressure = pressure
                )
            )
        }
        return result
    }

    private fun generateCirclePoints(cx: Float, cy: Float, radius: Float, pressure: Float): List<DrawingPoint> {
        val count = 36
        val result = mutableListOf<DrawingPoint>()
        for (i in 0..count) {
            val theta = (i.toFloat() / count) * 2f * PI.toFloat()
            result.add(
                DrawingPoint(
                    x = cx + radius * cos(theta),
                    y = cy + radius * sin(theta),
                    pressure = pressure
                )
            )
        }
        return result
    }

    private fun generateOvalPoints(cx: Float, cy: Float, rx: Float, ry: Float, pressure: Float): List<DrawingPoint> {
        val count = 36
        val result = mutableListOf<DrawingPoint>()
        for (i in 0..count) {
            val theta = (i.toFloat() / count) * 2f * PI.toFloat()
            result.add(
                DrawingPoint(
                    x = cx + rx * cos(theta),
                    y = cy + ry * sin(theta),
                    pressure = pressure
                )
            )
        }
        return result
    }

    private fun generateRectPoints(minX: Float, minY: Float, maxX: Float, maxY: Float, pressure: Float): List<DrawingPoint> {
        val corners = listOf(
            DrawingPoint(minX, minY, pressure),
            DrawingPoint(maxX, minY, pressure),
            DrawingPoint(maxX, maxY, pressure),
            DrawingPoint(minX, maxY, pressure),
            DrawingPoint(minX, minY, pressure)
        )
        return interpolateSegments(corners)
    }

    private fun generateRoundedRectPoints(minX: Float, minY: Float, maxX: Float, maxY: Float, radius: Float, pressure: Float): List<DrawingPoint> {
        val r = radius.coerceAtMost(min((maxX - minX) / 2f, (maxY - minY) / 2f))
        val corners = listOf(
            DrawingPoint(minX + r, minY, pressure),
            DrawingPoint(maxX - r, minY, pressure),
            DrawingPoint(maxX, minY + r, pressure),
            DrawingPoint(maxX, maxY - r, pressure),
            DrawingPoint(maxX - r, maxY, pressure),
            DrawingPoint(minX + r, maxY, pressure),
            DrawingPoint(minX, maxY - r, pressure),
            DrawingPoint(minX, minY + r, pressure),
            DrawingPoint(minX + r, minY, pressure)
        )
        return interpolateSegments(corners)
    }

    private fun generateRegularPolygonPoints(cx: Float, cy: Float, radius: Float, sides: Int, pressure: Float): List<DrawingPoint> {
        val vertices = mutableListOf<DrawingPoint>()
        for (i in 0..sides) {
            val theta = (i.toFloat() / sides) * 2f * PI.toFloat() - (PI.toFloat() / 2f)
            vertices.add(
                DrawingPoint(
                    x = cx + radius * cos(theta),
                    y = cy + radius * sin(theta),
                    pressure = pressure
                )
            )
        }
        return interpolateSegments(vertices)
    }

    private fun generateStarPoints(cx: Float, cy: Float, outerR: Float, innerR: Float, points: Int, pressure: Float): List<DrawingPoint> {
        val total = points * 2
        val vertices = mutableListOf<DrawingPoint>()
        for (i in 0..total) {
            val r = if (i % 2 == 0) outerR else innerR
            val theta = (i.toFloat() / total) * 2f * PI.toFloat() - (PI.toFloat() / 2f)
            vertices.add(
                DrawingPoint(
                    x = cx + r * cos(theta),
                    y = cy + r * sin(theta),
                    pressure = pressure
                )
            )
        }
        return interpolateSegments(vertices)
    }

    private fun generateArrowPoints(x1: Float, y1: Float, x2: Float, y2: Float, headSize: Float): List<DrawingPoint> {
        val dx = x2 - x1
        val dy = y2 - y1
        val angle = atan2(dy.toDouble(), dx.toDouble()).toFloat()
        val hSize = headSize.coerceIn(12f, 48f)

        val wingAngle = 0.5f // ~28 degrees
        val wing1X = x2 - hSize * cos(angle - wingAngle)
        val wing1Y = y2 - hSize * sin(angle - wingAngle)
        val wing2X = x2 - hSize * cos(angle + wingAngle)
        val wing2Y = y2 - hSize * sin(angle + wingAngle)

        val pts = listOf(
            DrawingPoint(x1, y1),
            DrawingPoint(x2, y2),
            DrawingPoint(wing1X, wing1Y),
            DrawingPoint(x2, y2),
            DrawingPoint(wing2X, wing2Y)
        )
        return interpolateSegments(pts)
    }

    private fun generatePolygonPoints(vertices: List<DrawingPoint>, pressure: Float): List<DrawingPoint> {
        val cleanVertices = vertices.map { it.copy(pressure = pressure) }
        return interpolateSegments(cleanVertices)
    }

    private fun interpolateSegments(vertices: List<DrawingPoint>): List<DrawingPoint> {
        val result = mutableListOf<DrawingPoint>()
        for (i in 0 until vertices.size - 1) {
            val p0 = vertices[i]
            val p1 = vertices[i + 1]
            val steps = 6
            for (s in 0 until steps) {
                val t = s.toFloat() / steps
                result.add(
                    DrawingPoint(
                        x = p0.x + t * (p1.x - p0.x),
                        y = p0.y + t * (p1.y - p0.y),
                        pressure = p0.pressure
                    )
                )
            }
        }
        result.add(vertices.last())
        return result
    }

    private fun simplifyDouglasPeucker(points: List<DrawingPoint>, epsilon: Float): List<DrawingPoint> {
        if (points.size < 3) return points

        var maxDist = 0f
        var maxIndex = 0
        val first = points.first()
        val last = points.last()

        for (i in 1 until points.size - 1) {
            val dist = perpendicularDistance(points[i], first, last)
            if (dist > maxDist) {
                maxDist = dist
                maxIndex = i
            }
        }

        return if (maxDist > epsilon) {
            val left = simplifyDouglasPeucker(points.subList(0, maxIndex + 1), epsilon)
            val right = simplifyDouglasPeucker(points.subList(maxIndex, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(first, last)
        }
    }

    private fun perpendicularDistance(pt: DrawingPoint, lineStart: DrawingPoint, lineEnd: DrawingPoint): Float {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        val lineLen = hypot(dx, dy)
        if (lineLen < 0.001f) {
            return hypot(pt.x - lineStart.x, pt.y - lineStart.y)
        }
        return abs(dy * pt.x - dx * pt.y + lineEnd.x * lineStart.y - lineEnd.y * lineStart.x) / lineLen
    }

    /**
     * Ray-casting point-in-polygon algorithm to test if a point is inside closed points.
     */
    fun isPointInsideClosedPolygon(px: Float, py: Float, polygon: List<DrawingPoint>): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val xi = polygon[i].x
            val yi = polygon[i].y
            val xj = polygon[j].x
            val yj = polygon[j].y

            val intersect = ((yi > py) != (yj > py)) &&
                (px < (xj - xi) * (py - yi) / (yj - yi + 0.00001f) + xi)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }
}
