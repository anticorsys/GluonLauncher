// --- Theme.kt ---
// app/src/main/java/com/gluon/launcher/core/theme/Theme.kt
package com.gluon.launcher.core.theme

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.gluon.launcher.theme.GluonDarkColorScheme
import com.gluon.launcher.theme.GluonLightColorScheme

class SystemBarState {
    var isDashboard = mutableStateOf(false)
    var isWallpaperDark = mutableStateOf(true)
    var isAppDrawerOpen = mutableStateOf(false)
    var openMenuCount = mutableIntStateOf(0)
}

val LocalThemeSystemBars = staticCompositionLocalOf { SystemBarState() }

val LocalWidgetConfigureLauncher = compositionLocalOf<ActivityResultLauncher<Intent>?> { null }
val LocalWidgetReconfigureLauncher = compositionLocalOf<ActivityResultLauncher<Intent>?> { null }

@Composable
fun GluonTheme(
    themeMode: Int,
    dynamicColor: Boolean = true,
    systemBarState: SystemBarState = remember { SystemBarState() },
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val systemInDark = isSystemInDarkTheme()

    val darkTheme = remember(themeMode, systemInDark) {
        when (themeMode) {
            ThemeManager.MODE_LIGHT -> false
            ThemeManager.MODE_DARK -> true
            else -> systemInDark
        }
    }

    val colorScheme = remember(darkTheme, dynamicColor) {
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> GluonDarkColorScheme
            else -> GluonLightColorScheme
        }
    }

    val window = (view.context as Activity).window
    val insetsController = WindowCompat.getInsetsController(window, view)

    @Suppress("DEPRECATION")
    LaunchedEffect(darkTheme, systemBarState.isDashboard.value, systemBarState.isWallpaperDark.value, systemBarState.isAppDrawerOpen.value) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        val useDarkIcons = if (systemBarState.isAppDrawerOpen.value) {
            !darkTheme
        } else if (systemBarState.isDashboard.value) {
            !systemBarState.isWallpaperDark.value
        } else {
            !darkTheme
        }

        insetsController.isAppearanceLightStatusBars = useDarkIcons
        insetsController.isAppearanceLightNavigationBars = useDarkIcons
    }

    CompositionLocalProvider(
        LocalThemeSystemBars provides systemBarState
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = getGluonTypography(),
            shapes = GluonShapes,
            content = content
        )
    }
}