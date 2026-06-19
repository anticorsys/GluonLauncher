// app/src/main/java/com/gluon/launcher/launcher/ui/screens/CellLayoutGrid.kt
package com.gluon.launcher.launcher.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.gluon.launcher.core.data.AppModel
import com.gluon.launcher.core.data.WorkspaceAppItem
import com.gluon.launcher.core.data.WorkspaceFolderItem
import com.gluon.launcher.core.data.WorkspaceItem
import com.gluon.launcher.core.data.WorkspaceWidgetItem
import com.gluon.launcher.core.utils.GridValidator
import com.gluon.launcher.core.widget.WidgetManager
import com.gluon.launcher.launcher.ui.components.AppIconItem
import com.gluon.launcher.launcher.ui.components.FolderIconItem
import com.gluon.launcher.launcher.ui.components.GluonAnimatedDropdownMenu
import com.gluon.launcher.launcher.ui.components.GluonMarqueeText
import com.gluon.launcher.launcher.ui.components.GluonPopupMenuCard
import com.gluon.launcher.launcher.ui.components.SmartWidgetStackContainer
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CellLayoutGrid(
    screenId: Int,
    items: List<WorkspaceItem>,
    allApps: List<AppModel>,
    gridColumns: Int,
    gridRows: Int,
    onItemClick: (WorkspaceItem) -> Unit,
    onDragStartItem: (WorkspaceItem) -> Unit,
    onRemoveItem: (WorkspaceItem) -> Unit,
    modifier: Modifier = Modifier,
    onHideApp: (String) -> Unit = {},
    onResizeWidget: ((String, Int, Int) -> Unit)? = null,
    onConfigureWidget: ((WorkspaceWidgetItem) -> Unit)? = null,
    onResizeStateChange: ((Boolean) -> Unit)? = null,
    onEmptySpaceLongPress: (Offset) -> Unit = {},
    draggedItemId: String? = null,
    isWallpaperDark: Boolean = true,
    isWorkspaceLocked: Boolean = false,
    showWorkspaceLabels: Boolean = true
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val haptic = LocalHapticFeedback.current

    var resizingWidget by remember { mutableStateOf<WorkspaceWidgetItem?>(null) }
    var previewSpanX by remember { mutableIntStateOf(0) }
    var previewSpanY by remember { mutableIntStateOf(0) }
    var isResizingActive by remember { mutableStateOf(false) }

    val gridGlobalPosRef = remember { FloatArray(2) }
    val activeWidgetsInStacks = remember { mutableStateMapOf<Pair<Int, Int>, WorkspaceWidgetItem>() }

    LaunchedEffect(isResizingActive) { onResizeStateChange?.invoke(isResizingActive) }

    val screenItems = remember(items, screenId, draggedItemId) {
        items.filter { it.screenId == screenId && it.id != draggedItemId }
    }
    val appItems = remember(screenItems) { screenItems.filterIsInstance<WorkspaceAppItem>() }
    val widgetItems = remember(screenItems) { screenItems.filterIsInstance<WorkspaceWidgetItem>() }
    val folderItems = remember(screenItems) { screenItems.filterIsInstance<WorkspaceFolderItem>() }

    val groupedWidgets = remember(widgetItems) { widgetItems.groupBy { Pair(it.cellX, it.cellY) } }

    BoxWithConstraints(modifier = modifier
        .fillMaxSize()
        .clipToBounds()
        .onGloballyPositioned {
            val pos = it.positionInWindow()
            gridGlobalPosRef[0] = pos.x
            gridGlobalPosRef[1] = pos.y
        }
    ) {
        val horizontalMargin = 12.dp
        val availableWidth = maxWidth - (horizontalMargin * 2)
        val cellWidthDp = availableWidth / gridColumns
        val cellHeightDp = maxHeight / gridRows

        val widgetHorizontalPadding = 8.dp
        val widgetVerticalPadding = 8.dp

        val iconSize = if (gridColumns >= 5) 54.dp else 60.dp
        val gridTextColor = if (isWallpaperDark) Color.White else Color.Black

        Box(
            modifier = Modifier.fillMaxSize().pointerInput(isResizingActive, isWorkspaceLocked, screenItems) {
                detectTapGestures(
                    onTap = {
                        if (isResizingActive) {
                            isResizingActive = false
                            resizingWidget = null
                        }
                    },
                    onLongPress = { offset ->
                        if (!isResizingActive) {
                            val localX = offset.x - horizontalMargin.toPx()
                            val xIdx = (localX / cellWidthDp.toPx()).toInt()
                            val yIdx = (offset.y / cellHeightDp.toPx()).toInt()

                            val isValidArea = localX >= 0 && localX <= availableWidth.toPx() &&
                                    xIdx in 0 until gridColumns && yIdx in 0 until gridRows

                            val isOverItem = if (isValidArea) {
                                screenItems.any {
                                    xIdx >= it.cellX && xIdx < it.cellX + it.spanX &&
                                            yIdx >= it.cellY && yIdx < it.cellY + it.spanY
                                }
                            } else false

                            if (!isOverItem) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onEmptySpaceLongPress(offset)
                            }
                        }
                    }
                )
            }
        ) {
            folderItems.forEach { folder ->
                key(folder.id) {
                    val appsInFolder = folder.packages.mapNotNull { pkg -> allApps.find { it.packageName == pkg } }
                    Box(
                        modifier = Modifier
                            .size(cellWidthDp * folder.spanX, cellHeightDp * folder.spanY)
                            .absoluteOffset(horizontalMargin + cellWidthDp * folder.cellX, cellHeightDp * folder.cellY),
                        contentAlignment = Alignment.Center
                    ) {
                        FolderIconItem(
                            name = folder.name,
                            apps = appsInFolder,
                            showLabel = showWorkspaceLabels,
                            textColor = gridTextColor,
                            iconSizeOverride = iconSize,
                            onClick = { onItemClick(folder) },
                            onLongPress = if (isWorkspaceLocked) null else { { onDragStartItem(folder) } },
                            onContextMenu = { dismiss ->
                                Column(modifier = Modifier.width(250.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    GluonPopupMenuCard(
                                        enabled = !isWorkspaceLocked,
                                        onClick = { if (!isWorkspaceLocked) { dismiss(); onRemoveItem(folder) } }
                                    ) {
                                        Icon(if (isWorkspaceLocked) Icons.Default.Lock else Icons.Default.Close, null, Modifier.size(28.dp).padding(end = 10.dp), tint = LocalContentColor.current)
                                        GluonMarqueeText("Удалить папку")
                                    }
                                }
                            }
                        )
                    }
                }
            }

            appItems.forEach { item ->
                key(item.id) {
                    Box(
                        modifier = Modifier
                            .size(cellWidthDp * item.spanX, cellHeightDp * item.spanY)
                            .absoluteOffset(horizontalMargin + cellWidthDp * item.cellX, cellHeightDp * item.cellY),
                        contentAlignment = Alignment.Center
                    ) {
                        allApps.find { it.packageName == item.packageName }?.let { appModel ->
                            AppIconItem(
                                app = appModel,
                                showLabel = showWorkspaceLabels,
                                showIconBorder = false,
                                isInGrid = true,
                                showContextMenu = true,
                                onAppClick = { onItemClick(item) },
                                onDragStart = if (isWorkspaceLocked) null else { { onDragStartItem(item) } },
                                iconSizeOverride = iconSize,
                                textColor = gridTextColor,
                                onContextMenu = { dismiss ->
                                    Column(modifier = Modifier.width(250.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        GluonPopupMenuCard(
                                            enabled = !isWorkspaceLocked,
                                            onClick = { if (!isWorkspaceLocked) { dismiss(); onRemoveItem(item) } }
                                        ) {
                                            Icon(if (isWorkspaceLocked) Icons.Default.Lock else Icons.Default.Close, null, Modifier.size(28.dp).padding(end = 10.dp), tint = LocalContentColor.current)
                                            GluonMarqueeText("Удалить с экрана")
                                        }

                                        GluonPopupMenuCard(
                                            enabled = !isWorkspaceLocked,
                                            onClick = { if (!isWorkspaceLocked) { dismiss(); onHideApp(appModel.packageName) } }
                                        ) {
                                            Icon(if (isWorkspaceLocked) Icons.Default.Lock else Icons.Default.VisibilityOff, null, Modifier.size(28.dp).padding(end = 10.dp), tint = LocalContentColor.current)
                                            GluonMarqueeText("Скрыть")
                                        }

                                        GluonPopupMenuCard(
                                            enabled = !isWorkspaceLocked,
                                            onClick = {
                                                if (!isWorkspaceLocked) {
                                                    dismiss()
                                                    val intent = Intent(Intent.ACTION_DELETE).apply { data = Uri.fromParts("package", appModel.packageName, null) }
                                                    context.startActivity(intent)
                                                }
                                            }
                                        ) {
                                            Icon(if (isWorkspaceLocked) Icons.Default.Lock else Icons.Default.Delete, null, Modifier.size(28.dp).padding(end = 10.dp), tint = LocalContentColor.current)
                                            GluonMarqueeText("Удалить приложение")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            groupedWidgets.forEach { (cellCoords, stack) ->
                val anchorItem = stack.first()
                var showWidgetMenu by remember { mutableStateOf(false) }
                var widgetMenuOffset by remember { mutableStateOf(DpOffset.Zero) }

                val currentActiveWidget = activeWidgetsInStacks[cellCoords] ?: anchorItem
                val latestActiveWidget by rememberUpdatedState(currentActiveWidget)

                val adjCellPxW = with(LocalDensity.current) { ((cellWidthDp * anchorItem.spanX - widgetHorizontalPadding * 2) / anchorItem.spanX).roundToPx() }
                val adjCellPxH = with(LocalDensity.current) { ((cellHeightDp * anchorItem.spanY - widgetVerticalPadding * 2) / anchorItem.spanY).roundToPx() }

                var isWidgetPressed by remember { mutableStateOf(false) }
                val widgetAnimatedScale by animateFloatAsState(
                    targetValue = if (isWidgetPressed) 0.90f else 1f,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                    label = "widget_scale"
                )

                Box(
                    modifier = Modifier
                        .size(cellWidthDp * anchorItem.spanX, cellHeightDp * anchorItem.spanY)
                        .absoluteOffset(horizontalMargin + cellWidthDp * anchorItem.cellX, cellHeightDp * anchorItem.cellY)
                        .pointerInput(anchorItem.id, "tap", isWorkspaceLocked) {
                            detectTapGestures(
                                onPress = {
                                    isWidgetPressed = true
                                    tryAwaitRelease()
                                    isWidgetPressed = false
                                }
                            )
                        }
                        .pointerInput(anchorItem.id, "drag", isWorkspaceLocked) {
                            var slopAccumulator = 0f
                            var isRealDrag = false

                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    isWidgetPressed = true
                                    if (!isResizingActive) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val screenWidth = windowInfo.containerSize.width.toFloat()
                                        val screenHeight = windowInfo.containerSize.height.toFloat()
                                        val menuW = with(density) { 250.dp.toPx() }
                                        val menuH = with(density) { 450.dp.toPx() }

                                        var localX = offset.x
                                        var localY = offset.y + with(density) { 16.dp.toPx() }

                                        val absX = gridGlobalPosRef[0] + horizontalMargin.toPx() + (cellWidthDp.toPx() * anchorItem.cellX) + localX
                                        val absY = gridGlobalPosRef[1] + (cellHeightDp.toPx() * anchorItem.cellY) + localY

                                        if (absX + menuW > screenWidth) {
                                            localX -= (absX + menuW - screenWidth + with(density) { 16.dp.toPx() })
                                        }
                                        if (absY + menuH > screenHeight) {
                                            localY -= (absY + menuH - screenHeight + with(density) { 24.dp.toPx() })
                                        }

                                        widgetMenuOffset = DpOffset(with(density) { localX.toDp() }, with(density) { localY.toDp() })
                                        showWidgetMenu = true
                                        slopAccumulator = 0f
                                        isRealDrag = false
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    if (isWorkspaceLocked) return@detectDragGesturesAfterLongPress
                                    if (!isResizingActive) {
                                        change.consume()
                                        if (!isRealDrag) {
                                            slopAccumulator += dragAmount.getDistance()
                                            if (slopAccumulator > 30f) {
                                                isRealDrag = true
                                                showWidgetMenu = false
                                                onDragStartItem(latestActiveWidget)
                                            }
                                        }
                                    }
                                },
                                onDragEnd = { isRealDrag = false; isWidgetPressed = false },
                                onDragCancel = { isRealDrag = false; isWidgetPressed = false }
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = widgetAnimatedScale
                                scaleY = widgetAnimatedScale
                            }
                            .padding(horizontal = widgetHorizontalPadding, vertical = widgetVerticalPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        key(cellCoords) {
                            SmartWidgetStackContainer(
                                widgets = stack,
                                modifier = Modifier.fillMaxSize(),
                                cellPixelWidth = adjCellPxW,
                                cellPixelHeight = adjCellPxH,
                                isEditMode = isResizingActive,
                                onActiveWidgetChanged = { activeWidgetsInStacks[cellCoords] = it }
                            )
                        }
                    }

                    GluonAnimatedDropdownMenu(
                        expanded = showWidgetMenu,
                        onDismissRequest = { showWidgetMenu = false },
                        offset = widgetMenuOffset,
                        modifier = Modifier.width(250.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            GluonPopupMenuCard(
                                enabled = !isWorkspaceLocked,
                                onClick = {
                                    if (!isWorkspaceLocked) {
                                        showWidgetMenu = false
                                        resizingWidget = latestActiveWidget
                                        previewSpanX = latestActiveWidget.spanX
                                        previewSpanY = latestActiveWidget.spanY
                                        isResizingActive = true
                                    }
                                }
                            ) {
                                Icon(if (isWorkspaceLocked) Icons.Default.Lock else Icons.Default.Dashboard, null, Modifier.size(28.dp).padding(end = 10.dp), tint = LocalContentColor.current)
                                GluonMarqueeText("Изменить размер")
                            }

                            val hasConfig = WidgetManager.appWidgetManager.getAppWidgetInfo(latestActiveWidget.appWidgetId)?.configure != null
                            if (hasConfig) {
                                GluonPopupMenuCard(
                                    enabled = !isWorkspaceLocked,
                                    onClick = {
                                        if (!isWorkspaceLocked) {
                                            showWidgetMenu = false
                                            onConfigureWidget?.invoke(latestActiveWidget)
                                        }
                                    }
                                ) {
                                    Icon(if (isWorkspaceLocked) Icons.Default.Lock else Icons.Default.Settings, null, Modifier.size(28.dp).padding(end = 10.dp), tint = LocalContentColor.current)
                                    GluonMarqueeText("Настроить")
                                }
                            }

                            GluonPopupMenuCard(
                                enabled = !isWorkspaceLocked,
                                onClick = {
                                    if (!isWorkspaceLocked) {
                                        showWidgetMenu = false
                                        onRemoveItem(latestActiveWidget)
                                    }
                                }
                            ) {
                                Icon(if (isWorkspaceLocked) Icons.Default.Lock else Icons.Default.Close, null, Modifier.size(28.dp).padding(end = 10.dp), tint = LocalContentColor.current)
                                GluonMarqueeText("Удалить виджет")
                            }
                        }
                    }
                }
            }
        }

        if (isResizingActive && resizingWidget != null && !isWorkspaceLocked) {
            val item = resizingWidget!!
            val info = WidgetManager.appWidgetManager.getAppWidgetInfo(item.appWidgetId)
            val maxSpans = info?.let { WidgetManager.getMaxSpans(gridColumns, gridRows) } ?: Pair(gridColumns, gridRows)
            val stackIds = items.filter { it is WorkspaceWidgetItem && it.cellX == item.cellX && it.cellY == item.cellY && it.screenId == item.screenId }.map { it.id }.toSet()
            val isValidSize = GridValidator.isAreaVacant(items, screenId, item.cellX, item.cellY, previewSpanX, previewSpanY, gridColumns, gridRows, stackIds)

            var dragOffsetXPx by remember { mutableFloatStateOf(0f) }
            var dragOffsetYPx by remember { mutableFloatStateOf(0f) }
            var startSpanX by remember { mutableIntStateOf(previewSpanX) }
            var startSpanY by remember { mutableIntStateOf(previewSpanY) }

            val cellWidthPx = with(density) { cellWidthDp.toPx() }
            val cellHeightPx = with(density) { cellHeightDp.toPx() }

            WidgetResizeOverlay(
                itemCellX = item.cellX,
                itemCellY = item.cellY,
                previewSpanX = previewSpanX,
                previewSpanY = previewSpanY,
                cellWidthDp = cellWidthDp,
                cellHeightDp = cellHeightDp,
                horizontalMargin = horizontalMargin,
                isValidSize = isValidSize,
                onDragRightStart = {
                    dragOffsetXPx = 0f
                    startSpanX = previewSpanX
                },
                onDragRight = { deltaX ->
                    dragOffsetXPx += deltaX
                    val delta = (dragOffsetXPx / cellWidthPx).roundToInt()
                    val newSpan = (startSpanX + delta).coerceIn(1, minOf(maxSpans.first, gridColumns - item.cellX))
                    if (GridValidator.isAreaVacant(items, screenId, item.cellX, item.cellY, newSpan, previewSpanY, gridColumns, gridRows, stackIds)) {
                        previewSpanX = newSpan
                    }
                },
                onDragBottomStart = {
                    dragOffsetYPx = 0f
                    startSpanY = previewSpanY
                },
                onDragBottom = { deltaY ->
                    dragOffsetYPx += deltaY
                    val delta = (dragOffsetYPx / cellHeightPx).roundToInt()
                    val newSpan = (startSpanY + delta).coerceIn(1, minOf(maxSpans.second, gridRows - item.cellY))
                    if (GridValidator.isAreaVacant(items, screenId, item.cellX, item.cellY, previewSpanX, newSpan, gridColumns, gridRows, stackIds)) {
                        previewSpanY = newSpan
                    }
                },
                onDragEnd = {
                    if (previewSpanX != item.spanX || previewSpanY != item.spanY) {
                        onResizeWidget?.invoke(item.id, previewSpanX, previewSpanY)
                    }
                    isResizingActive = false
                    resizingWidget = null
                },
                onDragCancel = {
                    isResizingActive = false
                    resizingWidget = null
                }
            )
        }
    }
}