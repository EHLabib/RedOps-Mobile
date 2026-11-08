package com.redops.mobile.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardStatsTest {

    @Test
    fun resolutionProgress_calculatesCorrectly_whenFindingsExist() {
        val stats = DashboardStats(
            totalFindings = 10,
            resolvedFindings = 4,
            unresolvedFindings = 6
        )

        assertEquals(0.40f, stats.resolutionProgress, 0.001f)
    }

    @Test
    fun resolutionProgress_returnsZero_whenNoFindings() {
        val stats = DashboardStats(
            totalFindings = 0,
            resolvedFindings = 0
        )

        assertEquals(0.0f, stats.resolutionProgress, 0.001f)
    }

    @Test
    fun resolutionProgress_returnsOne_whenAllResolved() {
        val stats = DashboardStats(
            totalFindings = 5,
            resolvedFindings = 5
        )

        assertEquals(1.0f, stats.resolutionProgress, 0.001f)
    }
}

