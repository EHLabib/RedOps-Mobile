package com.redops.mobile.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val CriticalColor = Color(0xFFFF5252)
val HighColor = Color(0xFFFF9100)
val MediumColor = Color(0xFFFFD740)
val LowColor = Color(0xFF69F0AE)
val InfoColor = Color(0xFF40C4FF)

fun getSeverityColor(severity: String): Color = when (severity.lowercase()) {
    "critical" -> CriticalColor
    "high" -> HighColor
    "medium" -> MediumColor
    "low" -> LowColor
    else -> InfoColor
}

@Composable
fun SeverityBadge(severity: String, modifier: Modifier = Modifier) {
    val color = getSeverityColor(severity)
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = severity.uppercase(),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

