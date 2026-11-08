package com.redops.mobile.presentation.evidence

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Info
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvidenceDialog(
    engagements: List<Engagement>,
    findings: List<Finding>,
    preselectedEngagementId: String? = null,
    preselectedFindingId: String? = null,
    onDismiss: () -> Unit,
    onSave: (engagementId: String, findingId: String?, title: String, description: String, category: String, uri: Uri?) -> Unit
) {
    var selectedEngagementId by remember { mutableStateOf(preselectedEngagementId ?: engagements.firstOrNull()?.id ?: "") }
    var selectedFindingId by remember { mutableStateOf(preselectedFindingId) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Screenshot") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            selectedFileName = uri.lastPathSegment ?: "Selected File"
            if (title.isBlank()) {
                val cleanSegment = uri.lastPathSegment?.substringAfterLast("/") ?: ""
                if (cleanSegment.isNotBlank()) {
                    title = cleanSegment.substringBeforeLast(".")
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Attach Evidence Record",
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
                // Info banner explaining file storage & size limit
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF182C2A)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Evidence files are securely copied into app-private storage with SHA-256 integrity checks. File size limit: 50 MB.",
                            color = Color(0xFF00E676),
                            fontSize = 10.5.sp
                        )
                    }
                }

                // Associated Engagement Selector
                if (engagements.isNotEmpty()) {
                    Text(
                        "Associated Engagement *",
                        color = Color(0xFFA7B0AA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    var engExpanded by remember { mutableStateOf(false) }
                    val currentEng = engagements.find { it.id == selectedEngagementId } ?: engagements.first()
                    ExposedDropdownMenuBox(
                        expanded = engExpanded,
                        onExpandedChange = { engExpanded = !engExpanded }
                    ) {
                        OutlinedTextField(
                            value = "${currentEng.title} (${currentEng.client})",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = engExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = engExpanded,
                            onDismissRequest = { engExpanded = false }
                        ) {
                            engagements.forEach { eng ->
                                DropdownMenuItem(
                                    text = { Text("${eng.title} — ${eng.client}", fontSize = 12.sp) },
                                    onClick = {
                                        selectedEngagementId = eng.id
                                        engExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Associated Finding Selector (Optional)
                val relevantFindings = findings.filter { it.engagementId == selectedEngagementId }
                if (relevantFindings.isNotEmpty()) {
                    Text(
                        "Associated Finding (Optional)",
                        color = Color(0xFFA7B0AA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    var fndExpanded by remember { mutableStateOf(false) }
                    val currentFnd = relevantFindings.find { it.id == selectedFindingId }
                    ExposedDropdownMenuBox(
                        expanded = fndExpanded,
                        onExpandedChange = { fndExpanded = !fndExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentFnd?.let { "[${it.customId}] ${it.title}" } ?: "None (Engagement-level Evidence)",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fndExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = fndExpanded,
                            onDismissRequest = { fndExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None (Engagement-level Evidence)", fontSize = 12.sp) },
                                onClick = {
                                    selectedFindingId = null
                                    fndExpanded = false
                                }
                            )
                            relevantFindings.forEach { fnd ->
                                DropdownMenuItem(
                                    text = { Text("[${fnd.customId}] ${fnd.title}", fontSize = 12.sp) },
                                    onClick = {
                                        selectedFindingId = fnd.id
                                        fndExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Category Chips
                Text(
                    "Evidence Category *",
                    color = Color(0xFFA7B0AA),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    EVIDENCE_CATEGORIES.take(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 9.5.sp) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    EVIDENCE_CATEGORIES.drop(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 9.5.sp) }
                        )
                    }
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Evidence Title *") },
                    placeholder = { Text("e.g. Captured SQL Injection Response") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & Technical Context") },
                    placeholder = { Text("Details of observed behavior, payload, or configuration...") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // SAF File Selection
                Surface(
                    color = Color(0xFF1B2934),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Local File / Image Attachment (SAF)",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            selectedFileName ?: "No file chosen yet. You can attach images (PNG/JPG), logs (TXT), or documents (PDF).",
                            color = Color(0xFFA7B0AA),
                            fontSize = 11.sp
                        )
                        OutlinedButton(
                            onClick = { filePickerLauncher.launch("*/*") },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E676))
                        ) {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (selectedFileUri == null) "Select File via SAF" else "Change File",
                                fontSize = 12.sp
                            )
                        }
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
                    when {
                        title.isBlank() -> errorMessage = "Please enter an evidence title."
                        selectedEngagementId.isBlank() -> errorMessage = "Please select an associated engagement."
                        else -> {
                            onSave(
                                selectedEngagementId,
                                selectedFindingId,
                                title.trim(),
                                description.trim(),
                                category,
                                selectedFileUri
                            )
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E676),
                    contentColor = Color(0xFF0B0F0E)
                )
            ) {
                Text("Attach Evidence", fontWeight = FontWeight.Bold)
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
