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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerPopup(
    currentColor: Long,
    currentOpacity: Float,
    recentColors: List<Long>,
    favoriteColors: List<Long>,
    onColorSelected: (Long) -> Unit,
    onOpacitySelected: (Float) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onClearRecent: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    // Extract ARGB components
    val initialRed = ((currentColor shr 16) and 0xFF).toFloat()
    val initialGreen = ((currentColor shr 8) and 0xFF).toFloat()
    val initialBlue = (currentColor and 0xFF).toFloat()

    var red by remember(currentColor) { mutableFloatStateOf(initialRed) }
    var green by remember(currentColor) { mutableFloatStateOf(initialGreen) }
    var blue by remember(currentColor) { mutableFloatStateOf(initialBlue) }

    val hexString = String.format(Locale.US, "#%02X%02X%02X", red.toInt(), green.toInt(), blue.toInt())
    var hexInputText by remember(currentColor) { mutableStateOf(hexString) }

    val activeColorLong = remember(red, green, blue) {
        0xFF000000L or
            ((red.toInt() and 0xFF).toLong() shl 16) or
            ((green.toInt() and 0xFF).toLong() shl 8) or
            (blue.toInt() and 0xFF).toLong()
    }

    val isFavorite = favoriteColors.contains(activeColorLong)

    Surface(
        modifier = modifier
            .testTag("color_picker_popup")
            .shadow(16.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xF2161822),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .width(280.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Title, Live Preview & Favorite Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(activeColorLong))
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                    Text(
                        text = "Color Studio",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onToggleFavorite(activeColorLong) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite Color",
                            tint = if (isFavorite) Color(0xFFFF4081) else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Quick Color Palette presets
            val quickSwatches = remember {
                listOf(
                    0xFFFFFFFF, 0xFF00E5FF, 0xFF00E676, 0xFFFF1744,
                    0xFFFFEA00, 0xFFFF9100, 0xFFD500F9, 0xFF7C4DFF,
                    0xFF80D8FF, 0xFFA7FFEB, 0xFFFF80AB, 0xFFFFD180
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickSwatches.forEach { col ->
                    val isSelected = activeColorLong == col
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(col))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFF00E5FF) else Color(0x33000000),
                                shape = CircleShape
                            )
                            .clickable {
                                red = ((col shr 16) and 0xFF).toFloat()
                                green = ((col shr 8) and 0xFF).toFloat()
                                blue = (col and 0xFF).toFloat()
                                onColorSelected(col)
                            }
                    )
                }
            }

            // RGB Sliders
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                RgbSlider(label = "R", value = red, color = Color(0xFFFF5252)) {
                    red = it
                    onColorSelected(activeColorLong)
                }
                RgbSlider(label = "G", value = green, color = Color(0xFF69F0AE)) {
                    green = it
                    onColorSelected(activeColorLong)
                }
                RgbSlider(label = "B", value = blue, color = Color(0xFF40C4FF)) {
                    blue = it
                    onColorSelected(activeColorLong)
                }
            }

            // HEX Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HEX:",
                    color = Color(0xFFA0A5B5),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                OutlinedTextField(
                    value = hexInputText,
                    onValueChange = { input ->
                        hexInputText = input
                        if (input.matches(Regex("^#[0-9a-fA-F]{6}$"))) {
                            try {
                                val parsed = java.lang.Long.parseLong(input.removePrefix("#"), 16)
                                red = ((parsed shr 16) and 0xFF).toFloat()
                                green = ((parsed shr 8) and 0xFF).toFloat()
                                blue = (parsed and 0xFF).toFloat()
                                onColorSelected(0xFF000000L or parsed)
                            } catch (_: Exception) {}
                        }
                    },
                    modifier = Modifier
                        .width(130.dp)
                        .height(44.dp)
                        .testTag("hex_color_input"),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0x33FFFFFF)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
            }

            // Opacity Presets & Slider
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                    listOf(0.10f, 0.25f, 0.50f, 0.75f, 1.00f).forEach { op ->
                        val isSelected = Math.abs(currentOpacity - op) < 0.05f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFF00E5FF) else Color(0xFF222530))
                                .clickable { onOpacitySelected(op) }
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
                    onValueChange = onOpacitySelected,
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF),
                        inactiveTrackColor = Color(0x33FFFFFF)
                    )
                )
            }

            // Recent Colors
            if (recentColors.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Colors", color = Color(0xFF8E92A4), fontSize = 11.sp)
                        Text(
                            text = "Clear",
                            color = Color(0xFFFF5252),
                            fontSize = 10.sp,
                            modifier = Modifier.clickable { onClearRecent() }
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        recentColors.forEach { col ->
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(col))
                                    .border(1.dp, Color(0x44FFFFFF), CircleShape)
                                    .clickable {
                                        red = ((col shr 16) and 0xFF).toFloat()
                                        green = ((col shr 8) and 0xFF).toFloat()
                                        blue = (col and 0xFF).toFloat()
                                        onColorSelected(col)
                                    }
                            )
                        }
                    }
                }
            }

            // Favorite Colors
            if (favoriteColors.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Favorites", color = Color(0xFF8E92A4), fontSize = 11.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        favoriteColors.forEach { col ->
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(col))
                                    .border(1.dp, Color(0x44FFFFFF), CircleShape)
                                    .clickable {
                                        red = ((col shr 16) and 0xFF).toFloat()
                                        green = ((col shr 8) and 0xFF).toFloat()
                                        blue = (col and 0xFF).toFloat()
                                        onColorSelected(col)
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RgbSlider(
    label: String,
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(14.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = Color(0x22FFFFFF)
            )
        )
        Text(
            text = "${value.toInt()}",
            color = Color.White,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(24.dp)
        )
    }
}
