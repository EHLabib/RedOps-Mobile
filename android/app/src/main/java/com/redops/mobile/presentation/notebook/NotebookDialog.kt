package com.redops.mobile.presentation.notebook

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redops.mobile.domain.model.Engagement
import com.redops.mobile.domain.model.NotebookEntry

val NOTEBOOK_CATEGORIES = listOf(
    "General Notes",
    "Reconnaissance",
    "Enumeration",
    "Vulnerability Analysis",
    "Exploitation Documentation",
    "Privilege Escalation Notes",
    "Post-Exploitation Documentation",
    "Remediation Notes",
    "Lessons Learned",
    "Other"
)

val NOTEBOOK_ENTRY_TYPES = listOf(
    "Text Note",
    "Technical Observation",
    "Command Reference",
    "Checklist",
    "Meeting Note",
    "Assessment Summary"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookDialog(
    initialEntry: NotebookEntry? = null,
    engagements: List<Engagement>,
    preselectedEngagementId: String? = null,
    onDismiss: () -> Unit,
    onSave: (NotebookEntry) -> Unit
) {
    var title by remember { mutableStateOf(initialEntry?.title ?: "") }
    var category by remember { mutableStateOf(initialEntry?.category ?: "General Notes") }
    var entryType by remember { mutableStateOf(initialEntry?.entryType ?: "Technical Observation") }
    var selectedEngagementId by remember { mutableStateOf(initialEntry?.engagementId ?: preselectedEngagementId) }
    var description by remember { mutableStateOf(initialEntry?.description ?: "") }
    var tags by remember { mutableStateOf(initialEntry?.tags ?: "") }
    var evidenceReference by remember { mutableStateOf(initialEntry?.evidenceReference ?: "") }
    var isPinned by remember { mutableStateOf(initialEntry?.isPinned ?: false) }
    var isFavorite by remember { mutableStateOf(initialEntry?.isFavorite ?: false) }
    var errorMessage by remember { mutableStateOf("") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }
    var engagementExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialEntry == null) "New Assessment Note" else "Edit Assessment Note",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category Dropdown
                Text("Category *", color = Color(0xFFA7B0AA), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        NOTEBOOK_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, fontSize = 12.sp) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Entry Type Dropdown
                Text("Entry Type *", color = Color(0xFFA7B0AA), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = entryType,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        NOTEBOOK_ENTRY_TYPES.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type, fontSize = 12.sp) },
                                onClick = {
                                    entryType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                // Associated Engagement (Optional)
                Text("Associated Engagement (Optional)", color = Color(0xFFA7B0AA), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                val currentEng = engagements.find { it.id == selectedEngagementId }
                ExposedDropdownMenuBox(
                    expanded = engagementExpanded,
                    onExpandedChange = { engagementExpanded = !engagementExpanded }
                ) {
                    OutlinedTextField(
                        value = currentEng?.let { "${it.title} (${it.client})" } ?: "None (General Note)",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = engagementExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = engagementExpanded,
                        onDismissRequest = { engagementExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (General Note)", fontSize = 12.sp) },
                            onClick = {
                                selectedEngagementId = null
                                engagementExpanded = false
                            }
                        )
                        engagements.forEach { eng ->
                            DropdownMenuItem(
                                text = { Text("${eng.title} — ${eng.client}", fontSize = 12.sp) },
                                onClick = {
                                    selectedEngagementId = eng.id
                                    engagementExpanded = false
                                }
                            )
                        }
                    }
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    placeholder = { Text("e.g. Subdomain Takeover Analysis on dev-portal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Description (Content)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Content / Technical Observation *") },
                    placeholder = { Text("Document observed behaviors, command syntaxes, checklist findings...") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                // Tags
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma-separated)") },
                    placeholder = { Text("e.g. DNS, CNAME, AWS, Takeover") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Evidence Reference
                OutlinedTextField(
                    value = evidenceReference,
                    onValueChange = { evidenceReference = it },
                    label = { Text("Evidence Reference / Artifact") },
                    placeholder = { Text("e.g. dig_trace.txt or screenshot_cname.png") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Pinned & Favorite toggles
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isPinned,
                            onCheckedChange = { isPinned = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00E676))
                        )
                        Icon(
                            imageVector = if (isPinned) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = if (isPinned) Color(0xFF00E676) else Color(0xFFA7B0AA),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Pin to Top", color = Color.White, fontSize = 12.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isFavorite,
                            onCheckedChange = { isFavorite = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFFD700))
                        )
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (isFavorite) Color(0xFFFFD700) else Color(0xFFA7B0AA),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Favorite", color = Color.White, fontSize = 12.sp)
                    }
                }

                if (errorMessage.isNotBlank()) {
                    Text(errorMessage, color = Color(0xFFFF5252), fontSize = 11.5.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank() || description.isBlank()) {
                        errorMessage = "Please enter both title and content."
                    } else {
                        val entry = (initialEntry ?: NotebookEntry(
                            title = title.trim(),
                            description = description.trim()
                        )).copy(
                            title = title.trim(),
                            category = category,
                            entryType = entryType,
                            engagementId = selectedEngagementId,
                            description = description.trim(),
                            tags = tags.trim(),
                            evidenceReference = evidenceReference.trim().ifBlank { null },
                            isPinned = isPinned,
                            isFavorite = isFavorite,
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(entry)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E676),
                    contentColor = Color(0xFF0B0F0E)
                )
            ) {
                Text("Save Entry", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFA7B0AA))
            }
        },
        containerColor = Color(0xFF141B18)
    )
}
