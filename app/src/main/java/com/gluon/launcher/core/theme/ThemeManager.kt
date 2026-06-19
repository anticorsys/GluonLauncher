// app/src/main/java/com/gluon/launcher/core/theme/ThemeManager.kt
package com.gluon.launcher.core.theme

import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val Context.themeDataStore by preferencesDataStore(name = "gluon_theme_settings")

class ThemeManager(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val keyThemeMode = intPreferencesKey("theme_mode")
    private val keyDynamicColor = booleanPreferencesKey("dynamic_color")
    private val keyIsProfileGlass = booleanPreferencesKey("is_profile_glass")
    private val keyShowProfileAvatar = booleanPreferencesKey("show_profile_avatar")
    private val keyGridColumns = intPreferencesKey("grid_columns")
    private val keyShowLabels = booleanPreferencesKey("show_labels")
    private val keyDockLabels = booleanPreferencesKey("dock_labels")
    private val keyIconBorder = booleanPreferencesKey("icon_border")
    private val keyDockBarHidden = booleanPreferencesKey("dock_bar_hidden")
    private val keyDockApps = stringPreferencesKey("dock_apps")
    private val keyDefaultDockSet = booleanPreferencesKey("default_dock_set")
    private val keyPredictiveDockEnabled = booleanPreferencesKey("predictive_dock_enabled")
    private val keyAppDrawerSortMode = intPreferencesKey("app_drawer_sort_mode")
    private val keyCustomAppCategories = stringPreferencesKey("custom_app_categories")
    private val keyWorkspaceLocked = booleanPreferencesKey("workspace_locked")
    private val keyWorkspaceLabels = booleanPreferencesKey("workspace_labels")
    private val keyDynamicDockBar = booleanPreferencesKey("dynamic_dock_bar")

    private val _themeMode = MutableStateFlow(MODE_SYSTEM)
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()

    private val _dynamicColors = MutableStateFlow(true)
    val dynamicColors: StateFlow<Boolean> = _dynamicColors.asStateFlow()

    private val _isProfileGlass = MutableStateFlow(true)
    val isProfileGlass: StateFlow<Boolean> = _isProfileGlass.asStateFlow()

    private val _showProfileAvatar = MutableStateFlow(true)
    val showProfileAvatar: StateFlow<Boolean> = _showProfileAvatar.asStateFlow()

    private val _gridColumns = MutableStateFlow(4)
    val gridColumns: StateFlow<Int> = _gridColumns.asStateFlow()

    private val _showLabels = MutableStateFlow(true)
    val showLabels: StateFlow<Boolean> = _showLabels.asStateFlow()

    private val _showDockLabels = MutableStateFlow(false)
    val showDockLabels: StateFlow<Boolean> = _showDockLabels.asStateFlow()

    private val _showIconBorder = MutableStateFlow(false)
    val showIconBorder: StateFlow<Boolean> = _showIconBorder.asStateFlow()

    private val _isDockBarHidden = MutableStateFlow(false)
    val isDockBarHidden: StateFlow<Boolean> = _isDockBarHidden.asStateFlow()

    private val _dockApps = MutableStateFlow<List<String>>(emptyList())
    val dockApps: StateFlow<List<String>> = _dockApps.asStateFlow()

    private val _isPredictiveDockEnabled = MutableStateFlow(false)
    val isPredictiveDockEnabled: StateFlow<Boolean> = _isPredictiveDockEnabled.asStateFlow()

    private val _appDrawerSortMode = MutableStateFlow(0)
    val appDrawerSortMode: StateFlow<Int> = _appDrawerSortMode.asStateFlow()

    private val _customAppCategories = MutableStateFlow<Map<String, String>>(emptyMap())
    val customAppCategories: StateFlow<Map<String, String>> = _customAppCategories.asStateFlow()

    private val _isWorkspaceLocked = MutableStateFlow(false)
    val isWorkspaceLocked: StateFlow<Boolean> = _isWorkspaceLocked.asStateFlow()

    private val _showWorkspaceLabels = MutableStateFlow(true)
    val showWorkspaceLabels: StateFlow<Boolean> = _showWorkspaceLabels.asStateFlow()

    private val _isDynamicDockBar = MutableStateFlow(false)
    val isDynamicDockBar: StateFlow<Boolean> = _isDynamicDockBar.asStateFlow()

    init {
        scope.launch {
            appContext.themeDataStore.data.collect { prefs ->
                _themeMode.value = prefs[keyThemeMode] ?: MODE_SYSTEM
                _dynamicColors.value = prefs[keyDynamicColor] ?: true
                _isProfileGlass.value = prefs[keyIsProfileGlass] ?: true
                _showProfileAvatar.value = prefs[keyShowProfileAvatar] ?: true
                _gridColumns.value = (prefs[keyGridColumns] ?: 4).coerceIn(4, 5)
                _showLabels.value = prefs[keyShowLabels] ?: true
                _showDockLabels.value = prefs[keyDockLabels] ?: false
                _showIconBorder.value = prefs[keyIconBorder] ?: false
                _isDockBarHidden.value = prefs[keyDockBarHidden] ?: false
                _isPredictiveDockEnabled.value = prefs[keyPredictiveDockEnabled] ?: false
                _appDrawerSortMode.value = prefs[keyAppDrawerSortMode] ?: 0
                _isWorkspaceLocked.value = prefs[keyWorkspaceLocked] ?: false
                _showWorkspaceLabels.value = prefs[keyWorkspaceLabels] ?: true
                _isDynamicDockBar.value = prefs[keyDynamicDockBar] ?: false

                val catsString = prefs[keyCustomAppCategories] ?: ""
                _customAppCategories.value = if (catsString.isNotEmpty()) {
                    catsString.split("||")
                        .map { it.split("::") }
                        .filter { it.size == 2 }
                        .associate { parts -> parts[0] to parts[1] }
                } else {
                    emptyMap()
                }

                val savedDockApps = prefs[keyDockApps]
                val defaultAlreadySet = prefs[keyDefaultDockSet] ?: false

                if (!defaultAlreadySet) {
                    val defaults = getDefaultDockApps()
                    if (defaults.isNotEmpty()) {
                        _dockApps.value = defaults
                        appContext.themeDataStore.edit {
                            it[keyDockApps] = defaults.joinToString(",")
                            it[keyDefaultDockSet] = true
                        }
                    } else {
                        _dockApps.value = savedDockApps?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                        appContext.themeDataStore.edit { it[keyDefaultDockSet] = true }
                    }
                } else {
                    _dockApps.value = savedDockApps?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                }
            }
        }
    }

    private fun getDefaultDockApps(): List<String> {
        val pm = appContext.packageManager
        val candidates = listOf(
            "com.google.android.dialer", "com.android.dialer", "com.samsung.android.dialer",
            "com.google.android.apps.messaging", "com.android.messaging", "com.samsung.android.messaging",
            "com.android.vending", "com.android.chrome", "com.sec.android.app.sbrowser", "org.mozilla.firefox"
        )

        val available = mutableListOf<String>()
        for (pkg in candidates) {
            try {
                pm.getApplicationInfo(pkg, 0)
                val isDialer = pkg.contains("dialer")
                val isMsg = pkg.contains("messaging")
                val isBrowser = pkg.contains("chrome") || pkg.contains("browser") || pkg.contains("firefox")
                val isStore = pkg.contains("vending")

                if (isDialer && available.any { it.contains("dialer") }) continue
                if (isMsg && available.any { it.contains("messaging") }) continue
                if (isStore && available.any { it.contains("vending") }) continue
                if (isBrowser && available.any { it.contains("chrome") || it.contains("browser") || it.contains("firefox") }) continue

                available.add(pkg)
            } catch (_: PackageManager.NameNotFoundException) { }
            if (available.size >= 4) break
        }
        return available.distinct()
    }

    fun setDockApps(packages: List<String>) {
        val newList = packages.distinct().take(_gridColumns.value)
        _dockApps.value = newList
        scope.launch {
            appContext.themeDataStore.edit { it[keyDockApps] = newList.joinToString(",") }
        }
    }

    fun setCustomAppCategory(pkg: String, category: String) {
        val map = _customAppCategories.value.toMutableMap()
        map[pkg] = category
        _customAppCategories.value = map
        scope.launch {
            appContext.themeDataStore.edit { it[keyCustomAppCategories] = map.entries.joinToString("||") { e -> "${e.key}::${e.value}" } }
        }
    }

    fun setIsProfileGlass(glass: Boolean) {
        if (_isProfileGlass.value == glass) return
        _isProfileGlass.value = glass
        scope.launch { appContext.themeDataStore.edit { it[keyIsProfileGlass] = glass } }
    }

    fun setShowProfileAvatar(show: Boolean) {
        if (_showProfileAvatar.value == show) return
        _showProfileAvatar.value = show
        scope.launch { appContext.themeDataStore.edit { it[keyShowProfileAvatar] = show } }
    }

    fun setIsDockBarHidden(hidden: Boolean) {
        if (_isDockBarHidden.value == hidden) return
        _isDockBarHidden.value = hidden
        scope.launch { appContext.themeDataStore.edit { it[keyDockBarHidden] = hidden } }
    }

    fun setGridColumns(columns: Int) {
        val clamped = columns.coerceIn(4, 5)
        if (_gridColumns.value == clamped) return
        _gridColumns.value = clamped
        scope.launch {
            appContext.themeDataStore.edit { it[keyGridColumns] = clamped }
            val currentDock = _dockApps.value
            if (currentDock.size > clamped) {
                val trimmed = currentDock.take(clamped)
                _dockApps.value = trimmed
                appContext.themeDataStore.edit { it[keyDockApps] = trimmed.joinToString(",") }
            }
        }
    }

    fun setShowLabels(show: Boolean) {
        if (_showLabels.value == show) return
        _showLabels.value = show
        scope.launch { appContext.themeDataStore.edit { it[keyShowLabels] = show } }
    }

    fun setShowDockLabels(show: Boolean) {
        if (_showDockLabels.value == show) return
        _showDockLabels.value = show
        scope.launch { appContext.themeDataStore.edit { it[keyDockLabels] = show } }
    }

    fun setShowIconBorder(show: Boolean) {
        if (_showIconBorder.value == show) return
        _showIconBorder.value = show
        scope.launch { appContext.themeDataStore.edit { it[keyIconBorder] = show } }
    }

    fun setThemeMode(mode: Int) {
        if (_themeMode.value == mode) return
        _themeMode.value = mode
        scope.launch { appContext.themeDataStore.edit { it[keyThemeMode] = mode } }
    }

    fun setDynamicEnabled(enabled: Boolean) {
        if (_dynamicColors.value == enabled) return
        _dynamicColors.value = enabled
        scope.launch { appContext.themeDataStore.edit { it[keyDynamicColor] = enabled } }
    }

    fun setPredictiveDockEnabled(enabled: Boolean) {
        if (_isPredictiveDockEnabled.value == enabled) return
        _isPredictiveDockEnabled.value = enabled
        scope.launch { appContext.themeDataStore.edit { it[keyPredictiveDockEnabled] = enabled } }
    }

    fun setAppDrawerSortMode(mode: Int) {
        if (_appDrawerSortMode.value == mode) return
        _appDrawerSortMode.value = mode
        scope.launch { appContext.themeDataStore.edit { it[keyAppDrawerSortMode] = mode } }
    }

    fun setIsWorkspaceLocked(locked: Boolean) {
        if (_isWorkspaceLocked.value == locked) return
        _isWorkspaceLocked.value = locked
        scope.launch { appContext.themeDataStore.edit { it[keyWorkspaceLocked] = locked } }
    }

    fun setShowWorkspaceLabels(show: Boolean) {
        if (_showWorkspaceLabels.value == show) return
        _showWorkspaceLabels.value = show
        scope.launch { appContext.themeDataStore.edit { it[keyWorkspaceLabels] = show } }
    }

    fun setDynamicDockBar(dynamic: Boolean) {
        if (_isDynamicDockBar.value == dynamic) return
        _isDynamicDockBar.value = dynamic
        scope.launch { appContext.themeDataStore.edit { it[keyDynamicDockBar] = dynamic } }
    }

    companion object {
        const val MODE_SYSTEM = 0
        const val MODE_LIGHT = 1
        const val MODE_DARK = 2
    }
}