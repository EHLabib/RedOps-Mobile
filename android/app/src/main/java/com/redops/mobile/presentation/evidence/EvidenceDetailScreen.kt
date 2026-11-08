package com.redops.mobile.presentation.evidence

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redops.mobile.domain.model.EvidenceRecord
import com.redops.mobile.presentation.components.ConfirmDeleteDialog
import com.redops.mobile.presentation.components.StatusBadge
import com.redops.mobile.utils.EvidenceFileManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvidenceDetailScreen(
    evidenceId: String,
    viewModel: EvidenceViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val evidenceFlow = remember(evidenceId) { viewModel.getEvidenceById(evidenceId) }
    val evidence by evidenceFlow.collectAsState(initial = null)
    val engagements by viewModel.engagements.collectAsState()
    val findings by viewModel.findings.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val currentEvidence = evidence
    val associatedEng = engagements.find { it.id == currentEvidence?.engagementId }
    val associatedFnd = findings.find { it.id == currentEvidence?.findingId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "EVIDENCE DETAILS",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Secure artifact inspector & file integrity",
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
                    if (currentEvidence != null) {
                        IconButton(onClick = {
                            EvidenceFileManager.shareFile(
                                context = context,
                                localPath = currentEvidence.uriOrPath,
                                mimeType = currentEvidence.fileType,
                                title = currentEvidence.title
                            )
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF00E5FF))
                        }
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF00E676))
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1412))
            )
        },
        containerColor = Color(0xFF0D1412),
        modifier = modifier
    ) { innerPadding ->
        if (currentEvidence == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF00E676))
            }
        } else {
            val createdStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(currentEvidence.createdAt))
            val updatedStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(currentEvidence.updatedAt))
            val formattedSize = formatFileSize(currentEvidence.fileSize)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // Title & Category Header Card
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusBadge(currentEvidence.category, customColor = Color(0xFF00E676))
                            StatusBadge(currentEvidence.fileType.substringAfter("/").uppercase(), customColor = Color(0xFF00E5FF))
                        }

                        Text(
                            text = currentEvidence.title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (currentEvidence.description.isNotBlank()) {
                            Text(
                                text = currentEvidence.description,
                                color = Color(0xFFA7B0AA),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // File Preview Card
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
                                "LOCAL ARTIFACT PREVIEW",
                                color = Color(0xFFA7B0AA),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            if (currentEvidence.uriOrPath.isNotBlank()) {
                                TextButton(
                                    onClick = {
                                        EvidenceFileManager.openFileWithExternalApp(
                                            context,
                                            currentEvidence.uriOrPath,
                                            currentEvidence.fileType
                                        )
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Open File", color = Color(0xFF00E5FF), fontSize = 11.5.sp)
                                }
                            }
                        }

                        EvidencePreviewContent(evidence = currentEvidence)
                    }
                }

                // Integrity & SHA-256 Card
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "SHA-256 CHECKSUM INTEGRITY",
                                    color = Color(0xFF00E676),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                            if (currentEvidence.sha256Checksum.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(currentEvidence.sha256Checksum))
                                        Toast.makeText(context, "SHA-256 checksum copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy Hash",
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        Surface(
                            color = Color(0xFF0B0F0E),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SelectionContainer {
                                Text(
                                    text = if (currentEvidence.sha256Checksum.isNotBlank()) {
                                        currentEvidence.sha256Checksum
                                    } else {
                                        "No file checksum computed"
                                    },
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                // Scope & Finding Associations Card
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
                        Text(
                            "ASSESSMENT ASSOCIATIONS",
                            color = Color(0xFFA7B0AA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Engagement Scope:", color = Color(0xFFA7B0AA), fontSize = 11.5.sp)
                            Text(
                                text = associatedEng?.let { "${it.title} (${it.client})" } ?: "Unassigned / General",
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Associated Finding:", color = Color(0xFFA7B0AA), fontSize = 11.5.sp)
                            Text(
                                text = associatedFnd?.let { "[${it.customId}] ${it.title}" } ?: "None (General Evidence)",
                                color = if (associatedFnd != null) Color(0xFFFF9100) else Color(0xFFA7B0AA),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Metadata Details Card
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
                            "STORAGE METADATA",
                            color = Color(0xFFA7B0AA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Original Filename:", color = Color(0xFFA7B0AA), fontSize = 11.sp)
                            Text(currentEvidence.originalFilename.ifBlank { "N/A" }, color = Color.White, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Stored Filename:", color = Color(0xFFA7B0AA), fontSize = 11.sp)
                            Text(currentEvidence.storedFilename.ifBlank { "N/A" }, color = Color.White, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("MIME Type:", color = Color(0xFFA7B0AA), fontSize = 11.sp)
                            Text(currentEvidence.fileType, color = Color.White, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("File Size:", color = Color(0xFFA7B0AA), fontSize = 11.sp)
                            Text(formattedSize, color = Color.White, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Imported At:", color = Color(0xFFA7B0AA), fontSize = 11.sp)
                            Text(createdStr, color = Color.White, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Last Modified:", color = Color(0xFFA7B0AA), fontSize = 11.sp)
                            Text(updatedStr, color = Color.White, fontSize = 11.sp)
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
                            EvidenceFileManager.shareFile(
                                context = context,
                                localPath = currentEvidence.uriOrPath,
                                mimeType = currentEvidence.fileType,
                                title = currentEvidence.title
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share", fontSize = 12.sp)
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
                        Text("Edit", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showEditDialog && currentEvidence != null) {
        EditEvidenceDialog(
            evidence = currentEvidence,
            findings = findings.filter { it.engagementId == currentEvidence.engagementId },
            onDismiss = { showEditDialog = false },
            onSave = { newTitle, newDesc, newCat, newFndId ->
                viewModel.updateEvidenceMetadata(
                    evidence = currentEvidence,
                    newTitle = newTitle,
                    newDescription = newDesc,
                    newCategory = newCat,
                    newFindingId = newFndId
                ) {
                    showEditDialog = false
                }
            }
        )
    }

    if (showDeleteDialog && currentEvidence != null) {
        ConfirmDeleteDialog(
            title = "Delete Evidence Artifact?",
            message = "Are you sure you want to permanently delete '${currentEvidence.title}' and its stored local file? This action cannot be undone.",
            onConfirm = {
                viewModel.deleteEvidence(currentEvidence.id) {
                    showDeleteDialog = false
                    onBack()
                }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun EvidencePreviewContent(evidence: EvidenceRecord) {
    val context = LocalContext.current
    val filePath = evidence.uriOrPath
    val mimeType = evidence.fileType.lowercase()

    val file = remember(filePath) { File(filePath) }
    val fileExists = file.exists()

    if (!fileExists) {
        Surface(
            color = Color(0xFF261919),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Local file artifact not found on internal storage ($filePath).",
                    color = Color(0xFFFF5252),
                    fontSize = 11.5.sp
                )
            }
        }
        return
    }

    when {
        // Image formats (PNG, JPG, JPEG, WEBP, GIF)
        mimeType.startsWith("image/") -> {
            val bitmap = remember(filePath) {
                try {
                    BitmapFactory.decodeFile(filePath)
                } catch (e: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = evidence.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .background(Color.Black, RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF1B2934), RoundedCornerShape(8.dp))
                )
            } else {
                Text(
                    "Unable to decode image preview. File may be corrupted or unsupported.",
                    color = Color(0xFFFF5252),
                    fontSize = 12.sp
                )
            }
        }

        // PDF document
        mimeType == "application/pdf" -> {
            val pdfBitmap = remember(filePath) {
                EvidenceFileManager.renderPdfFirstPage(context, filePath)
            }
            if (pdfBitmap != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "PDF Document (Page 1 Render):",
                        color = Color(0xFFA7B0AA),
                        fontSize = 11.sp
                    )
                    Image(
                        bitmap = pdfBitmap.asImageBitmap(),
                        contentDescription = "PDF Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF1B2934), RoundedCornerShape(8.dp))
                    )
                }
            } else {
                GenericFilePreview(
                    icon = Icons.Default.PictureAsPdf,
                    title = "PDF Document",
                    subtitle = "Tap 'Open File' above to view with your PDF application."
                )
            }
        }

        // Text & Log formats
        mimeType.startsWith("text/") ||
                mimeType.contains("json") ||
                mimeType.contains("xml") ||
                mimeType.contains("log") -> {
            val textContent = remember(filePath) {
                EvidenceFileManager.readTextPreview(filePath)
            }
            Surface(
                color = Color(0xFF0B0F0E),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = textContent,
                        color = Color(0xFF00E676),
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        }

        // Unsupported or Binary format
        else -> {
            GenericFilePreview(
                icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                title = evidence.originalFilename.ifBlank { "Binary / Configuration Export" },
                subtitle = "Preview not available for ${evidence.fileType}. Tap 'Open File' to view in a compatible app."
            )
        }
    }
}

@Composable
private fun GenericFilePreview(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        color = Color(0xFF1B2934),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(subtitle, color = Color(0xFFA7B0AA), fontSize = 11.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditEvidenceDialog(
    evidence: EvidenceRecord,
    findings: List<com.redops.mobile.domain.model.Finding>,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, category: String, findingId: String?) -> Unit
) {
    var title by remember { mutableStateOf(evidence.title) }
    var description by remember { mutableStateOf(evidence.description) }
    var category by remember { mutableStateOf(evidence.category) }
    var selectedFindingId by remember { mutableStateOf(evidence.findingId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Evidence Metadata", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category Chips
                Text("Category", color = Color(0xFFA7B0AA), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    EVIDENCE_CATEGORIES.take(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 9.5.sp) }
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    EVIDENCE_CATEGORIES.drop(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 9.5.sp) }
                        )
                    }
                }

                // Associated Finding (Optional)
                if (findings.isNotEmpty()) {
                    Text("Associated Finding", color = Color(0xFFA7B0AA), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    var fndExpanded by remember { mutableStateOf(false) }
                    val currentFnd = findings.find { it.id == selectedFindingId }
                    ExposedDropdownMenuBox(expanded = fndExpanded, onExpandedChange = { fndExpanded = !fndExpanded }) {
                        OutlinedTextField(
                            value = currentFnd?.let { "[${it.customId}] ${it.title}" } ?: "None (General Evidence)",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fndExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = fndExpanded, onDismissRequest = { fndExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("None (General Evidence)", fontSize = 12.sp) },
                                onClick = {
                                    selectedFindingId = null
                                    fndExpanded = false
                                }
                            )
                            findings.forEach { fnd ->
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

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title.trim(), description.trim(), category, selectedFindingId)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color(0xFF0B0F0E))
            ) {
                Text("Update Metadata", fontWeight = FontWeight.Bold)
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

