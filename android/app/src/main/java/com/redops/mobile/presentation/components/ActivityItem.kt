package com.redops.mobile.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redops.mobile.domain.model.ActivityLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityItem(
    activity: ActivityLog,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(activity.timestamp))

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (icon, tint) = when (activity.type.uppercase()) {
                "ENGAGEMENT" -> Icons.Default.FolderOpen to MaterialTheme.colorScheme.primary
                "FINDING" -> Icons.Default.BugReport to Color(0xFFFF5252)
                "NOTEBOOK" -> Icons.Default.EditNote to Color(0xFF40C4FF)
                "EVIDENCE" -> Icons.Default.AttachFile to Color(0xFFFFD740)
                "REPORT" -> Icons.Default.Description to Color(0xFF69F0AE)
                else -> Icons.Default.Security to MaterialTheme.colorScheme.primary
            }

            Surface(
                color = tint.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.padding(6.dp).size(16.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = dateStr,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.5.sp
                )
            }
            StatusBadge(activity.statusBadge)
        }
    }
}

