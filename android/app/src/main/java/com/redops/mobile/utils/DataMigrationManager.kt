package com.redops.mobile.utils

import android.content.Context
import com.google.gson.Gson
import com.redops.mobile.data.local.database.AppDatabase
import com.redops.mobile.data.local.entity.ActivityLogEntity
import com.redops.mobile.data.local.entity.EngagementEntity
import com.redops.mobile.data.local.entity.FindingEntity
import com.redops.mobile.data.local.entity.NotebookEntryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Handles one-time safe migration of user records from legacy SharedPreferences/Gson
 * storage ("redops_local_v1") into the Room database.
 */
object DataMigrationManager {

    private const val PREFS_LEGACY = "redops_local_v1"
    private const val KEY_DATA = "data"
    private const val KEY_MIGRATED = "room_migration_completed_v1"

    private data class LegacyEngagement(
        val id: String = UUID.randomUUID().toString(),
        val name: String = "",
        val client: String = "",
        val scope: String = "",
        val startDate: String = "",
        val endDate: String = "",
        val status: String = "Planned",
        val authorized: Boolean = false
    )

    private data class LegacyFinding(
        val id: String = UUID.randomUUID().toString(),
        val title: String = "",
        val severity: String = "Medium",
        val asset: String = "",
        val description: String = "",
        val remediation: String = "",
        val status: String = "Open",
        val engagementId: String = ""
    )

    private data class LegacyNote(
        val id: String = UUID.randomUUID().toString(),
        val category: String = "Assessment Note",
        val title: String = "",
        val detail: String = "",
        val createdAt: String = ""
    )

    private data class LegacyAppData(
        val engagements: List<LegacyEngagement> = emptyList(),
        val findings: List<LegacyFinding> = emptyList(),
        val notes: List<LegacyNote> = emptyList()
    )

    suspend fun migrateIfNeeded(context: Context, database: AppDatabase) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_LEGACY, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_MIGRATED, false)) {
            return@withContext
        }

        val rawJson = prefs.getString(KEY_DATA, null)
        if (!rawJson.isNullOrBlank()) {
            try {
                val gson = Gson()
                val legacyData = gson.fromJson(rawJson, LegacyAppData::class.java)

                if (legacyData != null) {
                    val engagementDao = database.engagementDao()
                    val findingDao = database.findingDao()
                    val notebookDao = database.notebookDao()
                    val activityDao = database.activityLogDao()

                    // Migrate Engagements
                    for (e in legacyData.engagements) {
                        val entity = EngagementEntity(
                            id = e.id,
                            title = e.name,
                            client = e.client,
                            description = "Migrated from legacy local storage.",
                            assessmentType = "General Security Assessment",
                            startDate = e.startDate,
                            endDate = e.endDate,
                            status = e.status,
                            authorizedScope = e.scope,
                            outOfScope = "",
                            assessmentObjectives = "Assessment objectives migrated from previous record.",
                            isAuthorized = e.authorized,
                            additionalNotes = ""
                        )
                        engagementDao.insertEngagement(entity)
                    }

                    // Migrate Findings
                    for (f in legacyData.findings) {
                        val validEngagementId = if (f.engagementId.isNotBlank() &&
                            legacyData.engagements.any { it.id == f.engagementId }
                        ) {
                            f.engagementId
                        } else {
                            legacyData.engagements.firstOrNull()?.id ?: continue
                        }

                        val entity = FindingEntity(
                            id = f.id,
                            customId = "MIG-${f.id.take(4)}",
                            engagementId = validEngagementId,
                            title = f.title,
                            category = "Vulnerability",
                            severity = f.severity,
                            cvssScore = null,
                            affectedAsset = f.asset,
                            cweReference = null,
                            description = f.description,
                            technicalImpact = "Identified technical impact during engagement.",
                            evidenceReference = null,
                            pocReference = null,
                            remediation = f.remediation,
                            status = f.status
                        )
                        findingDao.insertFinding(entity)
                    }

                    // Migrate Notes
                    for (n in legacyData.notes) {
                        val targetEngagementId = legacyData.engagements.firstOrNull()?.id ?: continue
                        val entity = NotebookEntryEntity(
                            id = n.id,
                            engagementId = targetEngagementId,
                            title = n.title,
                            entryType = n.category.ifBlank { "Assessment Note" },
                            description = n.detail,
                            tags = "legacy-migrated",
                            evidenceReference = null,
                            createdAt = System.currentTimeMillis()
                        )
                        notebookDao.insertEntry(entity)
                    }

                    // Log audit entry
                    activityDao.insertActivity(
                        ActivityLogEntity(
                            id = UUID.randomUUID().toString(),
                            engagementId = null,
                            title = "Migrated ${legacyData.engagements.size} engagements from legacy storage",
                            type = "SYSTEM",
                            statusBadge = "MIGRATED"
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Mark as completed so migration never re-runs
        prefs.edit().putBoolean(KEY_MIGRATED, true).apply()
    }
}
