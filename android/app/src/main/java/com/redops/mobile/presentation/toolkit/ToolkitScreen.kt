package com.redops.mobile.presentation.toolkit

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.redops.mobile.data.remote.model.*
import com.redops.mobile.domain.model.Engagement
import com.redops.mobile.domain.model.ToolObservation
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolkitScreen(
    viewModel: ToolkitViewModel,
    onNavigateToEngagements: () -> Unit,
    onPromoteToFinding: (ToolObservation) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val gson = remember { GsonBuilder().setPrettyPrinting().create() }

    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedToolId by viewModel.selectedToolId.collectAsState()
    val engagements by viewModel.engagements.collectAsState()
    val observations by viewModel.observations.collectAsState()

    // Dialog state for saving observation
    var showSaveObservationDialog by remember { mutableStateOf(false) }
    var observationToolName by remember { mutableStateOf("") }
    var observationToolCategory by remember { mutableStateOf("") }
    var observationTarget by remember { mutableStateOf("") }
    var observationTitle by remember { mutableStateOf("") }
    var observationSummary by remember { mutableStateOf("") }
    var observationRawJson by remember { mutableStateOf("") }

    // Listen for feedback messages
    LaunchedEffect(Unit) {
        viewModel.feedbackMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    fun promptSave(toolName: String, category: String, target: String, title: String, summary: String, rawJson: String) {
        observationToolName = toolName
        observationToolCategory = category
        observationTarget = target
        observationTitle = title
        observationSummary = summary
        observationRawJson = rawJson
        showSaveObservationDialog = true
    }

    fun copyToClipboard(text: String, label: String = "RedOps Toolkit") {
        val clip = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP APP/CATEGORY NAV HUB (Matching Images 1 & 4)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ToolkitCategory.values()) { category ->
                val isSelected = selectedCategory == category
                Surface(
                    onClick = { viewModel.selectCategory(category) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = category.iconEmoji, fontSize = 14.sp)
                        Text(
                            text = category.title,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // SUB-NAVIGATION BAR (Horizontally slideable, matching Images 2 & 3)
        when (selectedCategory) {
            ToolkitCategory.RECON -> {
                ReconSubNavBar(
                    selectedTool = selectedToolId,
                    onSelect = { viewModel.selectTool(it) }
                )
            }
            ToolkitCategory.WEBSEC -> {
                WebSecSubNavBar(
                    selectedTool = selectedToolId,
                    onSelect = { viewModel.selectTool(it) }
                )
            }
            ToolkitCategory.UTILITIES -> {
                UtilitiesSubNavBar(
                    selectedTool = selectedToolId,
                    onSelect = { viewModel.selectTool(it) }
                )
            }
            ToolkitCategory.WORKSPACE -> {
                WorkspaceSubNavBar(
                    selectedTool = selectedToolId,
                    onSelect = { viewModel.selectTool(it) }
                )
            }
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        // TOOL BODY / ACTIVE VIEWPORT
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedCategory) {
                ToolkitCategory.RECON -> {
                    when (selectedToolId) {
                        "dns" -> DnsToolScreen(viewModel, onCopy = ::copyToClipboard, onSave = ::promptSave)
                        "rdns" -> ReverseDnsToolScreen(viewModel, onCopy = ::copyToClipboard, onSave = ::promptSave)
                        "rdap" -> RdapToolScreen(viewModel, onCopy = ::copyToClipboard, onSave = ::promptSave)
                        "ip" -> IpInfoToolScreen(viewModel, onCopy = ::copyToClipboard, onSave = ::promptSave)
                        "subnet" -> SubnetToolScreen(viewModel, onCopy = ::copyToClipboard, onSave = ::promptSave)
                        else -> DnsToolScreen(viewModel, onCopy = ::copyToClipboard, onSave = ::promptSave)
                    }
                }
                ToolkitCategory.WEBSEC -> {
                    when (selectedToolId) {
                        "headers" -> HeadersToolScreen(viewModel, onCopy = ::copyToClipboard, onSave = ::promptSave)
                        "ssl" -> SslToolScreen(viewModel, onCopy = ::copyToClipboard, onSave = ::promptSave)
                        "redirects" -> RedirectsToolScreen(viewModel, onCopy = ::copyToClipboard, onSave = ::promptSave)
                        "url" -> UrlAnalyzerToolScreen(viewModel, onCopy = ::copyToClipboard, onSave = ::promptSave)
                        else -> HeadersToolScreen(viewModel, onCopy = ::copyToClipboard, onSave = ::promptSave)
                    }
                }
                ToolkitCategory.UTILITIES -> {
                    when (selectedToolId) {
                        "hash" -> HashGenToolScreen(viewModel, onCopy = ::copyToClipboard, onSave = ::promptSave)
                        "identify" -> HashIdentifyToolScreen(viewModel, onCopy = ::copyToClipboard, onSave = ::promptSave)
                        "jwt" -> JwtDecoderToolScreen(viewModel, onCopy = ::copyToClipboard, onSave = ::promptSave)
                        "cvss" -> CvssCalculatorToolScreen(viewModel, onCopy = ::copyToClipboard, onSave = ::promptSave)
                        "encoder" -> EncoderToolScreen(viewModel, onCopy = ::copyToClipboard, onSave = ::promptSave)
                        "password" -> PasswordStrengthToolScreen(viewModel, onCopy = ::copyToClipboard, onSave = ::promptSave)
                        "json" -> JsonFormatterToolScreen(viewModel, onCopy = ::copyToClipboard, onSave = ::promptSave)
                        else -> HashGenToolScreen(viewModel, onCopy = ::copyToClipboard, onSave = ::promptSave)
                    }
                }
                ToolkitCategory.WORKSPACE -> {
                    WorkspaceObservationsScreen(
                        observations = observations,
                        engagements = engagements,
                        onDeleteObservation = { viewModel.deleteObservation(it.id) },
                        onUpdateStatus = { obs, status -> viewModel.updateObservationStatus(obs, status) },
                        onPromoteToFinding = onPromoteToFinding,
                        onNavigateToEngagements = onNavigateToEngagements
                    )
                }
            }
        }
    }

    // DIALOG: SAVE TOOL RESULT AS ASSESSMENT OBSERVATION
    if (showSaveObservationDialog) {
        SaveObservationDialog(
            engagements = engagements,
            toolName = observationToolName,
            target = observationTarget,
            initialTitle = observationTitle,
            initialSummary = observationSummary,
            rawJson = observationRawJson,
            onDismiss = { showSaveObservationDialog = false },
            onConfirm = { engagementId, title, summary ->
                viewModel.saveObservation(
                    engagementId = engagementId,
                    toolName = observationToolName,
                    toolCategory = observationToolCategory,
                    target = observationTarget,
                    title = title,
                    summary = summary,
                    rawJson = observationRawJson
                )
                showSaveObservationDialog = false
            }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// SUB-NAV BARS (Horizontally scrollable with left-to-right gesture)
// -------------------------------------------------------------------------------------------------

@Composable
fun ReconSubNavBar(selectedTool: String, onSelect: (String) -> Unit) {
    val items = listOf(
        "dns" to "🌐 DNS Lookup",
        "rdns" to "🔄 Reverse DNS",
        "rdap" to "📜 RDAP / WHOIS",
        "ip" to "📍 IP Info",
        "subnet" to "🔢 Subnet Calc"
    )
    HorizontalSubNav(items = items, selectedId = selectedTool, onSelect = onSelect)
}

@Composable
fun WebSecSubNavBar(selectedTool: String, onSelect: (String) -> Unit) {
    val items = listOf(
        "headers" to "🛡️ Security Headers",
        "ssl" to "🔒 TLS/SSL Cert",
        "redirects" to "🔀 Redirect Trace",
        "url" to "🔗 URL Analyzer"
    )
    HorizontalSubNav(items = items, selectedId = selectedTool, onSelect = onSelect)
}

@Composable
fun UtilitiesSubNavBar(selectedTool: String, onSelect: (String) -> Unit) {
    val items = listOf(
        "hash" to "🔐 Hash Gen",
        "jwt" to "🎫 JWT Decoder",
        "identify" to "🏷️ Hash Identify",
        "cvss" to "📊 CVSS v3.1",
        "encoder" to "🔄 Multi-Encoder",
        "password" to "💪 Password Strength",
        "json" to "📄 JSON Formatter"
    )
    HorizontalSubNav(items = items, selectedId = selectedTool, onSelect = onSelect)
}

@Composable
fun WorkspaceSubNavBar(selectedTool: String, onSelect: (String) -> Unit) {
    val items = listOf(
        "observations" to "📋 Assessment Observations",
        "hub" to "🧭 Category Overview"
    )
    HorizontalSubNav(items = items, selectedId = selectedTool, onSelect = onSelect)
}

@Composable
fun HorizontalSubNav(
    items: List<Pair<String, String>>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(items) { (id, label) ->
            val isSelected = selectedId == id
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(id) },
                label = { Text(text = label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// COMMON TOOL CONTAINER COMPONENT
// -------------------------------------------------------------------------------------------------

@Composable
fun ToolScaffold(
    iconEmoji: String,
    title: String,
    description: String,
    authRequired: Boolean = true,
    authConfirmed: Boolean = true,
    onAuthCheckedChange: (Boolean) -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Tool Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = iconEmoji, fontSize = 22.sp)
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981))
                    ) {
                        Text(
                            text = "READY",
                            color = Color(0xFF10B981),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Authorization confirmation banner for target scanning
        if (authRequired) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = authConfirmed,
                        onCheckedChange = onAuthCheckedChange
                    )
                    Text(
                        text = "Authorized Audit: I confirm authorization to assess this target.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Tool Body
        content()
    }
}

// -------------------------------------------------------------------------------------------------
// TOOL SCREENS IMPLEMENTATIONS
// -------------------------------------------------------------------------------------------------

// 1. DNS TOOL
@Composable
fun DnsToolScreen(
    viewModel: ToolkitViewModel,
    onCopy: (String) -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var domain by remember { mutableStateOf("google.com") }
    var recordType by remember { mutableStateOf("ALL") }
    val authConfirmed by viewModel.authCheckDns.collectAsState()
    val state by viewModel.dnsState.collectAsState()

    ToolScaffold(
        iconEmoji = "🌐",
        title = "DNS Records Resolver",
        description = "Queries authoritative name servers for A, AAAA, MX, TXT, NS, CNAME, and SOA resource records.",
        authRequired = true,
        authConfirmed = authConfirmed,
        onAuthCheckedChange = { viewModel.authCheckDns.value = it }
    ) {
        OutlinedTextField(
            value = domain,
            onValueChange = { domain = it },
            label = { Text("Target Domain") },
            placeholder = { Text("example.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.runDnsLookup(domain, recordType) },
                modifier = Modifier.weight(1f),
                enabled = state !is ToolExecutionState.Loading
            ) {
                if (state is ToolExecutionState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Resolve Records")
            }
        }

        RenderResultBox(
            state = state,
            onCopy = { json -> onCopy(json) },
            onSave = { json ->
                onSave("DNS Lookup", "Reconnaissance", domain, "DNS Records for $domain", "Resolved authoritative DNS records.", json)
            }
        )
    }
}

// 2. REVERSE DNS
@Composable
fun ReverseDnsToolScreen(
    viewModel: ToolkitViewModel,
    onCopy: (String) -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var ip by remember { mutableStateOf("8.8.8.8") }
    val authConfirmed by viewModel.authCheckReverse.collectAsState()
    val state by viewModel.reverseDnsState.collectAsState()

    ToolScaffold(
        iconEmoji = "🔄",
        title = "Reverse DNS (PTR) Lookup",
        description = "Performs in-addr.arpa and ip6.arpa PTR pointer resolution to identify hostnames linked to IP addresses.",
        authRequired = true,
        authConfirmed = authConfirmed,
        onAuthCheckedChange = { viewModel.authCheckReverse.value = it }
    ) {
        OutlinedTextField(
            value = ip,
            onValueChange = { ip = it },
            label = { Text("IP Address") },
            placeholder = { Text("8.8.8.8") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.runReverseDns(ip) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is ToolExecutionState.Loading
        ) {
            if (state is ToolExecutionState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Lookup PTR Pointer")
        }

        RenderResultBox(
            state = state,
            onCopy = onCopy,
            onSave = { json ->
                onSave("Reverse DNS", "Reconnaissance", ip, "PTR Records for $ip", "Reverse DNS resolution findings.", json)
            }
        )
    }
}

// 3. RDAP WHOIS
@Composable
fun RdapToolScreen(
    viewModel: ToolkitViewModel,
    onCopy: (String) -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var domain by remember { mutableStateOf("google.com") }
    val authConfirmed by viewModel.authCheckRdap.collectAsState()
    val state by viewModel.rdapState.collectAsState()

    ToolScaffold(
        iconEmoji = "📜",
        title = "RDAP / WHOIS Registry Lookup",
        description = "Retrieves authoritative domain registration entities, registrar details, status codes, and name servers via ICANN RDAP.",
        authRequired = true,
        authConfirmed = authConfirmed,
        onAuthCheckedChange = { viewModel.authCheckRdap.value = it }
    ) {
        OutlinedTextField(
            value = domain,
            onValueChange = { domain = it },
            label = { Text("Domain Name") },
            placeholder = { Text("example.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.runRdapLookup(domain) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is ToolExecutionState.Loading
        ) {
            if (state is ToolExecutionState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Query RDAP Registry")
        }

        RenderResultBox(
            state = state,
            onCopy = onCopy,
            onSave = { json ->
                onSave("RDAP WHOIS", "Reconnaissance", domain, "RDAP Registration for $domain", "Domain registry ownership and dates.", json)
            }
        )
    }
}

// 4. IP INFO
@Composable
fun IpInfoToolScreen(
    viewModel: ToolkitViewModel,
    onCopy: (String) -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var ip by remember { mutableStateOf("1.1.1.1") }
    val authConfirmed by viewModel.authCheckIp.collectAsState()
    val state by viewModel.ipInfoState.collectAsState()

    ToolScaffold(
        iconEmoji = "📍",
        title = "IP Geolocation & ASN Lookup",
        description = "Gathers autonomous system (ASN), network owner, country, city, and ISP attribution for target IP addresses.",
        authRequired = true,
        authConfirmed = authConfirmed,
        onAuthCheckedChange = { viewModel.authCheckIp.value = it }
    ) {
        OutlinedTextField(
            value = ip,
            onValueChange = { ip = it },
            label = { Text("Target IP Address") },
            placeholder = { Text("1.1.1.1") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.runIpInfo(ip) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is ToolExecutionState.Loading
        ) {
            if (state is ToolExecutionState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Lookup IP Details")
        }

        RenderResultBox(
            state = state,
            onCopy = onCopy,
            onSave = { json ->
                onSave("IP Info", "Reconnaissance", ip, "IP Details for $ip", "ASN, organization, and geolocation attribution.", json)
            }
        )
    }
}

// 5. SUBNET CALCULATOR
@Composable
fun SubnetToolScreen(
    viewModel: ToolkitViewModel,
    onCopy: (String) -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var cidr by remember { mutableStateOf("192.168.1.0/24") }
    val state by viewModel.subnetState.collectAsState()
    val gson = remember { GsonBuilder().setPrettyPrinting().create() }

    ToolScaffold(
        iconEmoji = "🔢",
        title = "IPv4 Subnet Calculator",
        description = "Calculates network address, broadcast address, netmask, wildcard, usable IP range, and host count for CIDR blocks.",
        authRequired = false
    ) {
        OutlinedTextField(
            value = cidr,
            onValueChange = { cidr = it },
            label = { Text("CIDR Notation") },
            placeholder = { Text("10.0.0.0/16 or 192.168.1.0/24") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.runSubnetCalc(cidr) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is ToolExecutionState.Loading
        ) {
            if (state is ToolExecutionState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Calculate Subnet")
        }

        when (val s = state) {
            is ToolExecutionState.Success -> {
                val sub = s.data
                ResultCardWrapper(
                    title = "Subnet Calculation (${sub.cidr})",
                    onCopy = { onCopy(gson.toJson(sub)) },
                    onSave = {
                        onSave("Subnet Calc", "Reconnaissance", sub.cidr, "Subnet Allocation for ${sub.cidr}", "Network allocation and host range.", gson.toJson(sub))
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        KeyValueRow("Network Address", sub.networkAddress)
                        KeyValueRow("Broadcast Address", sub.broadcastAddress)
                        KeyValueRow("Subnet Mask", sub.netmask)
                        KeyValueRow("Wildcard Mask", sub.wildcard)
                        KeyValueRow("Usable Range", "${sub.firstHost} - ${sub.lastHost}")
                        KeyValueRow("Total / Usable Hosts", "${sub.totalHosts} / ${sub.usableHosts}")
                        KeyValueRow("IP Class", sub.ipClass)
                        KeyValueRow("Scope", if (sub.isPrivate) "RFC 1918 Private" else "Public Internet")
                    }
                }
            }
            is ToolExecutionState.Error -> ErrorCard(s.message)
            else -> {}
        }
    }
}

// 6. HTTP HEADERS
@Composable
fun HeadersToolScreen(
    viewModel: ToolkitViewModel,
    onCopy: (String) -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var url by remember { mutableStateOf("https://example.com") }
    var followRedirects by remember { mutableStateOf(true) }
    val authConfirmed by viewModel.authCheckHeaders.collectAsState()
    val state by viewModel.headersState.collectAsState()
    val gson = remember { GsonBuilder().setPrettyPrinting().create() }

    ToolScaffold(
        iconEmoji = "🛡️",
        title = "HTTP Security Header Analyzer",
        description = "Evaluates defensive HTTP response headers: CSP, HSTS, X-Frame-Options, X-Content-Type-Options, Referrer-Policy, and Permissions-Policy.",
        authRequired = true,
        authConfirmed = authConfirmed,
        onAuthCheckedChange = { viewModel.authCheckHeaders.value = it }
    ) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Target URL") },
            placeholder = { Text("https://example.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = followRedirects, onCheckedChange = { followRedirects = it })
            Text("Follow HTTP Redirects", fontSize = 12.sp)
        }

        Button(
            onClick = { viewModel.runAnalyzeHeaders(url, followRedirects) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is ToolExecutionState.Loading
        ) {
            if (state is ToolExecutionState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Analyze Security Headers")
        }

        when (val s = state) {
            is ToolExecutionState.Success -> {
                val data = s.data
                ResultCardWrapper(
                    title = "Header Audit Results (${data.statusCode})",
                    onCopy = { onCopy(gson.toJson(data)) },
                    onSave = {
                        onSave("Security Headers", "Web Security", data.url, "Header Audit for ${data.url}", "Defensive HTTP headers analysis and grade ${data.grade ?: ""}.", gson.toJson(data))
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Overall Security Grade:", fontWeight = FontWeight.Bold)
                            Surface(
                                color = if (data.grade?.startsWith("A") == true) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, if (data.grade?.startsWith("A") == true) Color(0xFF10B981) else Color(0xFFEF4444))
                            ) {
                                Text(
                                    text = data.grade ?: "N/A",
                                    color = if (data.grade?.startsWith("A") == true) Color(0xFF10B981) else Color(0xFFEF4444),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        data.securityHeaders?.forEach { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(item.header, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        Text(
                                            item.status,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = when (item.status) {
                                                "PASS" -> Color(0xFF10B981)
                                                "WARN" -> Color(0xFFF59E0B)
                                                else -> Color(0xFFEF4444)
                                            }
                                        )
                                    }
                                    if (!item.value.isNullOrBlank()) {
                                        Text(
                                            item.value,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (!item.recommendation.isNullOrBlank()) {
                                        Text(
                                            "Rec: ${item.recommendation}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is ToolExecutionState.Error -> ErrorCard(s.message)
            else -> {}
        }
    }
}

// 7. TLS CERTIFICATE
@Composable
fun SslToolScreen(
    viewModel: ToolkitViewModel,
    onCopy: (String) -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var host by remember { mutableStateOf("google.com") }
    var port by remember { mutableStateOf("443") }
    val authConfirmed by viewModel.authCheckSsl.collectAsState()
    val state by viewModel.sslState.collectAsState()
    val gson = remember { GsonBuilder().setPrettyPrinting().create() }

    ToolScaffold(
        iconEmoji = "🔒",
        title = "TLS Certificate Inspector",
        description = "Examines X.509 certificate validity, subject alternative names (SANs), cipher suites, issuer chain, and expiration dates.",
        authRequired = true,
        authConfirmed = authConfirmed,
        onAuthCheckedChange = { viewModel.authCheckSsl.value = it }
    ) {
        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text("Hostname") },
            placeholder = { Text("example.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = port,
            onValueChange = { port = it },
            label = { Text("Port") },
            placeholder = { Text("443") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.runInspectTls(host, port) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is ToolExecutionState.Loading
        ) {
            if (state is ToolExecutionState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Inspect TLS Certificate")
        }

        when (val s = state) {
            is ToolExecutionState.Success -> {
                val cert = s.data
                ResultCardWrapper(
                    title = "TLS Certificate (${cert.host}:${cert.port})",
                    onCopy = { onCopy(gson.toJson(cert)) },
                    onSave = {
                        onSave("TLS Certificate", "Web Security", cert.host, "TLS Audit for ${cert.host}", "X.509 certificate status (${if (cert.isValid) "Valid" else "Invalid"}).", gson.toJson(cert))
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        KeyValueRow("Valid To", cert.validTo ?: "N/A")
                        KeyValueRow("Days Remaining", "${cert.daysRemaining ?: 0} days")
                        KeyValueRow("Protocol", cert.protocol ?: "TLS")
                        KeyValueRow("Cipher Suite", cert.cipher ?: "N/A")
                        KeyValueRow("SANs", cert.sans?.joinToString(", ") ?: "None")
                    }
                }
            }
            is ToolExecutionState.Error -> ErrorCard(s.message)
            else -> {}
        }
    }
}

// 8. REDIRECTS
@Composable
fun RedirectsToolScreen(
    viewModel: ToolkitViewModel,
    onCopy: (String) -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var url by remember { mutableStateOf("http://google.com") }
    val authConfirmed by viewModel.authCheckRedirects.collectAsState()
    val state by viewModel.redirectsState.collectAsState()
    val gson = remember { GsonBuilder().setPrettyPrinting().create() }

    ToolScaffold(
        iconEmoji = "🔀",
        title = "HTTP Redirect Chain Tracer",
        description = "Maps redirect sequences, status codes (301, 302, 307, 308), and protocol transitions from HTTP to HTTPS.",
        authRequired = true,
        authConfirmed = authConfirmed,
        onAuthCheckedChange = { viewModel.authCheckRedirects.value = it }
    ) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Starting URL") },
            placeholder = { Text("http://example.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.runTraceRedirects(url) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is ToolExecutionState.Loading
        ) {
            if (state is ToolExecutionState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Trace Redirect Chain")
        }

        when (val s = state) {
            is ToolExecutionState.Success -> {
                val hops = s.data
                ResultCardWrapper(
                    title = "Redirect Chain (${hops.size} Hops)",
                    onCopy = { onCopy(gson.toJson(hops)) },
                    onSave = {
                        onSave("Redirect Trace", "Web Security", url, "Redirect Chain for $url", "Mapped ${hops.size} HTTP redirect hops.", gson.toJson(hops))
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        hops.forEach { hop ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("#${hop.hop} [${hop.statusCode}]", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(hop.url, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
            is ToolExecutionState.Error -> ErrorCard(s.message)
            else -> {}
        }
    }
}

// 9. URL ANALYZER
@Composable
fun UrlAnalyzerToolScreen(
    viewModel: ToolkitViewModel,
    onCopy: (String) -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var url by remember { mutableStateOf("https://example.com:8443/api/v1/auth?token=demo_token#profile") }
    val state by viewModel.urlAnalyzeState.collectAsState()
    val gson = remember { GsonBuilder().setPrettyPrinting().create() }

    ToolScaffold(
        iconEmoji = "🔗",
        title = "URL Security Analyzer",
        description = "Deconstructs URI schemes, host components, query string tokens, fragments, and identifies sensitive credential leakage risks.",
        authRequired = false
    ) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("URL to Analyze") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.runAnalyzeUrl(url) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is ToolExecutionState.Loading
        ) {
            if (state is ToolExecutionState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Analyze URL Structure")
        }

        when (val s = state) {
            is ToolExecutionState.Success -> {
                val data = s.data
                ResultCardWrapper(
                    title = "URL Breakdown (${data.hostname})",
                    onCopy = { onCopy(gson.toJson(data)) },
                    onSave = {
                        onSave("URL Analyzer", "Web Security", data.url, "URL Analysis for ${data.hostname}", "Component breakdown and parameters.", gson.toJson(data))
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        KeyValueRow("Protocol", data.protocol ?: "http")
                        KeyValueRow("Host / Port", "${data.hostname}:${data.port}")
                        KeyValueRow("Path", data.pathname ?: "/")
                        KeyValueRow("Query String", data.search ?: "")
                        if (!data.securityNotes.isNullOrEmpty()) {
                            Text("Security Observations:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                            data.securityNotes.forEach {
                                Text("• $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            is ToolExecutionState.Error -> ErrorCard(s.message)
            else -> {}
        }
    }
}

// 10. HASH GENERATOR
@Composable
fun HashGenToolScreen(
    viewModel: ToolkitViewModel,
    onCopy: (String) -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var text by remember { mutableStateOf("RedOps Security Assessment 2026") }
    var algorithm by remember { mutableStateOf("SHA-256") }
    var hmacKey by remember { mutableStateOf("") }
    val state by viewModel.hashState.collectAsState()
    val gson = remember { GsonBuilder().setPrettyPrinting().create() }

    val algorithms = listOf("MD5", "SHA-1", "SHA-256", "SHA-512")

    ToolScaffold(
        iconEmoji = "🔐",
        title = "Cryptographic Hash Generator",
        description = "Generates standard message digests and HMAC outputs across MD5, SHA-1, SHA-256, and SHA-512 algorithms.",
        authRequired = false
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Input Text / Secret") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            algorithms.forEach { algo ->
                FilterChip(
                    selected = algorithm == algo,
                    onClick = { algorithm = algo },
                    label = { Text(algo, fontSize = 11.sp) }
                )
            }
        }

        OutlinedTextField(
            value = hmacKey,
            onValueChange = { hmacKey = it },
            label = { Text("Optional HMAC Secret Key") },
            placeholder = { Text("Leave blank for standard digest") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.runGenerateHash(text, algorithm, hmacKey) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is ToolExecutionState.Loading
        ) {
            if (state is ToolExecutionState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Generate Hash Digest")
        }

        when (val s = state) {
            is ToolExecutionState.Success -> {
                val data = s.data
                ResultCardWrapper(
                    title = "${data.algorithm} Digest",
                    onCopy = { onCopy(data.hash) },
                    onSave = {
                        onSave("Hash Generator", "Security Utilities", "Text Input", "${data.algorithm} Hash", "Hash: ${data.hash}", gson.toJson(data))
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            data.hash,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            is ToolExecutionState.Error -> ErrorCard(s.message)
            else -> {}
        }
    }
}

// 11. HASH IDENTIFIER
@Composable
fun HashIdentifyToolScreen(
    viewModel: ToolkitViewModel,
    onCopy: (String) -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var hash by remember { mutableStateOf("5d41402abc4b2a76b9719d911017c592") }
    val state by viewModel.identifyState.collectAsState()
    val gson = remember { GsonBuilder().setPrettyPrinting().create() }

    ToolScaffold(
        iconEmoji = "🏷️",
        title = "Hash Type Identifier",
        description = "Detects cryptographic hash types and digest lengths from raw hexadecimal strings or crypt modular formats.",
        authRequired = false
    ) {
        OutlinedTextField(
            value = hash,
            onValueChange = { hash = it },
            label = { Text("Raw Hash Digest") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.runIdentifyHash(hash) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is ToolExecutionState.Loading
        ) {
            if (state is ToolExecutionState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Identify Algorithm")
        }

        when (val s = state) {
            is ToolExecutionState.Success -> {
                val matches = s.data
                ResultCardWrapper(
                    title = "Potential Algorithms (${matches.size} Matches)",
                    onCopy = { onCopy(gson.toJson(matches)) },
                    onSave = {
                        onSave("Hash Identifier", "Security Utilities", hash, "Hash Identification", "Matched algorithms: ${matches.map { it.algorithm }.joinToString()}", gson.toJson(matches))
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        matches.forEach { match ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(match.algorithm, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Confidence: ${match.confidence}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            if (!match.description.isNullOrBlank()) {
                                Text(match.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
            is ToolExecutionState.Error -> ErrorCard(s.message)
            else -> {}
        }
    }
}

// 12. JWT DECODER
@Composable
fun JwtDecoderToolScreen(
    viewModel: ToolkitViewModel,
    onCopy: (String) -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var token by remember { mutableStateOf("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkFsaWNlIFNlYyIsImlhdCI6MTUxNjIzOTAyMiwiZXhwIjoxNzk0OTk5OTk5fQ.4flk9Zt7Qv") }
    val state by viewModel.jwtState.collectAsState()

    ToolScaffold(
        iconEmoji = "🎫",
        title = "JWT Token Inspector",
        description = "Decodes JSON Web Tokens (JWT) into JOSE header and payload claims without transmitting private signature keys.",
        authRequired = false
    ) {
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Encoded JWT (header.payload.sig)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4
        )

        Button(
            onClick = { viewModel.runDecodeJwt(token) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is ToolExecutionState.Loading
        ) {
            if (state is ToolExecutionState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Decode JWT Claims")
        }

        RenderResultBox(
            state = state,
            onCopy = onCopy,
            onSave = { json ->
                onSave("JWT Decoder", "Security Utilities", "JWT Token", "Decoded JWT Claims", "Parsed JWT header and claims.", json)
            }
        )
    }
}

// 13. CVSS CALCULATOR
@Composable
fun CvssCalculatorToolScreen(
    viewModel: ToolkitViewModel,
    onCopy: (String) -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var av by remember { mutableStateOf("N") }
    var ac by remember { mutableStateOf("L") }
    var pr by remember { mutableStateOf("N") }
    var ui by remember { mutableStateOf("N") }
    var s by remember { mutableStateOf("U") }
    var c by remember { mutableStateOf("H") }
    var i by remember { mutableStateOf("H") }
    var a by remember { mutableStateOf("H") }

    val state by viewModel.cvssState.collectAsState()
    val gson = remember { GsonBuilder().setPrettyPrinting().create() }

    LaunchedEffect(av, ac, pr, ui, s, c, i, a) {
        viewModel.runCalculateCvss(CvssMetrics(av, ac, pr, ui, s, c, i, a))
    }

    ToolScaffold(
        iconEmoji = "📊",
        title = "CVSS v3.1 Severity Calculator",
        description = "Computes official FIRST CVSS v3.1 Base Scores, severity ratings, and vector strings for vulnerability assessments.",
        authRequired = false
    ) {
        Text("Attack Vector (AV):", fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("N" to "Network", "A" to "Adjacent", "L" to "Local", "P" to "Physical").forEach { (k, v) ->
                FilterChip(selected = av == k, onClick = { av = k }, label = { Text(v, fontSize = 10.sp) })
            }
        }

        Text("Attack Complexity (AC):", fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("L" to "Low", "H" to "High").forEach { (k, v) ->
                FilterChip(selected = ac == k, onClick = { ac = k }, label = { Text(v, fontSize = 10.sp) })
            }
        }

        Text("Confidentiality (C) / Integrity (I) / Availability (A):", fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("H" to "High Impact", "L" to "Low Impact", "N" to "None").forEach { (k, v) ->
                FilterChip(selected = c == k && i == k && a == k, onClick = { c = k; i = k; a = k }, label = { Text(v, fontSize = 10.sp) })
            }
        }

        when (val res = state) {
            is ToolExecutionState.Success -> {
                val data = res.data
                ResultCardWrapper(
                    title = "Score: ${data.baseScore} (${data.severity})",
                    onCopy = { onCopy(data.vector) },
                    onSave = {
                        onSave("CVSS Calc", "Security Utilities", data.vector, "CVSS ${data.baseScore} (${data.severity})", "Vector: ${data.vector}", gson.toJson(data))
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        KeyValueRow("Base Score", data.baseScore.toString())
                        KeyValueRow("Severity Rating", data.severity)
                        KeyValueRow("Vector String", data.vector)
                    }
                }
            }
            is ToolExecutionState.Error -> ErrorCard(res.message)
            else -> {}
        }
    }
}

// 14. ENCODER / DECODER
@Composable
fun EncoderToolScreen(
    viewModel: ToolkitViewModel,
    onCopy: (String) -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var input by remember { mutableStateOf("RedOps Security") }
    var from by remember { mutableStateOf("Plain") }
    var to by remember { mutableStateOf("Base64") }
    val state by viewModel.encoderState.collectAsState()
    val gson = remember { GsonBuilder().setPrettyPrinting().create() }

    val formats = listOf("Plain", "Base64", "Hex", "URL", "Binary")

    ToolScaffold(
        iconEmoji = "🔄",
        title = "Multi-Encoder & Decoder",
        description = "Converts strings across Plain Text, Base64, Hexadecimal, URL Encoding, and Binary byte streams.",
        authRequired = false
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Input Value") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("From Format:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(formats) { fmt ->
                FilterChip(selected = from == fmt, onClick = { from = fmt }, label = { Text(fmt, fontSize = 10.sp) })
            }
        }

        Text("To Format:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(formats) { fmt ->
                FilterChip(selected = to == fmt, onClick = { to = fmt }, label = { Text(fmt, fontSize = 10.sp) })
            }
        }

        Button(
            onClick = { viewModel.runConvertEncoding(input, from, to) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is ToolExecutionState.Loading
        ) {
            if (state is ToolExecutionState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Convert Encoding")
        }

        when (val s = state) {
            is ToolExecutionState.Success -> {
                val data = s.data
                ResultCardWrapper(
                    title = "Conversion Result ($from → $to)",
                    onCopy = { onCopy(data.output) },
                    onSave = {
                        onSave("Encoder", "Security Utilities", "Encoded Text", "Encoding Result", "Converted from $from to $to.", gson.toJson(data))
                    }
                ) {
                    Text(
                        data.output,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            is ToolExecutionState.Error -> ErrorCard(s.message)
            else -> {}
        }
    }
}

// 15. PASSWORD STRENGTH
@Composable
fun PasswordStrengthToolScreen(
    viewModel: ToolkitViewModel,
    onCopy: (String) -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var password by remember { mutableStateOf("RedOps#2026_Secure!") }
    val state by viewModel.passwordState.collectAsState()
    val gson = remember { GsonBuilder().setPrettyPrinting().create() }

    LaunchedEffect(password) {
        viewModel.runEstimatePassword(password)
    }

    ToolScaffold(
        iconEmoji = "💪",
        title = "Password Strength & Entropy Estimator",
        description = "Calculates character pool variety, Shannon entropy bits, and estimated brute-force offline cracking duration.",
        authRequired = false
    ) {
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password Input") },
            modifier = Modifier.fillMaxWidth()
        )

        when (val s = state) {
            is ToolExecutionState.Success -> {
                val data = s.data
                ResultCardWrapper(
                    title = "Rating: ${data.label}",
                    onCopy = { onCopy(gson.toJson(data)) },
                    onSave = {
                        onSave("Password Strength", "Security Utilities", "Evaluated Password", "Password Rating: ${data.label}", "Entropy: ${data.entropyBits} bits", gson.toJson(data))
                    }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        KeyValueRow("Entropy Bits", "${data.entropyBits} bits")
                        KeyValueRow("Est. Offline Crack Time", data.crackTimeText)
                        if (!data.feedback.isNullOrEmpty()) {
                            Text("Recommendations:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            data.feedback.forEach { Text("• $it", fontSize = 11.sp) }
                        }
                    }
                }
            }
            is ToolExecutionState.Error -> ErrorCard(s.message)
            else -> {}
        }
    }
}

// 16. JSON FORMATTER
@Composable
fun JsonFormatterToolScreen(
    viewModel: ToolkitViewModel,
    onCopy: (String) -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var rawJson by remember { mutableStateOf("{\"app\":\"RedOps Mobile\",\"tools\":15,\"status\":\"Active\"}") }
    val state by viewModel.jsonState.collectAsState()

    ToolScaffold(
        iconEmoji = "📄",
        title = "JSON Validator & Formatter",
        description = "Validates JSON structure, syntax correctness, and provides indented pretty printing for payload inspection.",
        authRequired = false
    ) {
        OutlinedTextField(
            value = rawJson,
            onValueChange = { rawJson = it },
            label = { Text("Raw JSON Input") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 6
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.runFormatJson(rawJson, 2) }, modifier = Modifier.weight(1f)) {
                Text("Pretty Print (2 Sp)")
            }
            Button(onClick = { viewModel.runFormatJson(rawJson, 4) }, modifier = Modifier.weight(1f)) {
                Text("Pretty Print (4 Sp)")
            }
        }

        when (val s = state) {
            is ToolExecutionState.Success -> {
                val data = s.data
                ResultCardWrapper(
                    title = if (data.valid) "Valid JSON" else "Invalid JSON Syntax",
                    onCopy = { onCopy(data.formatted) },
                    onSave = {
                        onSave("JSON Formatter", "Security Utilities", "JSON Snippet", "Formatted JSON", "Validated JSON payload.", data.formatted)
                    }
                ) {
                    if (data.valid) {
                        Text(
                            data.formatted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    } else {
                        Text(data.error ?: "Malformed JSON syntax", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
            is ToolExecutionState.Error -> ErrorCard(s.message)
            else -> {}
        }
    }
}

// -------------------------------------------------------------------------------------------------
// ASSESSMENT WORKSPACE / OBSERVATIONS TAB (Phase 5)
// -------------------------------------------------------------------------------------------------

@Composable
fun WorkspaceObservationsScreen(
    observations: List<ToolObservation>,
    engagements: List<Engagement>,
    onDeleteObservation: (ToolObservation) -> Unit,
    onUpdateStatus: (ToolObservation, String) -> Unit,
    onPromoteToFinding: (ToolObservation) -> Unit,
    onNavigateToEngagements: () -> Unit
) {
    var selectedEngagementFilter by remember { mutableStateOf("ALL") }

    val filteredObservations = remember(observations, selectedEngagementFilter) {
        if (selectedEngagementFilter == "ALL") observations
        else observations.filter { it.engagementId == selectedEngagementFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Saved Assessment Observations",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Verified or informational observations captured from toolkit inspections. Convert confirmed security risks into formal findings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Filter by Engagement
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                FilterChip(
                    selected = selectedEngagementFilter == "ALL",
                    onClick = { selectedEngagementFilter = "ALL" },
                    label = { Text("All Engagements (${observations.size})", fontSize = 11.sp) }
                )
            }
            items(engagements) { eng ->
                val count = observations.count { it.engagementId == eng.id }
                FilterChip(
                    selected = selectedEngagementFilter == eng.id,
                    onClick = { selectedEngagementFilter = eng.id },
                    label = { Text("${eng.title} ($count)", fontSize = 11.sp) }
                )
            }
        }

        if (filteredObservations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📋", fontSize = 36.sp)
                    Text("No saved observations yet.", style = MaterialTheme.typography.bodyMedium)
                    Text("Run any tool in Recon, Web Sec, or Utilities and tap 'Save Observation' to link results.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredObservations, key = { it.id }) { obs ->
                    ObservationCard(
                        observation = obs,
                        engagement = engagements.find { it.id == obs.engagementId },
                        onDelete = { onDeleteObservation(obs) },
                        onUpdateStatus = { st -> onUpdateStatus(obs, st) },
                        onPromote = { onPromoteToFinding(obs) }
                    )
                }
            }
        }
    }
}

@Composable
fun ObservationCard(
    observation: ToolObservation,
    engagement: Engagement?,
    onDelete: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onPromote: () -> Unit
) {
    val dateStr = remember(observation.createdAt) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(observation.createdAt))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = observation.toolName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = observation.target,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = dateStr,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = observation.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            if (observation.summary.isNotBlank()) {
                Text(
                    text = observation.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (engagement != null) {
                Text(
                    text = "Engagement: ${engagement.title}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = onPromote,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("➕ Convert to Finding", fontSize = 10.sp)
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// DIALOG FOR SAVING OBSERVATIONS
// -------------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveObservationDialog(
    engagements: List<Engagement>,
    toolName: String,
    target: String,
    initialTitle: String,
    initialSummary: String,
    rawJson: String,
    onDismiss: () -> Unit,
    onConfirm: (engagementId: String, title: String, summary: String) -> Unit
) {
    var selectedEngagementId by remember {
        mutableStateOf(engagements.firstOrNull()?.id ?: "")
    }
    var title by remember { mutableStateOf(initialTitle) }
    var summary by remember { mutableStateOf(initialSummary) }
    var expandedDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Observation to Assessment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Observations store non-destructive assessment evidence. They are distinguished from verified findings until explicitly converted.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (engagements.isEmpty()) {
                    Text(
                        text = "No active engagements found. Please create an engagement first in Workspace.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown }
                    ) {
                        val currentEng = engagements.find { it.id == selectedEngagementId }
                        OutlinedTextField(
                            value = currentEng?.title ?: "Select Engagement",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Target Engagement") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            engagements.forEach { eng ->
                                DropdownMenuItem(
                                    text = { Text(eng.title) },
                                    onClick = {
                                        selectedEngagementId = eng.id
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Observation Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Summary Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedEngagementId, title, summary) },
                enabled = selectedEngagementId.isNotBlank() && title.isNotBlank()
            ) {
                Text("Save Observation")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// -------------------------------------------------------------------------------------------------
// HELPER COMPONENTS
// -------------------------------------------------------------------------------------------------

@Composable
fun RenderResultBox(
    state: ToolExecutionState<JsonObject>,
    onCopy: (String) -> Unit,
    onSave: (String) -> Unit
) {
    val gson = remember { GsonBuilder().setPrettyPrinting().create() }
    when (state) {
        is ToolExecutionState.Success -> {
            val jsonStr = gson.toJson(state.data)
            ResultCardWrapper(
                title = "Execution Results",
                onCopy = { onCopy(jsonStr) },
                onSave = { onSave(jsonStr) }
            ) {
                Text(
                    text = jsonStr,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        is ToolExecutionState.Error -> ErrorCard(state.message)
        else -> {}
    }
}

@Composable
fun ResultCardWrapper(
    title: String,
    onCopy: () -> Unit,
    onSave: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onSave, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Save Observation", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            content()
        }
    }
}

@Composable
fun KeyValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
            Text(text = message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
    }
}

