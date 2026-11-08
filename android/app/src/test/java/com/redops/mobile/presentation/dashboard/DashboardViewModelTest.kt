package com.redops.mobile.presentation.dashboard

import com.redops.mobile.domain.model.ActivityLog
import com.redops.mobile.domain.model.DashboardStats
import com.redops.mobile.domain.model.Engagement
import com.redops.mobile.domain.repository.DashboardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeDashboardRepository(
        private val statsFlow: Flow<DashboardStats>,
        private val activitiesFlow: Flow<List<ActivityLog>>,
        private val engagementsFlow: Flow<List<Engagement>>
    ) : DashboardRepository {
        var loggedActivityTitle: String? = null

        override fun getDashboardStats(): Flow<DashboardStats> = statsFlow
        override fun getRecentActivities(): Flow<List<ActivityLog>> = activitiesFlow
        override fun getRecentEngagements(limit: Int): Flow<List<Engagement>> = engagementsFlow

        override suspend fun logActivity(title: String, type: String, statusBadge: String, engagementId: String?) {
            loggedActivityTitle = title
        }

        override suspend fun clearActivities() {}
    }

    @Test
    fun dashboardViewModel_initializesAndEmitsStatsCorrectly() = runTest(testDispatcher) {
        val testStats = DashboardStats(
            totalEngagements = 3,
            activeEngagements = 2,
            completedEngagements = 1,
            totalFindings = 8,
            criticalFindings = 2,
            highFindings = 3,
            mediumFindings = 2,
            lowFindings = 1,
            infoFindings = 0,
            resolvedFindings = 4,
            unresolvedFindings = 4
        )

        val testActivities = listOf(
            ActivityLog(id = "1", title = "Created Engagement Alpha", type = "ENGAGEMENT", statusBadge = "ACTIVE")
        )

        val testEngagements = listOf(
            Engagement(id = "eng-1", title = "Alpha Audit", client = "Client Alpha", startDate = "2026-11-01", endDate = "2026-11-05", authorizedScope = "*.alpha.com")
        )

        val fakeRepo = FakeDashboardRepository(
            statsFlow = flowOf(testStats),
            activitiesFlow = flowOf(testActivities),
            engagementsFlow = flowOf(testEngagements)
        )

        val viewModel = DashboardViewModel(fakeRepo)

        // Active collector to trigger WhileSubscribed
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertTrue("UiState should be Success but was $uiState", uiState is DashboardUiState.Success)

        val successState = uiState as DashboardUiState.Success
        assertEquals(3, successState.stats.totalEngagements)
        assertEquals(2, successState.stats.activeEngagements)
        assertEquals(1, successState.stats.completedEngagements)
        assertEquals(8, successState.stats.totalFindings)
        assertEquals(2, successState.stats.criticalFindings)
        assertEquals(3, successState.stats.highFindings)
        assertEquals(4, successState.stats.resolvedFindings)
        assertEquals(4, successState.stats.unresolvedFindings)

        assertEquals(1, successState.recentActivities.size)
        assertEquals("Created Engagement Alpha", successState.recentActivities[0].title)

        assertEquals(1, successState.recentEngagements.size)
        assertEquals("Alpha Audit", successState.recentEngagements[0].title)

        collectJob.cancel()
    }

    @Test
    fun dashboardViewModel_logQuickAction_callsRepository() = runTest(testDispatcher) {
        val fakeRepo = FakeDashboardRepository(
            statsFlow = flowOf(DashboardStats()),
            activitiesFlow = flowOf(emptyList()),
            engagementsFlow = flowOf(emptyList())
        )

        val viewModel = DashboardViewModel(fakeRepo)
        viewModel.logQuickAction("Export Report Triggered")

        advanceUntilIdle()

        assertEquals("Export Report Triggered", fakeRepo.loggedActivityTitle)
    }
}
