package com.redops.mobile.presentation.notebook

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
import com.redops.mobile.domain.model.NotebookEntry
import com.redops.mobile.presentation.components.ConfirmDeleteDialog
import com.redops.mobile.presentation.components.EmptyStateView
import com.redops.mobile.presentation.components.StatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookScreen(
    viewModel: NotebookViewModel,
    onAddEntry: () -> Unit,
    onEntryClick: (String) -> Unit,
    onOpenActivityLog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entries by viewModel.entries.collectAsState()
    val pinnedEntries by viewModel.pinnedEntries.collectAsState()
    val unpinnedEntries by viewModel.unpinnedEntries.collectAsState()
    val engagements by viewModel.engagements.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categoryFilter by viewModel.categoryFilter.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()
    val engagementFilter by viewModel.engagementFilter.collectAsState()
    val onlyFavorites by viewModel.onlyFavorites.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    var entryToEdit by remember { mutableStateOf<NotebookEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<NotebookEntry?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    val categoryList = listOf("ALL") + NOTEBOOK_CATEGORIES
    val typeList = listOf("ALL") + NOTEBOOK_ENTRY_TYPES

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "ASSESSMENT NOTEBOOK",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Technical observations, recon evidence & audit notes",
                    color = Color(0xFFA7B0AA),
                    fontSize = 11.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onOpenActivityLog,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = "Activity Audit Log",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Button(
                    onClick = onAddEntry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E676),
                        contentColor = Color(0xFF0B0F0E)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Note", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Safety Reminder Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF182C2A)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Manual documentation log only. RedOps Mobile does not conduct active scanning.",
                    color = Color(0xFF00E676),
                    fontSize = 10.5.sp
                )
            }
        }

        // Search Field with Sort Button
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
                placeholder = { Text("Search by title, content, or tags...", fontSize = 12.sp) },
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
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )

            Box {
                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        Icons.Default.Sort,
                        contentDescription = "Sort",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(22.dp)
                    )
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Newest First", fontSize = 12.sp) },
                        onClick = {
                            viewModel.setSortOrder(NotebookSortOrder.NEWEST)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Oldest First", fontSize = 12.sp) },
                        onClick = {
                            viewModel.setSortOrder(NotebookSortOrder.OLDEST)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Title (A-Z)", fontSize = 12.sp) },
                        onClick = {
                            viewModel.setSortOrder(NotebookSortOrder.TITLE_AZ)
                            showSortMenu = false
                        }
                    )
                }
            }
        }

        // Category Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(categoryList) { cat ->
                FilterChip(
                    selected = categoryFilter == cat,
                    onClick = { viewModel.setCategoryFilter(cat) },
                    label = { Text(cat, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00E676).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFF00E676)
                    )
                )
            }
        }

        // Secondary Filter Row (Type, Favorites Toggle)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                FilterChip(
                    selected = onlyFavorites,
                    onClick = { viewModel.toggleFavoritesFilter() },
                    leadingIcon = {
                        Icon(
                            imageVector = if (onlyFavorites) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (onlyFavorites) Color(0xFFFFD700) else Color(0xFFA7B0AA),
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = { Text("Favorites", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFD700).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFFFFD700)
                    )
                )
            }

            items(typeList) { type ->
                FilterChip(
                    selected = typeFilter == type,
                    onClick = { viewModel.setTypeFilter(type) },
                    label = { Text(type, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00E5FF).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFF00E5FF)
                    )
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Notebook List
        if (entries.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.EditNote,
                title = if (searchQuery.isBlank() && categoryFilter == "ALL" && typeFilter == "ALL" && !onlyFavorites) {
                    "Assessment notebook is empty"
                } else {
                    "No matching notes found"
                },
                description = "Record observation headers, port findings, proof-of-concept steps, and remediation checklists.",
                actionLabel = if (searchQuery.isBlank() && categoryFilter == "ALL") "+ Create First Entry" else null,
                onAction = onAddEntry
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Pinned Section
                if (pinnedEntries.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "PINNED ENTRIES (${pinnedEntries.size})",
                                color = Color(0xFF00E676),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    items(pinnedEntries, key = { "pinned_${it.id}" }) { entry ->
                        NotebookCard(
                            entry = entry,
                            engagements = engagements,
                            onClick = { onEntryClick(entry.id) },
                            onTogglePin = { viewModel.togglePin(entry) },
                            onToggleFavorite = { viewModel.toggleFavorite(entry) },
                            onEdit = { entryToEdit = entry },
                            onDelete = { entryToDelete = entry }
                        )
                    }

                    if (unpinnedEntries.isNotEmpty()) {
                        item {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 6.dp),
                                color = Color(0xFF1B2934),
                                thickness = 1.dp
                            )
                            Text(
                                "ALL ENTRIES",
                                color = Color(0xFFA7B0AA),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                // Unpinned Section
                items(unpinnedEntries, key = { it.id }) { entry ->
                    NotebookCard(
                        entry = entry,
                        engagements = engagements,
                        onClick = { onEntryClick(entry.id) },
                        onTogglePin = { viewModel.togglePin(entry) },
                        onToggleFavorite = { viewModel.toggleFavorite(entry) },
                        onEdit = { entryToEdit = entry },
                        onDelete = { entryToDelete = entry }
                    )
                }
            }
        }
    }

    entryToEdit?.let { entry ->
        NotebookDialog(
            initialEntry = entry,
            engagements = engagements,
            onDismiss = { entryToEdit = null },
            onSave = { updated ->
                viewModel.updateEntry(updated) {
                    entryToEdit = null
                }
            }
        )
    }

    entryToDelete?.let { entry ->
        ConfirmDeleteDialog(
            title = "Delete Notebook Entry?",
            message = "Are you sure you want to delete '${entry.title}'?",
            onConfirm = {
                viewModel.deleteEntry(entry.id)
                entryToDelete = null
            },
            onDismiss = { entryToDelete = null }
        )
    }
}

@Composable
private fun NotebookCard(
    entry: NotebookEntry,
    engagements: List<com.redops.mobile.domain.model.Engagement>,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(entry.createdAt))
    val associatedEng = engagements.find { it.id == entry.engagementId }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141B18)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (entry.isPinned) {
                        Icon(
                            Icons.Default.Bookmark,
                            contentDescription = "Pinned",
                            tint = Color(0xFF00E676),
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 4.dp)
                        )
                    }
                    Text(
                        entry.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(entry.category, customColor = Color(0xFF00E676))
                    StatusBadge(entry.entryType, customColor = Color(0xFF00E5FF))
                }
            }

            if (associatedEng != null) {
                Text(
                    "Engagement: ${associatedEng.title}",
                    color = Color(0xFF00E676),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                entry.description,
                color = Color(0xFFA7B0AA),
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                maxLines = 3
            )

            if (entry.tags.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    entry.tags.split(",").take(4).forEach { tag ->
                        if (tag.isNotBlank()) {
                            Surface(
                                color = Color(0xFF1B2934),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    tag.trim(),
                                    color = Color(0xFF00E5FF),
                                    fontSize = 9.5.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (!entry.evidenceReference.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = Color(0xFFA7B0AA),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Ref: ${entry.evidenceReference}",
                        color = Color(0xFFA7B0AA),
                        fontSize = 11.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(dateStr, color = Color(0xFFA7B0AA), fontSize = 10.sp)
                Spacer(Modifier.weight(1f))

                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = if (entry.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (entry.isFavorite) Color(0xFFFFD700) else Color(0xFFA7B0AA),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(onClick = onTogglePin, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = if (entry.isPinned) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Pin",
                        tint = if (entry.isPinned) Color(0xFF00E676) else Color(0xFFA7B0AA),
                        modifier = Modifier.size(16.dp)
                    )
                }

                TextButton(onClick = onEdit) {
                    Text("Edit", color = Color(0xFF00E5FF), fontSize = 11.5.sp)
                }
                TextButton(onClick = onDelete) {
                    Text("Delete", color = Color(0xFFFF5252), fontSize = 11.5.sp)
                }
            }
        }
    }
}
