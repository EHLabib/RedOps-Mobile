package com.redops.mobile.presentation.findings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redops.mobile.domain.model.Finding
import com.redops.mobile.presentation.components.ConfirmDeleteDialog
import com.redops.mobile.presentation.components.EmptyStateView
import com.redops.mobile.presentation.components.SeverityBadge
import com.redops.mobile.presentation.components.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindingsScreen(
    viewModel: FindingsViewModel,
    onFindingClick: (Finding) -> Unit,
    onAddFinding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val findings by viewModel.findings.collectAsState()
    val engagements by viewModel.engagements.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val severityFilter by viewModel.severityFilter.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val categoryFilter by viewModel.categoryFilter.collectAsState()
    val engagementFilter by viewModel.engagementFilter.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()

    var findingToEdit by remember { mutableStateOf<Finding?>(null) }
    var findingToDelete by remember { mutableStateOf<Finding?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var engFilterDropdownExpanded by remember { mutableStateOf(false) }

    val severityFilters = listOf("ALL", "Critical", "High", "Medium", "Low", "Informational")
    val statusFilters = listOf("ALL", "Open", "In Progress", "Resolved", "Accepted Risk", "False Positive")
    val categoryFilters = listOf("ALL", "Authentication", "Authorization", "Input Validation", "Injection", "Configuration", "Information Disclosure", "Cryptography", "Session Management", "Business Logic", "Other")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "VULNERABILITIES",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Security Findings & Remediation Tracking",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp
                )
            }
            Button(
                onClick = onAddFinding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Log Finding", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // Search Field & Sort Icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by ID, title, asset, category, CWE...", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )

            // Sort Dropdown
            Box {
                IconButton(
                    onClick = { sortMenuExpanded = true },
                    modifier = Modifier.size(52.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    FindingSortOption.values().forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.displayName,
                                    fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal,
                                    color = if (sortOption == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                viewModel.setSortOption(option)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Engagement Filter Bar (if multiple engagements exist)
        if (engagements.size > 1) {
            val selectedEngagementName = if (engagementFilter == "ALL") {
                "All Engagements (${engagements.size})"
            } else {
                engagements.find { it.id == engagementFilter }?.title ?: "Selected Engagement"
            }

            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().clickable { engFilterDropdownExpanded = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Scope: $selectedEngagementName",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                DropdownMenu(
                    expanded = engFilterDropdownExpanded,
                    onDismissRequest = { engFilterDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Engagements", fontSize = 12.sp) },
                        onClick = {
                            viewModel.setEngagementFilter("ALL")
                            engFilterDropdownExpanded = false
                        }
                    )
                    engagements.forEach { eng ->
                        DropdownMenuItem(
                            text = { Text("${eng.title} (${eng.client})", fontSize = 12.sp) },
                            onClick = {
                                viewModel.setEngagementFilter(eng.id)
                                engFilterDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Severity Filter Chips Row
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(severityFilters) { sev ->
                FilterChip(
                    selected = severityFilter == sev,
                    onClick = { viewModel.setSeverityFilter(sev) },
                    label = { Text(sev, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // Status Filter Chips Row
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(statusFilters) { st ->
                FilterChip(
                    selected = statusFilter == st,
                    onClick = { viewModel.setStatusFilter(st) },
                    label = { Text(st, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF40C4FF).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFF40C4FF)
                    )
                )
            }
        }

        // Category Filter Chips Row
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(categoryFilters) { cat ->
                FilterChip(
                    selected = categoryFilter == cat,
                    onClick = { viewModel.setCategoryFilter(cat) },
                    label = { Text(cat, fontSize = 9.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.secondary
                    )
                )
            }
        }

        // Findings List or Empty State
        if (findings.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.BugReport,
                title = if (searchQuery.isNotBlank() || severityFilter != "ALL" || statusFilter != "ALL" || categoryFilter != "ALL" || engagementFilter != "ALL")
                    "No Matching Findings"
                else
                    "No Vulnerabilities Logged",
                description = if (searchQuery.isNotBlank() || severityFilter != "ALL" || statusFilter != "ALL" || categoryFilter != "ALL" || engagementFilter != "ALL")
                    "Try broadening your search term or reset active severity, status, or category filters."
                else
                    "Document observable security exposures with affected targets, CVSS ratings, technical impact, and remediation roadmaps.",
                actionLabel = if (searchQuery.isNotBlank() || severityFilter != "ALL" || statusFilter != "ALL" || categoryFilter != "ALL" || engagementFilter != "ALL")
                    "Reset All Filters"
                else
                    "+ Log First Finding",
                onAction = {
                    if (searchQuery.isNotBlank() || severityFilter != "ALL" || statusFilter != "ALL" || categoryFilter != "ALL" || engagementFilter != "ALL") {
                        viewModel.setSearchQuery("")
                        viewModel.setSeverityFilter("ALL")
                        viewModel.setStatusFilter("ALL")
                        viewModel.setCategoryFilter("ALL")
                        viewModel.setEngagementFilter("ALL")
                    } else {
                        onAddFinding()
                    }
                },
                modifier = Modifier.padding(top = 20.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
            ) {
                items(findings, key = { it.id }) { finding ->
                    val associatedEngagement = engagements.find { it.id == finding.engagementId }
                    FindingCard(
                        finding = finding,
                        engagementName = associatedEngagement?.let { "${it.title} (${it.client})" } ?: "General Assessment",
                        onClick = { onFindingClick(finding) },
                        onToggle = { viewModel.toggleFindingResolved(finding) },
                        onEdit = { findingToEdit = finding },
                        onDelete = { findingToDelete = finding }
                    )
                }
            }
        }
    }

    findingToEdit?.let { f ->
        FindingDialog(
            initialFinding = f,
            engagements = engagements,
            onDismiss = { findingToEdit = null },
            onSave = { updated ->
                viewModel.updateFinding(updated) {
                    findingToEdit = null
                }
            }
        )
    }

    findingToDelete?.let { f ->
        ConfirmDeleteDialog(
            title = "Delete Finding?",
            message = "Are you sure you want to permanently delete '${f.customId}: ${f.title}'? This action cannot be undone.",
            onConfirm = {
                viewModel.deleteFinding(f.id) {
                    findingToDelete = null
                }
            },
            onDismiss = { findingToDelete = null }
        )
    }
}

@Composable
private fun FindingCard(
    finding: Finding,
    engagementName: String,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Engagement tag & Severity
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = engagementName,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
                SeverityBadge(finding.severity)
            }

            // Title & ID
            Text(
                text = "[${finding.customId}] ${finding.title}",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            // Asset & Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = finding.affectedAsset,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = finding.category,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Remediation Summary
            Text(
                text = "Fix: ${finding.remediation}",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 11.5.sp,
                maxLines = 1
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.background)

            // Bottom action row: Status badge + quick action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(finding.status)

                if (finding.cvssScore != null) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "CVSS ${finding.cvssScore}",
                            color = Color(0xFF40C4FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                TextButton(onClick = onToggle) {
                    Text(
                        text = if (finding.status == "Resolved") "Reopen" else "Resolve",
                        color = if (finding.status == "Resolved") Color(0xFFFF9100) else MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                TextButton(onClick = onEdit) {
                    Text("Edit", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                }
                TextButton(onClick = onDelete) {
                    Text("Delete", color = Color(0xFFFF5252), fontSize = 11.sp)
                }
            }
        }
    }
}
