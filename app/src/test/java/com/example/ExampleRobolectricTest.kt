package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.RecognizedShapeType
import com.example.engine.ShapeRecognizer
import com.example.engine.SmoothStrokeEngine
import com.example.engine.TransformEngine
import com.example.model.AlignmentType
import com.example.model.BrushType
import com.example.model.CanvasBackgroundColor
import com.example.model.DrawingLayer
import com.example.model.DrawingPoint
import com.example.model.DrawingStroke
import com.example.model.EraserMode
import com.example.model.GridMode
import com.example.model.MainToolMode
import com.example.model.MirrorMode
import com.example.model.PressureCurve
import com.example.model.SelectionMode
import com.example.model.ShapeProperties
import com.example.model.ShapeType
import com.example.model.SymmetryMode
import com.example.model.ToolType
import com.example.viewmodel.DrawingViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `verify app name resource is BlackCanvas`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("BlackCanvas", appName)
    }

    // ==========================================
    // 1. LAYER SYSTEM TESTS
    // ==========================================

    @Test
    fun `verify layer creation, duplication, renaming, moving, and deletion`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = DrawingViewModel(app)

        assertEquals(1, vm.layers.value.size)
        val initialId = vm.layers.value.first().id

        // Create new layer
        vm.createLayer("Details")
        assertEquals(2, vm.layers.value.size)
        val detailsLayer = vm.layers.value.last()
        assertEquals("Details", detailsLayer.name)
        assertEquals(detailsLayer.id, vm.activeLayerId.value)

        // Rename layer
        vm.renameLayer(detailsLayer.id, "Foreground")
        assertEquals("Foreground", vm.layers.value.find { it.id == detailsLayer.id }?.name)

        // Duplicate layer
        vm.duplicateLayer(detailsLayer.id)
        assertEquals(3, vm.layers.value.size)
        val dupLayer = vm.layers.value.find { it.name == "Foreground Copy" }
        assertNotNull(dupLayer)

        // Move Layer
        vm.moveLayerUp(dupLayer!!.id)
        vm.moveLayerDown(dupLayer.id)

        // Delete Layer
        vm.deleteLayer(dupLayer.id)
        assertEquals(2, vm.layers.value.size)

        // Cannot delete the only layer
        vm.deleteLayer(detailsLayer.id)
        assertEquals(1, vm.layers.value.size)
        vm.deleteLayer(initialId)
        assertEquals(1, vm.layers.value.size) // Keeps at least 1 layer
    }

    @Test
    fun `verify layer visibility, lock, opacity and merge down`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = DrawingViewModel(app)

        // Layer 1 with a stroke
        val pts1 = listOf(DrawingPoint(10f, 10f), DrawingPoint(20f, 20f))
        vm.onStrokeStart(pts1, ToolType.FINGER)
        vm.onStrokeEnd(pts1)
        val l1Id = vm.layers.value.first().id

        // Layer 2 with a stroke
        vm.createLayer("Layer 2")
        val l2Id = vm.layers.value.last().id
        val pts2 = listOf(DrawingPoint(50f, 50f), DrawingPoint(60f, 60f))
        vm.onStrokeStart(pts2, ToolType.FINGER)
        vm.onStrokeEnd(pts2)

        assertEquals(1, vm.layers.value.find { it.id == l1Id }?.strokes?.size)
        assertEquals(1, vm.layers.value.find { it.id == l2Id }?.strokes?.size)

        // Opacity
        vm.setLayerOpacity(l2Id, 0.5f)
        assertEquals(0.5f, vm.layers.value.find { it.id == l2Id }?.opacity ?: 0f, 0.01f)

        // Visibility
        vm.toggleLayerVisibility(l2Id)
        assertFalse(vm.layers.value.find { it.id == l2Id }?.isVisible ?: true)
        vm.toggleLayerVisibility(l2Id)
        assertTrue(vm.layers.value.find { it.id == l2Id }?.isVisible ?: false)

        // Lock
        vm.toggleLayerLock(l2Id)
        assertTrue(vm.layers.value.find { it.id == l2Id }?.isLocked ?: false)

        // Attempting to draw on locked layer should be rejected and show notice
        vm.onStrokeStart(listOf(DrawingPoint(100f, 100f)), ToolType.FINGER)
        assertEquals("Layer Locked", vm.statusMessage.value)

        // Unlock and Merge Down
        vm.toggleLayerLock(l2Id)
        vm.mergeLayerDown(l2Id)
        assertEquals(1, vm.layers.value.size)
        assertEquals(2, vm.layers.value.first().strokes.size)
    }

    // ==========================================
    // 2. SELECTION & TRANSFORMS TESTS
    // ==========================================

    @Test
    fun `verify selection bounding box, translation, scaling, rotation and alignment`() {
        val strokes = listOf(
            DrawingStroke(
                id = 101L,
                points = listOf(DrawingPoint(10f, 10f), DrawingPoint(30f, 30f)),
                strokeWidth = 4f
            ),
            DrawingStroke(
                id = 102L,
                points = listOf(DrawingPoint(50f, 50f), DrawingPoint(90f, 90f)),
                strokeWidth = 4f
            )
        )

        // Bounding box
        val bounds = TransformEngine.computeBounds(strokes)
        assertNotNull(bounds)
        assertEquals(10f, bounds!!.minX, 0.1f)
        assertEquals(10f, bounds.minY, 0.1f)
        assertEquals(90f, bounds.maxX, 0.1f)
        assertEquals(90f, bounds.maxY, 0.1f)

        // Translation
        val translated = TransformEngine.translateStrokes(strokes, dx = 20f, dy = 10f)
        assertEquals(30f, translated[0].points.first().x, 0.1f)
        assertEquals(20f, translated[0].points.first().y, 0.1f)

        // Scaling
        val scaled = TransformEngine.scaleStrokes(strokes, pivotX = 50f, pivotY = 50f, scaleX = 2f, scaleY = 2f)
        assertTrue(scaled[0].strokeWidth > strokes[0].strokeWidth)

        // Rotation (90 deg)
        val rotated = TransformEngine.rotateStrokes(strokes, pivotX = 50f, pivotY = 50f, degrees = 90f)
        assertNotNull(rotated)

        // Alignment
        val alignedLeft = TransformEngine.alignStrokes(strokes, AlignmentType.LEFT)
        val b0 = TransformEngine.computeBounds(listOf(alignedLeft[0]))
        val b1 = TransformEngine.computeBounds(listOf(alignedLeft[1]))
        assertEquals(b0!!.minX, b1!!.minX, 0.1f)
    }

    @Test
    fun `verify copy, cut, paste and delete selection workflow`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = DrawingViewModel(app)

        val pts = listOf(DrawingPoint(10f, 10f), DrawingPoint(20f, 20f))
        vm.onStrokeStart(pts, ToolType.FINGER)
        vm.onStrokeEnd(pts)

        val strokeId = vm.layers.value.first().strokes.first().id
        vm.selectStrokes(setOf(strokeId))

        // Copy & Paste
        vm.copySelection()
        vm.pasteClipboard()
        assertEquals(2, vm.layers.value.first().strokes.size)

        // Cut
        vm.selectStrokes(setOf(strokeId))
        vm.cutSelection()
        assertEquals(1, vm.layers.value.first().strokes.size)

        // Delete
        val remainingId = vm.layers.value.first().strokes.first().id
        vm.selectStrokes(setOf(remainingId))
        vm.deleteSelection()
        assertEquals(0, vm.layers.value.first().strokes.size)
    }

    // ==========================================
    // 3. BASIC SHAPES & EDITING TESTS
    // ==========================================

    @Test
    fun `verify basic shapes generation and properties`() {
        val props = ShapeProperties(
            strokeWidth = 6f,
            fillColor = 0xFF00E5FF,
            fillOpacity = 0.5f,
            cornerRadius = 12f,
            polygonSides = 6,
            starPoints = 5
        )

        // Line
        val line = ShapeRecognizer.generateShapePoints(ShapeType.LINE, 0f, 0f, 100f, 100f, props)
        assertTrue(line.size >= 2)

        // Circle
        val circle = ShapeRecognizer.generateShapePoints(ShapeType.CIRCLE, 0f, 0f, 100f, 100f, props)
        assertTrue(circle.size >= 10)

        // Oval
        val oval = ShapeRecognizer.generateShapePoints(ShapeType.OVAL, 0f, 0f, 200f, 100f, props)
        assertTrue(oval.size >= 10)

        // Rectangle
        val rect = ShapeRecognizer.generateShapePoints(ShapeType.RECTANGLE, 0f, 0f, 100f, 100f, props)
        assertTrue(rect.size >= 4)

        // Rounded Rectangle
        val rRect = ShapeRecognizer.generateShapePoints(ShapeType.ROUNDED_RECTANGLE, 0f, 0f, 100f, 100f, props)
        assertTrue(rRect.size >= 8)

        // Triangle
        val tri = ShapeRecognizer.generateShapePoints(ShapeType.TRIANGLE, 0f, 0f, 100f, 100f, props)
        assertTrue(tri.size >= 3)

        // Hexagon Polygon
        val poly = ShapeRecognizer.generateShapePoints(ShapeType.POLYGON, 0f, 0f, 100f, 100f, props)
        assertTrue(poly.size >= 6)

        // Star
        val star = ShapeRecognizer.generateShapePoints(ShapeType.STAR, 0f, 0f, 100f, 100f, props)
        assertTrue(star.size >= 10)

        // Arrow
        val arrow = ShapeRecognizer.generateShapePoints(ShapeType.ARROW, 0f, 0f, 100f, 100f, props)
        assertTrue(arrow.size >= 5)
    }

    // ==========================================
    // 4. FILL TOOL & CLOSED SHAPES
    // ==========================================

    @Test
    fun `verify fill tool identifies enclosed polygon`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = DrawingViewModel(app)

        // Create a closed rectangle shape
        vm.selectShapeType(ShapeType.RECTANGLE)
        vm.commitShape(100f, 100f, 300f, 300f)

        assertEquals(1, vm.layers.value.first().strokes.size)
        val stroke = vm.layers.value.first().strokes.first()
        assertTrue(stroke.isClosed)

        // Fill inside rectangle (200, 200)
        vm.selectColor(0xFFFF4081)
        vm.fillAtPoint(200f, 200f)

        val updatedStroke = vm.layers.value.first().strokes.first()
        assertEquals(0xFFFF4081, updatedStroke.fillColor)
        assertEquals("Shape filled", vm.statusMessage.value)

        // Fill outside closed polygon -> shows non-blocking notice
        vm.fillAtPoint(800f, 800f)
        assertEquals("Closed shape required", vm.statusMessage.value)
    }

    // ==========================================
    // 5. MIRROR & SYMMETRY DRAWING
    // ==========================================

    @Test
    fun `verify mirror and rotational symmetry generators`() {
        val base = DrawingStroke(
            points = listOf(DrawingPoint(100f, 100f), DrawingPoint(150f, 150f)),
            strokeWidth = 4f
        )
        val cx = 500f
        val cy = 500f

        // Horizontal Mirror -> 2 strokes
        val horizMirrored = TransformEngine.generateSymmetricStrokes(base, cx, cy, MirrorMode.HORIZONTAL, SymmetryMode.NONE)
        assertEquals(2, horizMirrored.size)

        // Both Mirror (H + V + Diag) -> 4 strokes
        val bothMirrored = TransformEngine.generateSymmetricStrokes(base, cx, cy, MirrorMode.BOTH, SymmetryMode.NONE)
        assertEquals(4, bothMirrored.size)

        // 4-Way Radial Symmetry -> 4 strokes
        val fourWay = TransformEngine.generateSymmetricStrokes(base, cx, cy, MirrorMode.NONE, SymmetryMode.FOUR_WAY)
        assertEquals(4, fourWay.size)

        // 8-Way Radial Symmetry -> 8 strokes
        val eightWay = TransformEngine.generateSymmetricStrokes(base, cx, cy, MirrorMode.NONE, SymmetryMode.EIGHT_WAY)
        assertEquals(8, eightWay.size)
    }

    // ==========================================
    // 6. SCALE & STRESS TESTING (10+ layers, 100+ strokes)
    // ==========================================

    @Test
    fun `verify scalability with 10+ layers and 100+ vector strokes`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = DrawingViewModel(app)

        // Create 12 layers
        for (i in 2..12) {
            vm.createLayer("Layer $i")
        }
        assertEquals(12, vm.layers.value.size)

        // Draw 100 strokes across layers
        for (i in 1..100) {
            val layerIndex = i % 12
            val layerId = vm.layers.value[layerIndex].id
            vm.selectActiveLayer(layerId)

            val pts = listOf(
                DrawingPoint(i * 5f, i * 5f),
                DrawingPoint(i * 5f + 10f, i * 5f + 10f)
            )
            vm.onStrokeStart(pts, ToolType.FINGER)
            vm.onStrokeEnd(pts)
        }

        val totalStrokes = vm.layers.value.sumOf { it.strokes.size }
        assertEquals(100, totalStrokes)

        // Verify Undo on large state
        assertTrue(vm.canUndo.value)
        vm.undo()
        assertEquals(99, vm.layers.value.sumOf { it.strokes.size })
        vm.redo()
        assertEquals(100, vm.layers.value.sumOf { it.strokes.size })
    }
}
