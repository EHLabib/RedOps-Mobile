package com.redops.mobile.presentation.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.redops.mobile.security.SecurityManager
import kotlinx.coroutines.delay

@Composable
fun PinLockScreen(
    expectedPinHash: String,
    isBiometricEnabled: Boolean,
    securityManager: SecurityManager,
    onUnlock: () -> Unit,
    onResetData: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLockedOut by remember { mutableStateOf(securityManager.isLockedOut()) }
    var remainingSeconds by remember { mutableLongStateOf(securityManager.getRemainingLockoutSeconds()) }
    var showResetConfirm by remember { mutableStateOf(false) }

    // Lockout timer ticker
    LaunchedEffect(isLockedOut) {
        while (isLockedOut) {
            remainingSeconds = securityManager.getRemainingLockoutSeconds()
            if (remainingSeconds <= 0) {
                isLockedOut = false
                errorMessage = ""
                break
            }
            delay(1000)
        }
    }

    fun checkPin(pin: String) {
        if (securityManager.isLockedOut()) {
            isLockedOut = true
            remainingSeconds = securityManager.getRemainingLockoutSeconds()
            errorMessage = "Too many failed attempts. Try again in ${remainingSeconds}s."
            enteredPin = ""
            return
        }

        if (securityManager.verifyPin(pin, expectedPinHash)) {
            errorMessage = ""
            onUnlock()
        } else {
            enteredPin = ""
            if (securityManager.isLockedOut()) {
                isLockedOut = true
                remainingSeconds = securityManager.getRemainingLockoutSeconds()
                errorMessage = "Too many failed attempts. Device locked for ${remainingSeconds}s."
            } else {
                val attempts = securityManager.getFailedAttempts()
                errorMessage = "Incorrect PIN. Failed attempts: $attempts of 5."
            }
        }
    }

    LaunchedEffect(Unit) {
        if (isBiometricEnabled && securityManager.canAuthenticateBiometrics() && context is FragmentActivity && !isLockedOut) {
            securityManager.showBiometricPrompt(
                activity = context,
                onSuccess = onUnlock,
                onError = { /* Fall back to PIN input */ }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F0E))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                color = if (isLockedOut) Color(0xFFFF5252).copy(alpha = 0.15f) else Color(0xFF00E676).copy(alpha = 0.12f),
                shape = CircleShape
            ) {
                Icon(
                    if (isLockedOut) Icons.Default.Lock else Icons.Default.Shield,
                    contentDescription = null,
                    tint = if (isLockedOut) Color(0xFFFF5252) else Color(0xFF00E676),
                    modifier = Modifier.padding(18.dp).size(44.dp)
                )
            }

            Text("REDOPS MOBILE", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(
                if (isLockedOut) "Security Lockout Active" else "Enter Security PIN to unlock",
                color = if (isLockedOut) Color(0xFFFF5252) else Color(0xFFA7B0AA),
                fontSize = 13.sp,
                fontWeight = if (isLockedOut) FontWeight.Bold else FontWeight.Normal
            )

            // PIN Indicator Dots
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { index ->
                    val isFilled = index < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isLockedOut -> Color(0xFF3A1E1E)
                                    isFilled -> Color(0xFF00E676)
                                    else -> Color(0xFF1B2934)
                                }
                            )
                    )
                }
            }

            if (errorMessage.isNotBlank()) {
                Text(
                    errorMessage,
                    color = Color(0xFFFF5252),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(Modifier.height(6.dp))

            // Numeric Keypad
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                keys.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        row.forEach { key ->
                            KeypadButton(
                                text = key,
                                enabled = !isLockedOut,
                                isBiometricAvailable = isBiometricEnabled && securityManager.canAuthenticateBiometrics(),
                                onClick = {
                                    errorMessage = ""
                                    when (key) {
                                        "DEL" -> if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                        "BIO" -> {
                                            if (isBiometricEnabled && context is FragmentActivity && !isLockedOut) {
                                                securityManager.showBiometricPrompt(
                                                    activity = context,
                                                    onSuccess = onUnlock,
                                                    onError = { errorMessage = it }
                                                )
                                            }
                                        }
                                        else -> {
                                            if (enteredPin.length < 8) {
                                                val newPin = enteredPin + key
                                                enteredPin = newPin
                                                if (newPin.length == 4) {
                                                    checkPin(newPin)
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Emergency Reset Option
            if (onResetData != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { showResetConfirm = true }) {
                    Text("Forgot PIN? Emergency Reset", color = Color(0xFF6B7A72), fontSize = 11.sp)
                }
            }
        }
    }

    if (showResetConfirm && onResetData != null) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Emergency Data Reset", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "To prevent unauthorized data access, forgotten PINs cannot be recovered. Resetting will securely erase all local assessment documentation, findings, and evidence.\n\nAre you sure you want to proceed?",
                    color = Color(0xFFA7B0AA),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onResetData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = Color.White)
                ) {
                    Text("Erase & Reset App", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF141B18),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun KeypadButton(
    text: String,
    enabled: Boolean,
    isBiometricAvailable: Boolean,
    onClick: () -> Unit
) {
    val isBioKey = text == "BIO"
    val showBio = !isBioKey || isBiometricAvailable

    Surface(
        onClick = onClick,
        enabled = enabled && showBio,
        color = if (enabled && showBio) Color(0xFF141B18) else Color(0xFF0F1412),
        shape = CircleShape,
        modifier = Modifier.size(68.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (text) {
                "DEL" -> Icon(
                    Icons.Default.Backspace,
                    contentDescription = "Delete",
                    tint = if (enabled) Color.White else Color(0xFF3A4B42),
                    modifier = Modifier.size(20.dp)
                )
                "BIO" -> {
                    if (isBiometricAvailable) {
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = "Biometrics",
                            tint = if (enabled) Color(0xFF00E676) else Color(0xFF3A4B42),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                else -> Text(
                    text,
                    color = if (enabled) Color.White else Color(0xFF3A4B42),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
