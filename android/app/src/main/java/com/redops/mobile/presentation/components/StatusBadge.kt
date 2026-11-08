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

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier,
    customColor: Color? = null
) {
    val color = customColor ?: when (status.lowercase()) {
        "active", "resolved", "confirmed", "authorized" -> Color(0xFF00E676)
        "in progress", "planned" -> Color(0xFF00E5FF)
        "reopened", "critical" -> Color(0xFFFF5252)
        "accepted risk", "on hold" -> Color(0xFFFFD740)
        else -> Color(0xFFA7B0AA)
    }

    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = status.uppercase(),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
        )
    }
}

