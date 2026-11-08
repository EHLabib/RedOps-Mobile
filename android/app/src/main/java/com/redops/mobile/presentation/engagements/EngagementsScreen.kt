package com.redops.mobile.presentation.engagements

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
import com.redops.mobile.domain.model.Engagement
import com.redops.mobile.presentation.components.ConfirmDeleteDialog
import com.redops.mobile.presentation.components.EmptyStateView
import com.redops.mobile.presentation.components.EngagementCard
import com.redops.mobile.presentation.components.SectionHeader
import com.redops.mobile.presentation.components.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngagementsScreen(
    viewModel: EngagementsViewModel,
    onEngagementClick: (Engagement) -> Unit,
    onAddEngagement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val engagements by viewModel.engagements.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()

    var engagementToEdit by remember { mutableStateOf<Engagement?>(null) }
    var engagementToDelete by remember { mutableStateOf<Engagement?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val statusFilters = listOf("ALL", "Planning", "Active", "On Hold", "Completed", "Cancelled")
    val typeFilters = listOf("ALL", "Web Application", "Network", "Mobile Application", "Internal", "External", "Other")

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
                    text = "ENGAGEMENTS",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Authorized Assessment Operations & Scopes",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp
                )
            }
            Button(
                onClick = onAddEngagement,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("New", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // Search Bar & Sort Menu
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
                placeholder = { Text("Search by title, client, or scope...", fontSize = 12.sp) },
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
                    modifier = Modifier
                        .size(52.dp)
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
                    EngagementSortOption.values().forEach { option ->
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

        // Status Filter Chips Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(statusFilters) { filter ->
                FilterChip(
                    selected = statusFilter == filter,
                    onClick = { viewModel.setStatusFilter(filter) },
                    label = { Text(filter, fontSize = 10.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        // Type Filter Chips Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(typeFilters) { type ->
                FilterChip(
                    selected = typeFilter == type,
                    onClick = { viewModel.setTypeFilter(type) },
                    label = { Text(type, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.secondary
                    )
                )
            }
        }

        // Engagements List or Empty State
        if (engagements.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.FolderOpen,
                title = if (searchQuery.isNotEmpty() || statusFilter != "ALL" || typeFilter != "ALL") "No Matching Engagements" else "No Engagements Found",
                description = if (searchQuery.isNotEmpty() || statusFilter != "ALL" || typeFilter != "ALL")
                    "Try adjusting your search criteria or clear status filters."
                else
                    "Create your first authorized security assessment engagement to document targets, scope, and findings.",
                actionLabel = if (searchQuery.isNotEmpty() || statusFilter != "ALL" || typeFilter != "ALL") "Reset Filters" else "New Engagement",
                onAction = {
                    if (searchQuery.isNotEmpty() || statusFilter != "ALL" || typeFilter != "ALL") {
                        viewModel.setSearchQuery("")
                        viewModel.setStatusFilter("ALL")
                        viewModel.setTypeFilter("ALL")
                    } else {
                        onAddEngagement()
                    }
                },
                modifier = Modifier.padding(top = 24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(engagements, key = { it.id }) { engagement ->
                    EngagementCard(
                        engagement = engagement,
                        onClick = { onEngagementClick(engagement) }
                    )
                }
            }
        }
    }

    // Edit Dialog
    if (engagementToEdit != null) {
        EngagementDialog(
            initialEngagement = engagementToEdit,
            onDismiss = { engagementToEdit = null },
            onSave = { updated ->
                viewModel.updateEngagement(updated) {
                    engagementToEdit = null
                }
            }
        )
    }

    // Delete Confirmation Dialog with Cascade Warning
    if (engagementToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Engagement?",
            message = "Are you sure you want to delete '${engagementToDelete?.title}'? All associated findings, notebook observations, and evidence links will be permanently deleted.",
            onConfirm = {
                engagementToDelete?.let { viewModel.deleteEngagement(it.id) }
                engagementToDelete = null
            },
            onDismiss = { engagementToDelete = null }
        )
    }
}
