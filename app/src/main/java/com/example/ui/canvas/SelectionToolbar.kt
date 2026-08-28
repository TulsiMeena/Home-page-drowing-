package com.example.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.model.AlignmentType
import com.example.model.SelectionMode

@Composable
fun SelectionToolbar(
    selectionMode: SelectionMode,
    selectedCount: Int,
    onSetSelectionMode: (SelectionMode) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onMove: (Float, Float) -> Unit,
    onScale: (Float, Float, Boolean) -> Unit,
    onRotate: (Float) -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onPaste: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onAlign: (AlignmentType) -> Unit,
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
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Selection",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                    if (selectedCount > 0) {
                        Text(
                            text = " ($selectedCount selected)",
                            color = Color(0xFF00E5FF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Selection Modes
                    SelectionModeBadge(
                        label = "Rect",
                        icon = Icons.Default.CropSquare,
                        isSelected = selectionMode == SelectionMode.RECTANGLE,
                        onClick = { onSetSelectionMode(SelectionMode.RECTANGLE) }
                    )
                    SelectionModeBadge(
                        label = "Lasso",
                        icon = Icons.Default.Gesture,
                        isSelected = selectionMode == SelectionMode.LASSO,
                        onClick = { onSetSelectionMode(SelectionMode.LASSO) }
                    )
                    SelectionModeBadge(
                        label = "Tap",
                        icon = Icons.Default.TouchApp,
                        isSelected = selectionMode == SelectionMode.SINGLE_STROKE,
                        onClick = { onSetSelectionMode(SelectionMode.SINGLE_STROKE) }
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Selection",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Action Rows (Scrollable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clipboard & Basic Actions
                ToolbarActionBtn(icon = Icons.Default.SelectAll, label = "All", onClick = onSelectAll)
                ToolbarActionBtn(icon = Icons.Default.ContentCopy, label = "Copy", enabled = selectedCount > 0, onClick = onCopy)
                ToolbarActionBtn(icon = Icons.Default.ContentCut, label = "Cut", enabled = selectedCount > 0, onClick = onCut)
                ToolbarActionBtn(icon = Icons.Default.ContentPaste, label = "Paste", onClick = onPaste)
                ToolbarActionBtn(icon = Icons.Default.Delete, label = "Delete", enabled = selectedCount > 0, tint = Color(0xFFFF5252), onClick = onDelete)

                // Scaling & Rotation
                ToolbarActionBtn(icon = Icons.Default.ZoomIn, label = "+10%", enabled = selectedCount > 0, onClick = { onScale(1.1f, 1.1f, true) })
                ToolbarActionBtn(icon = Icons.Default.ZoomOut, label = "-10%", enabled = selectedCount > 0, onClick = { onScale(0.9f, 0.9f, true) })
                ToolbarActionBtn(icon = Icons.Default.RotateLeft, label = "-15°", enabled = selectedCount > 0, onClick = { onRotate(-15f) })
                ToolbarActionBtn(icon = Icons.Default.RotateRight, label = "+15°", enabled = selectedCount > 0, onClick = { onRotate(15f) })

                // Alignment Actions
                ToolbarActionBtn(icon = Icons.Default.FormatAlignLeft, label = "Left", enabled = selectedCount > 1, onClick = { onAlign(AlignmentType.LEFT) })
                ToolbarActionBtn(icon = Icons.Default.FormatAlignRight, label = "Right", enabled = selectedCount > 1, onClick = { onAlign(AlignmentType.RIGHT) })
                ToolbarActionBtn(icon = Icons.Default.ArrowUpward, label = "Top", enabled = selectedCount > 1, onClick = { onAlign(AlignmentType.TOP) })
                ToolbarActionBtn(icon = Icons.Default.ArrowDownward, label = "Bottom", enabled = selectedCount > 1, onClick = { onAlign(AlignmentType.BOTTOM) })
            }
        }
    }
}

@Composable
private fun SelectionModeBadge(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF00E5FF) else Color(0x22FFFFFF))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color.Black else Color.White,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = label,
            color = if (isSelected) Color.Black else Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ToolbarActionBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) Color(0x1AFFFFFF) else Color(0x0AFFFFFF))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) tint else Color.Gray.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = if (enabled) Color.White.copy(alpha = 0.9f) else Color.Gray.copy(alpha = 0.4f),
            fontSize = 9.sp
        )
    }
}
