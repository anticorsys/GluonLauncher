// app/src/main/java/com/gluon/launcher/MainActivity.kt
package com.gluon.launcher

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.gluon.launcher.core.data.states.AppState
import com.gluon.launcher.core.theme.GluonTheme
import com.gluon.launcher.core.theme.LocalWidgetConfigureLauncher
import com.gluon.launcher.core.theme.LocalWidgetReconfigureLauncher
import com.gluon.launcher.core.theme.SystemBarState
import com.gluon.launcher.core.theme.ThemeManager.Companion.MODE_DARK
import com.gluon.launcher.core.theme.ThemeManager.Companion.MODE_LIGHT
import com.gluon.launcher.core.utils.toast
import com.gluon.launcher.core.widget.WidgetManager
import com.gluon.launcher.launcher.ui.AppNavigation
import com.gluon.launcher.launcher.ui.components.GluonMorphingLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("SourceLockedOrientationActivity")
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModelFactory(application) }
    private var screenOffReceiver: BroadcastReceiver? = null

    private val widgetConfigureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val config = viewModel.pendingWidgetConfig
        if (result.resultCode == RESULT_OK && config != null) {
            viewModel.addWidgetToWorkspace(appWidgetId = config.appWidgetId, spanX = config.spanX, spanY = config.spanY, preferredScreenId = config.screenId, preferredCellX = config.cellX, preferredCellY = config.cellY, currentGridRows = viewModel.currentGridRows.value)
        } else if (config != null) { WidgetManager.deleteWidget(config.appWidgetId) }
        viewModel.pendingWidgetConfig = null
        viewModel.setAwaitingWidgetConfigure(awaiting = false)
    }

    private val widgetBindLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val config = viewModel.pendingWidgetConfig
        if (result.resultCode == RESULT_OK && config != null) {
            val providerInfo = WidgetManager.appWidgetManager.getAppWidgetInfo(config.appWidgetId)
            if (providerInfo != null && WidgetManager.needsConfiguration(providerInfo)) {
                viewModel.setAwaitingWidgetConfigure(awaiting = true)
                val intent = WidgetManager.createConfigurationIntent(config.appWidgetId, providerInfo)
                if (intent != null) {
                    try { widgetConfigureLauncher.launch(intent) } catch (_: Exception) { viewModel.addWidgetToWorkspace(config.appWidgetId, config.spanX, config.spanY, config.screenId, config.cellX, config.cellY, currentGridRows = viewModel.currentGridRows.value); toast("Настройка виджета недоступна, добавлен по умолчанию") }
                } else { viewModel.addWidgetToWorkspace(config.appWidgetId, config.spanX, config.spanY, config.screenId, config.cellX, config.cellY, currentGridRows = viewModel.currentGridRows.value) }
            } else { viewModel.addWidgetToWorkspace(config.appWidgetId, config.spanX, config.spanY, config.screenId, config.cellX, config.cellY, currentGridRows = viewModel.currentGridRows.value) }
        } else if (config != null) { WidgetManager.deleteWidget(config.appWidgetId); viewModel.pendingWidgetConfig = null }
    }

    private val widgetReconfigureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) viewModel.refreshWorkspaceItems()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        onBackPressedDispatcher.addCallback(this) {
            if (viewModel.appState.value != AppState.MAIN) {
                viewModel.setAppState(AppState.MAIN)
            } else if (viewModel.isAppDrawerOpen.value) {
                viewModel.setAppDrawerOpen(false)
            }
        }

        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.appTasks.forEach { task ->
                try { task.setExcludeFromRecents(true) } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        WidgetManager.init(application)

        lifecycleScope.launch {
            delay(1500.milliseconds)
            viewModel.cleanOrphanWidgets()
        }

        screenOffReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    viewModel.resetToHome()
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        val receiverFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ContextCompat.RECEIVER_NOT_EXPORTED else 0
        ContextCompat.registerReceiver(this, screenOffReceiver, filter, receiverFlags)

        setContent {
            val isReady by viewModel.isReady.collectAsStateWithLifecycle()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val dynamicEnabled by viewModel.dynamicColors.collectAsStateWithLifecycle()
            val appState by viewModel.appState.collectAsStateWithLifecycle()
            val allApps by viewModel.allApps.collectAsStateWithLifecycle()
            val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()

            val systemBarState = remember { SystemBarState() }

            LaunchedEffect(Unit) { viewModel.widgetBindingRequest.collect { intent -> widgetBindLauncher.launch(intent) } }

            LaunchedEffect(Unit) {
                viewModel.widgetConfigureRequest.collect { intent ->
                    try { widgetConfigureLauncher.launch(intent) } catch (_: Exception) {
                        val config = viewModel.pendingWidgetConfig
                        if (config != null) {
                            viewModel.addWidgetToWorkspace(appWidgetId = config.appWidgetId, spanX = config.spanX, spanY = config.spanY, preferredScreenId = config.screenId, preferredCellX = config.cellX, preferredCellY = config.cellY, currentGridRows = viewModel.currentGridRows.value)
                            toast("Настройка виджета недоступна, добавлен по умолчанию")
                        }
                        viewModel.pendingWidgetConfig = null; viewModel.setAwaitingWidgetConfigure(awaiting = false)
                    }
                }
            }

            LaunchedEffect(themeMode) {
                when (themeMode) {
                    MODE_LIGHT -> AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)
                    MODE_DARK -> AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)
                    else -> AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }

            CompositionLocalProvider(LocalWidgetConfigureLauncher provides widgetConfigureLauncher, LocalWidgetReconfigureLauncher provides widgetReconfigureLauncher) {
                GluonTheme(themeMode = themeMode, dynamicColor = dynamicEnabled, systemBarState = systemBarState) {
                    if (!isReady) Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { GluonMorphingLoader() }
                    else AppNavigation(appState = appState, viewModel = viewModel, authManager = viewModel.authManager, profileManager = viewModel.profileManager, settingsState = settingsState, allApps = allApps, onStateChange = { viewModel.setAppState(newState = it) })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppForegrounded()
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.appTasks.forEach { task -> try { task.setExcludeFromRecents(true) } catch (_: Exception) {} }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        WidgetManager.clearCache()
        WidgetManager.destroy()
        screenOffReceiver?.let { unregisterReceiver(it) }
    }

    override fun onPause() {
        super.onPause()
        viewModel.onAppBackgrounded()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (Intent.ACTION_MAIN == intent.action && intent.hasCategory(Intent.CATEGORY_HOME)) viewModel.onHomePressed()
    }
}