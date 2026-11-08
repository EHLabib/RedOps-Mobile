package com.redops.mobile.presentation.evidence

import com.redops.mobile.domain.model.Engagement
import com.redops.mobile.domain.model.EvidenceRecord
import com.redops.mobile.domain.model.Finding
import com.redops.mobile.domain.repository.EngagementRepository
import com.redops.mobile.domain.repository.EvidenceRepository
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EvidenceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeEvidenceRepository : EvidenceRepository {
        val evidenceList = mutableListOf<EvidenceRecord>()
        private val _flow = MutableStateFlow<List<EvidenceRecord>>(emptyList())

        fun updateFlow() {
            _flow.value = evidenceList.toList()
        }

        override fun getAllEvidence(): Flow<List<EvidenceRecord>> = _flow

        override fun getEvidenceForEngagement(engagementId: String): Flow<List<EvidenceRecord>> =
            flowOf(evidenceList.filter { it.engagementId == engagementId })

        override fun getEvidenceForFinding(findingId: String): Flow<List<EvidenceRecord>> =
            flowOf(evidenceList.filter { it.findingId == findingId })

        override fun getEvidenceById(id: String): Flow<EvidenceRecord?> =
            flowOf(evidenceList.find { it.id == id })

        override suspend fun getEvidenceByIdDirect(id: String): EvidenceRecord? =
            evidenceList.find { it.id == id }

        override suspend fun saveEvidence(evidence: EvidenceRecord) {
            evidenceList.removeAll { it.id == evidence.id }
            evidenceList.add(evidence)
            updateFlow()
        }

        override suspend fun updateEvidence(evidence: EvidenceRecord) {
            val index = evidenceList.indexOfFirst { it.id == evidence.id }
            if (index != -1) {
                evidenceList[index] = evidence
            } else {
                evidenceList.add(evidence)
            }
            updateFlow()
        }

        override suspend fun deleteEvidence(id: String) {
            evidenceList.removeAll { it.id == id }
            updateFlow()
        }

        override fun searchEvidence(query: String): Flow<List<EvidenceRecord>> =
            flowOf(evidenceList.filter { it.title.contains(query, true) || it.originalFilename.contains(query, true) })

        override suspend fun clearAll() {
            evidenceList.clear()
            updateFlow()
        }
    }

    private class FakeEngagementRepository : EngagementRepository {
        val engagementList = mutableListOf<Engagement>()
        private val _flow = MutableStateFlow<List<Engagement>>(emptyList())
        override fun getAllEngagements(): Flow<List<Engagement>> = _flow
        override fun getEngagementById(id: String): Flow<Engagement?> = flowOf(engagementList.find { it.id == id })
        override suspend fun getEngagementByIdDirect(id: String): Engagement? = engagementList.find { it.id == id }
        override suspend fun saveEngagement(engagement: Engagement) { engagementList.add(engagement) }
        override suspend fun updateEngagement(engagement: Engagement) {}
        override suspend fun deleteEngagement(id: String) {}
        override fun searchEngagements(query: String): Flow<List<Engagement>> = flowOf(emptyList())
        override suspend fun clearAll() {}
    }

    private class FakeFindingRepository : FindingRepository {
        val findingList = mutableListOf<Finding>()
        private val _flow = MutableStateFlow<List<Finding>>(emptyList())
        override fun getAllFindings(): Flow<List<Finding>> = _flow
        override fun getFindingsForEngagement(engagementId: String): Flow<List<Finding>> = flowOf(emptyList())
        override suspend fun getFindingsForEngagementDirect(engagementId: String): List<Finding> = emptyList()
        override fun getFindingById(id: String): Flow<Finding?> = flowOf(findingList.find { it.id == id })
        override suspend fun getFindingByIdDirect(id: String): Finding? = findingList.find { it.id == id }
        override suspend fun saveFinding(finding: Finding) { findingList.add(finding) }
        override suspend fun updateFinding(finding: Finding) {}
        override suspend fun deleteFinding(id: String) {}
        override fun searchFindings(query: String): Flow<List<Finding>> = flowOf(emptyList())
        override suspend fun clearAll() {}
    }

    private fun sampleEvidence() = listOf(
        EvidenceRecord(
            id = "ev-1",
            engagementId = "eng-1",
            findingId = "f-101",
            title = "SQL Error Screenshot",
            category = "Screenshot",
            originalFilename = "sql_error.png",
            storedFilename = "ev_01_sql_error.png",
            uriOrPath = "/data/files/ev_01.png",
            fileType = "image/png",
            fileSize = 102400L,
            sha256Checksum = "hash1",
            createdAt = 1000L
        ),
        EvidenceRecord(
            id = "ev-2",
            engagementId = "eng-1",
            findingId = null,
            title = "Nmap Port Scan Output",
            category = "Log File",
            originalFilename = "nmap_target.log",
            storedFilename = "ev_02_nmap.log",
            uriOrPath = "/data/files/ev_02.log",
            fileType = "text/plain",
            fileSize = 40960L,
            sha256Checksum = "hash2",
            createdAt = 2000L
        ),
        EvidenceRecord(
            id = "ev-3",
            engagementId = "eng-2",
            findingId = "f-202",
            title = "Executive Report Draft",
            category = "Document",
            originalFilename = "executive_summary.pdf",
            storedFilename = "ev_03_exec.pdf",
            uriOrPath = "/data/files/ev_03.pdf",
            fileType = "application/pdf",
            fileSize = 512000L,
            sha256Checksum = "hash3",
            createdAt = 3000L
        )
    )

    @Test
    fun evidenceViewModel_emitsAllEvidenceInitially() = runTest {
        val repo = FakeEvidenceRepository()
        val engRepo = FakeEngagementRepository()
        val fndRepo = FakeFindingRepository()
        repo.evidenceList.addAll(sampleEvidence())
        repo.updateFlow()

        val viewModel = EvidenceViewModel(repo, engRepo, fndRepo)

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.evidenceList.collect()
        }
        advanceUntilIdle()

        assertEquals(3, viewModel.evidenceList.value.size)
        collectJob.cancel()
    }

    @Test
    fun evidenceViewModel_filtersBySearchQuery() = runTest {
        val repo = FakeEvidenceRepository()
        val engRepo = FakeEngagementRepository()
        val fndRepo = FakeFindingRepository()
        repo.evidenceList.addAll(sampleEvidence())
        repo.updateFlow()

        val viewModel = EvidenceViewModel(repo, engRepo, fndRepo)

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.evidenceList.collect()
        }
        advanceUntilIdle()

        // By title
        viewModel.setSearchQuery("SQL")
        advanceUntilIdle()
        assertEquals(1, viewModel.evidenceList.value.size)
        assertEquals("ev-1", viewModel.evidenceList.value.first().id)

        // By filename
        viewModel.setSearchQuery("nmap_target")
        advanceUntilIdle()
        assertEquals(1, viewModel.evidenceList.value.size)
        assertEquals("ev-2", viewModel.evidenceList.value.first().id)

        viewModel.setSearchQuery("")
        advanceUntilIdle()
        assertEquals(3, viewModel.evidenceList.value.size)

        collectJob.cancel()
    }

    @Test
    fun evidenceViewModel_filtersByCategoryAndScope() = runTest {
        val repo = FakeEvidenceRepository()
        val engRepo = FakeEngagementRepository()
        val fndRepo = FakeFindingRepository()
        repo.evidenceList.addAll(sampleEvidence())
        repo.updateFlow()

        val viewModel = EvidenceViewModel(repo, engRepo, fndRepo)

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.evidenceList.collect()
        }
        advanceUntilIdle()

        // Filter category
        viewModel.setCategoryFilter("Document")
        advanceUntilIdle()
        assertEquals(1, viewModel.evidenceList.value.size)
        assertEquals("ev-3", viewModel.evidenceList.value.first().id)

        // Reset category, filter engagement
        viewModel.setCategoryFilter("ALL")
        viewModel.setEngagementFilter("eng-1")
        advanceUntilIdle()
        assertEquals(2, viewModel.evidenceList.value.size)

        // Filter finding
        viewModel.setFindingFilter("f-101")
        advanceUntilIdle()
        assertEquals(1, viewModel.evidenceList.value.size)
        assertEquals("ev-1", viewModel.evidenceList.value.first().id)

        collectJob.cancel()
    }

    @Test
    fun evidenceViewModel_updatesMetadataAndDeletes() = runTest {
        val repo = FakeEvidenceRepository()
        val engRepo = FakeEngagementRepository()
        val fndRepo = FakeFindingRepository()
        val initial = sampleEvidence().first()
        repo.evidenceList.add(initial)
        repo.updateFlow()

        val viewModel = EvidenceViewModel(repo, engRepo, fndRepo)

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.evidenceList.collect()
        }
        advanceUntilIdle()

        // Update metadata
        var updated = false
        viewModel.updateEvidenceMetadata(
            evidence = initial,
            newTitle = "SQL Injection in User Profile POC",
            newDescription = "Detailed error payload",
            newCategory = "Proof of Concept Documentation",
            newFindingId = "f-101"
        ) {
            updated = true
        }
        advanceUntilIdle()

        assertTrue(updated)
        assertEquals("SQL Injection in User Profile POC", viewModel.evidenceList.value.first().title)
        assertEquals("Proof of Concept Documentation", viewModel.evidenceList.value.first().category)

        // Delete
        var deleted = false
        viewModel.deleteEvidence("ev-1") { deleted = true }
        advanceUntilIdle()

        assertTrue(deleted)
        assertEquals(0, viewModel.evidenceList.value.size)

        collectJob.cancel()
    }

    @Test
    fun evidenceViewModel_queriesEvidenceForFindingDirectly() = runTest {
        val repo = FakeEvidenceRepository()
        val engRepo = FakeEngagementRepository()
        val fndRepo = FakeFindingRepository()
        repo.evidenceList.addAll(sampleEvidence())
        repo.updateFlow()

        val viewModel = EvidenceViewModel(repo, engRepo, fndRepo)

        val flow = viewModel.getEvidenceForFinding("f-101")
        val results = mutableListOf<EvidenceRecord>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            flow.collect { results.addAll(it) }
        }
        advanceUntilIdle()

        assertEquals(1, results.size)
        assertEquals("ev-1", results.first().id)

        job.cancel()
    }
}

