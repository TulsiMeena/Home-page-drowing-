package com.example.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PressureCurve
import com.example.model.SmoothnessLevel

@Composable
fun AdvancedSettingsDialog(
    smoothnessLevel: SmoothnessLevel,
    pressureCurve: PressureCurve,
    customPressureFactor: Float,
    isTiltEnabled: Boolean,
    showPerformanceMonitor: Boolean,
    onSmoothnessChanged: (SmoothnessLevel) -> Unit,
    onPressureCurveChanged: (PressureCurve) -> Unit,
    onCustomPressureFactorChanged: (Float) -> Unit,
    onTiltToggled: (Boolean) -> Unit,
    onPerformanceMonitorToggled: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .testTag("advanced_settings_dialog")
            .shadow(16.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xF2161822),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .width(280.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stylus & Engine Settings",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }

            // Smoothness Level
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Stroke Smoothing", color = Color(0xFFA0A5B5), fontSize = 11.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1C1E2A))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(SmoothnessLevel.LOW, SmoothnessLevel.NORMAL, SmoothnessLevel.HIGH).forEach { level ->
                        val isSelected = smoothnessLevel == level
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFF00E5FF) else Color.Transparent)
                                .clickable { onSmoothnessChanged(level) }
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = level.name,
                                color = if (isSelected) Color.Black else Color(0xFFA0A5B5),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Pressure Curve
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Stylus Pressure Curve", color = Color(0xFFA0A5B5), fontSize = 11.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1C1E2A))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(PressureCurve.SOFT, PressureCurve.NORMAL, PressureCurve.FIRM, PressureCurve.CUSTOM).forEach { curve ->
                        val isSelected = pressureCurve == curve
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFF00E5FF) else Color.Transparent)
                                .clickable { onPressureCurveChanged(curve) }
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = curve.name,
                                color = if (isSelected) Color.Black else Color(0xFFA0A5B5),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (pressureCurve == PressureCurve.CUSTOM) {
                    Slider(
                        value = customPressureFactor,
                        onValueChange = onCustomPressureFactorChanged,
                        valueRange = 0.4f..2.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF),
                            inactiveTrackColor = Color(0x33FFFFFF)
                        )
                    )
                }
            }

            // Tilt Response Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Stylus Tilt Response", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Text("Widens stroke with tilt angle", color = Color(0xFF8E92A4), fontSize = 9.sp)
                }
                Switch(
                    checked = isTiltEnabled,
                    onCheckedChange = onTiltToggled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF00E5FF),
                        checkedTrackColor = Color(0xFF004D40)
                    )
                )
            }

            // Performance Monitor Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Performance HUD", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Text("FPS, Latency, Memory metrics", color = Color(0xFF8E92A4), fontSize = 9.sp)
                }
                Switch(
                    checked = showPerformanceMonitor,
                    onCheckedChange = { onPerformanceMonitorToggled() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF00E5FF),
                        checkedTrackColor = Color(0xFF004D40)
                    )
                )
            }
        }
    }
}
