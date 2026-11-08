package com.redops.mobile.presentation.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redops.mobile.domain.model.ActivityLog
import com.redops.mobile.domain.model.Engagement
import com.redops.mobile.presentation.components.ConfirmDeleteDialog
import com.redops.mobile.presentation.components.EmptyStateView
import com.redops.mobile.presentation.components.StatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLogScreen(
    viewModel: ActivityLogViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activities by viewModel.activities.collectAsState()
    val engagements by viewModel.engagements.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()
    val engagementFilter by viewModel.engagementFilter.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }
    var engagementDropdownExpanded by remember { mutableStateOf(false) }

    val typeFilters = listOf("ALL", "ENGAGEMENT", "FINDING", "NOTEBOOK", "EVIDENCE", "SYSTEM")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "AUDIT LOG",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Chronological security audit trail",
                            color = Color(0xFFA7B0AA),
                            fontSize = 10.5.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (activities.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear Activities",
                                tint = Color(0xFFFF5252)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D1412)
                )
            )
        },
        containerColor = Color(0xFF0D1412),
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search audit logs by title, action, or type...", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFFA7B0AA),
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color(0xFFA7B0AA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(10.dp)
            )

            // Type Filter Chips Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(typeFilters) { type ->
                    FilterChip(
                        selected = typeFilter == type,
                        onClick = { viewModel.setTypeFilter(type) },
                        label = { Text(type, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00E676).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF00E676)
                        )
                    )
                }
            }

            // Engagement Filter Dropdown (if engagements exist)
            if (engagements.isNotEmpty()) {
                val currentEng = engagements.find { it.id == engagementFilter }
                ExposedDropdownMenuBox(
                    expanded = engagementDropdownExpanded,
                    onExpandedChange = { engagementDropdownExpanded = !engagementDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = currentEng?.let { "Engagement: ${it.title}" } ?: "Scope: All Engagements",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = engagementDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = engagementDropdownExpanded,
                        onDismissRequest = { engagementDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Engagements", fontSize = 12.sp) },
                            onClick = {
                                viewModel.setEngagementFilter(null)
                                engagementDropdownExpanded = false
                            }
                        )
                        engagements.forEach { eng ->
                            DropdownMenuItem(
                                text = { Text("${eng.title} (${eng.client})", fontSize = 12.sp) },
                                onClick = {
                                    viewModel.setEngagementFilter(eng.id)
                                    engagementDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Activities Feed
            if (activities.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.HistoryToggleOff,
                    title = if (searchQuery.isBlank() && typeFilter == "ALL") "Audit log is empty" else "No matching logs",
                    description = "Assessment operations, findings creation, and notebook changes will automatically be recorded here."
                )
            } else {
                Text(
                    "LOGGED EVENTS (${activities.size})",
                    color = Color(0xFFA7B0AA),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(activities, key = { it.id }) { log ->
                        ActivityLogCard(log = log, engagements = engagements)
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        ConfirmDeleteDialog(
            title = "Clear Activity Audit Log?",
            message = "Are you sure you want to clear all recorded activity logs? This operation cannot be undone.",
            onConfirm = {
                viewModel.clearActivities()
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false }
        )
    }
}

@Composable
private fun ActivityLogCard(
    log: ActivityLog,
    engagements: List<Engagement>
) {
    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
    val associatedEng = engagements.find { it.id == log.engagementId }

    val icon: ImageVector = when (log.type.uppercase()) {
        "ENGAGEMENT" -> Icons.Default.Security
        "FINDING" -> Icons.Default.BugReport
        "NOTEBOOK" -> Icons.Default.MenuBook
        "EVIDENCE" -> Icons.Default.AttachFile
        else -> Icons.Default.Info
    }

    val iconColor = when (log.type.uppercase()) {
        "ENGAGEMENT" -> Color(0xFF00E676)
        "FINDING" -> Color(0xFFFF5252)
        "NOTEBOOK" -> Color(0xFF00E5FF)
        "EVIDENCE" -> Color(0xFFFFD700)
        else -> Color(0xFFA7B0AA)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141B18)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = iconColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.title,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    StatusBadge(log.statusBadge, customColor = iconColor)
                }

                if (associatedEng != null) {
                    Text(
                        "Scope: ${associatedEng.title}",
                        color = Color(0xFF00E676),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = dateStr,
                    color = Color(0xFFA7B0AA),
                    fontSize = 10.5.sp
                )
            }
        }
    }
}

