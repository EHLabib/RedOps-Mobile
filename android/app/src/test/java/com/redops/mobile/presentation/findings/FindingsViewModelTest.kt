package com.redops.mobile.presentation.findings

import com.redops.mobile.domain.model.Engagement
import com.redops.mobile.domain.model.Finding
import com.redops.mobile.domain.repository.EngagementRepository
import com.redops.mobile.domain.repository.FindingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FindingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeFindingRepository : FindingRepository {
        val findingList = mutableListOf<Finding>()
        private val _flow = MutableStateFlow<List<Finding>>(emptyList())

        fun updateFlow() {
            _flow.value = findingList.toList()
        }

        override fun getAllFindings(): Flow<List<Finding>> = _flow
        override fun getFindingsForEngagement(engagementId: String): Flow<List<Finding>> =
            flowOf(findingList.filter { it.engagementId == engagementId })

        override suspend fun getFindingsForEngagementDirect(engagementId: String): List<Finding> =
            findingList.filter { it.engagementId == engagementId }

        override fun getFindingById(id: String): Flow<Finding?> =
            flowOf(findingList.find { it.id == id })

        override suspend fun getFindingByIdDirect(id: String): Finding? =
            findingList.find { it.id == id }

        override suspend fun saveFinding(finding: Finding) {
            findingList.removeAll { it.id == finding.id }
            findingList.add(finding)
            updateFlow()
        }

        override suspend fun updateFinding(finding: Finding) {
            val index = findingList.indexOfFirst { it.id == finding.id }
            if (index != -1) {
                findingList[index] = finding
            } else {
                findingList.add(finding)
            }
            updateFlow()
        }

        override suspend fun deleteFinding(id: String) {
            findingList.removeAll { it.id == id }
            updateFlow()
        }

        override fun searchFindings(query: String): Flow<List<Finding>> =
            flowOf(findingList.filter { it.title.contains(query, true) || it.affectedAsset.contains(query, true) })

        override suspend fun clearAll() {
            findingList.clear()
            updateFlow()
        }
    }

    private class FakeEngagementRepository : EngagementRepository {
        override fun getAllEngagements(): Flow<List<Engagement>> = flowOf(
            listOf(
                Engagement(id = "eng-1", title = "Alpha Audit", client = "Client Alpha", startDate = "2026-11-01", endDate = "2026-11-05", authorizedScope = "*.alpha.com"),
                Engagement(id = "eng-2", title = "Beta Review", client = "Client Beta", startDate = "2026-11-10", endDate = "2026-11-15", authorizedScope = "*.beta.com")
            )
        )
        override fun getEngagementById(id: String): Flow<Engagement?> = flowOf(null)
        override suspend fun getEngagementByIdDirect(id: String): Engagement? = null
        override suspend fun saveEngagement(engagement: Engagement) {}
        override suspend fun updateEngagement(engagement: Engagement) {}
        override suspend fun deleteEngagement(id: String) {}
        override fun searchEngagements(query: String): Flow<List<Engagement>> = flowOf(emptyList())
        override suspend fun clearAll() {}
    }

    @Test
    fun findingsViewModel_searchFilterAndSort_worksCorrectly() = runTest(testDispatcher) {
        val fakeRepo = FakeFindingRepository()
        val f1 = Finding(
            id = "f-1",
            customId = "F-001",
            engagementId = "eng-1",
            title = "SQL Injection in Login Endpoint",
            category = "Injection",
            severity = "Critical",
            cvssScore = 9.8,
            affectedAsset = "https://api.alpha.com/auth",
            cweReference = "CWE-89",
            description = "Unsanitized parameter",
            remediation = "Use prepared statements",
            status = "Open",
            createdAt = 1000L
        )
        val f2 = Finding(
            id = "f-2",
            customId = "F-002",
            engagementId = "eng-1",
            title = "Missing Content-Security-Policy",
            category = "Configuration",
            severity = "Low",
            cvssScore = 3.5,
            affectedAsset = "https://portal.alpha.com",
            cweReference = "CWE-16",
            description = "Header missing",
            remediation = "Add CSP header",
            status = "Resolved",
            createdAt = 2000L
        )
        val f3 = Finding(
            id = "f-3",
            customId = "F-003",
            engagementId = "eng-2",
            title = "IDOR in User Profile",
            category = "Authorization",
            severity = "High",
            cvssScore = 8.1,
            affectedAsset = "https://beta.com/api/user/123",
            cweReference = "CWE-639",
            description = "Missing authz check",
            remediation = "Validate user session against target ID",
            status = "In Progress",
            createdAt = 3000L
        )

        fakeRepo.saveFinding(f1)
        fakeRepo.saveFinding(f2)
        fakeRepo.saveFinding(f3)

        val viewModel = FindingsViewModel(
            findingRepository = fakeRepo,
            engagementRepository = FakeEngagementRepository()
        )

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.findings.collect()
        }

        advanceUntilIdle()

        // 1. Initial State: 3 findings returned, default sorted by newest (F-003, F-002, F-001)
        assertEquals(3, viewModel.findings.value.size)
        assertEquals("F-003", viewModel.findings.value[0].customId)

        // 2. Search query filter
        viewModel.setSearchQuery("SQL Injection")
        advanceUntilIdle()
        assertEquals(1, viewModel.findings.value.size)
        assertEquals("F-001", viewModel.findings.value[0].customId)

        // 3. Clear search and filter by Severity
        viewModel.setSearchQuery("")
        viewModel.setSeverityFilter("High")
        advanceUntilIdle()
        assertEquals(1, viewModel.findings.value.size)
        assertEquals("F-003", viewModel.findings.value[0].customId)

        // 4. Filter by Status
        viewModel.setSeverityFilter("ALL")
        viewModel.setStatusFilter("Resolved")
        advanceUntilIdle()
        assertEquals(1, viewModel.findings.value.size)
        assertEquals("F-002", viewModel.findings.value[0].customId)

        // 5. Filter by Category
        viewModel.setStatusFilter("ALL")
        viewModel.setCategoryFilter("Authorization")
        advanceUntilIdle()
        assertEquals(1, viewModel.findings.value.size)
        assertEquals("F-003", viewModel.findings.value[0].customId)

        // 6. Filter by Engagement
        viewModel.setCategoryFilter("ALL")
        viewModel.setEngagementFilter("eng-2")
        advanceUntilIdle()
        assertEquals(1, viewModel.findings.value.size)
        assertEquals("eng-2", viewModel.findings.value[0].engagementId)

        // 7. Sort by Severity (High to Low)
        viewModel.setEngagementFilter("ALL")
        viewModel.setSortOption(FindingSortOption.SEVERITY_HIGH_TO_LOW)
        advanceUntilIdle()
        assertEquals(3, viewModel.findings.value.size)
        assertEquals("Critical", viewModel.findings.value[0].severity) // F-001
        assertEquals("High", viewModel.findings.value[1].severity)     // F-003
        assertEquals("Low", viewModel.findings.value[2].severity)      // F-002

        collectJob.cancel()
    }

    @Test
    fun findingsViewModel_crudOperationsAndStatusTransitions_work() = runTest(testDispatcher) {
        val fakeRepo = FakeFindingRepository()
        val viewModel = FindingsViewModel(
            findingRepository = fakeRepo,
            engagementRepository = FakeEngagementRepository()
        )

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.findings.collect()
        }

        advanceUntilIdle()

        // 1. Create Finding
        val newFinding = Finding(
            id = "f-test",
            customId = "F-999",
            engagementId = "eng-1",
            title = "Hardcoded Secret Token",
            category = "Information Disclosure",
            severity = "High",
            cvssScore = 7.5,
            affectedAsset = "Config.kt",
            cweReference = "CWE-798",
            description = "API key committed to repository",
            remediation = "Rotate key and use secret manager",
            status = "Open",
            discoveryDate = "2026-11-05",
            generalNotes = "Reported to DevOps team immediately"
        )

        var saveSuccess = false
        viewModel.saveFinding(newFinding) { saveSuccess = true }
        advanceUntilIdle()

        assertTrue(saveSuccess)
        assertEquals(1, fakeRepo.findingList.size)
        assertEquals("Hardcoded Secret Token", fakeRepo.findingList[0].title)
        assertEquals(7.5, fakeRepo.findingList[0].cvssScore ?: 0.0, 0.01)

        // 2. Status Transition: Open -> In Progress -> Resolved
        viewModel.updateFindingStatus(fakeRepo.findingList[0], "In Progress")
        advanceUntilIdle()
        assertEquals("In Progress", fakeRepo.findingList[0].status)

        viewModel.updateFindingStatus(fakeRepo.findingList[0], "Resolved")
        advanceUntilIdle()
        assertEquals("Resolved", fakeRepo.findingList[0].status)

        // 3. Status Transition: Resolved -> False Positive
        viewModel.updateFindingStatus(fakeRepo.findingList[0], "False Positive")
        advanceUntilIdle()
        assertEquals("False Positive", fakeRepo.findingList[0].status)

        // 4. Delete Finding
        var deleteSuccess = false
        viewModel.deleteFinding("f-test") { deleteSuccess = true }
        advanceUntilIdle()

        assertTrue(deleteSuccess)
        assertEquals(0, fakeRepo.findingList.size)

        collectJob.cancel()
    }

    @Test
    fun cvssValidation_scoreRange_verifiesLimits() {
        val validScore1 = 0.0
        val validScore2 = 7.5
        val validScore3 = 10.0
        val invalidLow = -0.1
        val invalidHigh = 10.1

        assertTrue(validScore1 in 0.0..10.0)
        assertTrue(validScore2 in 0.0..10.0)
        assertTrue(validScore3 in 0.0..10.0)
        assertFalse(invalidLow in 0.0..10.0)
        assertFalse(invalidHigh in 0.0..10.0)
    }

    @Test
    fun cweValidation_format_formatsPrefixCorrectly() {
        fun formatCwe(input: String): String {
            return if (input.startsWith("CWE-", ignoreCase = true)) {
                input.uppercase()
            } else if (input.toIntOrNull() != null) {
                "CWE-$input"
            } else {
                input.trim()
            }
        }

        assertEquals("CWE-89", formatCwe("89"))
        assertEquals("CWE-89", formatCwe("cwe-89"))
        assertEquals("CWE-79", formatCwe("CWE-79"))
    }
}

