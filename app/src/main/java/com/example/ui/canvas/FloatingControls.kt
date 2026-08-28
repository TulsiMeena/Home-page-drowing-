package com.example.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrushType
import com.example.model.MainToolMode

@Composable
fun FloatingControls(
    mainToolMode: MainToolMode,
    selectedBrush: BrushType,
    selectedColor: Long,
    selectedWidth: Float,
    selectedOpacity: Float,
    layersCount: Int,
    isStraightLineMode: Boolean,
    isShapeAssistEnabled: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    favoriteColors: List<Long>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSetMainToolMode: (MainToolMode) -> Unit,
    onSelectBrush: (BrushType) -> Unit,
    onOpenToolSettings: (BrushType) -> Unit,
    onOpenColorPicker: () -> Unit,
    onSelectQuickColor: (Long) -> Unit,
    onOpenLayersPanel: () -> Unit,
    onOpenCanvasTools: () -> Unit,
    onToggleStraightLine: () -> Unit,
    onToggleShapeAssist: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onResetView: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.testTag("floating_controls_container"),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (!isExpanded) {
            // Minimized Floating Bubble
            Box(
                modifier = Modifier
                    .testTag("minimized_floating_bubble")
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xE614161F))
                    .border(1.dp, Color(0x3300E5FF), CircleShape)
                    .shadow(12.dp, CircleShape)
                    .clickable { onToggleExpand() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (mainToolMode) {
                        MainToolMode.DRAW -> when (selectedBrush) {
                            BrushType.PEN -> "✏️"
                            BrushType.PENCIL -> "🖍️"
                            BrushType.MARKER -> "🖊️"
                            BrushType.SOFT_BRUSH -> "🖌️"
                            BrushType.HIGHLIGHTER -> "💡"
                            BrushType.ERASER -> "🧽"
                        }
                        MainToolMode.SELECT -> "✂️"
                        MainToolMode.SHAPES -> "📐"
                        MainToolMode.FILL -> "🪣"
                    },
                    fontSize = 20.sp
                )
            }
        } else {
            // Full Compact Floating Toolbar
            Surface(
                modifier = Modifier
                    .testTag("expanded_floating_toolbar")
                    .shadow(20.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xF212141C),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2EFFFFFF))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Row 0: Favorite Quick Colors
                    if (favoriteColors.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            favoriteColors.take(6).forEach { colorLong ->
                                val isSelected = selectedColor == colorLong && selectedBrush != BrushType.ERASER
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorLong))
                                        .border(
                                            width = if (isSelected) 2.dp else 0.5.dp,
                                            color = if (isSelected) Color(0xFF00E5FF) else Color(0x44FFFFFF),
                                            shape = CircleShape
                                        )
                                        .clickable { onSelectQuickColor(colorLong) }
                                )
                            }
                        }
                    }

                    // Row 1: Mode & Tool Switchers
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Drawing Mode Brush Selector
                        ToolIconButton(
                            icon = "✏️",
                            label = "Pen",
                            isSelected = mainToolMode == MainToolMode.DRAW && selectedBrush == BrushType.PEN,
                            onClick = {
                                if (mainToolMode == MainToolMode.DRAW && selectedBrush == BrushType.PEN) onOpenToolSettings(BrushType.PEN)
                                else {
                                    onSetMainToolMode(MainToolMode.DRAW)
                                    onSelectBrush(BrushType.PEN)
                                }
                            }
                        )

                        ToolIconButton(
                            icon = "🖌️",
                            label = "Soft",
                            isSelected = mainToolMode == MainToolMode.DRAW && selectedBrush == BrushType.SOFT_BRUSH,
                            onClick = {
                                if (mainToolMode == MainToolMode.DRAW && selectedBrush == BrushType.SOFT_BRUSH) onOpenToolSettings(BrushType.SOFT_BRUSH)
                                else {
                                    onSetMainToolMode(MainToolMode.DRAW)
                                    onSelectBrush(BrushType.SOFT_BRUSH)
                                }
                            }
                        )

                        ToolIconButton(
                            icon = "🖍️",
                            label = "Pencil",
                            isSelected = mainToolMode == MainToolMode.DRAW && selectedBrush == BrushType.PENCIL,
                            onClick = {
                                if (mainToolMode == MainToolMode.DRAW && selectedBrush == BrushType.PENCIL) onOpenToolSettings(BrushType.PENCIL)
                                else {
                                    onSetMainToolMode(MainToolMode.DRAW)
                                    onSelectBrush(BrushType.PENCIL)
                                }
                            }
                        )

                        ToolIconButton(
                            icon = "🖊️",
                            label = "Marker",
                            isSelected = mainToolMode == MainToolMode.DRAW && selectedBrush == BrushType.MARKER,
                            onClick = {
                                if (mainToolMode == MainToolMode.DRAW && selectedBrush == BrushType.MARKER) onOpenToolSettings(BrushType.MARKER)
                                else {
                                    onSetMainToolMode(MainToolMode.DRAW)
                                    onSelectBrush(BrushType.MARKER)
                                }
                            }
                        )

                        ToolIconButton(
                            icon = "💡",
                            label = "Highlight",
                            isSelected = mainToolMode == MainToolMode.DRAW && selectedBrush == BrushType.HIGHLIGHTER,
                            onClick = {
                                if (mainToolMode == MainToolMode.DRAW && selectedBrush == BrushType.HIGHLIGHTER) onOpenToolSettings(BrushType.HIGHLIGHTER)
                                else {
                                    onSetMainToolMode(MainToolMode.DRAW)
                                    onSelectBrush(BrushType.HIGHLIGHTER)
                                }
                            }
                        )

                        ToolIconButton(
                            icon = "🧽",
                            label = "Eraser",
                            isSelected = mainToolMode == MainToolMode.DRAW && selectedBrush == BrushType.ERASER,
                            onClick = {
                                if (mainToolMode == MainToolMode.DRAW && selectedBrush == BrushType.ERASER) onOpenToolSettings(BrushType.ERASER)
                                else {
                                    onSetMainToolMode(MainToolMode.DRAW)
                                    onSelectBrush(BrushType.ERASER)
                                }
                            }
                        )

                        // Selection Mode
                        ToolIconButton(
                            icon = "✂️",
                            label = "Select",
                            isSelected = mainToolMode == MainToolMode.SELECT,
                            onClick = {
                                if (mainToolMode == MainToolMode.SELECT) onSetMainToolMode(MainToolMode.DRAW)
                                else onSetMainToolMode(MainToolMode.SELECT)
                            }
                        )

                        // Shapes Mode
                        ToolIconButton(
                            icon = "📐",
                            label = "Shapes",
                            isSelected = mainToolMode == MainToolMode.SHAPES,
                            onClick = {
                                if (mainToolMode == MainToolMode.SHAPES) onSetMainToolMode(MainToolMode.DRAW)
                                else onSetMainToolMode(MainToolMode.SHAPES)
                            }
                        )

                        // Fill Mode
                        ToolIconButton(
                            icon = "🪣",
                            label = "Fill",
                            isSelected = mainToolMode == MainToolMode.FILL,
                            onClick = {
                                if (mainToolMode == MainToolMode.FILL) onSetMainToolMode(MainToolMode.DRAW)
                                else onSetMainToolMode(MainToolMode.FILL)
                            }
                        )

                        // Color Studio
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF222530))
                                .border(1.5.dp, Color(selectedColor), CircleShape)
                                .clickable { onOpenColorPicker() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎨", fontSize = 15.sp)
                        }
                    }

                    // Row 2: Secondary System Actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Layers Manager
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E212B))
                                .clickable { onOpenLayersPanel() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📑", fontSize = 13.sp)
                        }

                        // Canvas Tools (Grid / Symmetry / Center / Background)
                        ActionIconButton(
                            icon = "🌐",
                            onClick = onOpenCanvasTools,
                            testTag = "canvas_tools_btn"
                        )

                        // Straight Line Mode
                        ActionIconButton(
                            icon = "📏",
                            isActive = isStraightLineMode,
                            onClick = onToggleStraightLine,
                            testTag = "straight_line_btn"
                        )

                        // Shape Assist
                        ActionIconButton(
                            icon = "✨",
                            isActive = isShapeAssistEnabled,
                            onClick = onToggleShapeAssist,
                            testTag = "shape_assist_btn"
                        )

                        // Undo
                        ActionIconButton(
                            icon = "↩️",
                            enabled = canUndo,
                            onClick = onUndo,
                            testTag = "undo_btn"
                        )

                        // Redo
                        ActionIconButton(
                            icon = "↪️",
                            enabled = canRedo,
                            onClick = onRedo,
                            testTag = "redo_btn"
                        )

                        // Reset View (Zoom/Pan/Rotation)
                        ActionIconButton(
                            icon = "🔄",
                            onClick = onResetView,
                            testTag = "reset_view_btn"
                        )

                        // Clear Canvas
                        ActionIconButton(
                            icon = "🗑️",
                            onClick = onClear,
                            testTag = "clear_btn"
                        )

                        // Save Image & Project
                        ActionIconButton(
                            icon = "💾",
                            onClick = onSave,
                            testTag = "save_btn"
                        )

                        // Settings
                        ActionIconButton(
                            icon = "⚙️",
                            onClick = onOpenSettings,
                            testTag = "settings_btn"
                        )

                        // Minimize to floating bubble
                        ActionIconButton(
                            icon = "✕",
                            onClick = onToggleExpand,
                            testTag = "minimize_btn"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolIconButton(
    icon: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (isSelected) Color(0xFF00E5FF) else Color(0xFF222530))
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) Color.White else Color(0x33FFFFFF),
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(icon, fontSize = 15.sp)
    }
}

@Composable
private fun ActionIconButton(
    icon: String,
    enabled: Boolean = true,
    isActive: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .testTag(testTag)
            .size(30.dp)
            .clip(CircleShape)
            .background(
                when {
                    isActive -> Color(0xFF00E5FF)
                    !enabled -> Color(0x441A1C24)
                    else -> Color(0xFF1E212B)
                }
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            fontSize = 12.sp,
            color = if (enabled) Color.White else Color(0x44FFFFFF)
        )
    }
}
