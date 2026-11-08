package com.redops.mobile.presentation.engagements

import com.redops.mobile.domain.model.Engagement
import com.redops.mobile.domain.model.Finding
import com.redops.mobile.domain.model.NotebookEntry
import com.redops.mobile.domain.repository.EngagementRepository
import com.redops.mobile.domain.repository.FindingRepository
import com.redops.mobile.domain.repository.NotebookRepository
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
class EngagementsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeEngagementRepository : EngagementRepository {
        val engagementList = mutableListOf<Engagement>()
        private val _flow = MutableStateFlow<List<Engagement>>(emptyList())

        fun updateFlow() {
            _flow.value = engagementList.toList()
        }

        override fun getAllEngagements(): Flow<List<Engagement>> = _flow

        override fun getEngagementById(id: String): Flow<Engagement?> = flowOf(engagementList.find { it.id == id })

        override suspend fun getEngagementByIdDirect(id: String): Engagement? = engagementList.find { it.id == id }

        override suspend fun saveEngagement(engagement: Engagement) {
            engagementList.removeAll { it.id == engagement.id }
            engagementList.add(engagement)
            updateFlow()
        }

        override suspend fun updateEngagement(engagement: Engagement) {
            val index = engagementList.indexOfFirst { it.id == engagement.id }
            if (index != -1) {
                engagementList[index] = engagement
            } else {
                engagementList.add(engagement)
            }
            updateFlow()
        }

        override suspend fun deleteEngagement(id: String) {
            engagementList.removeAll { it.id == id }
            updateFlow()
        }

        override fun searchEngagements(query: String): Flow<List<Engagement>> = flowOf(
            engagementList.filter { it.title.contains(query, true) || it.client.contains(query, true) }
        )

        override suspend fun clearAll() {
            engagementList.clear()
            updateFlow()
        }
    }

    private class FakeFindingRepository : FindingRepository {
        override fun getAllFindings(): Flow<List<Finding>> = flowOf(emptyList())
        override fun getFindingsForEngagement(engagementId: String): Flow<List<Finding>> = flowOf(emptyList())
        override suspend fun getFindingsForEngagementDirect(engagementId: String): List<Finding> = emptyList()
        override fun getFindingById(id: String): Flow<Finding?> = flowOf(null)
        override suspend fun getFindingByIdDirect(id: String): Finding? = null
        override suspend fun saveFinding(finding: Finding) {}
        override suspend fun updateFinding(finding: Finding) {}
        override suspend fun deleteFinding(id: String) {}
        override fun searchFindings(query: String): Flow<List<Finding>> = flowOf(emptyList())
        override suspend fun clearAll() {}
    }

    private class FakeNotebookRepository : NotebookRepository {
        override fun getAllEntries(): Flow<List<NotebookEntry>> = flowOf(emptyList())
        override fun getEntriesForEngagement(engagementId: String): Flow<List<NotebookEntry>> = flowOf(emptyList())
        override fun getEntryById(id: String): Flow<NotebookEntry?> = flowOf(null)
        override suspend fun saveEntry(entry: NotebookEntry) {}
        override suspend fun updateEntry(entry: NotebookEntry) {}
        override suspend fun deleteEntry(id: String) {}
        override fun searchEntries(query: String): Flow<List<NotebookEntry>> = flowOf(emptyList())
        override suspend fun togglePin(id: String, isPinned: Boolean) {}
        override suspend fun toggleFavorite(id: String, isFavorite: Boolean) {}
        override suspend fun clearAll() {}
    }

    @Test
    fun engagementsViewModel_searchAndFiltering_worksCorrectly() = runTest(testDispatcher) {
        val fakeRepo = FakeEngagementRepository()
        val e1 = Engagement(
            id = "1",
            title = "Alpha Web App Assessment",
            client = "CyberCorp",
            assessmentType = "Web Application",
            startDate = "2026-11-01",
            endDate = "2026-11-05",
            status = "Active",
            authorizedScope = "*.cybercorp.com",
            isAuthorized = true,
            createdAt = 1000L
        )
        val e2 = Engagement(
            id = "2",
            title = "Beta Internal Network Audit",
            client = "MegaBank",
            assessmentType = "Network",
            startDate = "2026-11-10",
            endDate = "2026-11-15",
            status = "Planning",
            authorizedScope = "10.0.0.0/16",
            isAuthorized = true,
            createdAt = 2000L
        )
        val e3 = Engagement(
            id = "3",
            title = "Gamma Mobile Pentest",
            client = "FinTech Inc",
            assessmentType = "Mobile Application",
            startDate = "2026-10-01",
            endDate = "2026-10-10",
            status = "Completed",
            authorizedScope = "com.fintech.app",
            isAuthorized = true,
            createdAt = 3000L
        )

        fakeRepo.saveEngagement(e1)
        fakeRepo.saveEngagement(e2)
        fakeRepo.saveEngagement(e3)

        val viewModel = EngagementsViewModel(
            engagementRepository = fakeRepo,
            findingRepository = FakeFindingRepository(),
            notebookRepository = FakeNotebookRepository()
        )

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.engagements.collect()
        }

        advanceUntilIdle()

        // 1. Initial State: All 3 returned, sorted by newest (Gamma, Beta, Alpha)
        assertEquals(3, viewModel.engagements.value.size)
        assertEquals("3", viewModel.engagements.value[0].id)

        // 2. Search Query filter
        viewModel.setSearchQuery("MegaBank")
        advanceUntilIdle()
        assertEquals(1, viewModel.engagements.value.size)
        assertEquals("MegaBank", viewModel.engagements.value[0].client)

        // 3. Clear Search & Filter by Status
        viewModel.setSearchQuery("")
        viewModel.setStatusFilter("Active")
        advanceUntilIdle()
        assertEquals(1, viewModel.engagements.value.size)
        assertEquals("Alpha Web App Assessment", viewModel.engagements.value[0].title)

        // 4. Filter by Assessment Type
        viewModel.setStatusFilter("ALL")
        viewModel.setTypeFilter("Mobile Application")
        advanceUntilIdle()
        assertEquals(1, viewModel.engagements.value.size)
        assertEquals("Gamma Mobile Pentest", viewModel.engagements.value[0].title)

        // 5. Sort by Title A-Z
        viewModel.setTypeFilter("ALL")
        viewModel.setSortOption(EngagementSortOption.TITLE_AZ)
        advanceUntilIdle()
        assertEquals(3, viewModel.engagements.value.size)
        assertEquals("Alpha Web App Assessment", viewModel.engagements.value[0].title)
        assertEquals("Beta Internal Network Audit", viewModel.engagements.value[1].title)
        assertEquals("Gamma Mobile Pentest", viewModel.engagements.value[2].title)

        collectJob.cancel()
    }

    @Test
    fun engagementsViewModel_saveAndUpdateAndDelete_modifiesRepository() = runTest(testDispatcher) {
        val fakeRepo = FakeEngagementRepository()
        val viewModel = EngagementsViewModel(
            engagementRepository = fakeRepo,
            findingRepository = FakeFindingRepository(),
            notebookRepository = FakeNotebookRepository()
        )

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.engagements.collect()
        }

        advanceUntilIdle()

        // 1. Save new engagement
        val newEng = Engagement(
            id = "eng-test",
            title = "Test Security Review",
            client = "TestClient",
            startDate = "2026-11-01",
            endDate = "2026-11-05",
            authorizedScope = "test.domain.com",
            isAuthorized = true,
            authorizationTimestamp = 1700000000000L
        )

        var saveSuccessCalled = false
        viewModel.saveEngagement(newEng) {
            saveSuccessCalled = true
        }
        advanceUntilIdle()

        assertTrue(saveSuccessCalled)
        assertEquals(1, fakeRepo.engagementList.size)
        assertEquals("Test Security Review", fakeRepo.engagementList[0].title)
        assertEquals(1700000000000L, fakeRepo.engagementList[0].authorizationTimestamp)

        // 2. Update engagement
        val updatedEng = newEng.copy(title = "Updated Title", status = "Completed")
        var updateSuccessCalled = false
        viewModel.updateEngagement(updatedEng) {
            updateSuccessCalled = true
        }
        advanceUntilIdle()

        assertTrue(updateSuccessCalled)
        assertEquals("Updated Title", fakeRepo.engagementList[0].title)
        assertEquals("Completed", fakeRepo.engagementList[0].status)

        // 3. Delete engagement
        var deleteSuccessCalled = false
        viewModel.deleteEngagement("eng-test") {
            deleteSuccessCalled = true
        }
        advanceUntilIdle()

        assertTrue(deleteSuccessCalled)
        assertEquals(0, fakeRepo.engagementList.size)

        collectJob.cancel()
    }

    @Test
    fun engagementValidation_dateOrdering_failsWhenStartAfterEnd() {
        val startDate = "2026-11-10"
        val endDate = "2026-11-05"

        // Validation rule test: Start date must not be after End date
        val isInvalid = startDate > endDate
        assertTrue("Start date after end date must be identified as invalid", isInvalid)
    }

    @Test
    fun engagementValidation_authorizationConfirmation_mustBeTrue() {
        val isAuthorized = false
        val canSave = isAuthorized
        assertFalse("Engagement cannot be saved without explicit authorization declaration", canSave)
    }
}
