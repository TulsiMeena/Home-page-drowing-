package com.example.ui.canvas

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.BrushType
import com.example.model.CanvasBackgroundColor
import com.example.model.MainToolMode
import com.example.model.SaveFormat
import com.example.viewmodel.DrawingViewModel
import kotlinx.coroutines.delay

@Composable
fun DrawingScreen(
    viewModel: DrawingViewModel,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Layers & Tools State
    val layers by viewModel.layers.collectAsState()
    val activeLayerId by viewModel.activeLayerId.collectAsState()
    val mainToolMode by viewModel.mainToolMode.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()
    val selectedStrokeIds by viewModel.selectedStrokeIds.collectAsState()
    val selectedShapeType by viewModel.selectedShapeType.collectAsState()
    val shapeProperties by viewModel.shapeProperties.collectAsState()

    // Grid, Guides, Symmetry & Background
    val gridMode by viewModel.gridMode.collectAsState()
    val isSnapToGrid by viewModel.isSnapToGridEnabled.collectAsState()
    val guides by viewModel.guides.collectAsState()
    val mirrorMode by viewModel.mirrorMode.collectAsState()
    val symmetryMode by viewModel.symmetryMode.collectAsState()
    val canvasBackground by viewModel.canvasBackground.collectAsState()
    val backgroundOpacity by viewModel.backgroundOpacity.collectAsState()
    val customBackgroundColor by viewModel.customBackgroundColor.collectAsState()
    val isBackgroundLocked by viewModel.isBackgroundLocked.collectAsState()

    // Replay State
    val isReplaying by viewModel.isReplaying.collectAsState()
    val isReplayPaused by viewModel.isReplayPaused.collectAsState()
    val replaySpeed by viewModel.replaySpeed.collectAsState()
    val replayProgress by viewModel.replayProgress.collectAsState()
    val replayedLayers by viewModel.replayedLayers.collectAsState()

    // Brush State
    val selectedBrush by viewModel.selectedBrush.collectAsState()
    val selectedColor by viewModel.selectedColor.collectAsState()
    val selectedWidth by viewModel.selectedWidth.collectAsState()
    val selectedOpacity by viewModel.selectedOpacity.collectAsState()
    val eraserMode by viewModel.eraserMode.collectAsState()
    val isStraightLineMode by viewModel.isStraightLineMode.collectAsState()
    val isShapeAssistEnabled by viewModel.isShapeAssistEnabled.collectAsState()

    val smoothnessLevel by viewModel.smoothnessLevel.collectAsState()
    val pressureCurve by viewModel.pressureCurve.collectAsState()
    val customPressureFactor by viewModel.customPressureFactor.collectAsState()
    val isTiltEnabled by viewModel.isTiltEnabled.collectAsState()

    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val recentColors by viewModel.recentColors.collectAsState()
    val favoriteColors by viewModel.favoriteColors.collectAsState()

    val showPerformanceMonitor by viewModel.showPerformanceMonitor.collectAsState()
    val performanceMetrics by viewModel.performanceMetrics.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    // Popups Visibility
    var isControlsExpanded by remember { mutableStateOf(false) }
    var showLayersPopup by remember { mutableStateOf(false) }
    var showCanvasToolsPopup by remember { mutableStateOf(false) }
    var showToolSettingsPopup by remember { mutableStateOf<BrushType?>(null) }
    var showColorPickerPopup by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showSaveFormatDialog by remember { mutableStateOf(false) }
    var showExitConfirmation by remember { mutableStateOf(false) }

    var canvasViewRef by remember { mutableStateOf<BlackCanvasView?>(null) }

    // Auto-hide controls after 5 seconds of inactivity if expanded
    LaunchedEffect(isControlsExpanded) {
        if (isControlsExpanded) {
            delay(5000)
            if (!showLayersPopup && !showCanvasToolsPopup && showToolSettingsPopup == null &&
                !showColorPickerPopup && !showSettingsDialog && !showClearConfirmation && !showSaveFormatDialog
            ) {
                isControlsExpanded = false
            }
        }
    }

    // Status / Notice Toast
    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearStatusMessage()
        }
    }

    val hasUnsavedChanges = remember(layers) {
        layers.any { it.strokes.isNotEmpty() }
    }

    BackHandler {
        if (hasUnsavedChanges) {
            showExitConfirmation = true
        } else {
            viewModel.discardSession()
            onNavigateHome()
        }
    }

    // Background color for parent Box
    val boxBackground = if (canvasBackground == CanvasBackgroundColor.TRANSPARENT || backgroundOpacity <= 0.001f) {
        Color.Transparent
    } else {
        val baseColor = when (canvasBackground) {
            CanvasBackgroundColor.TRANSPARENT -> Color.Transparent
            CanvasBackgroundColor.BLACK -> Color.Black
            CanvasBackgroundColor.DARK_GRAY -> Color(0xFF1E2028)
            CanvasBackgroundColor.DARK_BLUE -> Color(0xFF0D1B2A)
            CanvasBackgroundColor.DARK_PURPLE -> Color(0xFF1B0A2A)
            CanvasBackgroundColor.WHITE -> Color.White
            CanvasBackgroundColor.CUSTOM -> Color(customBackgroundColor)
        }
        baseColor.copy(alpha = backgroundOpacity)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(boxBackground)
            .testTag("drawing_screen_container")
    ) {
        // High-Performance Hardware Accelerated Canvas View
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("black_canvas_surface"),
            factory = { ctx ->
                BlackCanvasView(ctx).apply {
                    canvasViewRef = this
                    onStrokeStartListener = { points, toolType ->
                        if (isControlsExpanded) {
                            isControlsExpanded = false
                        }
                        showLayersPopup = false
                        showCanvasToolsPopup = false
                        showToolSettingsPopup = null
                        showColorPickerPopup = false
                        showSettingsDialog = false
                        viewModel.onStrokeStart(points, toolType)
                    }
                    onStrokeEndListener = { points, cx, cy ->
                        viewModel.onStrokeEnd(points, cx, cy)
                    }
                    onStrokeCancelListener = {
                        viewModel.onStrokeCancel()
                    }
                    onInputDroppedListener = {
                        viewModel.onInputDropped()
                    }
                    onFpsCalculated = { fps ->
                        viewModel.updateFpsMetric(fps)
                    }
                    onStylusButtonToggleListener = {
                        viewModel.toggleStylusToolSwitch()
                    }
                    onSelectionCompleted = { matchedIds ->
                        viewModel.selectStrokes(matchedIds)
                    }
                    onSelectionMoved = { dx, dy ->
                        viewModel.moveSelection(dx, dy)
                    }
                    onShapeCommitted = { x1, y1, x2, y2 ->
                        viewModel.commitShape(x1, y1, x2, y2)
                    }
                    onFillTapped = { wx, wy ->
                        viewModel.fillAtPoint(wx, wy)
                    }
                }
            },
            update = { view ->
                val displayLayers = replayedLayers ?: layers
                view.updateLayers(displayLayers, activeLayerId)
                view.updateToolMode(mainToolMode, selectionMode, selectedStrokeIds)
                view.updateShapeTool(selectedShapeType, shapeProperties)
                view.updateGridAndGuides(
                    grid = gridMode,
                    guides = guides,
                    mirror = mirrorMode,
                    symmetry = symmetryMode,
                    bg = canvasBackground,
                    bgOpacity = backgroundOpacity,
                    customBg = customBackgroundColor
                )
                view.updateBrushConfig(
                    brush = selectedBrush,
                    color = selectedColor,
                    width = selectedWidth,
                    opacity = selectedOpacity,
                    eraserMode = eraserMode,
                    straightLineMode = isStraightLineMode
                )
                view.updateEngineSettings(
                    smoothness = smoothnessLevel,
                    pressureCurve = pressureCurve,
                    customFactor = customPressureFactor,
                    tiltEnabled = isTiltEnabled
                )
            }
        )

        // Top-Bar: Back/Close navigation
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (hasUnsavedChanges) {
                            showExitConfirmation = true
                        } else {
                            viewModel.discardSession()
                            onNavigateHome()
                        }
                    },
                shape = CircleShape,
                color = Color(0xCC14161F),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Drawing Overlay",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Performance HUD overlay
        if (showPerformanceMonitor) {
            PerformanceOverlay(
                metrics = performanceMetrics,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp)
            )
        }

        // Animation Replay Top Banner
        if (isReplaying) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp)
                    .width(340.dp)
                    .clip(RoundedCornerShape(16.dp)),
                color = Color(0xEE161824),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎬 Stroke Replay",
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Play / Pause
                            IconButton(
                                onClick = {
                                    if (isReplayPaused) viewModel.resumeReplay() else viewModel.pauseReplay()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isReplayPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            // Restart
                            IconButton(
                                onClick = { viewModel.restartReplay() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = "Restart",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            // Stop / Close
                            IconButton(
                                onClick = { viewModel.stopReplay() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Stop",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { replayProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color(0xFF00E5FF),
                        trackColor = Color(0x33FFFFFF)
                    )

                    // Replay Speed Options: 0.25x, 0.5x, 1x, 2x, 4x
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(0.25f to "0.25x", 0.5f to "0.5x", 1.0f to "1x", 2.0f to "2x", 4.0f to "4x").forEach { (spd, lbl) ->
                            val isSelected = kotlin.math.abs(replaySpeed - spd) < 0.05f
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Color(0xFF00E5FF) else Color(0x22FFFFFF))
                                    .clickable { viewModel.setReplaySpeed(spd) }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = lbl,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Layers Panel Popup
        if (showLayersPopup) {
            LayerPanelPopup(
                layers = layers,
                activeLayerId = activeLayerId,
                onSelectLayer = { viewModel.selectActiveLayer(it) },
                onCreateLayer = { viewModel.createLayer() },
                onDeleteLayer = { viewModel.deleteLayer(it) },
                onRenameLayer = { id, name -> viewModel.renameLayer(id, name) },
                onDuplicateLayer = { viewModel.duplicateLayer(it) },
                onToggleVisibility = { viewModel.toggleLayerVisibility(it) },
                onToggleLock = { viewModel.toggleLayerLock(it) },
                onOpacityChanged = { id, opacity -> viewModel.setLayerOpacity(id, opacity) },
                onMoveLayerUp = { viewModel.moveLayerUp(it) },
                onMoveLayerDown = { viewModel.moveLayerDown(it) },
                onMergeLayerDown = { viewModel.mergeLayerDown(it) },
                onDismiss = { showLayersPopup = false },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 74.dp)
            )
        }

        // Canvas & Symmetry Tools Popup
        if (showCanvasToolsPopup) {
            CanvasToolsPopup(
                gridMode = gridMode,
                isSnapToGrid = isSnapToGrid,
                mirrorMode = mirrorMode,
                symmetryMode = symmetryMode,
                canvasBackground = canvasBackground,
                backgroundOpacity = backgroundOpacity,
                isBackgroundLocked = isBackgroundLocked,
                onSetGridMode = { viewModel.setGridMode(it) },
                onToggleSnapToGrid = { viewModel.toggleSnapToGrid() },
                onSetMirrorMode = { viewModel.setMirrorMode(it) },
                onSetSymmetryMode = { viewModel.setSymmetryMode(it) },
                onSetCanvasBackground = { viewModel.setCanvasBackground(it) },
                onSetBackgroundOpacity = { viewModel.setBackgroundOpacity(it) },
                onToggleBackgroundLock = { viewModel.toggleBackgroundLock() },
                onAddGuide = { isH ->
                    val pos = if (isH) (canvasViewRef?.height ?: 1000) / 2f else (canvasViewRef?.width ?: 1000) / 2f
                    viewModel.addGuide(isH, pos)
                },
                onClearGuides = { viewModel.clearGuides() },
                onSmartCenter = {
                    canvasViewRef?.smartCenterArtwork()
                },
                onDismiss = { showCanvasToolsPopup = false },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 74.dp)
            )
        }

        // Selection Contextual Toolbar
        if (mainToolMode == MainToolMode.SELECT) {
            SelectionToolbar(
                selectionMode = selectionMode,
                selectedCount = selectedStrokeIds.size,
                onSetSelectionMode = { viewModel.setSelectionMode(it) },
                onSelectAll = { viewModel.selectAll() },
                onClearSelection = { viewModel.clearSelection() },
                onMove = { dx, dy -> viewModel.moveSelection(dx, dy) },
                onScale = { sx, sy, prop -> viewModel.scaleSelection(sx, sy, prop) },
                onRotate = { deg -> viewModel.rotateSelection(deg) },
                onCopy = { viewModel.copySelection() },
                onCut = { viewModel.cutSelection() },
                onPaste = { viewModel.pasteClipboard() },
                onDuplicate = { viewModel.duplicateSelection() },
                onDelete = { viewModel.deleteSelection() },
                onAlign = { viewModel.alignSelection(it) },
                onDismiss = { viewModel.setMainToolMode(MainToolMode.DRAW) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 76.dp)
            )
        }

        // Shapes Contextual Toolbar
        if (mainToolMode == MainToolMode.SHAPES) {
            ShapesToolbar(
                selectedShapeType = selectedShapeType,
                shapeProperties = shapeProperties,
                onSelectShapeType = { viewModel.selectShapeType(it) },
                onUpdateShapeProperties = { viewModel.updateShapeProperties(it) },
                onDismiss = { viewModel.setMainToolMode(MainToolMode.DRAW) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 76.dp)
            )
        }

        // Tool Size & Opacity Popup
        showToolSettingsPopup?.let { activeBrush ->
            ToolSizeOpacityPopup(
                brushType = activeBrush,
                currentColor = selectedColor,
                currentSize = selectedWidth,
                currentOpacity = selectedOpacity,
                eraserMode = eraserMode,
                onSizeChange = { viewModel.selectWidth(it) },
                onOpacityChange = { viewModel.selectOpacity(it) },
                onEraserModeChange = { viewModel.setEraserMode(it) },
                onDismiss = { showToolSettingsPopup = null },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 74.dp)
            )
        }

        // Color Picker Studio Popup
        if (showColorPickerPopup) {
            ColorPickerPopup(
                currentColor = selectedColor,
                currentOpacity = selectedOpacity,
                recentColors = recentColors,
                favoriteColors = favoriteColors,
                onColorSelected = { viewModel.selectColor(it) },
                onOpacitySelected = { viewModel.selectOpacity(it) },
                onToggleFavorite = { viewModel.toggleFavoriteColor(it) },
                onClearRecent = { viewModel.clearRecentColors() },
                onDismiss = { showColorPickerPopup = false },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 74.dp)
            )
        }

        // Stylus & Engine Settings Dialog
        if (showSettingsDialog) {
            AdvancedSettingsDialog(
                smoothnessLevel = smoothnessLevel,
                pressureCurve = pressureCurve,
                customPressureFactor = customPressureFactor,
                isTiltEnabled = isTiltEnabled,
                showPerformanceMonitor = showPerformanceMonitor,
                onSmoothnessChanged = { viewModel.setSmoothnessLevel(it) },
                onPressureCurveChanged = { viewModel.setPressureCurve(it) },
                onCustomPressureFactorChanged = { viewModel.setCustomPressureFactor(it) },
                onTiltToggled = { viewModel.toggleTilt(it) },
                onPerformanceMonitorToggled = { viewModel.togglePerformanceMonitor() },
                onDismiss = { showSettingsDialog = false },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 74.dp)
            )
        }

        // Floating Primary Toolbar
        FloatingControls(
            mainToolMode = mainToolMode,
            selectedBrush = selectedBrush,
            selectedColor = selectedColor,
            selectedWidth = selectedWidth,
            selectedOpacity = selectedOpacity,
            layersCount = layers.size,
            isStraightLineMode = isStraightLineMode,
            isShapeAssistEnabled = isShapeAssistEnabled,
            canUndo = canUndo,
            canRedo = canRedo,
            favoriteColors = favoriteColors,
            isExpanded = isControlsExpanded,
            onToggleExpand = {
                isControlsExpanded = !isControlsExpanded
                if (!isControlsExpanded) {
                    showLayersPopup = false
                    showCanvasToolsPopup = false
                    showToolSettingsPopup = null
                    showColorPickerPopup = false
                    showSettingsDialog = false
                }
            },
            onSetMainToolMode = { mode ->
                viewModel.setMainToolMode(mode)
                showLayersPopup = false
                showCanvasToolsPopup = false
                showToolSettingsPopup = null
                showColorPickerPopup = false
                showSettingsDialog = false
            },
            onSelectBrush = { brush ->
                viewModel.selectBrush(brush)
                showLayersPopup = false
                showCanvasToolsPopup = false
                showColorPickerPopup = false
                showSettingsDialog = false
            },
            onOpenToolSettings = { brush ->
                showToolSettingsPopup = if (showToolSettingsPopup == brush) null else brush
                showLayersPopup = false
                showCanvasToolsPopup = false
                showColorPickerPopup = false
                showSettingsDialog = false
            },
            onOpenColorPicker = {
                showColorPickerPopup = !showColorPickerPopup
                showLayersPopup = false
                showCanvasToolsPopup = false
                showToolSettingsPopup = null
                showSettingsDialog = false
            },
            onSelectQuickColor = { color ->
                viewModel.selectColor(color)
            },
            onOpenLayersPanel = {
                showLayersPopup = !showLayersPopup
                showCanvasToolsPopup = false
                showToolSettingsPopup = null
                showColorPickerPopup = false
                showSettingsDialog = false
            },
            onOpenCanvasTools = {
                showCanvasToolsPopup = !showCanvasToolsPopup
                showLayersPopup = false
                showToolSettingsPopup = null
                showColorPickerPopup = false
                showSettingsDialog = false
            },
            onToggleStraightLine = {
                viewModel.toggleStraightLineMode()
            },
            onToggleShapeAssist = {
                viewModel.toggleShapeAssist()
            },
            onUndo = { viewModel.undo() },
            onRedo = { viewModel.redo() },
            onResetView = {
                canvasViewRef?.resetView()
                Toast.makeText(context, "Canvas view reset to 100%", Toast.LENGTH_SHORT).show()
            },
            onClear = {
                val hasStrokes = layers.any { it.strokes.isNotEmpty() }
                if (hasStrokes) {
                    showClearConfirmation = true
                }
            },
            onSave = {
                showSaveFormatDialog = true
            },
            onOpenSettings = {
                showSettingsDialog = !showSettingsDialog
                showLayersPopup = false
                showCanvasToolsPopup = false
                showToolSettingsPopup = null
                showColorPickerPopup = false
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        )

        // Save Format Dialog
        if (showSaveFormatDialog) {
            AlertDialog(
                onDismissRequest = { showSaveFormatDialog = false },
                title = { Text("Save & Export Artwork", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Choose export format:", color = Color(0xFFA0A5B5), fontSize = 13.sp)

                        // PNG Normal
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    showSaveFormatDialog = false
                                    val w = canvasViewRef?.width ?: 1080
                                    val h = canvasViewRef?.height ?: 1920
                                    viewModel.saveDrawing(w, h, SaveFormat.PNG_NORMAL)
                                },
                            color = Color(0x2200E5FF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🖼️", fontSize = 18.sp)
                                Column {
                                    Text("Standard PNG (with Background)", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Saves drawing with current background color & opacity", color = Color(0xFFA0A5B5), fontSize = 11.sp)
                                }
                            }
                        }

                        // Transparent PNG
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    showSaveFormatDialog = false
                                    val w = canvasViewRef?.width ?: 1080
                                    val h = canvasViewRef?.height ?: 1920
                                    viewModel.saveDrawing(w, h, SaveFormat.PNG_TRANSPARENT)
                                },
                            color = Color(0x22FF4081),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4081))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("✨", fontSize = 18.sp)
                                Column {
                                    Text("Transparent PNG (Artwork Only)", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Saves transparent background strokes for overlays & sharing", color = Color(0xFFA0A5B5), fontSize = 11.sp)
                                }
                            }
                        }

                        // Project JSON
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    showSaveFormatDialog = false
                                    val w = canvasViewRef?.width ?: 1080
                                    val h = canvasViewRef?.height ?: 1920
                                    viewModel.saveDrawing(w, h, SaveFormat.PROJECT_JSON)
                                },
                            color = Color(0x22FFD600),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD600))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📁", fontSize = 18.sp)
                                Column {
                                    Text("Vector Project JSON", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Saves lossless multi-layer vector strokes backup", color = Color(0xFFA0A5B5), fontSize = 11.sp)
                                }
                            }
                        }

                        // Animation Replay Trigger
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    showSaveFormatDialog = false
                                    viewModel.startReplay()
                                },
                            color = Color(0x2200E676),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🎬", fontSize = 18.sp)
                                Column {
                                    Text("Animation Stroke Replay", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Replay drawing strokes with speed control", color = Color(0xFFA0A5B5), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                },
                containerColor = Color(0xFF161824),
                confirmButton = {
                    TextButton(onClick = { showSaveFormatDialog = false }) {
                        Text("Cancel", color = Color(0xFF8E92A4))
                    }
                }
            )
        }

        // Clear Confirmation Dialog
        if (showClearConfirmation) {
            AlertDialog(
                onDismissRequest = { showClearConfirmation = false },
                title = { Text("Clear Active Layer?", color = Color.White) },
                text = { Text("This will remove all strokes from the currently active layer.", color = Color(0xFFA0A5B5)) },
                containerColor = Color(0xFF1B1D26),
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearActiveLayer()
                            showClearConfirmation = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252))
                    ) {
                        Text("Clear Layer")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showClearConfirmation = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF8E92A4))
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Exit / Close Confirmation Dialog
        if (showExitConfirmation) {
            AlertDialog(
                onDismissRequest = { showExitConfirmation = false },
                title = { Text("Close Drawing Screen?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Would you like to save your artwork before returning to the Home screen?", color = Color(0xFFA0A5B5)) },
                containerColor = Color(0xFF1B1D26),
                confirmButton = {
                    TextButton(
                        onClick = {
                            val w = canvasViewRef?.width ?: 1080
                            val h = canvasViewRef?.height ?: 1920
                            viewModel.saveDrawing(w, h, SaveFormat.PNG_NORMAL)
                            showExitConfirmation = false
                            viewModel.discardSession()
                            onNavigateHome()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF00E5FF))
                    ) {
                        Text("Save & Close")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                showExitConfirmation = false
                                viewModel.discardSession()
                                onNavigateHome()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252))
                        ) {
                            Text("Discard")
                        }
                        TextButton(
                            onClick = { showExitConfirmation = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF8E92A4))
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
    }
}
