package com.redops.mobile.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.material3.lightColorScheme

// RedOps Enforces Professional Cyber Dark Mode by Default
private val CyberDarkColorScheme = darkColorScheme(
    primary = CyberGreen,
    onPrimary = CyberBackground,
    primaryContainer = CyberSurfaceHighlight,
    onPrimaryContainer = CyberGreenLight,
    secondary = CyberCyan,
    onSecondary = CyberBackground,
    secondaryContainer = CyberSurfaceVariant,
    onSecondaryContainer = CyberCyanLight,
    tertiary = CyberCyanLight,
    onTertiary = CyberBackground,
    background = CyberBackground,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CyberBorder,
    outlineVariant = CyberBorderSubtle,
    error = SeverityCritical,
    onError = TextPrimary
)

private val CyberLightColorScheme = lightColorScheme(
    primary = CyberGreenDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7F9E8),
    onPrimaryContainer = Color(0xFF003912),
    secondary = CyberCyanDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2F4FF),
    onSecondaryContainer = Color(0xFF003642),
    background = Color(0xFFF6F8F7),
    onBackground = Color(0xFF191C1B),
    surface = Color.White,
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFE0E5E2),
    onSurfaceVariant = Color(0xFF404944),
    outline = Color(0xFF707974),
    outlineVariant = Color(0xFFBFC9C3),
    error = SeverityCritical,
    onError = Color.White
)

@Composable
fun RedOpsTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CyberDarkColorScheme else CyberLightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = (if (darkTheme) CyberBackground else Color(0xFFF6F8F7)).toArgb()
                window.navigationBarColor = (if (darkTheme) CyberBackground else Color(0xFFF6F8F7)).toArgb()
                val windowInsetsController = WindowCompat.getInsetsController(window, view)
                windowInsetsController.isAppearanceLightStatusBars = !darkTheme
                windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
