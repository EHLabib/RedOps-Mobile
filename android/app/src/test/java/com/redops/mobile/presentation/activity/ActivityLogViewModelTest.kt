package com.redops.mobile.presentation.activity

import com.redops.mobile.domain.model.ActivityLog
import com.redops.mobile.domain.model.Engagement
import com.redops.mobile.domain.repository.ActivityLogRepository
import com.redops.mobile.domain.repository.EngagementRepository
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
class ActivityLogViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeActivityLogRepository : ActivityLogRepository {
        val logList = mutableListOf<ActivityLog>()
        private val _flow = MutableStateFlow<List<ActivityLog>>(emptyList())

        fun updateFlow() {
            _flow.value = logList.toList()
        }

        override fun getAllActivities(): Flow<List<ActivityLog>> = _flow

        override fun getRecentActivities(limit: Int): Flow<List<ActivityLog>> =
            flowOf(logList.take(limit))

        override fun getActivitiesForEngagement(engagementId: String): Flow<List<ActivityLog>> =
            flowOf(logList.filter { it.engagementId == engagementId })

        override suspend fun logActivity(
            title: String,
            type: String,
            statusBadge: String,
            entityId: String?,
            engagementId: String?
        ) {
            logList.add(
                ActivityLog(
                    title = title,
                    type = type,
                    statusBadge = statusBadge,
                    entityId = entityId,
                    engagementId = engagementId
                )
            )
            updateFlow()
        }

        override suspend fun clearActivities() {
            logList.clear()
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

    private fun sampleLogs() = listOf(
        ActivityLog(
            id = "act-1",
            title = "Created Engagement: Q4 External Pentest",
            type = "ENGAGEMENT",
            statusBadge = "CREATED",
            engagementId = "eng-1",
            timestamp = 1000L
        ),
        ActivityLog(
            id = "act-2",
            title = "Finding Logged: Stored XSS in Profile",
            type = "FINDING",
            statusBadge = "HIGH",
            engagementId = "eng-1",
            timestamp = 2000L
        ),
        ActivityLog(
            id = "act-3",
            title = "Note Recorded: SSLScan observed TLS 1.0",
            type = "NOTEBOOK",
            statusBadge = "TECHNICAL OBSERVATION",
            engagementId = "eng-2",
            timestamp = 3000L
        )
    )

    @Test
    fun activityLogViewModel_emitsAllActivitiesInitially() = runTest {
        val repo = FakeActivityLogRepository()
        val engRepo = FakeEngagementRepository()
        repo.logList.addAll(sampleLogs())
        repo.updateFlow()

        val viewModel = ActivityLogViewModel(repo, engRepo)

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.activities.collect()
        }
        advanceUntilIdle()

        assertEquals(3, viewModel.activities.value.size)
        collectJob.cancel()
    }

    @Test
    fun activityLogViewModel_filtersBySearchQuery() = runTest {
        val repo = FakeActivityLogRepository()
        val engRepo = FakeEngagementRepository()
        repo.logList.addAll(sampleLogs())
        repo.updateFlow()

        val viewModel = ActivityLogViewModel(repo, engRepo)

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.activities.collect()
        }
        advanceUntilIdle()

        viewModel.setSearchQuery("XSS")
        advanceUntilIdle()
        assertEquals(1, viewModel.activities.value.size)
        assertEquals("act-2", viewModel.activities.value.first().id)

        viewModel.setSearchQuery("")
        advanceUntilIdle()
        assertEquals(3, viewModel.activities.value.size)

        collectJob.cancel()
    }

    @Test
    fun activityLogViewModel_filtersByTypeAndEngagement() = runTest {
        val repo = FakeActivityLogRepository()
        val engRepo = FakeEngagementRepository()
        repo.logList.addAll(sampleLogs())
        repo.updateFlow()

        val viewModel = ActivityLogViewModel(repo, engRepo)

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.activities.collect()
        }
        advanceUntilIdle()

        viewModel.setTypeFilter("NOTEBOOK")
        advanceUntilIdle()
        assertEquals(1, viewModel.activities.value.size)
        assertEquals("act-3", viewModel.activities.value.first().id)

        viewModel.setTypeFilter("ALL")
        viewModel.setEngagementFilter("eng-1")
        advanceUntilIdle()
        assertEquals(2, viewModel.activities.value.size)

        collectJob.cancel()
    }

    @Test
    fun activityLogViewModel_clearsActivities() = runTest {
        val repo = FakeActivityLogRepository()
        val engRepo = FakeEngagementRepository()
        repo.logList.addAll(sampleLogs())
        repo.updateFlow()

        val viewModel = ActivityLogViewModel(repo, engRepo)

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.activities.collect()
        }
        advanceUntilIdle()

        assertEquals(3, viewModel.activities.value.size)

        viewModel.clearActivities()
        advanceUntilIdle()

        assertTrue(repo.logList.isEmpty())
        assertEquals(0, viewModel.activities.value.size)

        collectJob.cancel()
    }
}

