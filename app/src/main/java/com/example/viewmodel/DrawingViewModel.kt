package com.example.viewmodel

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import com.example.model.GuideLine
import com.example.model.MainToolMode
import com.example.model.MirrorMode
import com.example.model.PerformanceMetrics
import com.example.model.PressureCurve
import com.example.model.SaveFormat
import com.example.model.SelectionBounds
import com.example.model.SelectionMode
import com.example.model.ShapeProperties
import com.example.model.ShapeType
import com.example.model.SmoothnessLevel
import com.example.model.SymmetryMode
import com.example.model.ToolConfig
import com.example.model.ToolType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.round

class DrawingViewModel(application: Application) : AndroidViewModel(application) {

    private val maxHistorySize = 40
    private val maxRecentColors = 8

    // Layers System
    private val initialLayer = DrawingLayer(name = "Layer 1")
    private val _layers = MutableStateFlow<List<DrawingLayer>>(listOf(initialLayer))
    val layers: StateFlow<List<DrawingLayer>> = _layers.asStateFlow()

    private val _activeLayerId = MutableStateFlow(initialLayer.id)
    val activeLayerId: StateFlow<Long> = _activeLayerId.asStateFlow()

    // Current In-flight Stroke
    private val _currentStroke = MutableStateFlow<DrawingStroke?>(null)
    val currentStroke: StateFlow<DrawingStroke?> = _currentStroke.asStateFlow()

    // Main Tool Mode
    private val _mainToolMode = MutableStateFlow(MainToolMode.DRAW)
    val mainToolMode: StateFlow<MainToolMode> = _mainToolMode.asStateFlow()

    // Selection System
    private val _selectionMode = MutableStateFlow(SelectionMode.RECTANGLE)
    val selectionMode: StateFlow<SelectionMode> = _selectionMode.asStateFlow()

    private val _selectedStrokeIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedStrokeIds: StateFlow<Set<Long>> = _selectedStrokeIds.asStateFlow()

    private val internalClipboard = mutableListOf<DrawingStroke>()

    // Shapes System
    private val _selectedShapeType = MutableStateFlow(ShapeType.RECTANGLE)
    val selectedShapeType: StateFlow<ShapeType> = _selectedShapeType.asStateFlow()

    private val _shapeProperties = MutableStateFlow(ShapeProperties())
    val shapeProperties: StateFlow<ShapeProperties> = _shapeProperties.asStateFlow()

    // Grid, Snap & Guides
    private val _gridMode = MutableStateFlow(GridMode.OFF)
    val gridMode: StateFlow<GridMode> = _gridMode.asStateFlow()

    private val _isSnapToGridEnabled = MutableStateFlow(false)
    val isSnapToGridEnabled: StateFlow<Boolean> = _isSnapToGridEnabled.asStateFlow()

    private val _guides = MutableStateFlow<List<GuideLine>>(emptyList())
    val guides: StateFlow<List<GuideLine>> = _guides.asStateFlow()

    // Symmetry & Mirror
    private val _mirrorMode = MutableStateFlow(MirrorMode.NONE)
    val mirrorMode: StateFlow<MirrorMode> = _mirrorMode.asStateFlow()

    private val _symmetryMode = MutableStateFlow(SymmetryMode.NONE)
    val symmetryMode: StateFlow<SymmetryMode> = _symmetryMode.asStateFlow()

    // Canvas Background
    private val _canvasBackground = MutableStateFlow(CanvasBackgroundColor.TRANSPARENT)
    val canvasBackground: StateFlow<CanvasBackgroundColor> = _canvasBackground.asStateFlow()

    private val _backgroundOpacity = MutableStateFlow(0.0f)
    val backgroundOpacity: StateFlow<Float> = _backgroundOpacity.asStateFlow()

    private val _customBackgroundColor = MutableStateFlow(0xFF12141CL)
    val customBackgroundColor: StateFlow<Long> = _customBackgroundColor.asStateFlow()

    private val _isBackgroundLocked = MutableStateFlow(false)
    val isBackgroundLocked: StateFlow<Boolean> = _isBackgroundLocked.asStateFlow()

    // Animation Replay System
    private val _isReplaying = MutableStateFlow(false)
    val isReplaying: StateFlow<Boolean> = _isReplaying.asStateFlow()

    private val _isReplayPaused = MutableStateFlow(false)
    val isReplayPaused: StateFlow<Boolean> = _isReplayPaused.asStateFlow()

    private val _replaySpeed = MutableStateFlow(1.0f)
    val replaySpeed: StateFlow<Float> = _replaySpeed.asStateFlow()

    private val _replayProgress = MutableStateFlow(0.0f)
    val replayProgress: StateFlow<Float> = _replayProgress.asStateFlow()

    private val _replayedLayers = MutableStateFlow<List<DrawingLayer>?>(null)
    val replayedLayers: StateFlow<List<DrawingLayer>?> = _replayedLayers.asStateFlow()

    private var replayJob: kotlinx.coroutines.Job? = null

    // Undo / Redo Unified Command Stack
    private val undoStack = ArrayDeque<List<DrawingLayer>>()
    private val redoStack = ArrayDeque<List<DrawingLayer>>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Tool Memory Map
    private val toolMemory = mutableMapOf(
        BrushType.PEN to ToolConfig(size = 4.0f, opacity = 1.0f, color = 0xFFFFFFFF),
        BrushType.PENCIL to ToolConfig(size = 6.0f, opacity = 0.75f, color = 0xFFCCCCCC),
        BrushType.MARKER to ToolConfig(size = 24.0f, opacity = 0.50f, color = 0xFF00E5FF),
        BrushType.SOFT_BRUSH to ToolConfig(size = 16.0f, opacity = 0.65f, color = 0xFFFF4081),
        BrushType.HIGHLIGHTER to ToolConfig(size = 32.0f, opacity = 0.40f, color = 0xFFFFEA00),
        BrushType.ERASER to ToolConfig(size = 24.0f, opacity = 1.0f, color = 0xFF000000)
    )

    private val _selectedBrush = MutableStateFlow(BrushType.PEN)
    val selectedBrush: StateFlow<BrushType> = _selectedBrush.asStateFlow()

    private val _selectedColor = MutableStateFlow(0xFFFFFFFF)
    val selectedColor: StateFlow<Long> = _selectedColor.asStateFlow()

    private val _selectedWidth = MutableStateFlow(4.0f)
    val selectedWidth: StateFlow<Float> = _selectedWidth.asStateFlow()

    private val _selectedOpacity = MutableStateFlow(1.0f)
    val selectedOpacity: StateFlow<Float> = _selectedOpacity.asStateFlow()

    private val _eraserMode = MutableStateFlow(EraserMode.PIXEL)
    val eraserMode: StateFlow<EraserMode> = _eraserMode.asStateFlow()

    private val _isStraightLineMode = MutableStateFlow(false)
    val isStraightLineMode: StateFlow<Boolean> = _isStraightLineMode.asStateFlow()

    private val _isShapeAssistEnabled = MutableStateFlow(false)
    val isShapeAssistEnabled: StateFlow<Boolean> = _isShapeAssistEnabled.asStateFlow()

    private val _smoothnessLevel = MutableStateFlow(SmoothnessLevel.NORMAL)
    val smoothnessLevel: StateFlow<SmoothnessLevel> = _smoothnessLevel.asStateFlow()

    private val _pressureCurve = MutableStateFlow(PressureCurve.NORMAL)
    val pressureCurve: StateFlow<PressureCurve> = _pressureCurve.asStateFlow()

    private val _customPressureFactor = MutableStateFlow(1.0f)
    val customPressureFactor: StateFlow<Float> = _customPressureFactor.asStateFlow()

    private val _isTiltEnabled = MutableStateFlow(true)
    val isTiltEnabled: StateFlow<Boolean> = _isTiltEnabled.asStateFlow()

    private val _recentColors = MutableStateFlow<List<Long>>(
        listOf(0xFFFFFFFF, 0xFF00E5FF, 0xFF00E676, 0xFFFF1744, 0xFFFFEA00)
    )
    val recentColors: StateFlow<List<Long>> = _recentColors.asStateFlow()

    private val _favoriteColors = MutableStateFlow<List<Long>>(
        listOf(0xFFFFFFFF, 0xFF00E5FF, 0xFFFF5252, 0xFFFFD600, 0xFFE040FB)
    )
    val favoriteColors: StateFlow<List<Long>> = _favoriteColors.asStateFlow()

    // Performance & Status
    private val _showPerformanceMonitor = MutableStateFlow(false)
    val showPerformanceMonitor: StateFlow<Boolean> = _showPerformanceMonitor.asStateFlow()

    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var totalInputEvents: Long = 0
    private var totalDroppedEvents: Long = 0
    private var latencySum: Float = 0f
    private var latencyCount: Long = 0

    // Helper getter for active layer
    val activeLayer: DrawingLayer?
        get() = _layers.value.find { it.id == _activeLayerId.value } ?: _layers.value.firstOrNull()

    // ==========================================
    // LAYER MANAGEMENT
    // ==========================================

    fun selectActiveLayer(layerId: Long) {
        _activeLayerId.value = layerId
        clearSelection()
    }

    fun createLayer(name: String? = null) {
        val newName = name ?: "Layer ${_layers.value.size + 1}"
        val newLayer = DrawingLayer(name = newName)
        pushUndoSnapshot()
        _layers.update { it + newLayer }
        _activeLayerId.value = newLayer.id
        updatePerfMetrics()
    }

    fun deleteLayer(layerId: Long) {
        if (_layers.value.size <= 1) {
            _statusMessage.value = "Cannot delete the only layer"
            return
        }
        pushUndoSnapshot()
        _layers.update { it.filterNot { layer -> layer.id == layerId } }
        if (_activeLayerId.value == layerId) {
            _activeLayerId.value = _layers.value.first().id
        }
        clearSelection()
        updatePerfMetrics()
    }

    fun renameLayer(layerId: Long, newName: String) {
        _layers.update { list ->
            list.map { if (it.id == layerId) it.copy(name = newName) else it }
        }
    }

    fun duplicateLayer(layerId: Long) {
        val target = _layers.value.find { it.id == layerId } ?: return
        val duplicated = target.copy(
            id = System.nanoTime(),
            name = "${target.name} Copy",
            strokes = target.strokes.map { stroke ->
                stroke.copy(
                    id = System.nanoTime() + stroke.id % 1000,
                    points = stroke.points.map { it.copy() }
                )
            }
        )
        pushUndoSnapshot()
        val index = _layers.value.indexOf(target)
        val mutable = _layers.value.toMutableList()
        mutable.add(index + 1, duplicated)
        _layers.value = mutable
        _activeLayerId.value = duplicated.id
        updatePerfMetrics()
    }

    fun toggleLayerVisibility(layerId: Long) {
        _layers.update { list ->
            list.map { if (it.id == layerId) it.copy(isVisible = !it.isVisible) else it }
        }
        updatePerfMetrics()
    }

    fun toggleLayerLock(layerId: Long) {
        _layers.update { list ->
            list.map { if (it.id == layerId) it.copy(isLocked = !it.isLocked) else it }
        }
    }

    fun setLayerOpacity(layerId: Long, opacity: Float) {
        _layers.update { list ->
            list.map { if (it.id == layerId) it.copy(opacity = opacity.coerceIn(0f, 1f)) else it }
        }
    }

    fun moveLayerUp(layerId: Long) {
        val list = _layers.value.toMutableList()
        val index = list.indexOfFirst { it.id == layerId }
        if (index > 0) {
            pushUndoSnapshot()
            val temp = list[index]
            list[index] = list[index - 1]
            list[index - 1] = temp
            _layers.value = list
        }
    }

    fun moveLayerDown(layerId: Long) {
        val list = _layers.value.toMutableList()
        val index = list.indexOfFirst { it.id == layerId }
        if (index >= 0 && index < list.size - 1) {
            pushUndoSnapshot()
            val temp = list[index]
            list[index] = list[index + 1]
            list[index + 1] = temp
            _layers.value = list
        }
    }

    fun mergeLayerDown(layerId: Long) {
        val list = _layers.value
        val index = list.indexOfFirst { it.id == layerId }
        if (index > 0) {
            pushUndoSnapshot()
            val topLayer = list[index]
            val bottomLayer = list[index - 1]

            val mergedStrokes = bottomLayer.strokes + topLayer.strokes
            val mergedLayer = bottomLayer.copy(strokes = mergedStrokes)

            val updated = list.toMutableList().apply {
                removeAt(index)
                set(index - 1, mergedLayer)
            }
            _layers.value = updated
            _activeLayerId.value = mergedLayer.id
            clearSelection()
            updatePerfMetrics()
        } else {
            _statusMessage.value = "Cannot merge bottom layer"
        }
    }

    // ==========================================
    // TOOL MODES & SELECTION
    // ==========================================

    fun setMainToolMode(mode: MainToolMode) {
        _mainToolMode.value = mode
        if (mode != MainToolMode.SELECT) {
            clearSelection()
        }
    }

    fun setSelectionMode(mode: SelectionMode) {
        _selectionMode.value = mode
    }

    fun selectStrokes(strokeIds: Set<Long>) {
        _selectedStrokeIds.value = strokeIds
    }

    fun clearSelection() {
        _selectedStrokeIds.value = emptySet()
    }

    fun selectAll() {
        val active = activeLayer ?: return
        _selectedStrokeIds.value = active.strokes.map { it.id }.toSet()
    }

    fun moveSelection(dx: Float, dy: Float) {
        val active = activeLayer ?: return
        val selIds = _selectedStrokeIds.value
        if (selIds.isEmpty()) return

        pushUndoSnapshot()
        val updatedStrokes = active.strokes.map { stroke ->
            if (selIds.contains(stroke.id)) {
                stroke.copy(points = stroke.points.map { p -> p.copy(x = p.x + dx, y = p.y + dy) })
            } else stroke
        }
        updateActiveLayerStrokes(updatedStrokes)
    }

    fun scaleSelection(scaleX: Float, scaleY: Float, isProportional: Boolean = false) {
        val active = activeLayer ?: return
        val selIds = _selectedStrokeIds.value
        if (selIds.isEmpty()) return

        val selected = active.strokes.filter { selIds.contains(it.id) }
        val bounds = TransformEngine.computeBounds(selected) ?: return

        val finalScaleX = scaleX
        val finalScaleY = if (isProportional) scaleX else scaleY

        pushUndoSnapshot()
        val transformed = TransformEngine.scaleStrokes(selected, bounds.centerX, bounds.centerY, finalScaleX, finalScaleY)
        val map = transformed.associateBy { it.id }

        val updated = active.strokes.map { map[it.id] ?: it }
        updateActiveLayerStrokes(updated)
    }

    fun rotateSelection(degrees: Float) {
        val active = activeLayer ?: return
        val selIds = _selectedStrokeIds.value
        if (selIds.isEmpty()) return

        val selected = active.strokes.filter { selIds.contains(it.id) }
        val bounds = TransformEngine.computeBounds(selected) ?: return

        pushUndoSnapshot()
        val rotated = TransformEngine.rotateStrokes(selected, bounds.centerX, bounds.centerY, degrees)
        val map = rotated.associateBy { it.id }

        val updated = active.strokes.map { map[it.id] ?: it }
        updateActiveLayerStrokes(updated)
    }

    fun copySelection() {
        val active = activeLayer ?: return
        val selected = active.strokes.filter { _selectedStrokeIds.value.contains(it.id) }
        if (selected.isNotEmpty()) {
            internalClipboard.clear()
            internalClipboard.addAll(selected.map { it.copy() })
            _statusMessage.value = "Copied ${selected.size} objects"
        }
    }

    fun cutSelection() {
        val active = activeLayer ?: return
        val selIds = _selectedStrokeIds.value
        val selected = active.strokes.filter { selIds.contains(it.id) }
        if (selected.isNotEmpty()) {
            internalClipboard.clear()
            internalClipboard.addAll(selected.map { it.copy() })

            pushUndoSnapshot()
            val remaining = active.strokes.filterNot { selIds.contains(it.id) }
            updateActiveLayerStrokes(remaining)
            clearSelection()
            _statusMessage.value = "Cut ${selected.size} objects"
        }
    }

    fun pasteClipboard() {
        if (internalClipboard.isEmpty()) {
            _statusMessage.value = "Clipboard is empty"
            return
        }
        val active = activeLayer ?: return
        if (active.isLocked) {
            _statusMessage.value = "Layer Locked"
            return
        }

        pushUndoSnapshot()
        val offset = 20f
        val newStrokes = internalClipboard.map { stroke ->
            stroke.copy(
                id = System.nanoTime() + stroke.id % 1000,
                points = stroke.points.map { p -> p.copy(x = p.x + offset, y = p.y + offset) }
            )
        }

        updateActiveLayerStrokes(active.strokes + newStrokes)
        _selectedStrokeIds.value = newStrokes.map { it.id }.toSet()
        _statusMessage.value = "Pasted ${newStrokes.size} objects"
    }

    fun duplicateSelection() {
        copySelection()
        pasteClipboard()
    }

    fun deleteSelection() {
        val active = activeLayer ?: return
        val selIds = _selectedStrokeIds.value
        if (selIds.isEmpty()) return

        pushUndoSnapshot()
        val remaining = active.strokes.filterNot { selIds.contains(it.id) }
        updateActiveLayerStrokes(remaining)
        clearSelection()
        _statusMessage.value = "Deleted selection"
    }

    fun alignSelection(alignmentType: AlignmentType) {
        val active = activeLayer ?: return
        val selIds = _selectedStrokeIds.value
        if (selIds.isEmpty()) return

        val selected = active.strokes.filter { selIds.contains(it.id) }
        pushUndoSnapshot()
        val aligned = TransformEngine.alignStrokes(selected, alignmentType)
        val map = aligned.associateBy { it.id }

        val updated = active.strokes.map { map[it.id] ?: it }
        updateActiveLayerStrokes(updated)
    }

    // ==========================================
    // SHAPES & FILL TOOL
    // ==========================================

    fun selectShapeType(type: ShapeType) {
        _selectedShapeType.value = type
    }

    fun updateShapeProperties(properties: ShapeProperties) {
        _shapeProperties.value = properties
    }

    fun commitShape(x1: Float, y1: Float, x2: Float, y2: Float) {
        val active = activeLayer ?: return
        if (active.isLocked) {
            _statusMessage.value = "Layer Locked"
            return
        }

        var startX = x1
        var startY = y1
        var endX = x2
        var endY = y2

        if (_isSnapToGridEnabled.value && _gridMode.value != GridMode.OFF) {
            val gridSize = if (_gridMode.value == GridMode.SMALL) 24f else 48f
            startX = round(startX / gridSize) * gridSize
            startY = round(startY / gridSize) * gridSize
            endX = round(endX / gridSize) * gridSize
            endY = round(endY / gridSize) * gridSize
        }

        val props = _shapeProperties.value
        val points = ShapeRecognizer.generateShapePoints(
            shapeType = _selectedShapeType.value,
            x1 = startX,
            y1 = startY,
            x2 = endX,
            y2 = endY,
            properties = props
        )

        val stroke = DrawingStroke(
            points = points,
            color = props.strokeColor,
            strokeWidth = props.strokeWidth,
            opacity = props.strokeOpacity,
            brushType = BrushType.PEN,
            fillColor = props.fillColor,
            fillOpacity = props.fillOpacity,
            isClosed = _selectedShapeType.value != ShapeType.LINE && _selectedShapeType.value != ShapeType.ARROW
        )

        pushUndoSnapshot()
        updateActiveLayerStrokes(active.strokes + stroke)
        updatePerfMetrics()
    }

    fun fillAtPoint(worldX: Float, worldY: Float) {
        val active = activeLayer ?: return
        if (active.isLocked) {
            _statusMessage.value = "Layer Locked"
            return
        }

        // Find topmost closed stroke containing worldX, worldY
        val targetStroke = active.strokes.reversed().find { stroke ->
            stroke.isClosed && ShapeRecognizer.isPointInsideClosedPolygon(worldX, worldY, stroke.points)
        }

        if (targetStroke != null) {
            pushUndoSnapshot()
            val updated = active.strokes.map { stroke ->
                if (stroke.id == targetStroke.id) {
                    stroke.copy(
                        fillColor = _selectedColor.value,
                        fillOpacity = _selectedOpacity.value
                    )
                } else stroke
            }
            updateActiveLayerStrokes(updated)
            _statusMessage.value = "Shape filled"
        } else {
            _statusMessage.value = "Closed shape required"
        }
    }

    // ==========================================
    // GRID, GUIDES, SYMMETRY, BACKGROUND
    // ==========================================

    fun setGridMode(mode: GridMode) {
        _gridMode.value = mode
    }

    fun toggleSnapToGrid() {
        _isSnapToGridEnabled.update { !it }
    }

    fun addGuide(isHorizontal: Boolean, position: Float) {
        _guides.update { it + GuideLine(isHorizontal = isHorizontal, position = position) }
    }

    fun removeGuide(guideId: Long) {
        _guides.update { it.filterNot { g -> g.id == guideId } }
    }

    fun clearGuides() {
        _guides.value = emptyList()
    }

    fun setMirrorMode(mode: MirrorMode) {
        _mirrorMode.value = mode
    }

    fun setSymmetryMode(mode: SymmetryMode) {
        _symmetryMode.value = mode
    }

    fun setCanvasBackground(color: CanvasBackgroundColor) {
        _canvasBackground.value = color
        if (color == CanvasBackgroundColor.TRANSPARENT) {
            _backgroundOpacity.value = 0.0f
        } else if (_backgroundOpacity.value == 0.0f) {
            _backgroundOpacity.value = 1.0f
        }
    }

    fun setBackgroundOpacity(opacity: Float) {
        _backgroundOpacity.value = opacity.coerceIn(0.0f, 1.0f)
    }

    fun setCustomBackgroundColor(color: Long) {
        _customBackgroundColor.value = color
    }

    fun toggleBackgroundLock() {
        _isBackgroundLocked.update { !it }
    }

    // ==========================================
    // ANIMATION REPLAY
    // ==========================================

    fun startReplay() {
        val currentLayers = _layers.value
        val allStrokes = currentLayers.flatMap { it.strokes }
        if (allStrokes.isEmpty()) {
            _statusMessage.value = "No strokes to replay"
            return
        }

        replayJob?.cancel()
        _isReplaying.value = true
        _isReplayPaused.value = false
        _replayProgress.value = 0.0f

        replayJob = viewModelScope.launch {
            val emptyLayers = currentLayers.map { it.copy(strokes = emptyList()) }
            _replayedLayers.value = emptyLayers

            val totalStrokes = allStrokes.size
            var completedCount = 0

            // Collect all strokes in layer/order
            for (layer in currentLayers) {
                if (!layer.isVisible) continue
                for (stroke in layer.strokes) {
                    while (_isReplayPaused.value) {
                        kotlinx.coroutines.delay(50)
                    }

                    // Animate stroke point by point
                    val points = stroke.points
                    val step = (points.size / 6).coerceAtLeast(1)
                    for (i in 1..points.size step step) {
                        while (_isReplayPaused.value) {
                            kotlinx.coroutines.delay(50)
                        }
                        val partialStroke = stroke.copy(points = points.take(i))
                        _replayedLayers.update { layers ->
                            layers?.map { l ->
                                if (l.id == layer.id) {
                                    val existingWithoutThis = l.strokes.filterNot { it.id == stroke.id }
                                    l.copy(strokes = existingWithoutThis + partialStroke)
                                } else l
                            }
                        }
                        val delayMs = (25 / _replaySpeed.value).toLong().coerceAtLeast(5)
                        kotlinx.coroutines.delay(delayMs)
                    }

                    // Put final complete stroke
                    _replayedLayers.update { layers ->
                        layers?.map { l ->
                            if (l.id == layer.id) {
                                val existingWithoutThis = l.strokes.filterNot { it.id == stroke.id }
                                l.copy(strokes = existingWithoutThis + stroke)
                            } else l
                        }
                    }

                    completedCount++
                    _replayProgress.value = completedCount.toFloat() / totalStrokes.toFloat()
                }
            }

            kotlinx.coroutines.delay(500)
            _isReplaying.value = false
            _replayedLayers.value = null
            _statusMessage.value = "Replay completed"
        }
    }

    fun pauseReplay() {
        _isReplayPaused.value = true
    }

    fun resumeReplay() {
        _isReplayPaused.value = false
    }

    fun restartReplay() {
        startReplay()
    }

    fun stopReplay() {
        replayJob?.cancel()
        _isReplaying.value = false
        _isReplayPaused.value = false
        _replayedLayers.value = null
    }

    fun setReplaySpeed(speed: Float) {
        _replaySpeed.value = speed.coerceIn(0.25f, 4.0f)
    }

    // ==========================================
    // BRUSH CONFIG & TOUCH EVENTS
    // ==========================================

    fun selectBrush(brushType: BrushType) {
        val current = _selectedBrush.value
        toolMemory[current] = ToolConfig(
            size = _selectedWidth.value,
            opacity = _selectedOpacity.value,
            color = _selectedColor.value
        )
        _selectedBrush.value = brushType
        toolMemory[brushType]?.let { config ->
            _selectedWidth.value = config.size
            _selectedOpacity.value = config.opacity
            if (brushType != BrushType.ERASER) {
                _selectedColor.value = config.color
            }
        }
    }

    fun selectColor(color: Long) {
        _selectedColor.value = color
        if (_selectedBrush.value == BrushType.ERASER) {
            _selectedBrush.value = BrushType.PEN
        }
        toolMemory[_selectedBrush.value] = ToolConfig(
            size = _selectedWidth.value,
            opacity = _selectedOpacity.value,
            color = color
        )
        _recentColors.update { list ->
            (listOf(color) + list.filter { it != color }).take(maxRecentColors)
        }
    }

    fun clearRecentColors() {
        _recentColors.value = emptyList()
    }

    fun toggleFavoriteColor(color: Long) {
        _favoriteColors.update { list ->
            if (list.contains(color)) list.filter { it != color }
            else (list + color).take(12)
        }
    }

    fun selectWidth(width: Float) {
        _selectedWidth.value = width.coerceIn(1.0f, 64.0f)
        toolMemory[_selectedBrush.value] = ToolConfig(
            size = _selectedWidth.value,
            opacity = _selectedOpacity.value,
            color = _selectedColor.value
        )
    }

    fun selectOpacity(opacity: Float) {
        _selectedOpacity.value = opacity.coerceIn(0.1f, 1.0f)
        toolMemory[_selectedBrush.value] = ToolConfig(
            size = _selectedWidth.value,
            opacity = _selectedOpacity.value,
            color = _selectedColor.value
        )
    }

    fun setEraserMode(mode: EraserMode) {
        _eraserMode.value = mode
    }

    fun toggleStraightLineMode() {
        _isStraightLineMode.update { !it }
    }

    fun toggleShapeAssist() {
        _isShapeAssistEnabled.update { !it }
    }

    fun setSmoothnessLevel(level: SmoothnessLevel) {
        _smoothnessLevel.value = level
    }

    fun setPressureCurve(curve: PressureCurve) {
        _pressureCurve.value = curve
    }

    fun setCustomPressureFactor(factor: Float) {
        _customPressureFactor.value = factor.coerceIn(0.4f, 2.5f)
    }

    fun toggleTilt(enabled: Boolean) {
        _isTiltEnabled.value = enabled
    }

    fun toggleStylusToolSwitch() {
        if (_selectedBrush.value == BrushType.ERASER) selectBrush(BrushType.PEN)
        else selectBrush(BrushType.ERASER)
    }

    fun togglePerformanceMonitor() {
        _showPerformanceMonitor.update { !it }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // Touch Event Callbacks
    fun onStrokeStart(points: List<DrawingPoint>, toolType: ToolType) {
        val active = activeLayer
        if (active == null || active.isLocked || !active.isVisible) {
            if (active?.isLocked == true) {
                _statusMessage.value = "Layer Locked"
            }
            return
        }

        totalInputEvents++
        recordLatency(points.firstOrNull()?.timestamp ?: System.currentTimeMillis())

        val isEraser = _selectedBrush.value == BrushType.ERASER || toolType == ToolType.ERASER
        val stroke = DrawingStroke(
            points = points.toList(),
            color = if (isEraser) 0xFF000000 else _selectedColor.value,
            strokeWidth = _selectedWidth.value,
            opacity = if (isEraser) 1.0f else _selectedOpacity.value,
            brushType = if (isEraser) BrushType.ERASER else _selectedBrush.value,
            toolType = toolType,
            isEraser = isEraser,
            eraserMode = _eraserMode.value
        )
        _currentStroke.value = stroke
        updatePerfMetrics()
    }

    fun onStrokeMove(points: List<DrawingPoint>) {
        if (_currentStroke.value == null) return
        totalInputEvents++
        recordLatency(points.lastOrNull()?.timestamp ?: System.currentTimeMillis())

        _currentStroke.update { curr ->
            curr?.copy(points = points.toList())
        }
        updatePerfMetrics()
    }

    fun onStrokeEnd(points: List<DrawingPoint>, canvasCenterX: Float = 540f, canvasCenterY: Float = 960f) {
        val curr = _currentStroke.value ?: return
        val active = activeLayer ?: return

        if (points.isNotEmpty()) {
            val isEraser = curr.isEraser

            if (isEraser && curr.eraserMode == EraserMode.STROKE) {
                // Stroke Eraser mode
                val radius = curr.strokeWidth / 2f
                val remaining = active.strokes.filterNot { existingStroke ->
                    points.any { p ->
                        SmoothStrokeEngine.doesStrokeIntersectPoint(existingStroke, p.x, p.y, radius)
                    }
                }
                if (remaining.size != active.strokes.size) {
                    pushUndoSnapshot()
                    updateActiveLayerStrokes(remaining)
                }
            } else {
                var finalPoints = points.toList()
                var isClosed = false

                if (_isStraightLineMode.value && points.size >= 2) {
                    val pFirst = points.first()
                    val pLast = points.last()
                    val linePoints = mutableListOf<DrawingPoint>()
                    val steps = 8
                    for (i in 0..steps) {
                        val t = i.toFloat() / steps
                        linePoints.add(
                            DrawingPoint(
                                x = pFirst.x + t * (pLast.x - pFirst.x),
                                y = pFirst.y + t * (pLast.y - pFirst.y),
                                pressure = pLast.pressure
                            )
                        )
                    }
                    finalPoints = linePoints
                } else if (_isShapeAssistEnabled.value && !isEraser) {
                    val recognition = ShapeRecognizer.recognize(points)
                    if (recognition.type != RecognizedShapeType.NONE && recognition.perfectedPoints != null) {
                        finalPoints = recognition.perfectedPoints
                        isClosed = recognition.type != RecognizedShapeType.LINE
                    }
                }

                val finalizedStroke = curr.copy(points = finalPoints, isClosed = isClosed)

                // Generate symmetrical/mirrored strokes if enabled
                val symmetricStrokes = TransformEngine.generateSymmetricStrokes(
                    baseStroke = finalizedStroke,
                    canvasCenterX = canvasCenterX,
                    canvasCenterY = canvasCenterY,
                    mirrorMode = _mirrorMode.value,
                    symmetryMode = _symmetryMode.value
                )

                pushUndoSnapshot()
                updateActiveLayerStrokes(active.strokes + symmetricStrokes)
            }
        }
        _currentStroke.value = null
        updatePerfMetrics()
    }

    fun onStrokeCancel() {
        _currentStroke.value = null
        totalDroppedEvents++
        updatePerfMetrics()
    }

    fun onInputDropped() {
        totalDroppedEvents++
        updatePerfMetrics()
    }

    // ==========================================
    // UNDO / REDO & CLEANUP
    // ==========================================

    private fun pushUndoSnapshot() {
        undoStack.addLast(_layers.value.map { layer ->
            layer.copy(strokes = layer.strokes.map { it.copy() })
        })
        if (undoStack.size > maxHistorySize) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        _canUndo.value = true
        _canRedo.value = false
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previousState = undoStack.removeLast()
            redoStack.addLast(_layers.value.map { layer ->
                layer.copy(strokes = layer.strokes.map { it.copy() })
            })
            if (redoStack.size > maxHistorySize) {
                redoStack.removeFirst()
            }
            _layers.value = previousState
            _canUndo.value = undoStack.isNotEmpty()
            _canRedo.value = true
            clearSelection()
            updatePerfMetrics()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val nextState = redoStack.removeLast()
            undoStack.addLast(_layers.value.map { layer ->
                layer.copy(strokes = layer.strokes.map { it.copy() })
            })
            if (undoStack.size > maxHistorySize) {
                undoStack.removeFirst()
            }
            _layers.value = nextState
            _canUndo.value = true
            _canRedo.value = redoStack.isNotEmpty()
            clearSelection()
            updatePerfMetrics()
        }
    }

    fun clearActiveLayer() {
        val active = activeLayer ?: return
        if (active.strokes.isNotEmpty()) {
            pushUndoSnapshot()
            updateActiveLayerStrokes(emptyList())
            clearSelection()
            updatePerfMetrics()
        }
    }

    fun discardSession() {
        val newDefaultLayer = DrawingLayer(name = "Layer 1")
        _layers.value = listOf(newDefaultLayer)
        _activeLayerId.value = newDefaultLayer.id
        _currentStroke.value = null
        _selectedStrokeIds.value = emptySet()
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
        updatePerfMetrics()
    }

    private fun updateActiveLayerStrokes(strokes: List<DrawingStroke>) {
        val activeId = _activeLayerId.value
        _layers.update { list ->
            list.map { if (it.id == activeId) it.copy(strokes = strokes) else it }
        }
    }

    private fun recordLatency(eventTimestamp: Long) {
        val now = System.currentTimeMillis()
        val latency = (now - eventTimestamp).coerceAtLeast(0).toFloat()
        latencySum += latency
        latencyCount++
    }

    fun updateFpsMetric(fps: Int) {
        _performanceMetrics.update { it.copy(renderFps = fps, strokeFps = fps) }
    }

    private fun updatePerfMetrics() {
        val avgLatency = if (latencyCount > 0) latencySum / latencyCount else 1.2f
        val allStrokes = _layers.value.flatMap { it.strokes }
        val totalPoints = allStrokes.sumOf { it.points.size } + (_currentStroke.value?.points?.size ?: 0)
        val runtime = Runtime.getRuntime()
        val memoryUsedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024f * 1024f)

        _performanceMetrics.update {
            it.copy(
                inputEvents = totalInputEvents,
                avgLatencyMs = avgLatency,
                droppedEvents = totalDroppedEvents,
                strokePoints = totalPoints,
                activeStrokesCount = allStrokes.size + (if (_currentStroke.value != null) 1 else 0),
                memoryUsageMb = memoryUsedMb
            )
        }
    }

    // ==========================================
    // EXPORT & SAVE (IMAGE + PROJECT FORMAT)
    // ==========================================

    fun saveDrawing(
        canvasWidth: Int,
        canvasHeight: Int,
        format: SaveFormat = SaveFormat.PNG_NORMAL
    ) {
        val activeLayers = _layers.value.filter { it.isVisible && it.strokes.isNotEmpty() }
        if (activeLayers.isEmpty() && _currentStroke.value == null) {
            _statusMessage.value = "Canvas is empty, nothing to save"
            return
        }

        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val width = if (canvasWidth > 0) canvasWidth else 1080
                val height = if (canvasHeight > 0) canvasHeight else 1920

                val savedUri = withContext(Dispatchers.Default) {
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)

                    // Render Background
                    if (format == SaveFormat.PNG_TRANSPARENT) {
                        canvas.drawColor(AndroidColor.TRANSPARENT)
                    } else {
                        val bg = _canvasBackground.value
                        val opacity = _backgroundOpacity.value
                        if (bg != CanvasBackgroundColor.TRANSPARENT && opacity > 0.001f) {
                            val baseColor = when (bg) {
                                CanvasBackgroundColor.TRANSPARENT -> 0x00000000
                                CanvasBackgroundColor.BLACK -> 0xFF000000.toInt()
                                CanvasBackgroundColor.DARK_GRAY -> AndroidColor.rgb(30, 32, 40)
                                CanvasBackgroundColor.DARK_BLUE -> AndroidColor.rgb(13, 27, 42)
                                CanvasBackgroundColor.DARK_PURPLE -> AndroidColor.rgb(27, 10, 42)
                                CanvasBackgroundColor.WHITE -> AndroidColor.WHITE
                                CanvasBackgroundColor.CUSTOM -> _customBackgroundColor.value.toInt()
                            }
                            val alpha = (opacity * 255).toInt().coerceIn(0, 255)
                            val finalColor = (baseColor and 0x00FFFFFF) or (alpha shl 24)
                            canvas.drawColor(finalColor)
                        } else {
                            canvas.drawColor(AndroidColor.TRANSPARENT)
                        }
                    }

                    val paint = Paint()
                    val fillPaint = Paint().apply {
                        isAntiAlias = true
                        style = Paint.Style.FILL
                    }
                    val path = Path()

                    for (layer in activeLayers) {
                        for (stroke in layer.strokes) {
                            if (stroke.points.size < 2) continue

                            SmoothStrokeEngine.buildSmoothPath(stroke.points, path)

                            // Render Fill if present
                            if (stroke.isClosed && stroke.fillColor != 0x00000000L && stroke.fillOpacity > 0f) {
                                val fillAlpha = ((stroke.fillColor shr 24 and 0xFF) * stroke.fillOpacity * layer.opacity / 255f).toInt().coerceIn(0, 255)
                                fillPaint.color = (stroke.fillColor.toInt() and 0x00FFFFFF) or (fillAlpha shl 24)
                                canvas.drawPath(path, fillPaint)
                            }

                            // Render Stroke
                            SmoothStrokeEngine.configurePaintForBrush(
                                paint = paint,
                                brushType = stroke.brushType,
                                color = stroke.color,
                                opacity = stroke.opacity * layer.opacity,
                                strokeWidth = stroke.strokeWidth
                            )
                            canvas.drawPath(path, paint)
                        }
                    }

                    saveBitmapToMediaStore(bitmap, context, isTransparent = (format == SaveFormat.PNG_TRANSPARENT))
                }

                // Also export internal vector project json alongside
                exportProjectJson()

                if (savedUri != null) {
                    val label = if (format == SaveFormat.PNG_TRANSPARENT) "Transparent PNG" else "PNG & Project JSON"
                    _statusMessage.value = "Saved $label to Pictures/BlackCanvas"
                } else {
                    _statusMessage.value = "Failed to save image"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Save error: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    private fun exportProjectJson() {
        try {
            val root = JSONObject()
            root.put("version", 2)
            root.put("timestamp", System.currentTimeMillis())
            root.put("background", _canvasBackground.value.name)

            val layersArray = JSONArray()
            for (layer in _layers.value) {
                val lObj = JSONObject()
                lObj.put("id", layer.id)
                lObj.put("name", layer.name)
                lObj.put("isVisible", layer.isVisible)
                lObj.put("isLocked", layer.isLocked)
                lObj.put("opacity", layer.opacity.toDouble())

                val strokesArray = JSONArray()
                for (stroke in layer.strokes) {
                    val sObj = JSONObject()
                    sObj.put("id", stroke.id)
                    sObj.put("brushType", stroke.brushType.name)
                    sObj.put("color", stroke.color)
                    sObj.put("strokeWidth", stroke.strokeWidth.toDouble())
                    sObj.put("opacity", stroke.opacity.toDouble())
                    sObj.put("fillColor", stroke.fillColor)
                    sObj.put("fillOpacity", stroke.fillOpacity.toDouble())
                    sObj.put("isClosed", stroke.isClosed)

                    val ptsArray = JSONArray()
                    for (p in stroke.points) {
                        val pObj = JSONObject()
                        pObj.put("x", p.x.toDouble())
                        pObj.put("y", p.y.toDouble())
                        pObj.put("p", p.pressure.toDouble())
                        pObj.put("t", p.tiltRad.toDouble())
                        ptsArray.put(pObj)
                    }
                    sObj.put("points", ptsArray)
                    strokesArray.put(sObj)
                }
                lObj.put("strokes", strokesArray)
                layersArray.put(lObj)
            }
            root.put("layers", layersArray)

            // Save to app internal files directory
            val context = getApplication<Application>()
            val projectFile = File(context.filesDir, "latest_canvas_project.json")
            projectFile.writeText(root.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveBitmapToMediaStore(bitmap: Bitmap, context: Application, isTransparent: Boolean = false): Uri? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val suffix = if (isTransparent) "_transparent" else ""
        val filename = "BlackCanvas_$timeStamp$suffix.png"

        var outputStream: OutputStream? = null
        var imageUri: Uri? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + File.separator + "BlackCanvas"
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                if (imageUri != null) {
                    outputStream = resolver.openOutputStream(imageUri)
                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val canvasDir = File(imagesDir, "BlackCanvas")
                if (!canvasDir.exists()) {
                    canvasDir.mkdirs()
                }
                val imageFile = File(canvasDir, filename)
                outputStream = FileOutputStream(imageFile)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                imageUri = Uri.fromFile(imageFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            outputStream?.close()
        }

        return imageUri
    }
}
