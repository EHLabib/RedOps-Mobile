package com.redops.mobile.toolkit

import com.redops.mobile.domain.model.Finding
import com.redops.mobile.domain.model.ToolObservation
import com.redops.mobile.domain.repository.ToolObservationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class ToolObservationWorkflowTest {

    private lateinit var observationRepo: FakeToolObservationRepository

    class FakeToolObservationRepository : ToolObservationRepository {
        val list = MutableStateFlow<List<ToolObservation>>(emptyList())

        override fun getAllObservations(): Flow<List<ToolObservation>> = list

        override fun getObservationsForEngagement(engagementId: String): Flow<List<ToolObservation>> =
            list.map { obsList -> obsList.filter { it.engagementId == engagementId } }

        override suspend fun getObservationById(id: String): ToolObservation? =
            list.value.find { it.id == id }

        override suspend fun saveObservation(observation: ToolObservation) {
            list.value = list.value + observation
        }

        override suspend fun updateObservation(observation: ToolObservation) {
            list.value = list.value.map { if (it.id == observation.id) observation else it }
        }

        override suspend fun deleteObservation(id: String) {
            list.value = list.value.filter { it.id != id }
        }
    }

    @Before
    fun setup() {
        observationRepo = FakeToolObservationRepository()
    }

    @Test
    fun testSaveObservationAndPromoteToFinding() = runBlocking {
        val engagementId = "asm-acme-2026"
        val observation = ToolObservation(
            id = UUID.randomUUID().toString(),
            engagementId = engagementId,
            toolName = "HTTP Security Headers",
            toolCategory = "Web Security",
            target = "https://example.com",
            title = "Missing Content-Security-Policy (CSP) Header",
            summary = "The HTTP response does not include a Content-Security-Policy header, elevating XSS risk.",
            sourceInfo = "RedOps Security Headers Analyzer",
            rawResultJson = "{\"grade\":\"F\",\"missing\":[\"Content-Security-Policy\"]}",
            status = "Unverified"
        )

        observationRepo.saveObservation(observation)

        val saved = observationRepo.getObservationById(observation.id)
        assertNotNull(saved)
        assertEquals("Missing Content-Security-Policy (CSP) Header", saved!!.title)
        assertEquals("Unverified", saved.status)

        // Promotion workflow to Finding
        val finding = Finding(
            id = UUID.randomUUID().toString(),
            engagementId = saved.engagementId,
            title = saved.title,
            affectedAsset = saved.target,
            description = saved.summary + "\n\nRaw Evidence:\n" + saved.rawResultJson,
            severity = "Low",
            category = "Configuration",
            remediation = "Define and deploy a strict Content-Security-Policy header with nonce-based script restrictions."
        )

        // Mark observation as converted
        observationRepo.updateObservation(saved.copy(status = "Converted", convertedFindingId = finding.id))

        val updatedObs = observationRepo.getObservationById(observation.id)
        assertNotNull(updatedObs)
        assertEquals("Converted", updatedObs!!.status)
        assertEquals(finding.id, updatedObs.convertedFindingId)
        assertEquals(saved.target, finding.affectedAsset)
        assertEquals("Configuration", finding.category)
    }

    @Test
    fun testDeleteObservation() = runBlocking {
        val observation = ToolObservation(
            id = "obs-123",
            engagementId = "eng-1",
            toolName = "DNS Lookup",
            toolCategory = "Reconnaissance",
            target = "google.com",
            title = "DNS Resolution",
            summary = "Found 4 A records",
            sourceInfo = "DNS Tool"
        )
        observationRepo.saveObservation(observation)
        assertNotNull(observationRepo.getObservationById("obs-123"))

        observationRepo.deleteObservation("obs-123")
        assertNull(observationRepo.getObservationById("obs-123"))
    }
}

