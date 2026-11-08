package com.redops.mobile.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ABOUT REDOPS MOBILE", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
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
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141B18)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color(0xFF00E676).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.padding(14.dp).size(42.dp))
                        }
                        Text("RedOps Mobile", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Professional Edition v1.0.0", color = Color(0xFF00E676), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("Authorized Security Assessment Manager", color = Color(0xFFA7B0AA), fontSize = 11.sp)
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141B18)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("COURSE & PORTFOLIO SPECIFICATION", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Architecture: MVVM + Repository Pattern", color = Color.White, fontSize = 12.sp)
                        Text("Database: SQLite Room Database v2.6.1 with reactive StateFlow", color = Color.White, fontSize = 12.sp)
                        Text("Preferences: AndroidX DataStore Preferences", color = Color.White, fontSize = 12.sp)
                        Text("UI: 100% Jetpack Compose with Material 3 Design System", color = Color.White, fontSize = 12.sp)
                        Text("Security: Android Biometrics & Keystore AES-256-GCM", color = Color.White, fontSize = 12.sp)
                        Text("Reporting: Multi-page Vector PDF Document Generator", color = Color.White, fontSize = 12.sp)
                        Text("Target SDK: Android SDK Platform 34 (Android 14)", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

