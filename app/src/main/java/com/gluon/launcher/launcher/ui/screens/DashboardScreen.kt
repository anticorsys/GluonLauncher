package com.gluon.launcher.launcher.ui.screens

import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gluon.launcher.core.accessibility.NotificationPanelAccessibilityService
import com.gluon.launcher.core.data.AppModel
import com.gluon.launcher.core.data.WorkspaceAppItem
import com.gluon.launcher.core.data.WorkspaceFolderItem
import com.gluon.launcher.core.data.WorkspaceItem
import com.gluon.launcher.core.data.WorkspaceWidgetItem
import com.gluon.launcher.core.data.states.PendingWidgetConfig
import com.gluon.launcher.core.theme.LocalThemeSystemBars
import com.gluon.launcher.core.theme.LocalWidgetConfigureLauncher
import com.gluon.launcher.core.theme.LocalWidgetReconfigureLauncher
import com.gluon.launcher.core.utils.GridValidator
import com.gluon.launcher.core.utils.WallpaperBrightnessManager
import com.gluon.launcher.core.utils.launchApp
import com.gluon.launcher.core.utils.toast
import com.gluon.launcher.core.widget.WidgetManager
import com.gluon.launcher.launcher.components.ProfileGlassCard
import com.gluon.launcher.launcher.ui.components.AppDrawerSheet
import com.gluon.launcher.launcher.ui.components.GluonAnimatedDropdownMenu
import com.gluon.launcher.launcher.ui.components.MainDockBar
import com.gluon.launcher.launcher.ui.components.WallpaperActionPopup
import com.gluon.launcher.launcher.ui.components.WidgetPickerSheet
import com.gluon.launcher.launcher.ui.screens.dashboard.DashboardSystemDialogs
import com.gluon.launcher.launcher.ui.screens.dashboard.OpenedWorkspaceFolderOverlay
import com.gluon.launcher.launcher.ui.screens.dashboard.dashboardSwipeGestures
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun DashboardScreen(params: DashboardScreenParams) {
    val viewModel = params.viewModel
    val dockApps = params.dockApps
    val allApps = params.allApps
    val gridColumns = params.gridColumns
    val showLabels = params.showLabels
    val showDockLabels = params.showDockLabels
    val showIconBorder = params.showIconBorder
    val isDockBarHidden = params.isDockBarHidden
    val onDockAppsChange = params.onDockAppsChange
    val onHideApp = params.onHideApp
    val onOpenSettings = params.onOpenSettings
    val onToggleDockBarHidden = params.onToggleDockBarHidden
    val homePressCount = params.homePressCount
    val isProfileGlass = params.isProfileGlass
    val showProfileAvatar = params.showProfileAvatar
    val userName = params.userName
    val avatarUrl = params.avatarUrl
    val onToggleInfoPanel = params.onToggleInfoPanel
    val isWorkspaceLocked = params.isWorkspaceLocked
    val showWorkspaceLabels = params.showWorkspaceLabels
    val isDynamicDockBar = params.isDynamicDockBar

    val context = LocalContext.current
    val systemBars = LocalThemeSystemBars.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val windowInfo = LocalWindowInfo.current
    val widgetConfigureLauncher = LocalWidgetConfigureLauncher.current
    val widgetReconfigureLauncher = LocalWidgetReconfigureLauncher.current

    val brightnessManager = remember { WallpaperBrightnessManager(context) }
    DisposableEffect(Unit) { onDispose { brightnessManager.onDispose() } }
    LaunchedEffect(brightnessManager) { brightnessManager.update(); brightnessManager.isDark.collect { systemBars.isWallpaperDark.value = it } }

    val effectiveGridRows = remember(isProfileGlass, isDockBarHidden) { when { isProfileGlass && !isDockBarHidden -> 7; !isProfileGlass && isDockBarHidden -> 9; else -> 8 } }
    LaunchedEffect(effectiveGridRows) { viewModel.revalidateWorkspaceItemsRows(effectiveGridRows) }

    val showAppDrawer by viewModel.isAppDrawerOpen.collectAsStateWithLifecycle()
    val showWidgetPicker by viewModel.isWidgetPickerOpen.collectAsStateWithLifecycle()
    var showWorkspaceMenu by remember { mutableStateOf(false) }
    var workspaceMenuOffset by remember { mutableStateOf(DpOffset.Zero) }
    var showDeleteScreenDialog by remember { mutableStateOf(false) }
    var showClearScreenDialog by remember { mutableStateOf(false) }
    var pendingCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var openedFolderId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.closeSystemDialogsEvent.collect {
            showWorkspaceMenu = false
            showDeleteScreenDialog = false
            showClearScreenDialog = false
            openedFolderId = null
        }
    }

    LaunchedEffect(showAppDrawer) { systemBars.isAppDrawerOpen.value = showAppDrawer; if (showAppDrawer) { pendingCell = null; openedFolderId = null } }
    LaunchedEffect(showWorkspaceMenu) { if (!showWorkspaceMenu) pendingCell = null }
    LaunchedEffect(homePressCount) { viewModel.setAppDrawerOpen(isOpen = false); openedFolderId = null }
    BackHandler(enabled = showAppDrawer) { viewModel.setAppDrawerOpen(isOpen = false) }
    BackHandler(enabled = openedFolderId != null) { openedFolderId = null }

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val predictedApps by viewModel.predictiveDockManager.predictedApps.collectAsStateWithLifecycle()
    val isPredictiveEnabled by viewModel.predictiveDockManager.isPredictiveEnabled.collectAsStateWithLifecycle()

    val appDrawerSortMode by viewModel.appDrawerSortMode.collectAsStateWithLifecycle()
    val customAppCategories by viewModel.customAppCategories.collectAsStateWithLifecycle()
    val drawerFolders by viewModel.drawerFolders.collectAsStateWithLifecycle()

    var effectiveDockApps by remember { mutableStateOf(dockApps) }
    LaunchedEffect(dockApps) { effectiveDockApps = dockApps }
    val currentDockApps = if (isPredictiveEnabled && predictedApps.isNotEmpty()) predictedApps else effectiveDockApps

    val workspaceItems by viewModel.workspaceItems.collectAsStateWithLifecycle()
    val screenCount by viewModel.currentScreenCount.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { screenCount })

    var draggedApp by remember { mutableStateOf<AppModel?>(null) }
    var draggedWorkspaceItem by remember { mutableStateOf<WorkspaceItem?>(null) }
    var dragSource by remember { mutableStateOf(DragSource.DRAWER) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var draggedFromFolderId by remember { mutableStateOf<String?>(null) }

    val openedFolder = workspaceItems.find { it.id == openedFolderId } as? WorkspaceFolderItem
        ?: (workspaceItems.find { it.id == draggedFromFolderId } as? WorkspaceFolderItem)

    val gridWindowPosRef = remember { FloatArray(2) }
    var gridSize by remember { mutableStateOf(IntSize.Zero) }
    var currentPointerPosition by remember { mutableStateOf(Offset.Zero) }
    val dockBarWindowPosRef = remember { FloatArray(2) }
    var dockBarSize by remember { mutableStateOf(IntSize.Zero) }

    var targetCellRect by remember { mutableStateOf<Rect?>(null) }
    var targetDockInsertIndex by remember { mutableIntStateOf(-1) }
    var draggedWidgetProvider by remember { mutableStateOf<Pair<Int, AppWidgetProviderInfo>?>(null) }
    var isResizingWidget by remember { mutableStateOf(false) }
    var isHoveringDockBar by remember { mutableStateOf(false) }

    val currentGridColumns by rememberUpdatedState(gridColumns)
    val currentEffectiveGridRows by rememberUpdatedState(effectiveGridRows)
    val currentIsDockBarHidden by rememberUpdatedState(isDockBarHidden)

    val cellPixelWidth = if (gridSize.width > 0) gridSize.width / currentGridColumns else 0
    val cellPixelHeight = if (gridSize.height > 0) gridSize.height / effectiveGridRows else 0

    LaunchedEffect(pagerState.currentPage) { targetCellRect = null; targetDockInsertIndex = -1; pendingCell = null; openedFolderId = null }

    val isDragging by remember { derivedStateOf { draggedApp != null || draggedWorkspaceItem != null || draggedWidgetProvider != null } }
    val isFolderDragActive by remember { derivedStateOf { isDragging && dragSource == DragSource.WORKSPACE_FOLDER } }

    LaunchedEffect(isDragging) {
        if (isDragging) {
            while (true) {
                if (!isHoveringDockBar) {
                    val width = windowInfo.containerSize.width.toFloat()
                    val edgeThreshold = with(density) { 40.dp.toPx() }
                    if (currentPointerPosition.x > width - edgeThreshold) {
                        delay(400.milliseconds)
                        if (currentPointerPosition.x > width - edgeThreshold) {
                            if (pagerState.currentPage < screenCount - 1) pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            else if (screenCount < 5 && !isWorkspaceLocked) { viewModel.increaseScreenCount(); delay(100.milliseconds); pagerState.animateScrollToPage(screenCount) }
                        }
                    } else if (currentPointerPosition.x < edgeThreshold && pagerState.currentPage > 0) {
                        delay(400.milliseconds)
                        if (currentPointerPosition.x < edgeThreshold) pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                }
                delay(50.milliseconds)
            }
        }
    }

    LaunchedEffect(pagerState.currentPage, isDragging) { if (!isDragging) { targetCellRect = null; targetDockInsertIndex = -1; isHoveringDockBar = false } }

    var showAccessibilityDialog by remember { mutableStateOf(false) }
    fun openNotificationPanel() { if (NotificationPanelAccessibilityService.isServiceEnabled()) NotificationPanelAccessibilityService.openNotificationPanel() else showAccessibilityDialog = true }
    fun handleConfigureWidget(item: WorkspaceWidgetItem) {
        if (isWorkspaceLocked) return
        val info = WidgetManager.appWidgetManager.getAppWidgetInfo(item.appWidgetId) ?: return
        val intent = WidgetManager.createConfigurationIntent(item.appWidgetId, info)
        if (intent != null && widgetReconfigureLauncher != null) { try { widgetReconfigureLauncher.launch(intent) } catch (_: Exception) { context.toast("Невозможно открыть") } }
    }

    val predictiveToastMessage = "Вы изменили иконки вручную. Умный док-бар отключен."
    val removeFromDock: (String) -> Unit = { pkg -> if (isPredictiveEnabled) { viewModel.disablePredictiveDock(); context.toast(predictiveToastMessage) }; val newList = effectiveDockApps.toMutableList(); newList.remove(pkg); effectiveDockApps = newList; onDockAppsChange(newList) }
    val moveLeft: (String) -> Unit = { pkg -> if (isPredictiveEnabled) { viewModel.disablePredictiveDock(); context.toast(predictiveToastMessage) }; val list = effectiveDockApps.toMutableList(); val idx = list.indexOf(pkg); if (idx > 0) { val temp = list[idx - 1]; list[idx - 1] = list[idx]; list[idx] = temp; effectiveDockApps = list; onDockAppsChange(list) } }
    val moveRight: (String) -> Unit = { pkg -> if (isPredictiveEnabled) { viewModel.disablePredictiveDock(); context.toast(predictiveToastMessage) }; val list = effectiveDockApps.toMutableList(); val idx = list.indexOf(pkg); if (idx != -1 && idx < list.size - 1) { val temp = list[idx + 1]; list[idx + 1] = list[idx]; list[idx] = temp; effectiveDockApps = list; onDockAppsChange(list) } }
    val toggleDockApp: (String) -> Unit = { pkg ->
        if (isPredictiveEnabled) { viewModel.disablePredictiveDock(); context.toast(predictiveToastMessage) }
        val newList = effectiveDockApps.toMutableList()
        if (pkg in effectiveDockApps) {
            newList.remove(pkg)
        } else {
            if (newList.size < currentGridColumns) newList.add(pkg) else context.toast("Док-бар заполнен")
        }
        effectiveDockApps = newList
        onDockAppsChange(newList)
    }

    val dragGestureModifier = Modifier.pointerInput(Unit, isResizingWidget, isWorkspaceLocked) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull() ?: continue
                currentPointerPosition = change.position

                when {
                    draggedApp != null -> {
                        dragPosition = change.position
                        val overDockBar = isInDockBar(change.position, Offset(dockBarWindowPosRef[0], dockBarWindowPosRef[1]), dockBarSize, currentIsDockBarHidden)
                        if (isHoveringDockBar != overDockBar) isHoveringDockBar = overDockBar
                        if (overDockBar) { targetDockInsertIndex = calculateDockInsertIndex(change.position, Offset(dockBarWindowPosRef[0], dockBarWindowPosRef[1]), dockBarSize, currentDockApps.size - (if (dragSource == DragSource.DOCK) 1 else 0), currentGridColumns); targetCellRect = null }
                        else if (!isWorkspaceLocked) { targetDockInsertIndex = -1; targetCellRect = updateTargetCellLocal(change.position, Offset(gridWindowPosRef[0], gridWindowPosRef[1]), gridSize, currentGridColumns, currentEffectiveGridRows, null, density) }
                        else { targetDockInsertIndex = -1; targetCellRect = null }
                    }
                    draggedWorkspaceItem != null -> {
                        dragPosition = change.position
                        val overDockBar = isInDockBar(change.position, Offset(dockBarWindowPosRef[0], dockBarWindowPosRef[1]), dockBarSize, currentIsDockBarHidden)
                        if (isHoveringDockBar != overDockBar) isHoveringDockBar = overDockBar
                        if (overDockBar && draggedWorkspaceItem is WorkspaceAppItem) { targetDockInsertIndex = calculateDockInsertIndex(change.position, Offset(dockBarWindowPosRef[0], dockBarWindowPosRef[1]), dockBarSize, currentDockApps.size - (if (dragSource == DragSource.DOCK) 1 else 0), currentGridColumns); targetCellRect = null }
                        else if (!isWorkspaceLocked) { targetDockInsertIndex = -1; targetCellRect = updateTargetCellLocal(change.position, Offset(gridWindowPosRef[0], gridWindowPosRef[1]), gridSize, currentGridColumns, currentEffectiveGridRows, draggedWorkspaceItem, density) }
                        else { targetDockInsertIndex = -1; targetCellRect = null }
                    }
                    draggedWidgetProvider != null -> {
                        dragPosition = change.position
                        if (isHoveringDockBar) isHoveringDockBar = false
                        targetDockInsertIndex = -1
                        if (!isWorkspaceLocked) {
                            val minSpans = WidgetManager.getMinSpans()
                            val safeSpanX = minSpans.first.coerceAtMost(currentGridColumns)
                            val safeSpanY = minSpans.second.coerceAtMost(currentEffectiveGridRows)
                            targetCellRect = updateTargetCellLocal(change.position, Offset(gridWindowPosRef[0], gridWindowPosRef[1]), gridSize, currentGridColumns, currentEffectiveGridRows, WorkspaceWidgetItem("", 0, 0, 0, 0, safeSpanX, safeSpanY), density)
                        } else { targetCellRect = null }
                    }
                }

                if (!event.changes.any { it.pressed }) {
                    isHoveringDockBar = false
                    val dropPos = dragPosition
                    val onDockBar = isInDockBar(dropPos, Offset(dockBarWindowPosRef[0], dockBarWindowPosRef[1]), dockBarSize, currentIsDockBarHidden)
                    val localX = dropPos.x - gridWindowPosRef[0]
                    val localY = dropPos.y - gridWindowPosRef[1]
                    val marginY = 150f
                    val marginX = with(density) { 20.dp.toPx() }
                    val gridWidth = gridSize.width - (marginX * 2)

                    val inGrid = gridSize.width > 0 && gridSize.height > 0 && localX in -50f..(gridSize.width + 50f) && localY in -50f..(gridSize.height + marginY)

                    val getTargetCell = { spanX: Int, spanY: Int ->
                        if (inGrid && !isWorkspaceLocked) {
                            val cellWidth = gridWidth / currentGridColumns
                            val cellHeight = gridSize.height.toFloat() / currentEffectiveGridRows
                            val startCol = ((localX - marginX - (cellWidth * spanX) / 2) / cellWidth).roundToInt().coerceIn(0, maxOf(0, currentGridColumns - spanX))
                            val startRow = ((localY - (cellHeight * spanY) / 2) / cellHeight).roundToInt().coerceIn(0, maxOf(0, currentEffectiveGridRows - spanY))
                            startCol to startRow
                        } else null
                    }

                    when {
                        draggedWidgetProvider != null -> {
                            val (appWidgetId, providerInfo) = draggedWidgetProvider!!
                            if (!onDockBar && inGrid && !isWorkspaceLocked) {
                                val minSpans = WidgetManager.getMinSpans()
                                val safeSpanX = minSpans.first.coerceAtMost(currentGridColumns)
                                val safeSpanY = minSpans.second.coerceAtMost(currentEffectiveGridRows)
                                val targetCell = getTargetCell(safeSpanX, safeSpanY)
                                if (targetCell != null) {
                                    if (WidgetManager.needsConfiguration(providerInfo)) {
                                        viewModel.pendingWidgetConfig = PendingWidgetConfig(appWidgetId, safeSpanX, safeSpanY, pagerState.currentPage, targetCell.first, targetCell.second)
                                        viewModel.setAwaitingWidgetConfigure(awaiting = true)
                                        val intent = WidgetManager.createConfigurationIntent(appWidgetId, providerInfo)
                                        if (intent != null && widgetConfigureLauncher != null) { try { widgetConfigureLauncher.launch(intent) } catch (_: Exception) { viewModel.addWidgetToWorkspace(appWidgetId, safeSpanX, safeSpanY, pagerState.currentPage, targetCell.first, targetCell.second, currentGridRows = currentEffectiveGridRows); viewModel.setAwaitingWidgetConfigure(awaiting = false) } }
                                        else { WidgetManager.deleteWidget(appWidgetId); viewModel.setAwaitingWidgetConfigure(awaiting = false) }
                                    } else { viewModel.addWidgetToWorkspace(appWidgetId, safeSpanX, safeSpanY, pagerState.currentPage, targetCell.first, targetCell.second, currentGridRows = currentEffectiveGridRows) }
                                } else WidgetManager.deleteWidget(appWidgetId)
                            } else WidgetManager.deleteWidget(appWidgetId)
                            draggedWidgetProvider = null
                        }
                        draggedWorkspaceItem != null -> {
                            val item = draggedWorkspaceItem!!
                            if (!onDockBar && inGrid && !isWorkspaceLocked) {
                                val targetCell = getTargetCell(item.spanX, item.spanY)
                                if (targetCell != null) {
                                    val overlapping = GridValidator.getOverlappingItems(workspaceItems, pagerState.currentPage, targetCell.first, targetCell.second, item.spanX, item.spanY, setOf(item.id))
                                    val isMerge = overlapping.size == 1 && (overlapping[0] is WorkspaceAppItem || overlapping[0] is WorkspaceFolderItem) && item is WorkspaceAppItem
                                    if (isMerge) {
                                        viewModel.mergeIntoFolder(item.id, item.packageName, pagerState.currentPage, targetCell.first, targetCell.second)
                                    } else {
                                        viewModel.moveWorkspaceItem(item.id, pagerState.currentPage, targetCell.first, targetCell.second, currentEffectiveGridRows)
                                    }
                                }
                            } else if (onDockBar && item is WorkspaceAppItem) {
                                if (isPredictiveEnabled) { viewModel.disablePredictiveDock(); context.toast(predictiveToastMessage) }
                                val newList = effectiveDockApps.toMutableList()
                                val insertIdx = targetDockInsertIndex
                                if (!newList.contains(item.packageName)) {
                                    if (newList.size >= currentGridColumns) { val removeIdx = if (insertIdx < newList.size) insertIdx else newList.size - 1; newList.removeAt(removeIdx) }
                                    newList.add(insertIdx.coerceIn(0, newList.size), item.packageName)
                                    effectiveDockApps = newList; onDockAppsChange(newList)
                                    if (!isWorkspaceLocked) viewModel.removeWorkspaceItem(item.id)
                                }
                            }
                            draggedWorkspaceItem = null
                        }
                        draggedApp != null -> {
                            val app = draggedApp!!
                            if (!onDockBar && inGrid && !isWorkspaceLocked) {
                                val targetCell = getTargetCell(1, 1)
                                if (targetCell != null) {
                                    val overlapping = GridValidator.getOverlappingItems(workspaceItems, pagerState.currentPage, targetCell.first, targetCell.second, 1, 1)
                                    val isMerge = overlapping.size == 1 && (overlapping[0] is WorkspaceAppItem || overlapping[0] is WorkspaceFolderItem)

                                    if (dragSource == DragSource.WORKSPACE_FOLDER && draggedFromFolderId != null) {
                                        if (isMerge) {
                                            viewModel.removeFromFolderAndMerge(draggedFromFolderId!!, app.packageName, pagerState.currentPage, targetCell.first, targetCell.second)
                                        } else {
                                            viewModel.moveAppFromFolderToWorkspace(draggedFromFolderId!!, app.packageName, pagerState.currentPage, targetCell.first, targetCell.second, currentEffectiveGridRows)
                                        }
                                    } else {
                                        if (isMerge) {
                                            viewModel.mergeIntoFolder(null, app.packageName, pagerState.currentPage, targetCell.first, targetCell.second)
                                        } else {
                                            viewModel.addAppToWorkspace(app, targetCell.first, targetCell.second, pagerState.currentPage, currentEffectiveGridRows)
                                        }
                                    }
                                }
                                if (dragSource == DragSource.DOCK) {
                                    if (isPredictiveEnabled) { viewModel.disablePredictiveDock(); context.toast(predictiveToastMessage) }
                                    val newList = effectiveDockApps.toMutableList(); newList.remove(app.packageName); effectiveDockApps = newList; onDockAppsChange(newList)
                                }
                            } else if (onDockBar) {
                                if (isPredictiveEnabled) { viewModel.disablePredictiveDock(); context.toast(predictiveToastMessage) }
                                val newList = effectiveDockApps.toMutableList()
                                val insertIdx = targetDockInsertIndex

                                if (dragSource == DragSource.WORKSPACE_FOLDER && draggedFromFolderId != null) {
                                    viewModel.removeFromFolder(draggedFromFolderId!!, app.packageName)
                                }

                                if (dragSource == DragSource.DOCK) { newList.remove(app.packageName); newList.add(insertIdx.coerceIn(0, newList.size), app.packageName) }
                                else { if (!newList.contains(app.packageName)) { if (newList.size >= currentGridColumns) { val removeIdx = if (insertIdx < newList.size) insertIdx else newList.size - 1; newList.removeAt(removeIdx) }; newList.add(insertIdx.coerceIn(0, newList.size), app.packageName) } }
                                effectiveDockApps = newList; onDockAppsChange(newList)
                            }
                            draggedApp = null
                        }
                    }
                    targetCellRect = null; targetDockInsertIndex = -1
                    if (draggedFromFolderId != null) {
                        openedFolderId = null
                        draggedFromFolderId = null
                    }
                }
            }
        }
    }

    val currentShowAppDrawer by rememberUpdatedState(showAppDrawer)
    val currentShowWidgetPicker by rememberUpdatedState(showWidgetPicker)
    val currentIsResizingWidget by rememberUpdatedState(isResizingWidget)
    val currentIsDragging by rememberUpdatedState(isDragging)
    val currentShowWorkspaceMenu by rememberUpdatedState(showWorkspaceMenu)
    val currentOpenedFolder by rememberUpdatedState(openedFolder)
    val currentShowDeleteScreenDialog by rememberUpdatedState(showDeleteScreenDialog)
    val currentShowClearScreenDialog by rememberUpdatedState(showClearScreenDialog)
    val currentShowAccessibilityDialog by rememberUpdatedState(showAccessibilityDialog)
    val currentWorkspaceItems by rememberUpdatedState(workspaceItems)
    val currentPage by rememberUpdatedState(pagerState.currentPage)
    val currentCellPixelWidth by rememberUpdatedState(cellPixelWidth)
    val currentCellPixelHeight by rememberUpdatedState(cellPixelHeight)

    // ИСПРАВЛЕНИЕ: Вызов делегированного Gesture Modifier (существенно облегчает файл)
    val swipeGesture = Modifier.dashboardSwipeGestures(
        context = context,
        density = density,
        gridSize = gridSize,
        gridWindowPosRef = gridWindowPosRef,
        currentCellPixelWidth = currentCellPixelWidth,
        currentCellPixelHeight = currentCellPixelHeight,
        currentWorkspaceItems = currentWorkspaceItems,
        currentPage = currentPage,
        haptic = haptic,
        wasMenuOpenAtStartProvider = {
            currentShowAppDrawer || currentShowWidgetPicker || currentIsResizingWidget ||
                    currentIsDragging || currentShowWorkspaceMenu || systemBars.openMenuCount.intValue > 0 ||
                    currentShowDeleteScreenDialog || currentShowClearScreenDialog || currentShowAccessibilityDialog ||
                    currentOpenedFolder != null
        },
        onSwipeUp = { viewModel.setAppDrawerOpen(isOpen = true) },
        onSwipeDown = { openNotificationPanel() }
    )

    DashboardSystemDialogs(
        showDeleteScreenDialog = showDeleteScreenDialog,
        showClearScreenDialog = showClearScreenDialog,
        showAccessibilityDialog = showAccessibilityDialog,
        onDismissDelete = { showDeleteScreenDialog = false },
        onConfirmDelete = {
            showDeleteScreenDialog = false
            val c = pagerState.currentPage
            viewModel.deleteScreen(c)
            scope.launch { delay(50.milliseconds); pagerState.animateScrollToPage((c - 1).coerceAtLeast(0)) }
        },
        onDismissClear = { showClearScreenDialog = false },
        onConfirmClear = {
            showClearScreenDialog = false
            viewModel.clearScreen(pagerState.currentPage)
        },
        onDismissAccessibility = { showAccessibilityDialog = false },
        context = context
    )

    Box(modifier = Modifier.fillMaxSize().then(dragGestureModifier).then(swipeGesture).onGloballyPositioned { }) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = 1f, scaleY = 1f, transformOrigin = TransformOrigin.Center)) {
                Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    if (isProfileGlass) {
                        ProfileGlassCard(
                            userName = userName,
                            showAvatar = showProfileAvatar,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            avatarUrl = avatarUrl,
                            onAvatarClick = params.onEditProfile,
                            isInfoPanelVisible = true,
                            onToggleInfoPanel = onToggleInfoPanel,
                            isWorkspaceLocked = isWorkspaceLocked
                        )
                    } else Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.fillMaxWidth().weight(1f).onGloballyPositioned { c ->
                        val pos = c.positionInWindow()
                        gridWindowPosRef[0] = pos.x
                        gridWindowPosRef[1] = pos.y
                        if (gridSize != c.size) gridSize = c.size
                    }) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = !isDragging && !isResizingWidget && openedFolderId == null,
                            beyondViewportPageCount = 1
                        ) { page ->
                            key(page) {
                                CellLayoutGrid(
                                    screenId = page, items = workspaceItems, allApps = allApps, gridColumns = currentGridColumns, gridRows = effectiveGridRows,
                                    onItemClick = { item ->
                                        if (item is WorkspaceAppItem) allApps.find { it.packageName == item.packageName }?.let { context.launchApp(it) }
                                        else if (item is WorkspaceFolderItem) openedFolderId = item.id
                                    },
                                    onDragStartItem = { item -> draggedWorkspaceItem = item; dragSource = DragSource.WORKSPACE; dragPosition = currentPointerPosition },
                                    onRemoveItem = { item -> viewModel.removeWorkspaceItem(item.id, scatterFolder = item is WorkspaceFolderItem, currentGridRows = effectiveGridRows) },
                                    onHideApp = onHideApp, onResizeWidget = { id, sx, sy -> viewModel.resizeWidget(id, sx, sy, effectiveGridRows) },
                                    onConfigureWidget = ::handleConfigureWidget, onResizeStateChange = { isResizingWidget = it },
                                    onEmptySpaceLongPress = { offset ->
                                        if (!showAppDrawer && !isResizingWidget) {
                                            val screenWidth = windowInfo.containerSize.width.toFloat()
                                            val screenHeight = windowInfo.containerSize.height.toFloat()
                                            val menuW = with(density) { 250.dp.toPx() }
                                            val menuH = with(density) { 380.dp.toPx() }
                                            var rootX = gridWindowPosRef[0] + offset.x
                                            var rootY = gridWindowPosRef[1] + offset.y

                                            if (rootX + menuW > screenWidth) rootX -= (rootX + menuW - screenWidth + 20f)
                                            if (rootY + menuH > screenHeight) rootY -= (rootY + menuH - screenHeight + 20f)

                                            workspaceMenuOffset = DpOffset(with(density) { rootX.toDp() }, with(density) { rootY.toDp() })
                                            showWorkspaceMenu = true
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize(), draggedItemId = draggedWorkspaceItem?.id, isWallpaperDark = systemBars.isWallpaperDark.value, isWorkspaceLocked = isWorkspaceLocked, showWorkspaceLabels = showWorkspaceLabels
                                )
                            }
                        }
                        if (targetCellRect != null && targetDockInsertIndex == -1 && !isWorkspaceLocked) {
                            val rect = targetCellRect!!
                            Box(modifier = Modifier.offset { IntOffset((rect.left - gridWindowPosRef[0]).roundToInt(), (rect.top - gridWindowPosRef[1]).roundToInt()) }.size(width = with(density) { (rect.right - rect.left).toDp() }, height = with(density) { (rect.bottom - rect.top).toDp() }).border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)))
                        }
                    }

                    DynamicPageIndicator(pagerState, Modifier.fillMaxWidth().padding(vertical = 5.dp))

                    MainDockBar(
                        dockPackages = currentDockApps, allApps = allApps, showIconBorder = showIconBorder, showDockLabels = showDockLabels, maxDockApps = currentGridColumns,
                        isDockBarHidden = isDockBarHidden, onToggleDockBarHidden = onToggleDockBarHidden, onAppClick = { context.launchApp(it) }, onRemoveFromDock = removeFromDock,
                        onMoveLeft = moveLeft, onMoveRight = moveRight, onHideApp = onHideApp,
                        onDragStart = { app -> draggedApp = app; dragSource = DragSource.DOCK; dragPosition = currentPointerPosition },
                        modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).padding(horizontal = 20.dp).padding(bottom = 24.dp).onGloballyPositioned {
                            val pos = it.positionInWindow()
                            dockBarWindowPosRef[0] = pos.x
                            dockBarWindowPosRef[1] = pos.y
                            if (dockBarSize != it.size) dockBarSize = it.size
                        },
                        isEditMode = false, customHeight = 85.dp, isEditModePanel = false, isHoveringDockBar = isHoveringDockBar, draggedAppPackage = draggedApp?.packageName,
                        isDraggingFromDock = dragSource == DragSource.DOCK, targetInsertIndex = targetDockInsertIndex, isPredictiveEnabled = isPredictiveEnabled, onDisablePredictive = { viewModel.disablePredictiveDock() },
                        isDynamicDockBar = isDynamicDockBar,
                        isWorkspaceLocked = isWorkspaceLocked
                    )
                }
            }
        }

        GluonAnimatedDropdownMenu(expanded = showWorkspaceMenu, onDismissRequest = { showWorkspaceMenu = false }, offset = workspaceMenuOffset, modifier = Modifier.width(250.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                WallpaperActionPopup(onWallpaper = { try { context.startActivity(Intent(Intent.ACTION_SET_WALLPAPER).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {} }, onSettings = { onOpenSettings() }, onAddWidget = { viewModel.setWidgetPickerOpen(isOpen = true) }, onDismiss = { showWorkspaceMenu = false }, onAddScreen = { if (screenCount < 5) { viewModel.increaseScreenCount(); scope.launch { delay(100.milliseconds); pagerState.animateScrollToPage(screenCount) } } }, onRequestRemoveScreen = { if (screenCount > 1) showDeleteScreenDialog = true }, onRequestClearScreen = { showClearScreenDialog = true }, totalScreens = screenCount, maxScreens = 5, isWorkspaceLocked = isWorkspaceLocked, onToggleWorkspaceLock = { viewModel.onWorkspaceLockedToggle(!isWorkspaceLocked) })
            }
        }

        AnimatedVisibility(
            visible = showAppDrawer,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .pointerInput(Unit) { detectTapGestures { viewModel.setAppDrawerOpen(isOpen = false) } }
            )
        }

        AnimatedVisibility(
            visible = showAppDrawer,
            enter = slideInVertically(spring(dampingRatio = 0.9f, stiffness = 400f)) { it },
            exit = slideOutVertically(spring(dampingRatio = 0.9f, stiffness = 400f)) { it }
        ) {
            Box(modifier = Modifier.fillMaxSize().imePadding()) {
                AppDrawerSheet(
                    allApps = allApps, dockApps = currentDockApps, gridColumns = currentGridColumns, showLabels = showLabels, showIconBorder = showIconBorder, searchQuery = searchQuery, searchResults = searchResults, onSearchQueryChange = viewModel::onSearchQueryChange,
                    drawerFolders = drawerFolders,
                    onCreateDrawerFolder = viewModel::createDrawerFolder,
                    onDeleteDrawerFolder = viewModel::deleteDrawerFolder,
                    onRenameDrawerFolder = viewModel::renameDrawerFolder,
                    onRemoveFromDrawerFolder = viewModel::removeFromDrawerFolder,
                    onUpdateDrawerFolder = viewModel::updateDrawerFolder,
                    onAppClick = { app -> viewModel.setAppDrawerOpen(isOpen = false); context.launchApp(app); pendingCell = null },
                    onHideApp = onHideApp, onToggleDockApp = toggleDockApp, onDragStart = { app -> draggedApp = app; dragSource = DragSource.DRAWER; dragPosition = currentPointerPosition; viewModel.setAppDrawerOpen(isOpen = false) }, onDismiss = { viewModel.setAppDrawerOpen(isOpen = false); viewModel.onSearchQueryChange("") }, modifier = Modifier.fillMaxWidth(), maxDockApps = currentGridColumns, sortMode = appDrawerSortMode, onCycleSortMode = { viewModel.cycleAppDrawerSortMode() }, customAppCategories = customAppCategories, onSetAppCategory = viewModel::setAppCategoryOverride,
                    isWorkspaceLocked = isWorkspaceLocked,
                    isDockBarHidden = currentIsDockBarHidden
                )
            }
        }

        if (openedFolder != null && (openedFolderId != null || isFolderDragActive)) {
            OpenedWorkspaceFolderOverlay(
                openedFolder = openedFolder,
                isFolderDragActive = isFolderDragActive,
                allApps = allApps,
                showWorkspaceLabels = showWorkspaceLabels,
                isWorkspaceLocked = isWorkspaceLocked,
                onClose = { openedFolderId = null },
                onRename = { id, name -> viewModel.renameFolder(id, name) },
                onRemoveApp = { id, pkg -> viewModel.removeFromFolder(id, pkg) },
                onDragStartApp = { appModel, folderId ->
                    draggedApp = appModel
                    dragSource = DragSource.WORKSPACE_FOLDER
                    dragPosition = currentPointerPosition
                    draggedFromFolderId = folderId
                }
            )
        }

        if (isDragging) DraggedItemOverlay(draggedApp, draggedWorkspaceItem, draggedWidgetProvider, allApps, { dragPosition }, gridSize, currentGridColumns, effectiveGridRows, density, scale = 1f)
    }

    AnimatedVisibility(visible = showWidgetPicker, enter = fadeIn(tween(250)) + slideInVertically(spring(dampingRatio = 0.85f, stiffness = 250f)) { it / 3 }, exit = fadeOut(tween(200)) + slideOutVertically(spring(dampingRatio = 0.9f, stiffness = 300f)) { it / 3 }) {
        WidgetPickerSheet(onWidgetSelected = { info -> viewModel.setWidgetPickerOpen(isOpen = false); viewModel.tryAddWidget(info, pagerState.currentPage, effectiveGridRows) }, onWidgetDragStart = { id, info, offset -> viewModel.setWidgetPickerOpen(isOpen = false); draggedWidgetProvider = id to info; dragPosition = offset }, onDismiss = { viewModel.setWidgetPickerOpen(isOpen = false) })
    }
}