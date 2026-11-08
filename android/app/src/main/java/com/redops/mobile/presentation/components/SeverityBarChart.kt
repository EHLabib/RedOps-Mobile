package com.redops.mobile.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SeverityBarChart(
    critical: Int,
    high: Int,
    medium: Int,
    low: Int,
    info: Int,
    modifier: Modifier = Modifier
) {
    val total = critical + high + medium + low + info

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (total == 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1B2934)),
                contentAlignment = Alignment.Center
            ) {
                Text("No vulnerabilities recorded", color = Color(0xFFA7B0AA), fontSize = 11.sp)
            }
        } else {
            // Stacked Bar representation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
            ) {
                if (critical > 0) Box(Modifier.weight(critical.toFloat()).fillMaxHeight().background(CriticalColor))
                if (high > 0) Box(Modifier.weight(high.toFloat()).fillMaxHeight().background(HighColor))
                if (medium > 0) Box(Modifier.weight(medium.toFloat()).fillMaxHeight().background(MediumColor))
                if (low > 0) Box(Modifier.weight(low.toFloat()).fillMaxHeight().background(LowColor))
                if (info > 0) Box(Modifier.weight(info.toFloat()).fillMaxHeight().background(InfoColor))
            }

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SeverityLegendItem("Crit", critical, CriticalColor)
                SeverityLegendItem("High", high, HighColor)
                SeverityLegendItem("Med", medium, MediumColor)
                SeverityLegendItem("Low", low, LowColor)
                SeverityLegendItem("Info", info, InfoColor)
            }
        }
    }
}

@Composable
private fun SeverityLegendItem(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(
            text = "$label: $count",
            color = Color(0xFFA7B0AA),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

