package com.redops.mobile.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redops.mobile.domain.model.ActivityLog
import com.redops.mobile.domain.model.DashboardStats
import com.redops.mobile.domain.model.Engagement
import com.redops.mobile.presentation.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToEngagements: () -> Unit,
    onNavigateToFindings: () -> Unit,
    onNavigateToNotebook: () -> Unit,
    onNavigateToEvidence: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToToolkit: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToActivityLog: () -> Unit = {},
    onEngagementClick: (Engagement) -> Unit = {},
    onNewEngagement: () -> Unit,
    onNewFinding: () -> Unit,
    onNewNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is DashboardUiState.Loading -> {
            LoadingStateView(message = "Loading real-time assessment data...")
        }
        is DashboardUiState.Error -> {
            ErrorStateView(message = state.message)
        }
        is DashboardUiState.Success -> {
            DashboardContent(
                stats = state.stats,
                recentEngagements = state.recentEngagements,
                recentActivities = state.recentActivities,
                onNavigateToEngagements = onNavigateToEngagements,
                onNavigateToFindings = onNavigateToFindings,
                onNavigateToNotebook = onNavigateToNotebook,
                onNavigateToEvidence = onNavigateToEvidence,
                onNavigateToReports = onNavigateToReports,
                onNavigateToToolkit = onNavigateToToolkit,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToActivityLog = onNavigateToActivityLog,
                onEngagementClick = onEngagementClick,
                onNewEngagement = onNewEngagement,
                onNewFinding = onNewFinding,
                onNewNote = onNewNote,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun DashboardContent(
    stats: DashboardStats,
    recentEngagements: List<Engagement>,
    recentActivities: List<ActivityLog>,
    onNavigateToEngagements: () -> Unit,
    onNavigateToFindings: () -> Unit,
    onNavigateToNotebook: () -> Unit,
    onNavigateToEvidence: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToToolkit: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToActivityLog: () -> Unit = {},
    onEngagementClick: (Engagement) -> Unit,
    onNewEngagement: () -> Unit,
    onNewFinding: () -> Unit,
    onNewNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDateFormatted = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // A. Header: Branding, Shield Logo, Welcome Message, Date, Settings Shortcut
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Shield Logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(10.dp).size(28.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "REDOPS MOBILE",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = "Welcome Operator",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = currentDateFormatted,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "OFFLINE-READY",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // B. Primary Application Sections Hub (Matching User's Category Hub Cards)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = "Cybersecurity Toolkit", subtitle = "Passive reconnaissance, web auditing & utilities")

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        ToolkitHubCard(
                            emoji = "🌐",
                            title = "Passive Recon",
                            subtitle = "DNS queries, PTR, WHOIS/RDAP & Subnet CIDR",
                            badgeText = "4 LIVE TOOLS",
                            onClick = { onNavigateToToolkit("RECON") }
                        )
                    }
                    item {
                        ToolkitHubCard(
                            emoji = "🛡️",
                            title = "Web Security",
                            subtitle = "Headers analysis, TLS/SSL certs & Redirect hops",
                            badgeText = "3 LIVE TOOLS",
                            onClick = { onNavigateToToolkit("WEBSEC") }
                        )
                    }
                    item {
                        ToolkitHubCard(
                            emoji = "🧰",
                            title = "Security Utilities",
                            subtitle = "Multi-encoder, Hash digests, ID & CVSS v3.1",
                            badgeText = "4 LIVE TOOLS",
                            onClick = { onNavigateToToolkit("UTILITIES") }
                        )
                    }
                    item {
                        ToolkitHubCard(
                            emoji = "📋",
                            title = "Workspace",
                            subtitle = "Engagements, Targets, Findings, Notebook & Reports",
                            badgeText = "UNIFIED SUITE",
                            onClick = onNavigateToEngagements
                        )
                    }
                }
            }
        }

        // C. Overview Cards: Total Engagements, Active Engagements, Completed Engagements, Total Vulnerabilities
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = "Assessment Overview", subtitle = "Key engagement and vulnerability metrics")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatisticCard(
                        title = "Total Engagements",
                        value = stats.totalEngagements.toString(),
                        subtitle = "Recorded engagements",
                        icon = Icons.Default.FolderOpen,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToEngagements
                    )
                    StatisticCard(
                        title = "Active Assessments",
                        value = stats.activeEngagements.toString(),
                        subtitle = "Currently in progress",
                        icon = Icons.Default.PlayCircle,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToEngagements
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatisticCard(
                        title = "Completed",
                        value = stats.completedEngagements.toString(),
                        subtitle = "Formal assessments",
                        icon = Icons.Default.CheckCircle,
                        accentColor = Color(0xFF40C4FF),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToEngagements
                    )
                    StatisticCard(
                        title = "Total Findings",
                        value = stats.totalFindings.toString(),
                        subtitle = "${stats.unresolvedFindings} unresolved",
                        icon = Icons.Default.BugReport,
                        accentColor = if (stats.criticalFindings > 0) Color(0xFFFF5252) else Color(0xFFFF9100),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToFindings
                    )
                }
            }
        }

        // C. Vulnerability Severity Overview with visual chart & breakdown
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VULNERABILITY SEVERITY OVERVIEW",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Surface(
                            color = if (stats.criticalFindings > 0) Color(0xFFFF5252).copy(alpha = 0.15f) else Color(0xFF00E676).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${stats.totalFindings} FINDINGS",
                                color = if (stats.criticalFindings > 0) Color(0xFFFF5252) else Color(0xFF00E676),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Visual stacked bar chart
                    SeverityBarChart(
                        critical = stats.criticalFindings,
                        high = stats.highFindings,
                        medium = stats.mediumFindings,
                        low = stats.lowFindings,
                        info = stats.infoFindings
                    )

                    // Severity breakdown counts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SeverityCountPill("Critical", stats.criticalFindings, Color(0xFFFF5252))
                        SeverityCountPill("High", stats.highFindings, Color(0xFFFF9100))
                        SeverityCountPill("Medium", stats.mediumFindings, Color(0xFFFFD740))
                        SeverityCountPill("Low", stats.lowFindings, Color(0xFF69F0AE))
                        SeverityCountPill("Info", stats.infoFindings, Color(0xFF40C4FF))
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.background)

                    // Remediation Progress
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Remediation Progress",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${(stats.resolutionProgress * 100).toInt()}% Resolved",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { stats.resolutionProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.background,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${stats.resolvedFindings} Resolved", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            Text("${stats.unresolvedFindings} Actionable", color = Color(0xFFFF9100), fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // F. Quick Actions (New Engagement, Add Finding, New Note, Generate Report)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(title = "Quick Actions", subtitle = "Direct workflow execution")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        label = "New Engagement",
                        icon = Icons.Default.AddBusiness,
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = onNewEngagement
                    )
                    QuickActionButton(
                        label = "Add Finding",
                        icon = Icons.Default.BugReport,
                        accent = Color(0xFFFF5252),
                        modifier = Modifier.weight(1f),
                        onClick = onNewFinding
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        label = "New Note",
                        icon = Icons.Default.EditNote,
                        accent = Color(0xFF40C4FF),
                        modifier = Modifier.weight(1f),
                        onClick = onNewNote
                    )
                    QuickActionButton(
                        label = "Generate Report",
                        icon = Icons.Default.Description,
                        accent = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToReports
                    )
                }
            }
        }

        // D. Engagement Overview (Recent Engagements from DB)
        item {
            SectionHeader(
                title = "Recent Engagements",
                subtitle = "Active and latest assessment scopes",
                actionLabel = if (recentEngagements.isNotEmpty()) "View All (${stats.totalEngagements})" else null,
                onActionClick = onNavigateToEngagements
            )
        }

        if (recentEngagements.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.FolderOpen,
                    title = "No Engagements Logged",
                    description = "Create your first assessment engagement to record scope, objectives, and findings.",
                    actionLabel = "Create Engagement",
                    onAction = onNewEngagement
                )
            }
        } else {
            items(recentEngagements, key = { it.id }) { engagement ->
                EngagementCard(
                    engagement = engagement,
                    onClick = { onEngagementClick(engagement) }
                )
            }
        }

        // E. Recent Activity Feed from DB
        item {
            SectionHeader(
                title = "Recent Activity Audit Feed",
                subtitle = "Chronological log of assessment events",
                actionLabel = "VIEW ALL",
                onActionClick = onNavigateToActivityLog
            )
        }

        if (recentActivities.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF40C4FF), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "No activities recorded. Log an engagement or finding to start audit trail.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(recentActivities, key = { it.id }) { activity ->
                ActivityItem(
                    activity = activity,
                    onClick = {
                        when (activity.type.uppercase()) {
                            "ENGAGEMENT" -> onNavigateToEngagements()
                            "FINDING" -> onNavigateToFindings()
                            "NOTEBOOK" -> onNavigateToNotebook()
                            "EVIDENCE" -> onNavigateToEvidence()
                            "REPORT" -> onNavigateToReports()
                            else -> onNavigateToActivityLog()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ToolkitHubCard(
    emoji: String,
    title: String,
    subtitle: String,
    badgeText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(135.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = emoji, fontSize = 24.sp)
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 13.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Surface(
                color = if (badgeText.contains("UNIFIED")) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF00E5FF).copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, if (badgeText.contains("UNIFIED")) Color(0xFF10B981) else Color(0xFF00E5FF))
            ) {
                Text(
                    text = badgeText,
                    color = if (badgeText.contains("UNIFIED")) Color(0xFF10B981) else Color(0xFF00E5FF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun SeverityCountPill(label: String, count: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = count.toString(), color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.5.sp)
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = accent.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.padding(6.dp).size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
