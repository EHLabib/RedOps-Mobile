package com.redops.mobile

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.redops.mobile.data.local.preferences.ThemeMode
import com.redops.mobile.di.AppContainer
import com.redops.mobile.di.ViewModelFactory
import com.redops.mobile.presentation.activity.ActivityLogScreen
import com.redops.mobile.presentation.activity.ActivityLogViewModel
import com.redops.mobile.presentation.dashboard.DashboardScreen
import com.redops.mobile.presentation.dashboard.DashboardViewModel
import com.redops.mobile.presentation.engagements.EngagementDetailScreen
import com.redops.mobile.presentation.engagements.EngagementDialog
import com.redops.mobile.presentation.engagements.EngagementsScreen
import com.redops.mobile.presentation.engagements.EngagementsViewModel
import com.redops.mobile.presentation.evidence.EvidenceDetailScreen
import com.redops.mobile.presentation.evidence.EvidenceDialog
import com.redops.mobile.presentation.evidence.EvidenceScreen
import com.redops.mobile.presentation.evidence.EvidenceViewModel
import com.redops.mobile.presentation.findings.FindingDetailScreen
import com.redops.mobile.presentation.findings.FindingDialog
import com.redops.mobile.presentation.findings.FindingsScreen
import com.redops.mobile.presentation.findings.FindingsViewModel
import com.redops.mobile.presentation.navigation.Screen
import com.redops.mobile.presentation.notebook.NotebookDetailScreen
import com.redops.mobile.presentation.notebook.NotebookDialog
import com.redops.mobile.presentation.notebook.NotebookScreen
import com.redops.mobile.presentation.notebook.NotebookViewModel
import com.redops.mobile.presentation.reports.ReportPreviewScreen
import com.redops.mobile.presentation.reports.ReportsScreen
import com.redops.mobile.presentation.reports.ReportsViewModel
import com.redops.mobile.presentation.security.PinLockScreen
import com.redops.mobile.presentation.settings.AboutScreen
import com.redops.mobile.presentation.settings.PrivacyScreen
import com.redops.mobile.presentation.settings.SettingsScreen
import com.redops.mobile.presentation.settings.SettingsViewModel
import com.redops.mobile.presentation.toolkit.ToolkitCategory
import com.redops.mobile.presentation.toolkit.ToolkitScreen
import com.redops.mobile.presentation.toolkit.ToolkitViewModel
import com.redops.mobile.domain.model.Finding
import com.redops.mobile.ui.theme.RedOpsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedOpsApp(container: AppContainer) {
    val userPrefs = container.userPreferencesRepository
    val prefsState by userPrefs.userPreferencesFlow.collectAsState(
        initial = com.redops.mobile.data.local.preferences.UserPreferences(
            themeMode = ThemeMode.DARK,
            isBiometricEnabled = false,
            isPinEnabled = false,
            pinHash = ""
        )
    )

    val isDark = when (prefsState.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    var isUnlocked by remember { mutableStateOf(!prefsState.isPinEnabled) }
    var lastBackgroundTime by remember { mutableLongStateOf(0L) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner, prefsState.isPinEnabled, prefsState.autoLockEnabled, prefsState.inactivityTimeoutMinutes) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    lastBackgroundTime = System.currentTimeMillis()
                }
                androidx.lifecycle.Lifecycle.Event.ON_START -> {
                    if (prefsState.isPinEnabled && prefsState.autoLockEnabled && isUnlocked && lastBackgroundTime > 0L) {
                        val elapsedMs = System.currentTimeMillis() - lastBackgroundTime
                        val thresholdMs = prefsState.inactivityTimeoutMinutes * 60 * 1000L
                        if (prefsState.inactivityTimeoutMinutes == 0 || elapsedMs >= thresholdMs) {
                            isUnlocked = false
                        }
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(prefsState.isPinEnabled) {
        if (!prefsState.isPinEnabled) {
            isUnlocked = true
        }
    }

    RedOpsTheme(darkTheme = isDark) {
        if (!isUnlocked && prefsState.isPinEnabled && prefsState.pinHash.isNotEmpty()) {
            PinLockScreen(
                expectedPinHash = prefsState.pinHash,
                isBiometricEnabled = prefsState.isBiometricEnabled,
                securityManager = container.securityManager,
                onUnlock = { isUnlocked = true },
                onResetData = {
                    coroutineScope.launch {
                        container.engagementRepository.clearAll()
                        container.findingRepository.clearAll()
                        container.notebookRepository.clearAll()
                        container.evidenceRepository.clearAll()
                        container.reportHistoryRepository.clearAll()
                        container.dashboardRepository.clearActivities()
                        userPrefs.setPin(false, "")
                        isUnlocked = true
                    }
                }
            )
        } else {
            RedOpsMainContent(container = container)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RedOpsMainContent(container: AppContainer) {
    val navController = rememberNavController()
    val factory = remember { ViewModelFactory(container) }

    val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
    val engagementsViewModel: EngagementsViewModel = viewModel(factory = factory)
    val findingsViewModel: FindingsViewModel = viewModel(factory = factory)
    val notebookViewModel: NotebookViewModel = viewModel(factory = factory)
    val activityLogViewModel: ActivityLogViewModel = viewModel(factory = factory)
    val evidenceViewModel: EvidenceViewModel = viewModel(factory = factory)
    val reportsViewModel: ReportsViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
    val toolkitViewModel: ToolkitViewModel = viewModel(factory = factory)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Quick-action dialog states
    var showCreateEngagementDialog by remember { mutableStateOf(false) }
    var showCreateFindingDialog by remember { mutableStateOf(false) }
    var initialFindingForDialog by remember { mutableStateOf<Finding?>(null) }
    var showCreateNoteDialog by remember { mutableStateOf(false) }
    var showCreateEvidenceDialog by remember { mutableStateOf(false) }
    var activeEngagementIdForNewItem by remember { mutableStateOf<String?>(null) }
    var activeFindingIdForNewItem by remember { mutableStateOf<String?>(null) }

    val bottomNavItems = listOf(
        NavigationItem(Screen.Dashboard.route, "Dashboard", Icons.Default.Dashboard),
        NavigationItem(Screen.Toolkit.route, "Toolkit", Icons.Default.Security),
        NavigationItem(Screen.Engagements.route, "Engagements", Icons.Default.WorkOutline),
        NavigationItem(Screen.Findings.route, "Findings", Icons.Default.BugReport),
        NavigationItem(Screen.Notebook.route, "Notebook", Icons.Default.Book),
        NavigationItem(Screen.Reports.route, "Reports", Icons.Default.Description)
    )

    val isTopLevelDestination = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "REDOPS MOBILE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Security Assessment Suite",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (isTopLevelDestination) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Dashboard
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToEngagements = { navController.navigate(Screen.Engagements.route) },
                    onNavigateToFindings = { navController.navigate(Screen.Findings.route) },
                    onNavigateToNotebook = { navController.navigate(Screen.Notebook.route) },
                    onNavigateToEvidence = { navController.navigate(Screen.Evidence.route) },
                    onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                    onNavigateToToolkit = { categoryStr ->
                        val category = when (categoryStr) {
                            "RECON" -> ToolkitCategory.RECON
                            "WEBSEC" -> ToolkitCategory.WEBSEC
                            "UTILITIES" -> ToolkitCategory.UTILITIES
                            "WORKSPACE" -> ToolkitCategory.WORKSPACE
                            else -> ToolkitCategory.RECON
                        }
                        toolkitViewModel.selectCategory(category)
                        navController.navigate(Screen.Toolkit.route)
                    },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToActivityLog = { navController.navigate(Screen.ActivityLog.route) },
                    onEngagementClick = { engagement ->
                        navController.navigate(Screen.EngagementDetail.createRoute(engagement.id))
                    },
                    onNewEngagement = {
                        activeEngagementIdForNewItem = null
                        showCreateEngagementDialog = true
                    },
                    onNewFinding = {
                        activeEngagementIdForNewItem = null
                        initialFindingForDialog = null
                        showCreateFindingDialog = true
                    },
                    onNewNote = {
                        activeEngagementIdForNewItem = null
                        showCreateNoteDialog = true
                    }
                )
            }

            // Toolkit Hub
            composable(Screen.Toolkit.route) {
                ToolkitScreen(
                    viewModel = toolkitViewModel,
                    onNavigateToEngagements = { navController.navigate(Screen.Engagements.route) },
                    onPromoteToFinding = { observation ->
                        initialFindingForDialog = Finding(
                            engagementId = observation.engagementId,
                            title = observation.title,
                            affectedAsset = observation.target,
                            description = observation.summary + (if (observation.rawResultJson.isNotBlank()) "\n\nRaw Evidence / Tool Output:\n" + observation.rawResultJson else ""),
                            severity = "Informational",
                            category = "Information Disclosure",
                            remediation = "Verify observation context within target scope and enforce appropriate defensive controls."
                        )
                        activeEngagementIdForNewItem = observation.engagementId
                        showCreateFindingDialog = true
                    }
                )
            }

            // Engagements
            composable(Screen.Engagements.route) {
                EngagementsScreen(
                    viewModel = engagementsViewModel,
                    onEngagementClick = { engagement ->
                        navController.navigate(Screen.EngagementDetail.createRoute(engagement.id))
                    },
                    onAddEngagement = {
                        activeEngagementIdForNewItem = null
                        showCreateEngagementDialog = true
                    }
                )
            }

            // Engagement Detail
            composable(
                route = Screen.EngagementDetail.route,
                arguments = listOf(navArgument("engagementId") { type = NavType.StringType })
            ) { backStackEntry ->
                val engagementId = backStackEntry.arguments?.getString("engagementId") ?: ""
                EngagementDetailScreen(
                    engagementId = engagementId,
                    viewModel = engagementsViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToFindingDetail = { findingId ->
                        navController.navigate(Screen.FindingDetail.createRoute(findingId))
                    },
                    onNewFindingForEngagement = { engId ->
                        activeEngagementIdForNewItem = engId
                        showCreateFindingDialog = true
                    },
                    onNewNoteForEngagement = { engId ->
                        activeEngagementIdForNewItem = engId
                        showCreateNoteDialog = true
                    },
                    onGenerateReport = {
                        navController.navigate(Screen.Reports.route)
                    }
                )
            }

            // Findings
            composable(Screen.Findings.route) {
                FindingsScreen(
                    viewModel = findingsViewModel,
                    onFindingClick = { finding ->
                        navController.navigate(Screen.FindingDetail.createRoute(finding.id))
                    },
                    onAddFinding = {
                        activeEngagementIdForNewItem = null
                        showCreateFindingDialog = true
                    }
                )
            }

            // Finding Detail
            composable(
                route = Screen.FindingDetail.route,
                arguments = listOf(navArgument("findingId") { type = NavType.StringType })
            ) { backStackEntry ->
                val findingId = backStackEntry.arguments?.getString("findingId") ?: ""
                FindingDetailScreen(
                    findingId = findingId,
                    viewModel = findingsViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToEngagement = { engId ->
                        navController.navigate(Screen.EngagementDetail.createRoute(engId))
                    },
                    onNavigateToEvidenceDetail = { evidenceId ->
                        navController.navigate(Screen.EvidenceDetail.createRoute(evidenceId))
                    },
                    onAddEvidence = { fndId, engId ->
                        activeEngagementIdForNewItem = engId
                        activeFindingIdForNewItem = fndId
                        showCreateEvidenceDialog = true
                    }
                )
            }

            // Notebook
            composable(Screen.Notebook.route) {
                NotebookScreen(
                    viewModel = notebookViewModel,
                    onAddEntry = {
                        activeEngagementIdForNewItem = null
                        showCreateNoteDialog = true
                    },
                    onEntryClick = { entryId ->
                        navController.navigate(Screen.NotebookDetail.createRoute(entryId))
                    },
                    onOpenActivityLog = {
                        navController.navigate(Screen.ActivityLog.route)
                    }
                )
            }

            // Notebook Detail
            composable(
                route = Screen.NotebookDetail.route,
                arguments = listOf(navArgument("entryId") { type = NavType.StringType })
            ) { backStackEntry ->
                val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
                NotebookDetailScreen(
                    entryId = entryId,
                    viewModel = notebookViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Activity Log
            composable(Screen.ActivityLog.route) {
                ActivityLogScreen(
                    viewModel = activityLogViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Evidence
            composable(Screen.Evidence.route) {
                EvidenceScreen(
                    viewModel = evidenceViewModel,
                    onBack = { navController.popBackStack() },
                    onEvidenceClick = { evidenceId ->
                        navController.navigate(Screen.EvidenceDetail.createRoute(evidenceId))
                    }
                )
            }

            // Evidence Detail
            composable(
                route = Screen.EvidenceDetail.route,
                arguments = listOf(navArgument("evidenceId") { type = NavType.StringType })
            ) { backStackEntry ->
                val evidenceId = backStackEntry.arguments?.getString("evidenceId") ?: ""
                EvidenceDetailScreen(
                    evidenceId = evidenceId,
                    viewModel = evidenceViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Reports
            composable(Screen.Reports.route) {
                ReportsScreen(
                    viewModel = reportsViewModel,
                    onNavigateToPreview = { navController.navigate(Screen.ReportPreview.route) }
                )
            }

            // Report Preview
            composable(Screen.ReportPreview.route) {
                ReportPreviewScreen(
                    viewModel = reportsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Settings
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) },
                    onNavigateToAbout = { navController.navigate(Screen.About.route) }
                )
            }

            // Privacy
            composable(Screen.Privacy.route) {
                PrivacyScreen(onBack = { navController.popBackStack() })
            }

            // About
            composable(Screen.About.route) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }

    // Quick-action Modals
    if (showCreateEngagementDialog) {
        EngagementDialog(
            initialEngagement = null,
            onDismiss = { showCreateEngagementDialog = false },
            onSave = { newEngagement ->
                engagementsViewModel.saveEngagement(newEngagement, onSuccess = {
                    showCreateEngagementDialog = false
                })
            }
        )
    }

    val allEngagements by engagementsViewModel.engagements.collectAsState()
    val allFindings by findingsViewModel.findings.collectAsState()

    if (showCreateFindingDialog) {
        FindingDialog(
            initialFinding = initialFindingForDialog,
            engagements = allEngagements,
            preselectedEngagementId = activeEngagementIdForNewItem,
            onDismiss = {
                showCreateFindingDialog = false
                initialFindingForDialog = null
            },
            onSave = { newFinding ->
                findingsViewModel.saveFinding(newFinding, onSuccess = {
                    showCreateFindingDialog = false
                    initialFindingForDialog = null
                })
            }
        )
    }

    if (showCreateNoteDialog) {
        NotebookDialog(
            initialEntry = null,
            engagements = allEngagements,
            preselectedEngagementId = activeEngagementIdForNewItem,
            onDismiss = { showCreateNoteDialog = false },
            onSave = { newNote ->
                notebookViewModel.saveEntry(newNote, onSuccess = {
                    showCreateNoteDialog = false
                })
            }
        )
    }

    if (showCreateEvidenceDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        EvidenceDialog(
            engagements = allEngagements,
            findings = allFindings,
            preselectedEngagementId = activeEngagementIdForNewItem,
            preselectedFindingId = activeFindingIdForNewItem,
            onDismiss = {
                showCreateEvidenceDialog = false
                activeFindingIdForNewItem = null
            },
            onSave = { engId, fndId, title, desc, cat, uri ->
                evidenceViewModel.importAndSaveEvidence(
                    context = context,
                    engagementId = engId,
                    findingId = fndId,
                    title = title,
                    description = desc,
                    category = cat,
                    fileUri = uri
                ) {
                    showCreateEvidenceDialog = false
                    activeFindingIdForNewItem = null
                }
            }
        )
    }
}

private data class NavigationItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
