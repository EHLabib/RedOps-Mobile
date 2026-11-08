package com.redops.mobile.presentation.evidence

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redops.mobile.domain.model.EvidenceRecord
import com.redops.mobile.presentation.components.ConfirmDeleteDialog
import com.redops.mobile.presentation.components.EmptyStateView
import com.redops.mobile.presentation.components.StatusBadge
import com.redops.mobile.utils.EvidenceFileManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvidenceScreen(
    viewModel: EvidenceViewModel,
    onBack: () -> Unit,
    onEvidenceClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val evidenceList by viewModel.evidenceList.collectAsState()
    val engagements by viewModel.engagements.collectAsState()
    val findings by viewModel.findings.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categoryFilter by viewModel.categoryFilter.collectAsState()
    val engagementFilter by viewModel.engagementFilter.collectAsState()
    val findingFilter by viewModel.findingFilter.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val importState by viewModel.importState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var evidenceToDelete by remember { mutableStateOf<EvidenceRecord?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var engDropdownExpanded by remember { mutableStateOf(false) }
    var fndDropdownExpanded by remember { mutableStateOf(false) }

    val categoryList = listOf("ALL") + EVIDENCE_CATEGORIES

    // Handle import state toasts/snackbars
    LaunchedEffect(importState) {
        when (importState) {
            is FileImportState.Error -> {
                // Keep error visible or let user retry
            }
            is FileImportState.Success -> {
                viewModel.resetImportState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "EVIDENCE REPOSITORY",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Secure local artifacts & attachment references",
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
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Evidence",
                            tint = Color(0xFF00E676)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1412))
            )
        },
        containerColor = Color(0xFF0D1412)
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Storage Protection Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF182C2A)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "App-private sandbox protection: Artifacts are copied to internal app storage with SHA-256 integrity validation.",
                        color = Color(0xFF00E676),
                        fontSize = 10.5.sp
                    )
                }
            }

            // Search & Sort Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search by title, filename, or description...", fontSize = 12.sp) },
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
                            Icons.AutoMirrored.Filled.Sort,
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
                                viewModel.setSortOrder(EvidenceSortOrder.NEWEST)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Oldest First", fontSize = 12.sp) },
                            onClick = {
                                viewModel.setSortOrder(EvidenceSortOrder.OLDEST)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Title (A-Z)", fontSize = 12.sp) },
                            onClick = {
                                viewModel.setSortOrder(EvidenceSortOrder.TITLE_AZ)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }

            // Category Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
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

            // Scope Filter Dropdown Row (Engagement and Finding)
            if (engagements.isNotEmpty()) {
                val currentEng = engagements.find { it.id == engagementFilter }
                val relevantFindings = findings.filter { engagementFilter == null || it.engagementId == engagementFilter }
                val currentFnd = findings.find { it.id == findingFilter }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Engagement Filter
                    ExposedDropdownMenuBox(
                        expanded = engDropdownExpanded,
                        onExpandedChange = { engDropdownExpanded = !engDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = currentEng?.let { it.client.take(14) } ?: "All Engagements",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = engDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = engDropdownExpanded,
                            onDismissRequest = { engDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Engagements", fontSize = 12.sp) },
                                onClick = {
                                    viewModel.setEngagementFilter(null)
                                    viewModel.setFindingFilter(null)
                                    engDropdownExpanded = false
                                }
                            )
                            engagements.forEach { eng ->
                                DropdownMenuItem(
                                    text = { Text("${eng.title} (${eng.client})", fontSize = 12.sp) },
                                    onClick = {
                                        viewModel.setEngagementFilter(eng.id)
                                        viewModel.setFindingFilter(null)
                                        engDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Finding Filter
                    if (relevantFindings.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = fndDropdownExpanded,
                            onExpandedChange = { fndDropdownExpanded = !fndDropdownExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = currentFnd?.let { "[${it.customId}]" } ?: "All Findings",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fndDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = fndDropdownExpanded,
                                onDismissRequest = { fndDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Findings", fontSize = 12.sp) },
                                    onClick = {
                                        viewModel.setFindingFilter(null)
                                        fndDropdownExpanded = false
                                    }
                                )
                                relevantFindings.forEach { fnd ->
                                    DropdownMenuItem(
                                        text = { Text("[${fnd.customId}] ${fnd.title}", fontSize = 12.sp) },
                                        onClick = {
                                            viewModel.setFindingFilter(fnd.id)
                                            fndDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Loading state while importing
            if (importState is FileImportState.Importing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = Color(0xFF00E676)
                )
            }

            // Evidence List
            if (evidenceList.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.AttachFile,
                    title = if (searchQuery.isBlank() && categoryFilter == "ALL") "No evidence files recorded" else "No matching evidence",
                    description = "Attach proof-of-concept screenshots, response logs, and exported configuration files via Android's Storage Access Framework.",
                    actionLabel = if (searchQuery.isBlank() && categoryFilter == "ALL") "+ Attach Evidence File" else null,
                    onAction = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(evidenceList, key = { it.id }) { ev ->
                        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ev.createdAt))
                        val eng = engagements.find { it.id == ev.engagementId }
                        val fnd = findings.find { it.id == ev.findingId }
                        val formattedSize = formatFileSize(ev.fileSize)

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF141B18)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEvidenceClick(ev.id) }
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
                                    Text(
                                        ev.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    StatusBadge(ev.category, customColor = Color(0xFF00E676))
                                }

                                if (ev.description.isNotBlank()) {
                                    Text(
                                        ev.description,
                                        color = Color(0xFFA7B0AA),
                                        fontSize = 12.5.sp,
                                        lineHeight = 17.sp,
                                        maxLines = 2
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (eng != null) {
                                        Text(
                                            "Scope: ${eng.client}",
                                            color = Color(0xFF00E676),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    if (fnd != null) {
                                        Surface(
                                            color = Color(0xFFFF9100).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "[${fnd.customId}] ${fnd.title.take(18)}",
                                                color = Color(0xFFFF9100),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                if (ev.originalFilename.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.InsertDriveFile,
                                            contentDescription = null,
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            ev.originalFilename,
                                            color = Color(0xFF00E5FF),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1
                                        )
                                    }
                                }

                                if (ev.sha256Checksum.isNotBlank()) {
                                    Text(
                                        "SHA-256: ${ev.sha256Checksum.take(16)}...",
                                        color = Color(0xFFA7B0AA),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "$dateStr  •  $formattedSize  •  ${ev.fileType.substringAfter("/")}",
                                        color = Color(0xFFA7B0AA),
                                        fontSize = 10.sp
                                    )
                                    Spacer(Modifier.weight(1f))

                                    IconButton(
                                        onClick = {
                                            EvidenceFileManager.shareFile(
                                                context = context,
                                                localPath = ev.uriOrPath,
                                                mimeType = ev.fileType,
                                                title = ev.title
                                            )
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = "Share",
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    TextButton(onClick = { onEvidenceClick(ev.id) }) {
                                        Text("Details", color = Color(0xFF00E676), fontSize = 11.5.sp)
                                    }

                                    TextButton(onClick = { evidenceToDelete = ev }) {
                                        Text("Delete", color = Color(0xFFFF5252), fontSize = 11.5.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            EvidenceDialog(
                engagements = engagements,
                findings = findings,
                preselectedEngagementId = engagementFilter,
                preselectedFindingId = findingFilter,
                onDismiss = { showAddDialog = false },
                onSave = { engId, fndId, title, desc, cat, uri ->
                    viewModel.importAndSaveEvidence(
                        context = context,
                        engagementId = engId,
                        findingId = fndId,
                        title = title,
                        description = desc,
                        category = cat,
                        fileUri = uri
                    ) {
                        showAddDialog = false
                    }
                }
            )
        }

        evidenceToDelete?.let { ev ->
            ConfirmDeleteDialog(
                title = "Delete Evidence Artifact?",
                message = "Are you sure you want to delete '${ev.title}' and its stored local file?",
                onConfirm = {
                    viewModel.deleteEvidence(ev.id)
                    evidenceToDelete = null
                },
                onDismiss = { evidenceToDelete = null }
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format(Locale.getDefault(), "%.2f MB", mb)
        kb >= 1.0 -> String.format(Locale.getDefault(), "%.1f KB", kb)
        else -> "$bytes B"
    }
}
