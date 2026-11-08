package com.redops.mobile.presentation.findings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindingDialog(
    initialFinding: Finding? = null,
    engagements: List<Engagement>,
    preselectedEngagementId: String? = null,
    onDismiss: () -> Unit,
    onSave: (Finding) -> Unit
) {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    var selectedEngagementId by remember {
        mutableStateOf(
            initialFinding?.engagementId ?: preselectedEngagementId ?: engagements.firstOrNull()?.id ?: ""
        )
    }

    var title by remember { mutableStateOf(initialFinding?.title ?: "") }
    var customId by remember {
        mutableStateOf(
            initialFinding?.customId ?: "F-${(100..999).random()}"
        )
    }
    var category by remember { mutableStateOf(initialFinding?.category ?: "Input Validation") }
    var severity by remember { mutableStateOf(initialFinding?.severity ?: "Medium") }
    var cvssScoreStr by remember { mutableStateOf(initialFinding?.cvssScore?.toString() ?: "") }
    var cweReference by remember { mutableStateOf(initialFinding?.cweReference ?: "") }
    var affectedAsset by remember { mutableStateOf(initialFinding?.affectedAsset ?: "") }
    var description by remember { mutableStateOf(initialFinding?.description ?: "") }
    var technicalImpact by remember { mutableStateOf(initialFinding?.technicalImpact ?: "") }
    var pocReference by remember { mutableStateOf(initialFinding?.pocReference ?: "") }
    var evidenceReference by remember { mutableStateOf(initialFinding?.evidenceReference ?: "") }
    var remediation by remember { mutableStateOf(initialFinding?.remediation ?: "") }
    var status by remember { mutableStateOf(initialFinding?.status ?: "Open") }
    var discoveryDate by remember { mutableStateOf(initialFinding?.discoveryDate?.ifBlank { today } ?: today) }
    var generalNotes by remember { mutableStateOf(initialFinding?.generalNotes ?: "") }
    var errorMessage by remember { mutableStateOf("") }

    val categories = listOf(
        "Authentication",
        "Authorization",
        "Input Validation",
        "Injection",
        "Configuration",
        "Information Disclosure",
        "Cryptography",
        "Session Management",
        "Business Logic",
        "Other"
    )

    val severities = listOf("Critical", "High", "Medium", "Low", "Informational")
    val statuses = listOf("Open", "In Progress", "Resolved", "Accepted Risk", "False Positive")

    var engDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var severityDropdownExpanded by remember { mutableStateOf(false) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp).size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (initialFinding == null) "Log Vulnerability Finding" else "Edit Finding",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Technical Documentation & Risk Rating",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // If no engagements exist in DB, display warning blocking creation
                if (engagements.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF5252).copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("No Active Engagements", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                "Findings must be associated with an authorized engagement. Please create an engagement first.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                } else {
                    // Associated Engagement Dropdown
                    Column {
                        Text("Associated Engagement *", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        val currentEngagement = engagements.find { it.id == selectedEngagementId } ?: engagements.first()

                        ExposedDropdownMenuBox(
                            expanded = engDropdownExpanded,
                            onExpandedChange = { engDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = "${currentEngagement.title} (${currentEngagement.client})",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = engDropdownExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = engDropdownExpanded,
                                onDismissRequest = { engDropdownExpanded = false }
                            ) {
                                engagements.forEach { eng ->
                                    DropdownMenuItem(
                                        text = { Text("${eng.title} — ${eng.client}", fontSize = 12.sp) },
                                        onClick = {
                                            selectedEngagementId = eng.id
                                            engDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Custom ID and Finding Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customId,
                        onValueChange = { customId = it.trim() },
                        label = { Text("ID *", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.width(95.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            errorMessage = ""
                        },
                        label = { Text("Finding Title *", fontSize = 11.sp) },
                        placeholder = { Text("e.g. SQL Injection in Search Endpoint", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Category Dropdown
                Column {
                    Text("Vulnerability Category *", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, fontSize = 13.sp) },
                                    onClick = {
                                        category = cat
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Severity and Status Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Severity Dropdown
                    Column(Modifier.weight(1f)) {
                        Text("Severity *", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        ExposedDropdownMenuBox(
                            expanded = severityDropdownExpanded,
                            onExpandedChange = { severityDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = severity,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = severityDropdownExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = severityDropdownExpanded,
                                onDismissRequest = { severityDropdownExpanded = false }
                            ) {
                                severities.forEach { sev ->
                                    DropdownMenuItem(
                                        text = { Text(sev, fontSize = 12.sp) },
                                        onClick = {
                                            severity = sev
                                            severityDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Status Dropdown
                    Column(Modifier.weight(1f)) {
                        Text("Status *", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        ExposedDropdownMenuBox(
                            expanded = statusDropdownExpanded,
                            onExpandedChange = { statusDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = status,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusDropdownExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = statusDropdownExpanded,
                                onDismissRequest = { statusDropdownExpanded = false }
                            ) {
                                statuses.forEach { st ->
                                    DropdownMenuItem(
                                        text = { Text(st, fontSize = 12.sp) },
                                        onClick = {
                                            status = st
                                            statusDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // CVSS and CWE Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = cvssScoreStr,
                        onValueChange = {
                            cvssScoreStr = it.trim()
                            errorMessage = ""
                        },
                        label = { Text("CVSS (0.0-10.0)", fontSize = 11.sp) },
                        placeholder = { Text("e.g. 8.5", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = cweReference,
                        onValueChange = {
                            cweReference = it.trim()
                            errorMessage = ""
                        },
                        label = { Text("CWE ID", fontSize = 11.sp) },
                        placeholder = { Text("e.g. CWE-89", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Affected Asset
                OutlinedTextField(
                    value = affectedAsset,
                    onValueChange = {
                        affectedAsset = it
                        errorMessage = ""
                    },
                    label = { Text("Affected Asset / URL / Component *", fontSize = 12.sp) },
                    placeholder = { Text("e.g. https://api.target.com/v1/auth/search?q=", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Vulnerability Description
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        errorMessage = ""
                    },
                    label = { Text("Vulnerability Description *", fontSize = 12.sp) },
                    placeholder = { Text("Technical explanation of the flaw and underlying cause", fontSize = 11.sp) },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Technical Details / PoC Reference
                OutlinedTextField(
                    value = pocReference,
                    onValueChange = { pocReference = it },
                    label = { Text("Technical Details & Proof of Concept", fontSize = 12.sp) },
                    placeholder = { Text("Steps to reproduce, HTTP request headers, sample payload", fontSize = 11.sp) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Potential Impact
                OutlinedTextField(
                    value = technicalImpact,
                    onValueChange = { technicalImpact = it },
                    label = { Text("Potential Technical & Business Impact", fontSize = 12.sp) },
                    placeholder = { Text("Consequences if exploited (confidentiality, integrity, availability)", fontSize = 11.sp) },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Remediation Recommendation
                OutlinedTextField(
                    value = remediation,
                    onValueChange = {
                        remediation = it
                        errorMessage = ""
                    },
                    label = { Text("Recommended Remediation *", fontSize = 12.sp) },
                    placeholder = { Text("Actionable fix, parameterization, architectural mitigation", fontSize = 11.sp) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Evidence Reference & Discovery Date Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = evidenceReference,
                        onValueChange = { evidenceReference = it },
                        label = { Text("Evidence File Reference", fontSize = 11.sp) },
                        placeholder = { Text("evidence_sql_error.png", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = discoveryDate,
                        onValueChange = { discoveryDate = it },
                        label = { Text("Discovery Date", fontSize = 11.sp) },
                        placeholder = { Text("YYYY-MM-DD", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // General Notes
                OutlinedTextField(
                    value = generalNotes,
                    onValueChange = { generalNotes = it },
                    label = { Text("Additional Internal Notes", fontSize = 12.sp) },
                    placeholder = { Text("Retesting notes or assessment context", fontSize = 11.sp) },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Error Message Display
                if (errorMessage.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF5252).copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(errorMessage, color = Color(0xFFFF5252), fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedTitle = title.trim()
                    val trimmedAsset = affectedAsset.trim()
                    val trimmedDesc = description.trim()
                    val trimmedRem = remediation.trim()

                    // Validation checks
                    if (engagements.isEmpty() || selectedEngagementId.isBlank()) {
                        errorMessage = "A valid associated engagement is required."
                        return@Button
                    }
                    if (trimmedTitle.isBlank()) {
                        errorMessage = "Finding Title is required."
                        return@Button
                    }
                    if (trimmedAsset.isBlank()) {
                        errorMessage = "Affected Asset or URL is required."
                        return@Button
                    }
                    if (trimmedDesc.isBlank()) {
                        errorMessage = "Vulnerability Description is required."
                        return@Button
                    }
                    if (trimmedRem.isBlank()) {
                        errorMessage = "Recommended Remediation is required."
                        return@Button
                    }

                    // CVSS Score validation (0.0 to 10.0)
                    val parsedCvss: Double? = if (cvssScoreStr.isNotBlank()) {
                        val score = cvssScoreStr.toDoubleOrNull()
                        if (score == null || score < 0.0 || score > 10.0) {
                            errorMessage = "CVSS Score must be a valid number between 0.0 and 10.0."
                            return@Button
                        }
                        score
                    } else null

                    // Optional CWE format validation
                    val formattedCwe = if (cweReference.isNotBlank()) {
                        if (cweReference.startsWith("CWE-", ignoreCase = true)) {
                            cweReference.uppercase()
                        } else if (cweReference.toIntOrNull() != null) {
                            "CWE-$cweReference"
                        } else {
                            cweReference.trim()
                        }
                    } else null

                    val finding = (initialFinding ?: Finding(
                        id = initialFinding?.id ?: java.util.UUID.randomUUID().toString(),
                        customId = customId.ifBlank { "F-${(100..999).random()}" },
                        engagementId = selectedEngagementId,
                        title = trimmedTitle,
                        affectedAsset = trimmedAsset,
                        description = trimmedDesc,
                        remediation = trimmedRem,
                        createdAt = System.currentTimeMillis()
                    )).copy(
                        customId = customId.ifBlank { "F-${(100..999).random()}" },
                        engagementId = selectedEngagementId,
                        title = trimmedTitle,
                        category = category,
                        severity = severity,
                        cvssScore = parsedCvss,
                        affectedAsset = trimmedAsset,
                        cweReference = formattedCwe,
                        description = trimmedDesc,
                        technicalImpact = technicalImpact.trim(),
                        pocReference = pocReference.trim(),
                        evidenceReference = evidenceReference.trim().ifBlank { null },
                        remediation = trimmedRem,
                        status = status,
                        discoveryDate = discoveryDate.trim().ifBlank { today },
                        generalNotes = generalNotes.trim(),
                        updatedAt = System.currentTimeMillis()
                    )

                    onSave(finding)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (initialFinding == null) "Log Finding" else "Save Changes",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
