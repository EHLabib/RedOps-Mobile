package com.redops.mobile.presentation.notebook

import com.redops.mobile.domain.model.Engagement
import com.redops.mobile.domain.model.NotebookEntry
import com.redops.mobile.domain.repository.EngagementRepository
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
class NotebookViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeNotebookRepository : NotebookRepository {
        val entryList = mutableListOf<NotebookEntry>()
        private val _flow = MutableStateFlow<List<NotebookEntry>>(emptyList())

        fun updateFlow() {
            _flow.value = entryList.toList()
        }

        override fun getAllEntries(): Flow<List<NotebookEntry>> = _flow

        override fun getEntriesForEngagement(engagementId: String): Flow<List<NotebookEntry>> =
            flowOf(entryList.filter { it.engagementId == engagementId })

        override fun getEntryById(id: String): Flow<NotebookEntry?> =
            flowOf(entryList.find { it.id == id })

        override suspend fun saveEntry(entry: NotebookEntry) {
            entryList.removeAll { it.id == entry.id }
            entryList.add(entry)
            updateFlow()
        }

        override suspend fun updateEntry(entry: NotebookEntry) {
            val index = entryList.indexOfFirst { it.id == entry.id }
            if (index != -1) {
                entryList[index] = entry
            } else {
                entryList.add(entry)
            }
            updateFlow()
        }

        override suspend fun deleteEntry(id: String) {
            entryList.removeAll { it.id == id }
            updateFlow()
        }

        override fun searchEntries(query: String): Flow<List<NotebookEntry>> =
            flowOf(entryList.filter { it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true) })

        override suspend fun togglePin(id: String, isPinned: Boolean) {
            val index = entryList.indexOfFirst { it.id == id }
            if (index != -1) {
                entryList[index] = entryList[index].copy(isPinned = isPinned)
                updateFlow()
            }
        }

        override suspend fun toggleFavorite(id: String, isFavorite: Boolean) {
            val index = entryList.indexOfFirst { it.id == id }
            if (index != -1) {
                entryList[index] = entryList[index].copy(isFavorite = isFavorite)
                updateFlow()
            }
        }

        override suspend fun clearAll() {
            entryList.clear()
            updateFlow()
        }
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
            engagementList.add(engagement)
            updateFlow()
        }
        override suspend fun updateEngagement(engagement: Engagement) {
            val idx = engagementList.indexOfFirst { it.id == engagement.id }
            if (idx != -1) engagementList[idx] = engagement
            updateFlow()
        }
        override suspend fun deleteEngagement(id: String) {
            engagementList.removeAll { it.id == id }
            updateFlow()
        }
        override fun searchEngagements(query: String): Flow<List<Engagement>> = flowOf(emptyList())
        override suspend fun clearAll() {
            engagementList.clear()
            updateFlow()
        }
    }

    private fun sampleEntries() = listOf(
        NotebookEntry(
            id = "note-1",
            engagementId = "eng-1",
            title = "Subdomain Takeover POC",
            category = "Reconnaissance",
            entryType = "Technical Observation",
            description = "Observed dangling CNAME record pointing to nonexistent AWS S3 bucket.",
            tags = "dns,s3,takeover",
            isPinned = true,
            isFavorite = false,
            createdAt = 1000L
        ),
        NotebookEntry(
            id = "note-2",
            engagementId = "eng-1",
            title = "JWT Secret Brute Force Notes",
            category = "Vulnerability Analysis",
            entryType = "Command Reference",
            description = "flask-unsign dictionary attack completed without finding secret.",
            tags = "jwt,auth,crypto",
            isPinned = false,
            isFavorite = true,
            createdAt = 2000L
        ),
        NotebookEntry(
            id = "note-3",
            engagementId = "eng-2",
            title = "Scoping Kickoff Meeting",
            category = "General Notes",
            entryType = "Meeting Note",
            description = "Agreed testing window is between 18:00 and 04:00 UTC daily.",
            tags = "roe,meeting,schedule",
            isPinned = false,
            isFavorite = false,
            createdAt = 3000L
        )
    )

    @Test
    fun notebookViewModel_emitsAllEntriesInitially() = runTest {
        val repo = FakeNotebookRepository()
        val engRepo = FakeEngagementRepository()
        repo.entryList.addAll(sampleEntries())
        repo.updateFlow()

        val viewModel = NotebookViewModel(repo, engRepo)

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.entries.collect()
        }
        advanceUntilIdle()

        assertEquals(3, viewModel.entries.value.size)
        collectJob.cancel()
    }

    @Test
    fun notebookViewModel_filtersBySearchQuery_matchingTitleDescriptionTags() = runTest {
        val repo = FakeNotebookRepository()
        val engRepo = FakeEngagementRepository()
        repo.entryList.addAll(sampleEntries())
        repo.updateFlow()

        val viewModel = NotebookViewModel(repo, engRepo)

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.entries.collect()
        }
        advanceUntilIdle()

        // Search by title substring
        viewModel.setSearchQuery("Takeover")
        advanceUntilIdle()
        assertEquals(1, viewModel.entries.value.size)
        assertEquals("note-1", viewModel.entries.value.first().id)

        // Search by description substring
        viewModel.setSearchQuery("flask-unsign")
        advanceUntilIdle()
        assertEquals(1, viewModel.entries.value.size)
        assertEquals("note-2", viewModel.entries.value.first().id)

        // Search by tag
        viewModel.setSearchQuery("meeting")
        advanceUntilIdle()
        assertEquals(1, viewModel.entries.value.size)
        assertEquals("note-3", viewModel.entries.value.first().id)

        // Clear query
        viewModel.setSearchQuery("")
        advanceUntilIdle()
        assertEquals(3, viewModel.entries.value.size)

        collectJob.cancel()
    }

    @Test
    fun notebookViewModel_filtersByCategoryAndType() = runTest {
        val repo = FakeNotebookRepository()
        val engRepo = FakeEngagementRepository()
        repo.entryList.addAll(sampleEntries())
        repo.updateFlow()

        val viewModel = NotebookViewModel(repo, engRepo)

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.entries.collect()
        }
        advanceUntilIdle()

        viewModel.setCategoryFilter("Reconnaissance")
        advanceUntilIdle()
        assertEquals(1, viewModel.entries.value.size)
        assertEquals("note-1", viewModel.entries.value.first().id)

        viewModel.setCategoryFilter("ALL")
        viewModel.setTypeFilter("Meeting Note")
        advanceUntilIdle()
        assertEquals(1, viewModel.entries.value.size)
        assertEquals("note-3", viewModel.entries.value.first().id)

        collectJob.cancel()
    }

    @Test
    fun notebookViewModel_filtersByEngagementAndFavorites() = runTest {
        val repo = FakeNotebookRepository()
        val engRepo = FakeEngagementRepository()
        repo.entryList.addAll(sampleEntries())
        repo.updateFlow()

        val viewModel = NotebookViewModel(repo, engRepo)

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.entries.collect()
        }
        advanceUntilIdle()

        // Filter by engagement
        viewModel.setEngagementFilter("eng-2")
        advanceUntilIdle()
        assertEquals(1, viewModel.entries.value.size)
        assertEquals("note-3", viewModel.entries.value.first().id)

        // Filter by favorites
        viewModel.setEngagementFilter(null)
        viewModel.toggleFavoritesFilter()
        advanceUntilIdle()
        assertEquals(1, viewModel.entries.value.size)
        assertEquals("note-2", viewModel.entries.value.first().id)

        collectJob.cancel()
    }

    @Test
    fun notebookViewModel_segregatesPinnedAndUnpinnedEntries() = runTest {
        val repo = FakeNotebookRepository()
        val engRepo = FakeEngagementRepository()
        repo.entryList.addAll(sampleEntries())
        repo.updateFlow()

        val viewModel = NotebookViewModel(repo, engRepo)

        val pinnedJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.pinnedEntries.collect()
        }
        val unpinnedJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.unpinnedEntries.collect()
        }
        advanceUntilIdle()

        assertEquals(1, viewModel.pinnedEntries.value.size)
        assertEquals("note-1", viewModel.pinnedEntries.value.first().id)

        assertEquals(2, viewModel.unpinnedEntries.value.size)

        pinnedJob.cancel()
        unpinnedJob.cancel()
    }

    @Test
    fun notebookViewModel_togglesPinAndFavorite() = runTest {
        val repo = FakeNotebookRepository()
        val engRepo = FakeEngagementRepository()
        val entry = NotebookEntry(
            id = "note-toggle",
            title = "Toggle Target",
            category = "Other",
            entryType = "Text Note",
            description = "Testing toggling logic",
            isPinned = false,
            isFavorite = false
        )
        repo.entryList.add(entry)
        repo.updateFlow()

        val viewModel = NotebookViewModel(repo, engRepo)

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.entries.collect()
        }
        advanceUntilIdle()

        // Toggle pin
        viewModel.togglePin(entry)
        advanceUntilIdle()
        assertTrue(repo.entryList.first().isPinned)

        // Toggle favorite
        viewModel.toggleFavorite(repo.entryList.first())
        advanceUntilIdle()
        assertTrue(repo.entryList.first().isFavorite)

        collectJob.cancel()
    }

    @Test
    fun notebookViewModel_savesUpdatesAndDeletesEntry() = runTest {
        val repo = FakeNotebookRepository()
        val engRepo = FakeEngagementRepository()

        val viewModel = NotebookViewModel(repo, engRepo)

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.entries.collect()
        }
        advanceUntilIdle()

        // Save
        val newEntry = NotebookEntry(
            id = "new-01",
            title = "Discovered SUID Binary",
            category = "Privilege Escalation Notes",
            entryType = "Technical Observation",
            description = "/usr/local/bin/backup has SUID root flag."
        )
        var saved = false
        viewModel.saveEntry(newEntry) { saved = true }
        advanceUntilIdle()

        assertTrue(saved)
        assertEquals(1, viewModel.entries.value.size)

        // Update
        val updated = newEntry.copy(title = "Discovered SUID Binary (Vulnerable)")
        var updatedCallback = false
        viewModel.updateEntry(updated) { updatedCallback = true }
        advanceUntilIdle()

        assertTrue(updatedCallback)
        assertEquals("Discovered SUID Binary (Vulnerable)", viewModel.entries.value.first().title)

        // Delete
        viewModel.deleteEntry("new-01")
        advanceUntilIdle()
        assertEquals(0, viewModel.entries.value.size)

        collectJob.cancel()
    }
}

