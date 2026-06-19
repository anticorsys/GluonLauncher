// app/src/main/java/com/gluon/launcher/core/workspace/WorkspaceController.kt
package com.gluon.launcher.core.workspace

import android.content.Intent
import com.gluon.launcher.core.data.AppModel
import com.gluon.launcher.core.data.DrawerFolderItem
import com.gluon.launcher.core.data.WorkspaceItem
import com.gluon.launcher.core.data.WorkspaceWidgetItem
import com.gluon.launcher.core.data.repository.WorkspaceRepository
import com.gluon.launcher.core.data.states.PendingWidgetConfig
import com.gluon.launcher.core.theme.ThemeManager
import com.gluon.launcher.core.widget.WidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class WorkspaceController(
    internal val scope: CoroutineScope,
    internal val workspaceRepository: WorkspaceRepository,
    internal val themeManager: ThemeManager,
    internal val allApps: StateFlow<List<AppModel>>,
    internal val onWidgetConfigPending: (PendingWidgetConfig) -> Unit,
    internal val onWidgetBindingRequest: suspend (Intent) -> Unit,
    internal val onWidgetConfigureRequest: suspend (Intent) -> Unit,
    internal val setAwaitingWidgetConfigure: (Boolean) -> Unit
) {
    internal val mutableWorkspaceItems = MutableStateFlow<List<WorkspaceItem>>(emptyList())
    val workspaceItems: StateFlow<List<WorkspaceItem>> = mutableWorkspaceItems.asStateFlow()

    internal val mutableDrawerFolders = MutableStateFlow<List<DrawerFolderItem>>(emptyList())
    val drawerFolders: StateFlow<List<DrawerFolderItem>> = mutableDrawerFolders.asStateFlow()

    internal val mutableCurrentScreenCount = MutableStateFlow(1)
    val currentScreenCount: StateFlow<Int> = mutableCurrentScreenCount.asStateFlow()

    internal val mutableCurrentGridRows = MutableStateFlow(6)
    val currentGridRows: StateFlow<Int> = mutableCurrentGridRows.asStateFlow()

    var isSavingEnabled = true
    internal var saveJob: Job? = null

    init {
        loadWorkspaceItems()
        loadDrawerFolders()
        scope.launch(Dispatchers.IO) {
            mutableWorkspaceItems.collect {
                if (isSavingEnabled) scheduleSave()
            }
        }
    }

    internal fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch(Dispatchers.IO) {
            delay(2000.milliseconds)
            workspaceRepository.saveWorkspaceItems(mutableWorkspaceItems.value)
        }
    }

    fun saveNow() {
        scope.launch(Dispatchers.IO) {
            workspaceRepository.saveWorkspaceItems(mutableWorkspaceItems.value)
            workspaceRepository.saveDrawerFolders(mutableDrawerFolders.value)
        }
    }

    fun cleanOrphanWidgets() {
        scope.launch(Dispatchers.IO) {
            val validIds = mutableWorkspaceItems.value.filterIsInstance<WorkspaceWidgetItem>().map { it.appWidgetId }.toSet()
            WidgetManager.cleanOrphanWidgets(validIds)
        }
    }

    private fun loadWorkspaceItems() {
        scope.launch(Dispatchers.IO) {
            val items = workspaceRepository.loadWorkspaceItems()
            mutableWorkspaceItems.value = items
            updateScreenCountFromItems(items)
        }
    }

    fun refreshWorkspaceItems() {
        scope.launch(Dispatchers.IO) { loadWorkspaceItems() }
    }

    private fun loadDrawerFolders() {
        scope.launch(Dispatchers.IO) {
            mutableDrawerFolders.value = workspaceRepository.loadDrawerFolders()
        }
    }
}