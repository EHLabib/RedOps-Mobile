package com.redops.mobile.presentation.notebook

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redops.mobile.domain.model.NotebookEntry
import com.redops.mobile.presentation.components.ConfirmDeleteDialog
import com.redops.mobile.presentation.components.StatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookDetailScreen(
    entryId: String,
    viewModel: NotebookViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val entryFlow = remember(entryId) { viewModel.getEntryById(entryId) }
    val entry by entryFlow.collectAsState(initial = null)
    val engagements by viewModel.engagements.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val currentEntry = entry
    val associatedEngagement = engagements.find { it.id == currentEntry?.engagementId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "NOTEBOOK ENTRY",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
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
                    if (currentEntry != null) {
                        IconButton(onClick = { viewModel.togglePin(currentEntry) }) {
                            Icon(
                                imageVector = if (currentEntry.isPinned) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Pin",
                                tint = if (currentEntry.isPinned) Color(0xFF00E676) else Color(0xFFA7B0AA)
                            )
                        }
                        IconButton(onClick = { viewModel.toggleFavorite(currentEntry) }) {
                            Icon(
                                imageVector = if (currentEntry.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (currentEntry.isFavorite) Color(0xFFFFD700) else Color(0xFFA7B0AA)
                            )
                        }
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF00E5FF))
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
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
        if (currentEntry == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF00E676))
            }
        } else {
            val createdStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(currentEntry.createdAt))
            val updatedStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(currentEntry.updatedAt))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // Title and badges
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141B18)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
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
                            StatusBadge(currentEntry.category, customColor = Color(0xFF00E676))
                            StatusBadge(currentEntry.entryType, customColor = Color(0xFF00E5FF))
                        }

                        Text(
                            text = currentEntry.title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Engagement affiliation
                        Surface(
                            color = Color(0xFF1A2622),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (associatedEngagement != null) {
                                        "Engagement: ${associatedEngagement.title} (${associatedEngagement.client})"
                                    } else {
                                        "Engagement: Unassigned (General Note)"
                                    },
                                    color = Color(0xFFA7B0AA),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Content Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141B18)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
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
                                "NOTE DETAILS",
                                color = Color(0xFFA7B0AA),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(currentEntry.description))
                                    Toast.makeText(context, "Note content copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy content",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        SelectionContainer {
                            Text(
                                text = currentEntry.description,
                                color = Color.White,
                                fontSize = 13.5.sp,
                                lineHeight = 20.sp,
                                fontFamily = if (currentEntry.entryType == "Command Reference") FontFamily.Monospace else FontFamily.Default
                            )
                        }
                    }
                }

                // Tags & Evidence
                if (currentEntry.tags.isNotBlank() || !currentEntry.evidenceReference.isNullOrBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141B18)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (currentEntry.tags.isNotBlank()) {
                                Text(
                                    "TAGS",
                                    color = Color(0xFFA7B0AA),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    currentEntry.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { tag ->
                                        Surface(
                                            color = Color(0xFF1B2934),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                tag,
                                                color = Color(0xFF00E5FF),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (!currentEntry.evidenceReference.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "ATTACHED EVIDENCE REFERENCE",
                                    color = Color(0xFFA7B0AA),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AttachFile,
                                        contentDescription = null,
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = currentEntry.evidenceReference,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Timestamps and metadata
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141B18)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "METADATA & TIMESTAMPS",
                            color = Color(0xFFA7B0AA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Created:", color = Color(0xFFA7B0AA), fontSize = 11.sp)
                            Text(createdStr, color = Color.White, fontSize = 11.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Last Updated:", color = Color(0xFFA7B0AA), fontSize = 11.sp)
                            Text(updatedStr, color = Color.White, fontSize = 11.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Entry UUID:", color = Color(0xFFA7B0AA), fontSize = 11.sp)
                            Text(currentEntry.id.take(16) + "...", color = Color(0xFFA7B0AA), fontSize = 11.sp)
                        }
                    }
                }

                // Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val exportText = """
                                === REDOPS ASSESSMENT NOTE ===
                                Title: ${currentEntry.title}
                                Category: ${currentEntry.category}
                                Type: ${currentEntry.entryType}
                                Engagement: ${associatedEngagement?.title ?: "Unassigned"}
                                Tags: ${currentEntry.tags}
                                Created: $createdStr
                                
                                CONTENT:
                                ${currentEntry.description}
                            """.trimIndent()
                            clipboardManager.setText(AnnotatedString(exportText))
                            Toast.makeText(context, "Full note summary copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Full", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { showEditDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E676),
                            contentColor = Color(0xFF0B0F0E)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit Note", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showEditDialog && currentEntry != null) {
        NotebookDialog(
            initialEntry = currentEntry,
            engagements = engagements,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                viewModel.updateEntry(updated) {
                    showEditDialog = false
                }
            }
        )
    }

    if (showDeleteDialog && currentEntry != null) {
        ConfirmDeleteDialog(
            title = "Delete Notebook Entry?",
            message = "Are you sure you want to permanently delete '${currentEntry.title}'? This action cannot be undone.",
            onConfirm = {
                viewModel.deleteEntry(currentEntry.id)
                showDeleteDialog = false
                onBack()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}
