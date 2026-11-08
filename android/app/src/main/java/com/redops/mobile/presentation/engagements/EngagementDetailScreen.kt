package com.redops.mobile.presentation.engagements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.redops.mobile.domain.model.Finding
import com.redops.mobile.domain.model.NotebookEntry
import com.redops.mobile.presentation.components.ConfirmDeleteDialog
import com.redops.mobile.presentation.components.SeverityBadge
import com.redops.mobile.presentation.components.StatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngagementDetailScreen(
    engagementId: String,
    viewModel: EngagementsViewModel,
    onBack: () -> Unit,
    onNavigateToFindingDetail: (String) -> Unit,
    onNewFindingForEngagement: (String) -> Unit,
    onNewNoteForEngagement: (String) -> Unit,
    onGenerateReport: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val engagement by viewModel.getEngagementById(engagementId).collectAsState(initial = null)
    val findings by viewModel.getFindingsForEngagement(engagementId).collectAsState(initial = emptyList())
    val notes by viewModel.getNotesForEngagement(engagementId).collectAsState(initial = emptyList())

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val dateTimeFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    engagement?.let { eng ->
        val authDateStr = eng.authorizationTimestamp?.let {
            dateTimeFormatter.format(Date(it))
        } ?: "Prior to creation"

        val createdDateStr = dateTimeFormatter.format(Date(eng.createdAt))
        val updatedDateStr = dateTimeFormatter.format(Date(eng.updatedAt))

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = eng.title,
                                maxLines = 1,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = eng.client,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
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
                // Main Header Info Card
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
                                Text(
                                    text = eng.client.uppercase(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.8.sp
                                )
                                StatusBadge(status = eng.status)
                            }

                            Text(
                                text = eng.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = eng.assessmentType,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = "${eng.startDate} → ${eng.endDate}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.5.sp
                                )
                            }

                            if (eng.description.isNotBlank()) {
                                Text(
                                    text = eng.description,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.background, modifier = Modifier.padding(vertical = 2.dp))

                            // Scope & Restrictions
                            Text("AUTHORIZED SCOPE:", color = MaterialTheme.colorScheme.primary, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            Surface(
                                color = MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = eng.authorizedScope,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }

                            if (eng.outOfScope.isNotBlank()) {
                                Text("EXPLICIT OUT-OF-SCOPE TARGETS:", color = Color(0xFFFF5252), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                Surface(
                                    color = MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = eng.outOfScope,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            if (eng.assessmentObjectives.isNotBlank()) {
                                Text("ASSESSMENT OBJECTIVES:", color = Color(0xFF40C4FF), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = eng.assessmentObjectives,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }

                            if (eng.additionalNotes.isNotBlank()) {
                                Text("OPERATIONAL NOTES:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = eng.additionalNotes,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.5.sp
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.background, modifier = Modifier.padding(vertical = 2.dp))

                            // Authorization Declaration Status
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Authorization Declaration: Confirmed ($authDateStr)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Created / Updated Timestamps
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Created: $createdDateStr", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                Text("Updated: $updatedDateStr", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            }
                        }
                    }
                }

                // Quick Action Bar: Log Finding, Add Note, Generate Report
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onNewFindingForEngagement(eng.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Finding", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onNewNoteForEngagement(eng.id) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Note", fontSize = 11.sp)
                        }

                        if (onGenerateReport != null) {
                            OutlinedButton(
                                onClick = { onGenerateReport(eng.id) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Report", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Associated Findings Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ASSOCIATED VULNERABILITY FINDINGS (${findings.size})",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        if (findings.isNotEmpty()) {
                            Text(
                                text = "${findings.count { it.status == "Resolved" }} Resolved",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (findings.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "No findings logged for this engagement yet. Tap '+ Finding' to record technical vulnerabilities.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                } else {
                    items(findings, key = { it.id }) { finding ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToFindingDetail(finding.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = "[${finding.customId}] ${finding.title}",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp
                                    )
                                    Text(
                                        text = finding.affectedAsset,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                                SeverityBadge(finding.severity)
                            }
                        }
                    }
                }

                // Associated Notebook Entries Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ASSESSMENT NOTEBOOK ENTRIES (${notes.size})",
                            color = Color(0xFF40C4FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                if (notes.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "No notebook entries recorded for this engagement. Tap '+ Note' to document reconnaissance or observations.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                } else {
                    items(notes, key = { it.id }) { note ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = note.title,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    StatusBadge(note.entryType, customColor = Color(0xFF40C4FF))
                                }
                                Text(
                                    text = note.description,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Edit Dialog
        if (showEditDialog) {
            EngagementDialog(
                initialEngagement = eng,
                onDismiss = { showEditDialog = false },
                onSave = { updated ->
                    viewModel.updateEngagement(updated) {
                        showEditDialog = false
                    }
                }
            )
        }

        // Deliberate Cascade Deletion Confirmation Dialog
        if (showDeleteConfirm) {
            val countFindings = findings.size
            val countNotes = notes.size

            ConfirmDeleteDialog(
                title = "Permanently Delete Engagement?",
                message = "Are you sure you want to delete '${eng.title}'?\n\nThis will also permanently delete:\n• $countFindings associated vulnerability findings\n• $countNotes associated notebook entries\n• Any attached evidence records\n\nThis action cannot be undone.",
                onConfirm = {
                    viewModel.deleteEngagement(eng.id) {
                        showDeleteConfirm = false
                        onBack()
                    }
                },
                onDismiss = { showDeleteConfirm = false }
            )
        }
    }
}
