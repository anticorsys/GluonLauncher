package com.gluon.launcher

import android.app.Application
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gluon.launcher.core.auth.AuthManager
import com.gluon.launcher.core.auth.ProfileManager
import com.gluon.launcher.core.data.AppModel
import com.gluon.launcher.core.data.DrawerFolderItem
import com.gluon.launcher.core.data.WorkspaceItem
import com.gluon.launcher.core.data.repository.AppRepository
import com.gluon.launcher.core.data.repository.WorkspaceRepository
import com.gluon.launcher.core.data.states.AppState
import com.gluon.launcher.core.data.states.PendingWidgetConfig
import com.gluon.launcher.core.data.states.SettingsState
import com.gluon.launcher.core.predictive.PredictiveDockManager
import com.gluon.launcher.core.search.OmniSearchManager
import com.gluon.launcher.core.search.SearchResultItem
import com.gluon.launcher.core.theme.ThemeManager
import com.gluon.launcher.core.update.UpdateInfo
import com.gluon.launcher.core.update.UpdateManager
import com.gluon.launcher.core.workspace.WorkspaceController
import com.gluon.launcher.core.workspace.addAppToWorkspace
import com.gluon.launcher.core.workspace.addWidgetToWorkspace
import com.gluon.launcher.core.workspace.clearScreen
import com.gluon.launcher.core.workspace.createDrawerFolder
import com.gluon.launcher.core.workspace.deleteDrawerFolder
import com.gluon.launcher.core.workspace.deleteScreen
import com.gluon.launcher.core.workspace.increaseScreenCount
import com.gluon.launcher.core.workspace.mergeIntoFolder
import com.gluon.launcher.core.workspace.moveAppFromFolderToWorkspace
import com.gluon.launcher.core.workspace.moveWorkspaceItem
import com.gluon.launcher.core.workspace.removeFromDrawerFolder
import com.gluon.launcher.core.workspace.removeFromFolder
import com.gluon.launcher.core.workspace.removeFromFolderAndMerge
import com.gluon.launcher.core.workspace.removeHiddenWorkspaceApps
import com.gluon.launcher.core.workspace.removeWorkspaceItem
import com.gluon.launcher.core.workspace.renameDrawerFolder
import com.gluon.launcher.core.workspace.renameFolder
import com.gluon.launcher.core.workspace.resizeWidget
import com.gluon.launcher.core.workspace.revalidateWorkspaceItemsBounds
import com.gluon.launcher.core.workspace.revalidateWorkspaceItemsRows
import com.gluon.launcher.core.workspace.tryAddWidget
import com.gluon.launcher.core.workspace.updateDrawerFolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class MainViewModel(
    application: Application,
    val authManager: AuthManager,
    val themeManager: ThemeManager,
    val profileManager: ProfileManager,
    val appRepository: AppRepository,
    private val workspaceRepository: WorkspaceRepository
) : AndroidViewModel(application) {

    val predictiveDockManager = PredictiveDockManager(application)
    private val omniSearchManager = OmniSearchManager(application)
    private val updateManager = UpdateManager(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val searchResults: StateFlow<List<SearchResultItem>> = _searchResults.asStateFlow()

    private val _appState = MutableStateFlow(AppState.MAIN)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    private val _isAppDrawerOpen = MutableStateFlow(false)
    val isAppDrawerOpen: StateFlow<Boolean> = _isAppDrawerOpen.asStateFlow()

    private val _isWidgetPickerOpen = MutableStateFlow(false)
    val isWidgetPickerOpen: StateFlow<Boolean> = _isWidgetPickerOpen.asStateFlow()

    private val _closeSystemDialogsEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val closeSystemDialogsEvent = _closeSystemDialogsEvent.asSharedFlow()

    val isReady: StateFlow<Boolean> = authManager.isReady
    val allApps: StateFlow<List<AppModel>> = appRepository.appsFlow

    val themeMode = themeManager.themeMode
    val dynamicColors = themeManager.dynamicColors
    val isDockBarHidden = themeManager.isDockBarHidden
    val appDrawerSortMode = themeManager.appDrawerSortMode
    val customAppCategories = themeManager.customAppCategories

    private val _hiddenApps = MutableStateFlow(emptySet<String>())
    private val _hiddenAppModels = MutableStateFlow<Map<String, AppModel>>(emptyMap())
    val hiddenAppModels: StateFlow<Map<String, AppModel>> = _hiddenAppModels.asStateFlow()

    private val _homePressCount = MutableStateFlow(0)
    val homePressCount: StateFlow<Int> = _homePressCount.asStateFlow()

    private val _awaitingWidgetConfigure = MutableStateFlow(false)

    private val _widgetBindingRequest = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    val widgetBindingRequest: SharedFlow<Intent> = _widgetBindingRequest.asSharedFlow()

    private val _widgetConfigureRequest = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    val widgetConfigureRequest: SharedFlow<Intent> = _widgetConfigureRequest.asSharedFlow()

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    var pendingWidgetConfig by mutableStateOf<PendingWidgetConfig?>(null)
    private var searchJob: Job? = null

    private val workspaceController = WorkspaceController(
        scope = viewModelScope,
        workspaceRepository = workspaceRepository,
        themeManager = themeManager,
        allApps = allApps,
        onWidgetConfigPending = { pendingWidgetConfig = it },
        onWidgetBindingRequest = { _widgetBindingRequest.emit(it) },
        onWidgetConfigureRequest = { _widgetConfigureRequest.emit(it) },
        setAwaitingWidgetConfigure = { setAwaitingWidgetConfigure(it) }
    )

    val workspaceItems: StateFlow<List<WorkspaceItem>> = workspaceController.workspaceItems
    val drawerFolders: StateFlow<List<DrawerFolderItem>> = workspaceController.drawerFolders
    val currentScreenCount: StateFlow<Int> = workspaceController.currentScreenCount
    val currentGridRows: StateFlow<Int> = workspaceController.currentGridRows

    @OptIn(FlowPreview::class)
    val settingsState: StateFlow<SettingsState> = combine(
        themeManager.isProfileGlass, themeManager.showProfileAvatar, themeManager.dynamicColors,
        themeManager.themeMode, themeManager.dockApps, themeManager.gridColumns, themeManager.showLabels, themeManager.showDockLabels,
        themeManager.showIconBorder, _hiddenApps,
        themeManager.isDockBarHidden, themeManager.isPredictiveDockEnabled,
        themeManager.isWorkspaceLocked, themeManager.showWorkspaceLabels, themeManager.isDynamicDockBar
    ) { values: Array<Any> ->
        SettingsState(
            isProfileGlass = values[0] as? Boolean ?: true,
            showProfileAvatar = values[1] as? Boolean ?: true,
            isDynamicStyle = values[2] as? Boolean ?: true,
            themeMode = values[3] as? Int ?: ThemeManager.MODE_SYSTEM,
            dockApps = @Suppress("UNCHECKED_CAST") (values[4] as? List<String> ?: emptyList()),
            gridColumns = values[5] as? Int ?: 4,
            showLabels = values[6] as? Boolean ?: true,
            showDockLabels = values[7] as? Boolean ?: false,
            showIconBorder = values[8] as? Boolean ?: false,
            hiddenApps = @Suppress("UNCHECKED_CAST") (values[9] as? Set<String> ?: emptySet()),
            isDockBarHidden = values[10] as? Boolean ?: false,
            isPredictiveDockEnabled = values[11] as? Boolean ?: false,
            isWorkspaceLocked = values[12] as? Boolean ?: false,
            showWorkspaceLabels = values[13] as? Boolean ?: true,
            isDynamicDockBar = values[14] as? Boolean ?: false
        )
    }
        .debounce(150.milliseconds)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SettingsState(
                isProfileGlass = true,
                showProfileAvatar = true,
                isDynamicStyle = true,
                themeMode = ThemeManager.MODE_SYSTEM,
                dockApps = emptyList(),
                gridColumns = 4,
                showLabels = true,
                showDockLabels = false,
                showIconBorder = false,
                hiddenApps = emptySet(),
                isDockBarHidden = false,
                isPredictiveDockEnabled = false,
                isWorkspaceLocked = false,
                showWorkspaceLabels = true,
                isDynamicDockBar = false
            )
        )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            refreshHiddenAppsData()
            authManager.isReady.collect { ready ->
                if (ready && !authManager.isLoggedIn() && !authManager.isGuestMode()) setAppState(newState = AppState.WELCOME)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            themeManager.isPredictiveDockEnabled.collect { enabled ->
                predictiveDockManager.setPredictiveEnabled(enabled = enabled)
                if (enabled) predictiveDockManager.updatePredictions()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            if (updateManager.shouldCheckForUpdatesAutomated()) {
                _updateInfo.value = updateManager.checkForUpdates()
            }
        }
    }

    fun checkForUpdatesManual(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val info = updateManager.checkForUpdates()
            _updateInfo.value = info
            _isCheckingUpdate.value = false
            onResult(info != null)
        }
    }

    fun downloadUpdate() {
        _updateInfo.value?.let { updateManager.downloadAndInstall(it) }
    }

    fun setAppDrawerOpen(isOpen: Boolean) { _isAppDrawerOpen.value = isOpen }
    fun setWidgetPickerOpen(isOpen: Boolean) { _isWidgetPickerOpen.value = isOpen }

    fun resetToHome() {
        setAppState(newState = AppState.MAIN)
        _isAppDrawerOpen.value = false
        _isWidgetPickerOpen.value = false
        _closeSystemDialogsEvent.tryEmit(Unit)
    }

    fun setAppCategoryOverride(pkg: String, cat: String) = themeManager.setCustomAppCategory(pkg, cat)
    fun disablePredictiveDock() {
        themeManager.setPredictiveDockEnabled(enabled = false)
        predictiveDockManager.setPredictiveEnabled(enabled = false)
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            _searchResults.value = omniSearchManager.performOmniSearch(query, allApps.value)
        }
    }

    fun cycleAppDrawerSortMode() {
        val nextMode = (themeManager.appDrawerSortMode.value + 1) % 3
        themeManager.setAppDrawerSortMode(nextMode)
    }

    fun setAwaitingWidgetConfigure(awaiting: Boolean) { _awaitingWidgetConfigure.value = awaiting }

    fun onAppBackgrounded() { workspaceController.isSavingEnabled = false }
    fun onAppForegrounded() { workspaceController.isSavingEnabled = true }

    private fun refreshHiddenAppsData() {
        viewModelScope.launch(Dispatchers.IO) {
            _hiddenApps.value = appRepository.getHiddenApps()
            _hiddenAppModels.value = appRepository.getHiddenAppModels()
        }
    }

    fun cleanOrphanWidgets() = workspaceController.cleanOrphanWidgets()
    fun refreshWorkspaceItems() = workspaceController.refreshWorkspaceItems()
    fun createDrawerFolder(name: String, packages: List<String>) = workspaceController.createDrawerFolder(name, packages)
    fun updateDrawerFolder(folderId: String, packages: List<String>) = workspaceController.updateDrawerFolder(folderId, packages)
    fun deleteDrawerFolder(folderId: String) = workspaceController.deleteDrawerFolder(folderId)
    fun removeFromDrawerFolder(folderId: String, packageName: String) = workspaceController.removeFromDrawerFolder(folderId, packageName)
    fun renameDrawerFolder(folderId: String, newName: String) = workspaceController.renameDrawerFolder(folderId, newName)
    fun increaseScreenCount() = workspaceController.increaseScreenCount()
    fun mergeIntoFolder(sourceItemId: String?, sourceAppPkg: String?, screenId: Int, cellX: Int, cellY: Int) = workspaceController.mergeIntoFolder(sourceItemId, sourceAppPkg, screenId, cellX, cellY)
    fun renameFolder(folderId: String, newName: String) = workspaceController.renameFolder(folderId, newName)
    fun removeFromFolder(folderId: String, packageName: String) = workspaceController.removeFromFolder(folderId, packageName)
    fun removeFromFolderAndMerge(folderId: String, packageName: String, screenId: Int, cellX: Int, cellY: Int) = workspaceController.removeFromFolderAndMerge(folderId, packageName, screenId, cellX, cellY)
    fun moveAppFromFolderToWorkspace(folderId: String, packageName: String, screenId: Int, cellX: Int, cellY: Int, currentGridRows: Int) = workspaceController.moveAppFromFolderToWorkspace(folderId, packageName, screenId, cellX, cellY, currentGridRows)
    fun addAppToWorkspace(app: AppModel, preferredCellX: Int? = null, preferredCellY: Int? = null, targetScreen: Int = 0, currentGridRows: Int) = workspaceController.addAppToWorkspace(app, preferredCellX, preferredCellY, targetScreen, currentGridRows)
    fun tryAddWidget(providerInfo: AppWidgetProviderInfo, screenId: Int, currentGridRows: Int) = workspaceController.tryAddWidget(providerInfo, screenId, currentGridRows)
    fun addWidgetToWorkspace(appWidgetId: Int, spanX: Int, spanY: Int, preferredScreenId: Int = 0, preferredCellX: Int? = null, preferredCellY: Int? = null, currentGridRows: Int) = workspaceController.addWidgetToWorkspace(appWidgetId, spanX, spanY, preferredScreenId, preferredCellX, preferredCellY, currentGridRows)
    fun moveWorkspaceItem(itemId: String, screenId: Int, cellX: Int, cellY: Int, currentGridRows: Int) = workspaceController.moveWorkspaceItem(itemId, screenId, cellX, cellY, currentGridRows)
    fun removeWorkspaceItem(itemId: String, scatterFolder: Boolean = false, currentGridRows: Int = 6) = workspaceController.removeWorkspaceItem(itemId, scatterFolder, currentGridRows)
    fun clearScreen(screenId: Int) = workspaceController.clearScreen(screenId)
    fun deleteScreen(screenId: Int) = workspaceController.deleteScreen(screenId)
    fun resizeWidget(itemId: String, newSpanX: Int, newSpanY: Int, currentGridRows: Int) = workspaceController.resizeWidget(itemId, newSpanX, newSpanY, currentGridRows)
    fun revalidateWorkspaceItemsRows(newRows: Int) = workspaceController.revalidateWorkspaceItemsRows(newRows)

    // =========================================================================

    fun onGridColumnsChange(cols: Int, currentGridRows: Int) {
        val oldCols = themeManager.gridColumns.value
        themeManager.setGridColumns(cols)
        if (cols < oldCols) workspaceController.revalidateWorkspaceItemsBounds(cols, currentGridRows)
    }

    fun onProfileGlassToggle(value: Boolean) { themeManager.setIsProfileGlass(glass = value) }
    fun onProfileAvatarToggle(value: Boolean) { themeManager.setShowProfileAvatar(show = value) }
    fun onDynamicStyleToggle(value: Boolean) { themeManager.setDynamicEnabled(enabled = value) }
    fun onThemeModeChange(mode: Int) { themeManager.setThemeMode(mode) }
    fun onDockAppsChange(list: List<String>) { themeManager.setDockApps(list) }
    fun onShowLabelsChange(show: Boolean) { themeManager.setShowLabels(show = show) }
    fun onShowDockLabelsChange(show: Boolean) { themeManager.setShowDockLabels(show = show) }
    fun onShowIconBorderChange(show: Boolean) { themeManager.setShowIconBorder(show = show) }
    fun onUnhideApp(pkg: String) {
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.unhideApp(pkg)
            refreshHiddenAppsData()
        }
    }
    fun onDockBarHiddenToggle(hidden: Boolean) { themeManager.setIsDockBarHidden(hidden = hidden) }
    fun onWorkspaceLockedToggle(locked: Boolean) { themeManager.setIsWorkspaceLocked(locked = locked) }
    fun onShowWorkspaceLabelsChange(show: Boolean) { themeManager.setShowWorkspaceLabels(show = show) }
    fun onDynamicDockBarToggle(dynamic: Boolean) { themeManager.setDynamicDockBar(dynamic = dynamic) }

    fun setAppState(newState: AppState) { if (_appState.value != newState) _appState.value = newState }
    fun setGuestMode(isGuest: Boolean) { authManager.setGuestMode(isGuest = isGuest) }
    fun logout() {
        authManager.clearData()
        setAppState(newState = AppState.WELCOME)
    }

    fun deleteAccount(onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            profileManager.deleteAccount().onSuccess {
                authManager.clearData()
                setAppState(newState = AppState.WELCOME)
                onResult(true)
            }.onFailure { onResult(false) }
        }
    }

    fun onHomePressed() {
        setAppState(newState = AppState.MAIN)
        _homePressCount.value++
    }

    fun hideApp(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            appRepository.hideApp(packageName)
            workspaceController.removeHiddenWorkspaceApps(packageName)
            refreshHiddenAppsData()
        }
    }

    override fun onCleared() {
        super.onCleared()
        CoroutineScope(NonCancellable + Dispatchers.IO).launch {
            workspaceController.saveNow()
        }
        appRepository.cleanup()
        predictiveDockManager.unregister()
    }
}