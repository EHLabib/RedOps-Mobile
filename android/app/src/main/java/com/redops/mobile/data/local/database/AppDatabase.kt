package com.redops.mobile.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.redops.mobile.data.local.dao.*
import com.redops.mobile.data.local.entity.*

@Database(
    entities = [
        EngagementEntity::class,
        FindingEntity::class,
        NotebookEntryEntity::class,
        EvidenceRecordEntity::class,
        ActivityLogEntity::class,
        ReportHistoryEntity::class,
        ToolObservationEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun engagementDao(): EngagementDao
    abstract fun findingDao(): FindingDao
    abstract fun notebookDao(): NotebookDao
    abstract fun evidenceDao(): EvidenceDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun reportHistoryDao(): ReportHistoryDao
    abstract fun toolObservationDao(): ToolObservationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE engagements ADD COLUMN authorizationTimestamp INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE findings ADD COLUMN discoveryDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE findings ADD COLUMN generalNotes TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notebook_entries ADD COLUMN category TEXT NOT NULL DEFAULT 'General Notes'")
                db.execSQL("ALTER TABLE notebook_entries ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notebook_entries ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE activity_logs ADD COLUMN entityId TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE evidence_records ADD COLUMN originalFilename TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE evidence_records ADD COLUMN storedFilename TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE evidence_records ADD COLUMN category TEXT NOT NULL DEFAULT 'Screenshot'")
                db.execSQL("ALTER TABLE evidence_records ADD COLUMN sha256Checksum TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE evidence_records ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `report_history` (
                        `id` TEXT NOT NULL,
                        `engagementId` TEXT NOT NULL,
                        `engagementTitle` TEXT NOT NULL,
                        `clientName` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `reportType` TEXT NOT NULL,
                        `format` TEXT NOT NULL,
                        `classification` TEXT NOT NULL,
                        `filePath` TEXT NOT NULL,
                        `fileSize` INTEGER NOT NULL,
                        `findingsCount` INTEGER NOT NULL,
                        `reportVersion` TEXT NOT NULL,
                        `generatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`engagementId`) REFERENCES `engagements`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_report_history_engagementId` ON `report_history` (`engagementId`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tool_observations` (
                        `id` TEXT NOT NULL,
                        `engagementId` TEXT NOT NULL,
                        `toolName` TEXT NOT NULL,
                        `toolCategory` TEXT NOT NULL,
                        `target` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `summary` TEXT NOT NULL,
                        `sourceInfo` TEXT NOT NULL,
                        `rawResultJson` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `convertedFindingId` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`engagementId`) REFERENCES `engagements`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tool_observations_engagementId` ON `tool_observations` (`engagementId`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "redops_assessment_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
