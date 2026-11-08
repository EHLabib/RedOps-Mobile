package com.redops.mobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.redops.mobile.di.AppContainer
import com.redops.mobile.di.RedOpsContainer

/**
 * RedOpsApplication: Core Application class for RedOps Mobile.
 *
 * Responsibilities:
 * - Initializes system notification channels for assessment deadlines and background sync.
 * - Sets up global error handlers and lifecycle configurations.
 */
class RedOpsApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = RedOpsContainer(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Assessment Deadlines Channel
            val assessmentChannel = NotificationChannel(
                CHANNEL_ASSESSMENTS,
                "Assessment Alerts & Deadlines",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when an assessment scope is nearing deadline or requires review."
                enableVibration(true)
            }

            // Sync Channel
            val syncChannel = NotificationChannel(
                CHANNEL_SYNC,
                "Data Synchronization",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background synchronization status with authorized backend server."
            }

            notificationManager.createNotificationChannel(assessmentChannel)
            notificationManager.createNotificationChannel(syncChannel)
        }
    }

    companion object {
        const val CHANNEL_ASSESSMENTS = "redops_assessments_channel"
        const val CHANNEL_SYNC = "redops_sync_channel"

        lateinit var instance: RedOpsApplication
            private set
    }
}
