package com.example.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CanvasBackgroundColor
import com.example.model.GridMode
import com.example.model.MirrorMode
import com.example.model.SymmetryMode

@Composable
fun CanvasToolsPopup(
    gridMode: GridMode,
    isSnapToGrid: Boolean,
    mirrorMode: MirrorMode,
    symmetryMode: SymmetryMode,
    canvasBackground: CanvasBackgroundColor,
    backgroundOpacity: Float,
    isBackgroundLocked: Boolean,
    onSetGridMode: (GridMode) -> Unit,
    onToggleSnapToGrid: () -> Unit,
    onSetMirrorMode: (MirrorMode) -> Unit,
    onSetSymmetryMode: (SymmetryMode) -> Unit,
    onSetCanvasBackground: (CanvasBackgroundColor) -> Unit,
    onSetBackgroundOpacity: (Float) -> Unit,
    onToggleBackgroundLock: () -> Unit,
    onAddGuide: (Boolean) -> Unit,
    onClearGuides: () -> Unit,
    onSmartCenter: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(320.dp)
            .clip(RoundedCornerShape(20.dp)),
        color = Color(0xFA141620),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Canvas & Background",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // 1. Smart Centering Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x2200E5FF))
                    .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(10.dp))
                    .clickable {
                        onSmartCenter()
                        onDismiss()
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CenterFocusStrong,
                    contentDescription = "Center Artwork",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Center Artwork Bounds",
                    color = Color(0xFF00E5FF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 2. Canvas Background Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Background Color",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CanvasBackgroundColor.values().forEach { bg ->
                        val isSelected = bg == canvasBackground
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0x3300E5FF) else Color(0x1AFFFFFF))
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF00E5FF) else Color(0x22FFFFFF),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    onSetCanvasBackground(bg)
                                    if (bg == CanvasBackgroundColor.TRANSPARENT) {
                                        onSetBackgroundOpacity(0.0f)
                                    } else if (backgroundOpacity == 0.0f) {
                                        onSetBackgroundOpacity(1.0f)
                                    }
                                }
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (bg) {
                                            CanvasBackgroundColor.BLACK -> Color.Black
                                            CanvasBackgroundColor.DARK_GRAY -> Color(0xFF1E2028)
                                            CanvasBackgroundColor.DARK_BLUE -> Color(0xFF0D1B2A)
                                            CanvasBackgroundColor.DARK_PURPLE -> Color(0xFF1B0A2A)
                                            CanvasBackgroundColor.WHITE -> Color.White
                                            CanvasBackgroundColor.CUSTOM -> Color(0xFF12141C)
                                            CanvasBackgroundColor.TRANSPARENT -> Color.DarkGray
                                        }
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when (bg) {
                                    CanvasBackgroundColor.TRANSPARENT -> "Trans"
                                    CanvasBackgroundColor.BLACK -> "Black"
                                    CanvasBackgroundColor.DARK_GRAY -> "Gray"
                                    CanvasBackgroundColor.DARK_BLUE -> "Blue"
                                    CanvasBackgroundColor.DARK_PURPLE -> "Purple"
                                    CanvasBackgroundColor.WHITE -> "White"
                                    CanvasBackgroundColor.CUSTOM -> "Custom"
                                },
                                color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }

            // 3. Background Opacity Controls
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Background Opacity",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${(backgroundOpacity * 100).toInt()}%",
                        color = Color(0xFF00E5FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(0.0f to "0%", 0.10f to "10%", 0.25f to "25%", 0.50f to "50%", 0.75f to "75%", 1.0f to "100%").forEach { (op, label) ->
                        val isSelected = kotlin.math.abs(backgroundOpacity - op) < 0.05f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFF00E5FF) else Color(0x22FFFFFF))
                                .clickable { onSetBackgroundOpacity(op) }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Slider(
                    value = backgroundOpacity,
                    onValueChange = onSetBackgroundOpacity,
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF),
                        inactiveTrackColor = Color(0x33FFFFFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 4. Grid Mode & Snap
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Grid & Guides",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        GridMode.OFF to "Off",
                        GridMode.SMALL to "Small (24)",
                        GridMode.LARGE to "Large (48)"
                    ).forEach { (mode, label) ->
                        val isSelected = gridMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF00E5FF) else Color(0x22FFFFFF))
                                .clickable { onSetGridMode(mode) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Snap switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Snap Shapes to Grid", color = Color.White, fontSize = 12.sp)
                    Switch(
                        checked = isSnapToGrid,
                        onCheckedChange = { onToggleSnapToGrid() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00E5FF),
                            checkedTrackColor = Color(0xFF005B66)
                        )
                    )
                }

                // Guide Adders
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x22FFFFFF))
                            .clickable { onAddGuide(true) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+ H-Guide", color = Color.White, fontSize = 10.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x22FFFFFF))
                            .clickable { onAddGuide(false) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+ V-Guide", color = Color.White, fontSize = 10.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x22FF5252))
                            .clickable { onClearGuides() }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Clear Guides", color = Color.White, fontSize = 10.sp)
                    }
                }
            }

            // 5. Mirror & Symmetry Drawing
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Mirror Drawing",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        MirrorMode.NONE to "None",
                        MirrorMode.HORIZONTAL to "Horiz",
                        MirrorMode.VERTICAL to "Vert",
                        MirrorMode.BOTH to "Both"
                    ).forEach { (m, label) ->
                        val isSelected = mirrorMode == m
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFFF4081) else Color(0x22FFFFFF))
                                .clickable { onSetMirrorMode(m) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Radial Symmetry",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        SymmetryMode.NONE to "Off",
                        SymmetryMode.TWO_WAY to "2-Way",
                        SymmetryMode.FOUR_WAY to "4-Way",
                        SymmetryMode.SIX_WAY to "6-Way",
                        SymmetryMode.EIGHT_WAY to "8-Way"
                    ).forEach { (s, label) ->
                        val isSelected = symmetryMode == s
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFFFD600) else Color(0x22FFFFFF))
                                .clickable { onSetSymmetryMode(s) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
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
}
