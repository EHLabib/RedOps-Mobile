package com.redops.mobile.presentation.settings

import com.redops.mobile.data.local.preferences.ThemeMode
import com.redops.mobile.data.local.preferences.UserPreferences
import com.redops.mobile.data.local.preferences.UserPreferencesRepository
import com.redops.mobile.domain.model.*
import com.redops.mobile.domain.repository.*
import com.redops.mobile.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeUserPreferencesRepository : UserPreferencesRepository(null) {
        private val _flow = MutableStateFlow(UserPreferences())
        override val userPreferencesFlow: Flow<UserPreferences> = _flow

        override suspend fun setThemeMode(mode: ThemeMode) {
            _flow.value = _flow.value.copy(themeMode = mode)
        }
        override suspend fun setBiometricEnabled(enabled: Boolean) {
            _flow.value = _flow.value.copy(isBiometricEnabled = enabled)
        }
        override suspend fun setPin(enabled: Boolean, hash: String) {
            _flow.value = _flow.value.copy(isPinEnabled = enabled, pinHash = hash)
        }
        override suspend fun setAutoLockEnabled(enabled: Boolean) {
            _flow.value = _flow.value.copy(autoLockEnabled = enabled)
        }
        override suspend fun setInactivityTimeoutMinutes(minutes: Int) {
            _flow.value = _flow.value.copy(inactivityTimeoutMinutes = minutes)
        }
        override suspend fun setScreenshotProtectionEnabled(enabled: Boolean) {
            _flow.value = _flow.value.copy(screenshotProtectionEnabled = enabled)
        }
        override suspend fun clearAll() {
            _flow.value = UserPreferences()
        }
    }

    private class FakeEngagementRepository : EngagementRepository {
        var clearCalled = false
        override fun getAllEngagements(): Flow<List<Engagement>> = flowOf(emptyList())
        override fun getEngagementById(id: String): Flow<Engagement?> = flowOf(null)
        override suspend fun getEngagementByIdDirect(id: String): Engagement? = null
        override suspend fun saveEngagement(engagement: Engagement) {}
        override suspend fun updateEngagement(engagement: Engagement) {}
        override suspend fun deleteEngagement(id: String) {}
        override fun searchEngagements(query: String): Flow<List<Engagement>> = flowOf(emptyList())
        override suspend fun clearAll() { clearCalled = true }
    }

    private class FakeFindingRepository : FindingRepository {
        var clearCalled = false
        override fun getAllFindings(): Flow<List<Finding>> = flowOf(emptyList())
        override fun getFindingsForEngagement(engagementId: String): Flow<List<Finding>> = flowOf(emptyList())
        override suspend fun getFindingsForEngagementDirect(engagementId: String): List<Finding> = emptyList()
        override fun getFindingById(id: String): Flow<Finding?> = flowOf(null)
        override suspend fun getFindingByIdDirect(id: String): Finding? = null
        override suspend fun saveFinding(finding: Finding) {}
        override suspend fun updateFinding(finding: Finding) {}
        override suspend fun deleteFinding(id: String) {}
        override fun searchFindings(query: String): Flow<List<Finding>> = flowOf(emptyList())
        override suspend fun clearAll() { clearCalled = true }
    }

    private class FakeNotebookRepository : NotebookRepository {
        var clearCalled = false
        override fun getAllEntries(): Flow<List<NotebookEntry>> = flowOf(emptyList())
        override fun getEntriesForEngagement(engagementId: String): Flow<List<NotebookEntry>> = flowOf(emptyList())
        override fun getEntryById(id: String): Flow<NotebookEntry?> = flowOf(null)
        override suspend fun saveEntry(entry: NotebookEntry) {}
        override suspend fun updateEntry(entry: NotebookEntry) {}
        override suspend fun deleteEntry(id: String) {}
        override fun searchEntries(query: String): Flow<List<NotebookEntry>> = flowOf(emptyList())
        override suspend fun togglePin(id: String, isPinned: Boolean) {}
        override suspend fun toggleFavorite(id: String, isFavorite: Boolean) {}
        override suspend fun clearAll() { clearCalled = true }
    }

    private class FakeEvidenceRepository : EvidenceRepository {
        var clearCalled = false
        override fun getAllEvidence(): Flow<List<EvidenceRecord>> = flowOf(emptyList())
        override fun getEvidenceForEngagement(engagementId: String): Flow<List<EvidenceRecord>> = flowOf(emptyList())
        override fun getEvidenceForFinding(findingId: String): Flow<List<EvidenceRecord>> = flowOf(emptyList())
        override fun getEvidenceById(id: String): Flow<EvidenceRecord?> = flowOf(null)
        override suspend fun getEvidenceByIdDirect(id: String): EvidenceRecord? = null
        override suspend fun saveEvidence(evidence: EvidenceRecord) {}
        override suspend fun updateEvidence(evidence: EvidenceRecord) {}
        override suspend fun deleteEvidence(id: String) {}
        override fun searchEvidence(query: String): Flow<List<EvidenceRecord>> = flowOf(emptyList())
        override suspend fun clearAll() { clearCalled = true }
    }

    private class FakeDashboardRepository : DashboardRepository {
        var clearCalled = false
        override fun getDashboardStats(): Flow<DashboardStats> = flowOf(DashboardStats())
        override fun getRecentActivities(): Flow<List<ActivityLog>> = flowOf(emptyList())
        override fun getRecentEngagements(limit: Int): Flow<List<Engagement>> = flowOf(emptyList())
        override suspend fun logActivity(title: String, type: String, statusBadge: String, engagementId: String?) {}
        override suspend fun clearActivities() { clearCalled = true }
    }

    private class FakeReportHistoryRepository : ReportHistoryRepository {
        var clearCalled = false
        override fun getAllReports(): Flow<List<ReportHistory>> = flowOf(emptyList())
        override fun getReportsForEngagement(engagementId: String): Flow<List<ReportHistory>> = flowOf(emptyList())
        override fun getReportById(id: String): Flow<ReportHistory?> = flowOf(null)
        override suspend fun getReportByIdDirect(id: String): ReportHistory? = null
        override suspend fun recordReport(report: ReportHistory) {}
        override suspend fun deleteReport(id: String) {}
        override suspend fun clearAll() { clearCalled = true }
    }

    @Test
    fun setPin_configuresPbkdf2PinHash() = runTest(testDispatcher) {
        val userPrefsRepo = FakeUserPreferencesRepository()
        val secMgr = SecurityManager(null)
        val engRepo = FakeEngagementRepository()
        val findRepo = FakeFindingRepository()
        val noteRepo = FakeNotebookRepository()
        val evRepo = FakeEvidenceRepository()
        val dashRepo = FakeDashboardRepository()
        val repRepo = FakeReportHistoryRepository()

        val viewModel = SettingsViewModel(userPrefsRepo, secMgr, engRepo, findRepo, noteRepo, evRepo, dashRepo, repRepo)
        val collectJob = launch { viewModel.preferences.collect {} }

        viewModel.setPin("7788")
        testScheduler.advanceUntilIdle()

        val prefs = viewModel.preferences.value
        assertTrue(prefs.isPinEnabled)
        assertTrue(prefs.pinHash.startsWith("v2:"))
        assertTrue(secMgr.verifyPin("7788", prefs.pinHash))

        collectJob.cancel()
    }

    @Test
    fun changePin_validOldPin_updatesSuccessfully() = runTest(testDispatcher) {
        val userPrefsRepo = FakeUserPreferencesRepository()
        val secMgr = SecurityManager(null)
        val engRepo = FakeEngagementRepository()
        val findRepo = FakeFindingRepository()
        val noteRepo = FakeNotebookRepository()
        val evRepo = FakeEvidenceRepository()
        val dashRepo = FakeDashboardRepository()
        val repRepo = FakeReportHistoryRepository()

        val viewModel = SettingsViewModel(userPrefsRepo, secMgr, engRepo, findRepo, noteRepo, evRepo, dashRepo, repRepo)
        val collectJob = launch { viewModel.preferences.collect {} }

        viewModel.setPin("1111")
        testScheduler.advanceUntilIdle()

        val success = viewModel.changePin("1111", "2222")
        testScheduler.advanceUntilIdle()

        assertTrue(success)
        assertTrue(secMgr.verifyPin("2222", viewModel.preferences.value.pinHash))
        assertFalse(secMgr.verifyPin("1111", viewModel.preferences.value.pinHash))

        collectJob.cancel()
    }

    @Test
    fun changePin_invalidOldPin_failsAndPreservesOriginal() = runTest(testDispatcher) {
        val userPrefsRepo = FakeUserPreferencesRepository()
        val secMgr = SecurityManager(null)
        val engRepo = FakeEngagementRepository()
        val findRepo = FakeFindingRepository()
        val noteRepo = FakeNotebookRepository()
        val evRepo = FakeEvidenceRepository()
        val dashRepo = FakeDashboardRepository()
        val repRepo = FakeReportHistoryRepository()

        val viewModel = SettingsViewModel(userPrefsRepo, secMgr, engRepo, findRepo, noteRepo, evRepo, dashRepo, repRepo)
        val collectJob = launch { viewModel.preferences.collect {} }

        viewModel.setPin("9999")
        testScheduler.advanceUntilIdle()

        val success = viewModel.changePin("0000", "8888")
        testScheduler.advanceUntilIdle()

        assertFalse(success)
        assertTrue(secMgr.verifyPin("9999", viewModel.preferences.value.pinHash))

        collectJob.cancel()
    }

    @Test
    fun securityToggles_updatePreferencesCorrectly() = runTest(testDispatcher) {
        val userPrefsRepo = FakeUserPreferencesRepository()
        val secMgr = SecurityManager(null)
        val engRepo = FakeEngagementRepository()
        val findRepo = FakeFindingRepository()
        val noteRepo = FakeNotebookRepository()
        val evRepo = FakeEvidenceRepository()
        val dashRepo = FakeDashboardRepository()
        val repRepo = FakeReportHistoryRepository()

        val viewModel = SettingsViewModel(userPrefsRepo, secMgr, engRepo, findRepo, noteRepo, evRepo, dashRepo, repRepo)
        val collectJob = launch { viewModel.preferences.collect {} }

        viewModel.setAutoLockEnabled(false)
        viewModel.setInactivityTimeout(5)
        viewModel.setScreenshotProtection(true)
        testScheduler.advanceUntilIdle()

        val prefs = viewModel.preferences.value
        assertFalse(prefs.autoLockEnabled)
        assertEquals(5, prefs.inactivityTimeoutMinutes)
        assertTrue(prefs.screenshotProtectionEnabled)

        collectJob.cancel()
    }

    @Test
    fun clearAllData_invokesAllRepositories() = runTest(testDispatcher) {
        val userPrefsRepo = FakeUserPreferencesRepository()
        val secMgr = SecurityManager(null)
        val engRepo = FakeEngagementRepository()
        val findRepo = FakeFindingRepository()
        val noteRepo = FakeNotebookRepository()
        val evRepo = FakeEvidenceRepository()
        val dashRepo = FakeDashboardRepository()
        val repRepo = FakeReportHistoryRepository()

        val viewModel = SettingsViewModel(userPrefsRepo, secMgr, engRepo, findRepo, noteRepo, evRepo, dashRepo, repRepo)

        viewModel.clearAllData()
        testScheduler.advanceUntilIdle()

        assertTrue(engRepo.clearCalled)
        assertTrue(findRepo.clearCalled)
        assertTrue(noteRepo.clearCalled)
        assertTrue(evRepo.clearCalled)
        assertTrue(dashRepo.clearCalled)
        assertTrue(repRepo.clearCalled)
    }
}
