package com.redops.mobile.presentation.findings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redops.mobile.presentation.components.ConfirmDeleteDialog
import com.redops.mobile.presentation.components.SeverityBadge
import com.redops.mobile.presentation.components.StatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindingDetailScreen(
    findingId: String,
    viewModel: FindingsViewModel,
    onBack: () -> Unit,
    onNavigateToEngagement: (String) -> Unit,
    onNavigateToEvidenceDetail: (String) -> Unit = {},
    onAddEvidence: (findingId: String, engagementId: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val finding by viewModel.getFindingById(findingId).collectAsState(initial = null)
    val engagements by viewModel.engagements.collectAsState()
    val associatedEvidence by viewModel.getEvidenceForFinding(findingId).collectAsState(initial = emptyList())

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val dateTimeFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    finding?.let { f ->
        val associatedEngagement = engagements.find { it.id == f.engagementId }
        val createdDateStr = dateTimeFormatter.format(Date(f.createdAt))
        val updatedDateStr = dateTimeFormatter.format(Date(f.updatedAt))

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "[${f.customId}] ${f.title}",
                                maxLines = 1,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Vulnerability Finding Details",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)
            ) {
                // Main Header Status & Rating Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SeverityBadge(f.severity)
                                StatusBadge(f.status)
                            }

                            Text(
                                text = f.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Metadata metrics
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (f.cvssScore != null) {
                                    Surface(
                                        color = Color(0xFF40C4FF).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "CVSS v3.1: ${f.cvssScore}",
                                            color = Color(0xFF40C4FF),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                if (!f.cweReference.isNullOrBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = f.cweReference,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = f.category,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.background)

                            // Quick Status Workflow Selector
                            Text(
                                text = "LIFECYCLE STATUS TRANSITION",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )

                            val availableStatuses = listOf("Open", "In Progress", "Resolved", "Accepted Risk", "False Positive")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                availableStatuses.take(3).forEach { st ->
                                    val isCurrent = f.status.equals(st, ignoreCase = true)
                                    FilterChip(
                                        selected = isCurrent,
                                        onClick = { if (!isCurrent) viewModel.updateFindingStatus(f, st) },
                                        label = { Text(st, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            selectedLabelColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                availableStatuses.drop(3).forEach { st ->
                                    val isCurrent = f.status.equals(st, ignoreCase = true)
                                    FilterChip(
                                        selected = isCurrent,
                                        onClick = { if (!isCurrent) viewModel.updateFindingStatus(f, st) },
                                        label = { Text(st, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            selectedLabelColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Associated Engagement Card (Clickable to open engagement)
                associatedEngagement?.let { eng ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToEngagement(eng.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(8.dp).size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = "ASSOCIATED ENGAGEMENT",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = eng.title,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Client: ${eng.client}  •  Scope: ${eng.authorizedScope}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Open",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Affected Asset
                item {
                    DetailCard(
                        title = "AFFECTED ASSET & COMPONENT",
                        content = f.affectedAsset,
                        accentColor = Color(0xFF40C4FF)
                    )
                }

                // Vulnerability Description
                item {
                    DetailCard(
                        title = "VULNERABILITY DESCRIPTION",
                        content = f.description
                    )
                }

                // Technical Impact
                if (f.technicalImpact.isNotBlank()) {
                    item {
                        DetailCard(
                            title = "POTENTIAL TECHNICAL & BUSINESS IMPACT",
                            content = f.technicalImpact,
                            accentColor = Color(0xFFFF5252)
                        )
                    }
                }

                // Remediation Recommendation
                item {
                    DetailCard(
                        title = "RECOMMENDED REMEDIATION",
                        content = f.remediation,
                        accentColor = MaterialTheme.colorScheme.secondary
                    )
                }

                // Technical Details / Proof-of-Concept
                if (!f.pocReference.isNullOrBlank()) {
                    item {
                        DetailCard(
                            title = "TECHNICAL DETAILS & PROOF OF CONCEPT",
                            content = f.pocReference
                        )
                    }
                }

                // Evidence File Reference
                if (!f.evidenceReference.isNullOrBlank()) {
                    item {
                        DetailCard(
                            title = "EVIDENCE REFERENCE",
                            content = f.evidenceReference,
                            accentColor = Color(0xFFFFD740)
                        )
                    }
                }

                // Attached Evidence Records Section
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ATTACHED EVIDENCE ARTIFACTS (${associatedEvidence.size})",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                TextButton(
                                    onClick = { onAddEvidence(f.id, f.engagementId) },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("+ Attach", color = MaterialTheme.colorScheme.primary, fontSize = 11.5.sp)
                                }
                            }

                            if (associatedEvidence.isEmpty()) {
                                Text(
                                    text = "No evidence files linked to this finding yet. Tap '+ Attach' to link proof-of-concept screenshots or logs.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.5.sp
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    associatedEvidence.forEach { ev ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.background,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onNavigateToEvidenceDetail(ev.id) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = ev.title,
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        StatusBadge(ev.category, customColor = Color(0xFF00E676))
                                                    }
                                                    if (ev.originalFilename.isNotBlank()) {
                                                        Text(
                                                            text = "${ev.originalFilename} • ${ev.fileType.substringAfter("/")}",
                                                            color = Color(0xFFA7B0AA),
                                                            fontSize = 10.5.sp
                                                        )
                                                    }
                                                }
                                                Icon(
                                                    Icons.Default.ChevronRight,
                                                    contentDescription = "View",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Additional Notes
                if (f.generalNotes.isNotBlank()) {
                    item {
                        DetailCard(
                            title = "ADDITIONAL NOTES",
                            content = f.generalNotes
                        )
                    }
                }

                // Discovery Date & Timestamps Footer
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (f.discoveryDate.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Discovery Date:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                    Text(f.discoveryDate, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Recorded Timestamp:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                Text(createdDateStr, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Last Modified:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                Text(updatedDateStr, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Edit Finding Dialog
        if (showEditDialog) {
            FindingDialog(
                initialFinding = f,
                engagements = engagements,
                onDismiss = { showEditDialog = false },
                onSave = { updated ->
                    viewModel.updateFinding(updated) {
                        showEditDialog = false
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirm) {
            ConfirmDeleteDialog(
                title = "Permanently Delete Finding?",
                message = "Are you sure you want to delete finding '[${f.customId}] ${f.title}'? This action cannot be undone.",
                onConfirm = {
                    viewModel.deleteFinding(f.id) {
                        showDeleteConfirm = false
                        onBack()
                    }
                },
                onDismiss = { showDeleteConfirm = false }
            )
        }
    }
}

@Composable
private fun DetailCard(
    title: String,
    content: String,
    accentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Text(
                text = content,
                color = accentColor,
                fontSize = 12.5.sp,
                lineHeight = 17.sp
            )
        }
    }
}
