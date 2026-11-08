package com.redops.mobile.presentation.engagements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngagementDialog(
    initialEngagement: Engagement? = null,
    onDismiss: () -> Unit,
    onSave: (Engagement) -> Unit
) {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    var title by remember { mutableStateOf(initialEngagement?.title ?: "") }
    var client by remember { mutableStateOf(initialEngagement?.client ?: "") }
    var description by remember { mutableStateOf(initialEngagement?.description ?: "") }
    var assessmentType by remember { mutableStateOf(initialEngagement?.assessmentType ?: "Web Application") }
    var startDate by remember { mutableStateOf(initialEngagement?.startDate ?: today) }
    var endDate by remember { mutableStateOf(initialEngagement?.endDate ?: today) }
    var status by remember { mutableStateOf(initialEngagement?.status ?: "Planning") }
    var authorizedScope by remember { mutableStateOf(initialEngagement?.authorizedScope ?: "") }
    var outOfScope by remember { mutableStateOf(initialEngagement?.outOfScope ?: "") }
    var objectives by remember { mutableStateOf(initialEngagement?.assessmentObjectives ?: "") }
    var notes by remember { mutableStateOf(initialEngagement?.additionalNotes ?: "") }
    var isAuthorized by remember { mutableStateOf(initialEngagement?.isAuthorized ?: false) }
    var errorMessage by remember { mutableStateOf("") }

    val assessmentTypes = listOf(
        "Web Application",
        "Network",
        "Mobile Application",
        "Internal",
        "External",
        "Other"
    )

    val statuses = listOf(
        "Planning",
        "Active",
        "On Hold",
        "Completed",
        "Cancelled"
    )

    var typeDropdownExpanded by remember { mutableStateOf(false) }
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
                        imageVector = if (initialEngagement == null) Icons.Default.AddBusiness else Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp).size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (initialEngagement == null) "New Assessment Engagement" else "Edit Engagement",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Authorized Scope & Rules of Engagement",
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
                // Assessment Type Dropdown
                Column {
                    Text("Assessment Type *", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = typeDropdownExpanded,
                        onExpandedChange = { typeDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = assessmentType,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = typeDropdownExpanded,
                            onDismissRequest = { typeDropdownExpanded = false }
                        ) {
                            assessmentTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type, fontSize = 13.sp) },
                                    onClick = {
                                        assessmentType = type
                                        typeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Status Dropdown
                Column {
                    Text("Engagement Status *", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = statusDropdownExpanded,
                            onDismissRequest = { statusDropdownExpanded = false }
                        ) {
                            statuses.forEach { st ->
                                DropdownMenuItem(
                                    text = { Text(st, fontSize = 13.sp) },
                                    onClick = {
                                        status = st
                                        statusDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Title and Client
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = ""
                    },
                    label = { Text("Engagement Title *", fontSize = 12.sp) },
                    placeholder = { Text("e.g. Q4 External Web Audit", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = client,
                    onValueChange = {
                        client = it
                        errorMessage = ""
                    },
                    label = { Text("Client / Organization *", fontSize = 12.sp) },
                    placeholder = { Text("e.g. Acme Financial Group", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Date Fields with validation support
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = {
                            startDate = it
                            errorMessage = ""
                        },
                        label = { Text("Start Date *", fontSize = 11.sp) },
                        placeholder = { Text("YYYY-MM-DD", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = {
                            endDate = it
                            errorMessage = ""
                        },
                        label = { Text("End Date *", fontSize = 11.sp) },
                        placeholder = { Text("YYYY-MM-DD", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Scope Specifications
                OutlinedTextField(
                    value = authorizedScope,
                    onValueChange = {
                        authorizedScope = it
                        errorMessage = ""
                    },
                    label = { Text("Authorized In-Scope Targets *", fontSize = 12.sp) },
                    placeholder = { Text("e.g. *.acme.com, 192.168.10.0/24 (CIDRs/URLs)", fontSize = 11.sp) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = outOfScope,
                    onValueChange = { outOfScope = it },
                    label = { Text("Explicit Out-of-Scope Assets", fontSize = 12.sp) },
                    placeholder = { Text("e.g. billing.acme.com, prod-payment-gateway", fontSize = 11.sp) },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = objectives,
                    onValueChange = { objectives = it },
                    label = { Text("Assessment Objectives", fontSize = 12.sp) },
                    placeholder = { Text("e.g. Identify OWASP Top 10 risks, verify RBAC", fontSize = 11.sp) },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Executive Description", fontSize = 12.sp) },
                    placeholder = { Text("Brief context and assessment approach", fontSize = 11.sp) },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Additional Operational Notes", fontSize = 12.sp) },
                    placeholder = { Text("Emergency contacts, communication channels", fontSize = 11.sp) },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Mandatory Authorization Declaration Box
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isAuthorized) MaterialTheme.colorScheme.primary else Color(0xFFFF9100).copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = isAuthorized,
                                onCheckedChange = {
                                    isAuthorized = it
                                    errorMessage = ""
                                },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "I declare and confirm that explicit, documented written authorization exists for the defined target scope.",
                                    fontSize = 11.5.sp,
                                    color = if (isAuthorized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Notice: This confirmation records the user's formal declaration of authorization. It does not constitute independent legal verification by the software.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }

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
                    val trimmedClient = client.trim()
                    val trimmedScope = authorizedScope.trim()
                    val trimmedStart = startDate.trim()
                    val trimmedEnd = endDate.trim()

                    when {
                        trimmedTitle.isBlank() -> {
                            errorMessage = "Engagement Title is required."
                        }
                        trimmedClient.isBlank() -> {
                            errorMessage = "Client or Organization Name is required."
                        }
                        trimmedScope.isBlank() -> {
                            errorMessage = "Authorized Scope is required."
                        }
                        trimmedStart.isBlank() || trimmedEnd.isBlank() -> {
                            errorMessage = "Start and End dates are required."
                        }
                        // Date comparison validation: Start date must not be after End date
                        trimmedStart > trimmedEnd -> {
                            errorMessage = "Invalid Timeline: Start date ($trimmedStart) cannot be after End date ($trimmedEnd)."
                        }
                        !isAuthorized -> {
                            errorMessage = "Authorization declaration must be explicitly confirmed before saving."
                        }
                        else -> {
                            val engagement = (initialEngagement ?: Engagement(
                                title = trimmedTitle,
                                client = trimmedClient,
                                authorizedScope = trimmedScope,
                                startDate = trimmedStart,
                                endDate = trimmedEnd
                            )).copy(
                                title = trimmedTitle,
                                client = trimmedClient,
                                description = description.trim(),
                                assessmentType = assessmentType,
                                startDate = trimmedStart,
                                endDate = trimmedEnd,
                                status = status,
                                authorizedScope = trimmedScope,
                                outOfScope = outOfScope.trim(),
                                assessmentObjectives = objectives.trim(),
                                additionalNotes = notes.trim(),
                                isAuthorized = true,
                                authorizationTimestamp = initialEngagement?.authorizationTimestamp ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(engagement)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (initialEngagement == null) "Create Engagement" else "Save Changes",
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
