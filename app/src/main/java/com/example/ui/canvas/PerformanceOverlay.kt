package com.example.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PerformanceMetrics
import java.util.Locale

@Composable
fun PerformanceOverlay(
    metrics: PerformanceMetrics,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .testTag("performance_overlay")
            .background(Color(0xCC11131A), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "DEV PERFORMANCE MONITOR",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00E5FF),
            fontFamily = FontFamily.Monospace
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Render FPS: ${metrics.renderFps}",
                fontSize = 10.sp,
                color = if (metrics.renderFps >= 55) Color(0xFF69F0AE) else Color(0xFFFFD54F),
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Stroke FPS: ${metrics.strokeFps}",
                fontSize = 10.sp,
                color = Color(0xFF69F0AE),
                fontFamily = FontFamily.Monospace
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = String.format(Locale.US, "Latency: %.1f ms", metrics.avgLatencyMs),
                fontSize = 10.sp,
                color = if (metrics.avgLatencyMs <= 16f) Color(0xFF69F0AE) else Color(0xFFFF8A80),
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Points: ${metrics.strokePoints}",
                fontSize = 10.sp,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Dropped: ${metrics.droppedEvents}",
                fontSize = 10.sp,
                color = if (metrics.droppedEvents == 0L) Color(0xFF69F0AE) else Color(0xFFFF8A80),
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = String.format(Locale.US, "Memory: %.1f MB", metrics.memoryUsageMb),
                fontSize = 10.sp,
                color = Color(0xFFB0BEC5),
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
