package com.example.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ShapeProperties
import com.example.model.ShapeType

@Composable
fun ShapesToolbar(
    selectedShapeType: ShapeType,
    shapeProperties: ShapeProperties,
    onSelectShapeType: (ShapeType) -> Unit,
    onUpdateShapeProperties: (ShapeProperties) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        color = Color(0xFA1E2028),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shapes (Drag on canvas to draw)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Shapes",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Shape Types Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShapeTypeBadge("Line", Icons.Default.HorizontalRule, selectedShapeType == ShapeType.LINE) {
                    onSelectShapeType(ShapeType.LINE)
                }
                ShapeTypeBadge("Circle", Icons.Default.RadioButtonUnchecked, selectedShapeType == ShapeType.CIRCLE) {
                    onSelectShapeType(ShapeType.CIRCLE)
                }
                ShapeTypeBadge("Oval", Icons.Default.RadioButtonUnchecked, selectedShapeType == ShapeType.OVAL) {
                    onSelectShapeType(ShapeType.OVAL)
                }
                ShapeTypeBadge("Rect", Icons.Default.CropSquare, selectedShapeType == ShapeType.RECTANGLE) {
                    onSelectShapeType(ShapeType.RECTANGLE)
                }
                ShapeTypeBadge("Round Rect", Icons.Default.CropSquare, selectedShapeType == ShapeType.ROUNDED_RECTANGLE) {
                    onSelectShapeType(ShapeType.ROUNDED_RECTANGLE)
                }
                ShapeTypeBadge("Triangle", Icons.Default.ChangeHistory, selectedShapeType == ShapeType.TRIANGLE) {
                    onSelectShapeType(ShapeType.TRIANGLE)
                }
                ShapeTypeBadge("Polygon", Icons.Default.Hexagon, selectedShapeType == ShapeType.POLYGON) {
                    onSelectShapeType(ShapeType.POLYGON)
                }
                ShapeTypeBadge("Star", Icons.Default.Star, selectedShapeType == ShapeType.STAR) {
                    onSelectShapeType(ShapeType.STAR)
                }
                ShapeTypeBadge("Arrow", Icons.Default.ArrowForward, selectedShapeType == ShapeType.ARROW) {
                    onSelectShapeType(ShapeType.ARROW)
                }
            }

            // Shape Quick Parameters: Fill & Stroke width
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stroke Width
                Text(
                    text = "Width: ${shapeProperties.strokeWidth.toInt()}px",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
                Slider(
                    value = shapeProperties.strokeWidth,
                    onValueChange = { onUpdateShapeProperties(shapeProperties.copy(strokeWidth = it)) },
                    valueRange = 1f..32f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF)
                    )
                )

                // Fill Toggle
                val isFilled = shapeProperties.fillColor != 0x00000000L && shapeProperties.fillOpacity > 0f
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isFilled) Color(0xFF00E5FF) else Color(0x22FFFFFF))
                        .clickable {
                            if (isFilled) {
                                onUpdateShapeProperties(shapeProperties.copy(fillColor = 0x00000000L, fillOpacity = 0f))
                            } else {
                                onUpdateShapeProperties(shapeProperties.copy(fillColor = shapeProperties.strokeColor, fillOpacity = 0.4f))
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFilled) "Fill: ON" else "Fill: OFF",
                        color = if (isFilled) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ShapeTypeBadge(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF00E5FF) else Color(0x22FFFFFF))
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) Color.White else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color.Black else Color.White,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            color = if (isSelected) Color.Black else Color.White,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
