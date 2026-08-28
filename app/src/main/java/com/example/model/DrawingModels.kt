package com.example.model

enum class ToolType {
    FINGER,
    STYLUS,
    ERASER,
    UNKNOWN
}

enum class BrushType {
    PEN,
    PENCIL,
    MARKER,
    SOFT_BRUSH,
    HIGHLIGHTER,
    ERASER
}

enum class EraserMode {
    PIXEL,
    STROKE
}

enum class PressureCurve {
    SOFT,
    NORMAL,
    FIRM,
    CUSTOM
}

enum class SmoothnessLevel {
    LOW,
    NORMAL,
    HIGH
}

enum class MainToolMode {
    DRAW,
    SELECT,
    SHAPES,
    FILL
}

enum class SelectionMode {
    RECTANGLE,
    LASSO,
    SINGLE_STROKE,
    MULTI_STROKE
}

enum class ShapeType {
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

enum class GridMode {
    OFF,
    SMALL,
    LARGE
}

enum class MirrorMode {
    NONE,
    HORIZONTAL,
    VERTICAL,
    BOTH
}

enum class SymmetryMode {
    NONE,
    TWO_WAY,
    FOUR_WAY,
    SIX_WAY,
    EIGHT_WAY
}

enum class CanvasBackgroundColor(val colorLong: Long, val label: String) {
    TRANSPARENT(0x00000000, "Transparent"),
    BLACK(0xFF000000, "Black"),
    DARK_GRAY(0xFF1E2028, "Dark Gray"),
    DARK_BLUE(0xFF0D1B2A, "Dark Blue"),
    DARK_PURPLE(0xFF1B0A2A, "Dark Purple"),
    WHITE(0xFFFFFFFF, "White"),
    CUSTOM(0xFF12141C, "Custom")
}

enum class FloatingIconSize(val label: String, val dpSize: Int) {
    SMALL("Small", 44),
    MEDIUM("Medium", 56),
    LARGE("Large", 68)
}

enum class SaveFormat {
    PNG_NORMAL,
    PNG_TRANSPARENT,
    PROJECT_JSON
}

enum class AlignmentType {
    LEFT,
    CENTER_HORIZONTAL,
    RIGHT,
    TOP,
    CENTER_VERTICAL,
    BOTTOM
}

data class ToolConfig(
    val size: Float,
    val opacity: Float,
    val color: Long
)

data class DrawingPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f,
    val tiltRad: Float = 0.0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class DrawingStroke(
    val id: Long = System.nanoTime(),
    val points: List<DrawingPoint>,
    val color: Long = 0xFFFFFFFF,
    val strokeWidth: Float = 6.0f,
    val opacity: Float = 1.0f,
    val brushType: BrushType = BrushType.PEN,
    val toolType: ToolType = ToolType.FINGER,
    val isEraser: Boolean = false,
    val eraserMode: EraserMode = EraserMode.PIXEL,
    val fillColor: Long = 0x00000000,
    val fillOpacity: Float = 0.0f,
    val isClosed: Boolean = false
)

data class DrawingLayer(
    val id: Long = System.nanoTime(),
    val name: String = "Layer",
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val opacity: Float = 1.0f,
    val strokes: List<DrawingStroke> = emptyList()
)

data class GuideLine(
    val id: Long = System.nanoTime(),
    val isHorizontal: Boolean,
    val position: Float
)

data class ShapeProperties(
    val strokeColor: Long = 0xFFFFFFFF,
    val fillColor: Long = 0x00000000,
    val strokeWidth: Float = 4.0f,
    val strokeOpacity: Float = 1.0f,
    val fillOpacity: Float = 1.0f,
    val cornerRadius: Float = 16.0f,
    val polygonSides: Int = 5,
    val starPoints: Int = 5,
    val arrowHeadSize: Float = 24.0f
)

data class SelectionBounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    val width: Float get() = maxX - minX
    val height: Float get() = maxY - minY
    val centerX: Float get() = (minX + maxX) / 2f
    val centerY: Float get() = (minY + maxY) / 2f
}

data class PerformanceMetrics(
    val inputEvents: Long = 0,
    val renderFps: Int = 60,
    val strokeFps: Int = 60,
    val avgLatencyMs: Float = 0f,
    val droppedEvents: Long = 0,
    val strokePoints: Int = 0,
    val activeStrokesCount: Int = 0,
    val memoryUsageMb: Float = 0f
)
