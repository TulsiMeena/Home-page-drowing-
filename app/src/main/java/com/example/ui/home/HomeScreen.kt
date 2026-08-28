package com.example.ui.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FloatingIconSize
import com.example.service.FloatingDrawingService

@Composable
fun HomeScreen(
    onOpenDrawing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(FloatingDrawingService.PREFS_NAME, Context.MODE_PRIVATE)
    }

    var hasOverlayPermission by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }

    var isFloatingServiceEnabled by remember {
        mutableStateOf(false)
    }

    var selectedIconSize by remember {
        val sizeName = prefs.getString(FloatingDrawingService.KEY_ICON_SIZE, FloatingIconSize.SMALL.name)
        mutableStateOf(
            try {
                FloatingIconSize.valueOf(sizeName ?: FloatingIconSize.SMALL.name)
            } catch (_: Exception) {
                FloatingIconSize.SMALL
            }
        )
    }

    var isEdgeSnapEnabled by remember {
        mutableStateOf(prefs.getBoolean(FloatingDrawingService.KEY_EDGE_SNAP, true))
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val granted = Settings.canDrawOverlays(context)
        hasOverlayPermission = granted
        if (granted) {
            Toast.makeText(context, "Overlay permission granted! Starting floating icon.", Toast.LENGTH_SHORT).show()
            FloatingDrawingService.startService(context)
            isFloatingServiceEnabled = true
        } else {
            Toast.makeText(context, "Permission needed to show floating drawing icon", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_root"),
        containerColor = Color(0xFF090A0E),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenDrawing,
                modifier = Modifier
                    .testTag("open_drawing_fab")
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp, end = 12.dp)
                    .scale(pulseScale)
                    .shadow(20.dp, CircleShape, spotColor = Color(0xFF00E5FF)),
                containerColor = Color(0xFF00E5FF),
                contentColor = Color.Black,
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Brush,
                        contentDescription = "Drawing",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Studio",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Color.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BlackCanvas",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Draw directly over home screen & apps",
                        color = Color(0xFFA0A5B5),
                        fontSize = 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF161822))
                        .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "v2.0 PRO",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // FLOATING ON-SCREEN DRAWING ICON CONTROLLER (Core Requirement)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141622)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E5FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🖌️", fontSize = 20.sp)
                            }
                            Column {
                                Text(
                                    text = "Floating Drawing Icon",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (isFloatingServiceEnabled) "Active on your screen" else "Shows over all apps & Home Screen",
                                    color = if (isFloatingServiceEnabled) Color(0xFF00E676) else Color(0xFF8E92A4),
                                    fontSize = 12.sp,
                                    fontWeight = if (isFloatingServiceEnabled) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }

                        Switch(
                            checked = isFloatingServiceEnabled,
                            onCheckedChange = { enable ->
                                if (!hasOverlayPermission) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    overlayPermissionLauncher.launch(intent)
                                } else {
                                    isFloatingServiceEnabled = enable
                                    if (enable) {
                                        FloatingDrawingService.startService(context)
                                        Toast.makeText(context, "Floating icon activated!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        FloatingDrawingService.stopService(context)
                                        Toast.makeText(context, "Floating icon stopped.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color(0xFF00E5FF),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color(0xFF222430)
                            )
                        )
                    }

                    // If permission is not granted yet, show prompt button
                    if (!hasOverlayPermission) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    overlayPermissionLauncher.launch(intent)
                                },
                            color = Color(0x3300E5FF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Permission",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Grant Overlay Permission to enable on-screen drawing",
                                    color = Color(0xFF00E5FF),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Icon Size Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Icon Size:",
                            color = Color(0xFFA0A5B5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FloatingIconSize.values().forEach { sizeEnum ->
                                val isSelected = selectedIconSize == sizeEnum
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E2030))
                                        .border(1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                                        .clickable {
                                            selectedIconSize = sizeEnum
                                            prefs.edit().putString(FloatingDrawingService.KEY_ICON_SIZE, sizeEnum.name).apply()
                                            if (isFloatingServiceEnabled) {
                                                FloatingDrawingService.startService(context)
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${sizeEnum.label} (${sizeEnum.dpSize}dp)",
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // Edge Snap Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Snap Icon to Nearest Screen Edge", color = Color.White, fontSize = 13.sp)
                            Text("Keeps phone home screen tidy", color = Color(0xFF8E92A4), fontSize = 11.sp)
                        }

                        Switch(
                            checked = isEdgeSnapEnabled,
                            onCheckedChange = { snap ->
                                isEdgeSnapEnabled = snap
                                prefs.edit().putBoolean(FloatingDrawingService.KEY_EDGE_SNAP, snap).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color(0xFF00E5FF)
                            )
                        )
                    }
                }
            }

            // Direct Studio Launch Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onOpenDrawing() },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF12141C)),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2200E5FF))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x2200E5FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Draw,
                                contentDescription = "Studio Canvas",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Direct Studio Canvas",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Multi-layer vector drawing workspace",
                                color = Color(0xFF6B7280),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Button(
                        onClick = onOpenDrawing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E2235),
                            contentColor = Color(0xFF00E5FF)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x5500E5FF))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brush,
                                contentDescription = "Open Studio",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Open Drawing Canvas",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Studio Feature Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeaturePill(icon = Icons.Default.Layers, label = "Multi-Layers", modifier = Modifier.weight(1f))
                FeaturePill(icon = Icons.Default.Transform, label = "Selection & Shapes", modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeaturePill(icon = Icons.Default.Palette, label = "Radial Symmetry & Fill", modifier = Modifier.weight(1f))
                FeaturePill(icon = Icons.Default.Edit, label = "Pressure & Tilt Stylus", modifier = Modifier.weight(1f))
            }

            // How to use guide
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                color = Color(0xFF10121A)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "💡 How to Use Floating Icon:",
                        color = Color(0xFF00E5FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "1. Turn ON the Floating Drawing Icon switch above.\n" +
                            "2. Go to your Phone's Home Screen or any app.\n" +
                            "3. Drag the small 🖌️ icon anywhere on screen.\n" +
                            "4. Tap the icon once to draw smoothly directly over your screen.\n" +
                            "5. Tap ✕ to finish and return immediately.",
                        color = Color(0xFFA0A5B5),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}

@Composable
private fun FeaturePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141620))
            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = Color(0xFFD0D4E0),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
