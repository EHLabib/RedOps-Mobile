package com.redops.mobile.presentation.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redops.mobile.data.local.preferences.ThemeMode
import com.redops.mobile.presentation.components.ConfirmDeleteDialog
import com.redops.mobile.security.BiometricStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferences by viewModel.preferences.collectAsState()
    val message by viewModel.message.collectAsState()
    val pendingRestore by viewModel.pendingRestorePayload.collectAsState()

    var showSetPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var showClearDataConfirm by remember { mutableStateOf(false) }
    var showSecurityInfoDialog by remember { mutableStateOf(false) }

    var selectedImportUri by remember { mutableStateOf<Uri?>(null) }
    var pendingExportPassword by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportEncryptedBackup(context, it, pendingExportPassword) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImportUri = it
            showImportPasswordDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("SETTINGS & SECURITY", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showSecurityInfoDialog = true }) {
                        Icon(Icons.Default.Security, contentDescription = "Security Architecture", tint = Color(0xFF00E676))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B0F0E))
            )
        },
        containerColor = Color(0xFF0B0F0E)
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
        ) {
            // Theme Section
            item {
                Text("INTERFACE THEME", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141B18)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = preferences.themeMode == ThemeMode.DARK,
                                onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                                label = { Text("Cyber Dark", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00E676).copy(alpha = 0.2f),
                                    selectedLabelColor = Color(0xFF00E676)
                                )
                            )
                            FilterChip(
                                selected = preferences.themeMode == ThemeMode.LIGHT,
                                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                                label = { Text("Light Mode", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00E676).copy(alpha = 0.2f),
                                    selectedLabelColor = Color(0xFF00E676)
                                )
                            )
                            FilterChip(
                                selected = preferences.themeMode == ThemeMode.SYSTEM,
                                onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                                label = { Text("System Default", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00E676).copy(alpha = 0.2f),
                                    selectedLabelColor = Color(0xFF00E676)
                                )
                            )
                        }
                    }
                }
            }

            // Security & Access Control Section
            item {
                Text("SECURITY & ACCESS CONTROL", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141B18)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // PIN Lock
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Application PIN Lock", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                                Text(
                                    if (preferences.isPinEnabled) "PBKDF2-protected PIN is active (12,000 iterations)" else "Require a secure 4-8 digit PIN with attempt throttling",
                                    color = Color(0xFFA7B0AA),
                                    fontSize = 11.sp
                                )
                            }
                            if (preferences.isPinEnabled) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { showChangePinDialog = true },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF))
                                    ) {
                                        Text("Change", fontSize = 10.5.sp)
                                    }
                                    Button(
                                        onClick = { viewModel.removePin() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Remove", fontSize = 10.5.sp, color = Color.White)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { showSetPinDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color(0xFF0B0F0E)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Set PIN", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFF1B2934))

                        // Biometrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Biometric Authentication", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                                val bioDesc = when (viewModel.getBiometricStatus()) {
                                    BiometricStatus.AVAILABLE -> "Fingerprint or face authentication supported"
                                    BiometricStatus.NOT_ENROLLED -> "No biometrics enrolled in device settings"
                                    BiometricStatus.NO_HARDWARE -> "Hardware biometric sensor not available"
                                    BiometricStatus.UNAVAILABLE -> "Biometrics currently unavailable"
                                }
                                Text(bioDesc, color = Color(0xFFA7B0AA), fontSize = 11.sp)
                            }
                            Switch(
                                checked = preferences.isBiometricEnabled,
                                onCheckedChange = { checked ->
                                    viewModel.setBiometricEnabled(checked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF00E676),
                                    checkedTrackColor = Color(0xFF1B3D2B)
                                )
                            )
                        }

                        HorizontalDivider(color = Color(0xFF1B2934))

                        // Auto-Lock on Background
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Auto-Lock on Background", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                                Text("Lock application immediately when switching apps or locking screen", color = Color(0xFFA7B0AA), fontSize = 11.sp)
                            }
                            Switch(
                                checked = preferences.autoLockEnabled,
                                onCheckedChange = { viewModel.setAutoLockEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF00E676),
                                    checkedTrackColor = Color(0xFF1B3D2B)
                                )
                            )
                        }

                        HorizontalDivider(color = Color(0xFF1B2934))

                        // Inactivity Timeout Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Inactivity Timeout", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                            Text("Duration before requiring PIN/biometric unlock while backgrounded", color = Color(0xFFA7B0AA), fontSize = 11.sp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    0 to "Immediate",
                                    1 to "1 Min",
                                    5 to "5 Min",
                                    15 to "15 Min"
                                ).forEach { (minutes, label) ->
                                    val isSelected = preferences.inactivityTimeoutMinutes == minutes
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setInactivityTimeout(minutes) },
                                        label = { Text(label, fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF00E5FF).copy(alpha = 0.2f),
                                            selectedLabelColor = Color(0xFF00E5FF)
                                        )
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFF1B2934))

                        // Screenshot & Task Switcher Protection (FLAG_SECURE)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Screenshot & Preview Protection", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                                Text("Blocks screenshots and blurs app preview in Android task switcher (FLAG_SECURE)", color = Color(0xFFA7B0AA), fontSize = 11.sp)
                            }
                            Switch(
                                checked = preferences.screenshotProtectionEnabled,
                                onCheckedChange = { viewModel.setScreenshotProtection(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF00E676),
                                    checkedTrackColor = Color(0xFF1B3D2B)
                                )
                            )
                        }
                    }
                }
            }

            // Toolkit & Backend API Connectivity Section
            item {
                Text("TOOLKIT & BACKEND CONNECTIVITY", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141B18)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Configured endpoint for reconnaissance and web security tools. Default is http://10.0.2.2:5000/api/v1/ for Android Emulator, or enter LAN IP for physical device testing.",
                            color = Color(0xFFA7B0AA),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        var apiUrlInput by remember(preferences.apiBaseUrl) { mutableStateOf(preferences.apiBaseUrl) }
                        var tokenInput by remember(preferences.authToken) { mutableStateOf(preferences.authToken) }

                        OutlinedTextField(
                            value = apiUrlInput,
                            onValueChange = { apiUrlInput = it },
                            label = { Text("API Base URL") },
                            placeholder = { Text("http://10.0.2.2:5000/api/v1/") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            label = { Text("Operator Auth Token (JWT)") },
                            placeholder = { Text("Bearer token for authenticated endpoints") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    viewModel.setApiBaseUrl(apiUrlInput.trim())
                                    viewModel.setAuthToken(tokenInput.trim())
                                }
                            ) {
                                Text("Save Endpoint Settings")
                            }
                        }
                    }
                }
            }

            // Encrypted Backup & Data Management Section
            item {
                Text("ENCRYPTED BACKUP & DATA RETENTION", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141B18)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Export password-protected encrypted archives (.redops) using PBKDF2 + AES-256-GCM. Backups outside app storage require your user password to restore.",
                            color = Color(0xFFA7B0AA),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showExportPasswordDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E676)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Encrypted Backup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { importLauncher.launch("*/*") },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Restore Backup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = Color(0xFF1B2934))

                        // Load Sample Course Evaluation Data
                        Button(
                            onClick = { viewModel.loadSampleData() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF182C2A), contentColor = Color(0xFF00E676)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Dataset, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Load Sample Course Project Dataset", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }

                        // Clear All Data
                        Button(
                            onClick = { showClearDataConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C1818), contentColor = Color(0xFFFF5252)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Secure Erase All Data", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Information & Privacy Links
            item {
                Text("DOCUMENTATION & ABOUT", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141B18)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                        TextButton(
                            onClick = onNavigateToPrivacy,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("Privacy Policy & Security Architecture", fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFA7B0AA), modifier = Modifier.size(16.dp))
                            }
                        }

                        HorizontalDivider(color = Color(0xFF1B2934))

                        TextButton(
                            onClick = onNavigateToAbout,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("About RedOps Mobile v1.0.0", fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFA7B0AA), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // =====================================
    // Dialogs
    // =====================================

    // Set PIN Dialog
    if (showSetPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        var confirmPinInput by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSetPinDialog = false },
            title = { Text("Configure Security PIN", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a 4 to 8 digit PIN. Keys are derived using PBKDF2WithHmacSHA256 with 12,000 iterations.", color = Color(0xFFA7B0AA), fontSize = 12.sp)
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 8) pinInput = it },
                        label = { Text("New PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = { if (it.length <= 8) confirmPinInput = it },
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError.isNotBlank()) {
                        Text(pinError, color = Color(0xFFFF5252), fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            pinInput.length < 4 -> pinError = "PIN must be at least 4 digits."
                            pinInput != confirmPinInput -> pinError = "PINs do not match."
                            else -> {
                                viewModel.setPin(pinInput)
                                showSetPinDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color(0xFF0B0F0E))
                ) {
                    Text("Save PIN", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPinDialog = false }) {
                    Text("Cancel", color = Color(0xFFA7B0AA))
                }
            },
            containerColor = Color(0xFF141B18),
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        var currentPinInput by remember { mutableStateOf("") }
        var newPinInput by remember { mutableStateOf("") }
        var confirmNewPinInput by remember { mutableStateOf("") }
        var changeError by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Change Security PIN", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Verify current PIN before configuring your new PIN verifier.", color = Color(0xFFA7B0AA), fontSize = 12.sp)
                    OutlinedTextField(
                        value = currentPinInput,
                        onValueChange = { if (it.length <= 8) currentPinInput = it },
                        label = { Text("Current PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 8) newPinInput = it },
                        label = { Text("New PIN (4-8 digits)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmNewPinInput,
                        onValueChange = { if (it.length <= 8) confirmNewPinInput = it },
                        label = { Text("Confirm New PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (changeError.isNotBlank()) {
                        Text(changeError, color = Color(0xFFFF5252), fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            currentPinInput.isBlank() -> changeError = "Please enter your current PIN."
                            newPinInput.length < 4 -> changeError = "New PIN must be at least 4 digits."
                            newPinInput != confirmNewPinInput -> changeError = "New PIN confirmation does not match."
                            else -> {
                                val success = viewModel.changePin(currentPinInput, newPinInput)
                                if (success) {
                                    showChangePinDialog = false
                                } else {
                                    changeError = "Current PIN is incorrect."
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color(0xFF0B0F0E))
                ) {
                    Text("Update PIN", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel", color = Color(0xFFA7B0AA))
                }
            },
            containerColor = Color(0xFF141B18),
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Export Password Dialog
    if (showExportPasswordDialog) {
        var exportPassword by remember { mutableStateOf("") }
        var exportPasswordConfirm by remember { mutableStateOf("") }
        var exportError by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showExportPasswordDialog = false },
            title = { Text("Set Backup Archive Password", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Backup archives are protected using authenticated AES-256-GCM encryption with keys derived via PBKDF2 (15,000 iterations). You will need this password to restore.",
                        color = Color(0xFFA7B0AA),
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                    OutlinedTextField(
                        value = exportPassword,
                        onValueChange = { exportPassword = it },
                        label = { Text("Backup Password (min 6 chars)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = exportPasswordConfirm,
                        onValueChange = { exportPasswordConfirm = it },
                        label = { Text("Confirm Backup Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (exportError.isNotBlank()) {
                        Text(exportError, color = Color(0xFFFF5252), fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            exportPassword.length < 6 -> exportError = "Password must be at least 6 characters."
                            exportPassword != exportPasswordConfirm -> exportError = "Passwords do not match."
                            else -> {
                                pendingExportPassword = exportPassword
                                showExportPasswordDialog = false
                                exportLauncher.launch("RedOps_Backup_${System.currentTimeMillis()}.redops")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color(0xFF0B0F0E))
                ) {
                    Text("Proceed to Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportPasswordDialog = false }) {
                    Text("Cancel", color = Color(0xFFA7B0AA))
                }
            },
            containerColor = Color(0xFF141B18),
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Import Password Dialog
    if (showImportPasswordDialog) {
        var importPasswordInput by remember { mutableStateOf("") }
        var importError by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                showImportPasswordDialog = false
                selectedImportUri = null
            },
            title = { Text("Unlock Backup Archive", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter the password configured when this backup archive was exported.", color = Color(0xFFA7B0AA), fontSize = 12.sp)
                    OutlinedTextField(
                        value = importPasswordInput,
                        onValueChange = { importPasswordInput = it },
                        label = { Text("Backup Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (importError.isNotBlank()) {
                        Text(importError, color = Color(0xFFFF5252), fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importPasswordInput.isBlank()) {
                            importError = "Please enter password."
                        } else {
                            selectedImportUri?.let { uri ->
                                viewModel.inspectBackup(context, uri, importPasswordInput)
                                showImportPasswordDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color(0xFF0B0F0E))
                ) {
                    Text("Decrypt & Inspect", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportPasswordDialog = false
                    selectedImportUri = null
                }) {
                    Text("Cancel", color = Color(0xFFA7B0AA))
                }
            },
            containerColor = Color(0xFF141B18),
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Pending Restore Confirmation Dialog
    pendingRestore?.let { payload ->
        AlertDialog(
            onDismissRequest = { viewModel.clearPendingRestore() },
            title = { Text("Confirm Backup Restore", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Verified backup archive containing:\n• ${payload.engagements.size} Engagements\n• ${payload.findings.size} Vulnerability Findings\n• ${payload.notes.size} Notebook Entries\n• ${payload.evidence.size} Evidence Records",
                        color = Color.White,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Select whether to replace all current database records or merge the backup with existing records.",
                        color = Color(0xFFA7B0AA),
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { viewModel.confirmRestore(RestoreStrategy.MERGE_RECORDS) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color(0xFF0B0F0E))
                    ) {
                        Text("Merge Records", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Button(
                        onClick = { viewModel.confirmRestore(RestoreStrategy.REPLACE_ALL) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = Color.White)
                    ) {
                        Text("Replace All", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearPendingRestore() }) {
                    Text("Cancel", color = Color(0xFFA7B0AA))
                }
            },
            containerColor = Color(0xFF141B18),
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Security Architecture & Limitations Dialog
    if (showSecurityInfoDialog) {
        AlertDialog(
            onDismissRequest = { showSecurityInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Security Architecture & Status", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• PIN Key Derivation: PBKDF2WithHmacSHA256 with 12,000 iterations and 16-byte random salt. Constant-time byte comparison.", color = Color(0xFFD0D8D4), fontSize = 11.sp)
                    Text("• Attempt Throttling: 5-attempt threshold triggers a 30s-120s progressive lockout.", color = Color(0xFFD0D8D4), fontSize = 11.sp)
                    Text("• Keystore: Hardware-backed Android Keystore with AES-256-GCM master key.", color = Color(0xFFD0D8D4), fontSize = 11.sp)
                    Text("• Backup Archives: Encrypted with PBKDF2 (15,000 iter) + AES-256-GCM. Unencrypted backups are rejected.", color = Color(0xFFD0D8D4), fontSize = 11.sp)
                    Text("• Database Encryption Status: Room SQLite database resides in Linux UID-isolated app-private storage with Android File-Based Encryption (FBE). Full SQLCipher page-level encryption is not enabled to maintain architecture stability across emulator and physical CPU targets.", color = Color(0xFFFFB300), fontSize = 11.sp)
                    Text("• Evidence Shredding: Evidence files are overwritten with zeroes before deletion.", color = Color(0xFFD0D8D4), fontSize = 11.sp)
                    Text("• Platform Safeguards: allowBackup=false, usesCleartextTraffic=false, FLAG_SECURE window protection.", color = Color(0xFFD0D8D4), fontSize = 11.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showSecurityInfoDialog = false }) {
                    Text("Close", color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF141B18),
            shape = RoundedCornerShape(12.dp)
        )
    }

    if (showClearDataConfirm) {
        ConfirmDeleteDialog(
            title = "Secure Erase All Data?",
            message = "This will irreversibly delete all local engagements, findings, notebook entries, report history, and evidence records from your device.",
            onConfirm = {
                viewModel.clearAllData()
                showClearDataConfirm = false
            },
            onDismiss = { showClearDataConfirm = false }
        )
    }

    message?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearMessage() },
            title = { Text("Security Notice", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text(msg, color = Color(0xFFA7B0AA), fontSize = 12.sp) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearMessage() }) {
                    Text("OK", color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF141B18),
            shape = RoundedCornerShape(12.dp)
        )
    }
}
