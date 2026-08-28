package com.example.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.model.EraserMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolSizeOpacityPopup(
    brushType: BrushType,
    currentColor: Long,
    currentSize: Float,
    currentOpacity: Float,
    eraserMode: EraserMode,
    onSizeChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onEraserModeChange: (EraserMode) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sizePresets = remember {
        listOf(1f, 2f, 4f, 6f, 10f, 16f, 24f, 32f, 48f, 64f)
    }

    val opacityPresets = remember {
        listOf(0.10f, 0.25f, 0.50f, 0.75f, 1.00f)
    }

    val toolTitle = when (brushType) {
        BrushType.PEN -> "Pen Settings"
        BrushType.PENCIL -> "Pencil Settings"
        BrushType.MARKER -> "Marker Settings"
        BrushType.SOFT_BRUSH -> "Soft Brush Settings"
        BrushType.HIGHLIGHTER -> "Highlighter Settings"
        BrushType.ERASER -> "Eraser Settings"
    }

    Surface(
        modifier = modifier
            .testTag("tool_size_opacity_popup")
            .shadow(16.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xF2161822),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .width(260.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Title & Live Brush Preview Dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Live preview circle
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10121A))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val previewDiameter = (currentSize * 0.45f).coerceIn(2f, 24f)
                        Box(
                            modifier = Modifier
                                .size(previewDiameter.dp)
                                .clip(CircleShape)
                                .background(
                                    if (brushType == BrushType.ERASER) Color.White.copy(alpha = 0.8f)
                                    else Color(currentColor).copy(alpha = currentOpacity)
                                )
                        )
                    }

                    Text(
                        text = toolTitle,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }

            // Eraser Mode Toggle (if Eraser selected)
            if (brushType == BrushType.ERASER) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1C1E2A))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (eraserMode == EraserMode.PIXEL) Color(0xFF00E5FF) else Color.Transparent)
                            .clickable { onEraserModeChange(EraserMode.PIXEL) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Pixel Eraser",
                            color = if (eraserMode == EraserMode.PIXEL) Color.Black else Color(0xFFA0A5B5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (eraserMode == EraserMode.STROKE) Color(0xFF00E5FF) else Color.Transparent)
                            .clickable { onEraserModeChange(EraserMode.STROKE) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Stroke Eraser",
                            color = if (eraserMode == EraserMode.STROKE) Color.Black else Color(0xFFA0A5B5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Size Selector Presets
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Size", color = Color(0xFFA0A5B5), fontSize = 11.sp)
                    Text("${currentSize.toInt()} px", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    sizePresets.forEach { preset ->
                        val isSelected = Math.abs(currentSize - preset) < 0.5f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFF00E5FF) else Color(0xFF222530))
                                .clickable { onSizeChange(preset) }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${preset.toInt()}p",
                                color = if (isSelected) Color.Black else Color(0xFFD0D4E0),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Slider(
                    value = currentSize,
                    onValueChange = onSizeChange,
                    valueRange = 1f..64f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF),
                        inactiveTrackColor = Color(0x33FFFFFF)
                    )
                )
            }

            // Opacity (Hidden for Eraser)
            if (brushType != BrushType.ERASER) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Opacity", color = Color(0xFFA0A5B5), fontSize = 11.sp)
                        Text("${(currentOpacity * 100).toInt()}%", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        opacityPresets.forEach { op ->
                            val isSelected = Math.abs(currentOpacity - op) < 0.05f
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Color(0xFF00E5FF) else Color(0xFF222530))
                                    .clickable { onOpacityChange(op) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${(op * 100).toInt()}%",
                                    color = if (isSelected) Color.Black else Color(0xFFD0D4E0),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Slider(
                        value = currentOpacity,
                        onValueChange = onOpacityChange,
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF),
                            inactiveTrackColor = Color(0x33FFFFFF)
                        )
                    )
                }
            }
        }
    }
}
