package com.redops.mobile

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.redops.mobile.utils.DataMigrationManager
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as RedOpsApplication
        val container = app.container

        // Observe screenshot protection preference to prevent screen capture & task switcher leaks
        lifecycleScope.launch {
            container.userPreferencesRepository.userPreferencesFlow.collect { prefs ->
                if (prefs.screenshotProtectionEnabled) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

        // Run safe one-time legacy data migration into Room database
        lifecycleScope.launch {
            DataMigrationManager.migrateIfNeeded(applicationContext, container.database)
        }

        setContent {
            RedOpsApp(container = container)
        }
    }
}
