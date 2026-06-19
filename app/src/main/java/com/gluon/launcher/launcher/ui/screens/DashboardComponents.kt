// app/src/main/java/com/gluon/launcher/launcher/ui/screens/DashboardComponents.kt
package com.gluon.launcher.launcher.ui.screens

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.gluon.launcher.core.data.AppModel
import com.gluon.launcher.core.data.WorkspaceAppItem
import com.gluon.launcher.core.data.WorkspaceFolderItem
import com.gluon.launcher.core.data.WorkspaceItem
import com.gluon.launcher.core.data.WorkspaceWidgetItem
import com.gluon.launcher.core.widget.WidgetManager
import com.gluon.launcher.launcher.ui.components.AppIconItem
import com.gluon.launcher.launcher.ui.components.FolderIconItem
import com.gluon.launcher.core.theme.LocalThemeSystemBars

enum class DragSource { DOCK, DRAWER, WORKSPACE, WORKSPACE_FOLDER }

@Composable
fun DraggedItemOverlay(
    draggedApp: AppModel?,
    draggedWorkspaceItem: WorkspaceItem?,
    draggedWidgetProvider: Pair<Int, AppWidgetProviderInfo>?,
    allApps: List<AppModel>,
    dragPositionProvider: () -> Offset,
    gridSize: IntSize,
    gridColumns: Int,
    gridRows: Int,
    density: androidx.compose.ui.unit.Density,
    scale: Float
) {
    if (draggedApp != null || draggedWorkspaceItem is WorkspaceAppItem) {
        val targetApp = draggedApp ?: (draggedWorkspaceItem as? WorkspaceAppItem)?.let { item ->
            allApps.find { it.packageName == item.packageName }
        }

        if (targetApp != null) {
            val iconSizeDp = if (gridColumns >= 5) 54.dp * scale else 60.dp * scale
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        val dragPosition = dragPositionProvider()
                        translationX = dragPosition.x - with(density) { iconSizeDp.toPx() / 2 }
                        translationY = dragPosition.y - with(density) { iconSizeDp.toPx() / 2 }
                    }
                    .size(iconSizeDp)
            ) {
                AppIconItem(
                    app = targetApp,
                    showLabel = false,
                    isInGrid = true,
                    iconSizeOverride = iconSizeDp,
                )
            }
        }
    }
    else if (draggedWorkspaceItem is WorkspaceFolderItem) {
        val iconSizeDp = if (gridColumns >= 5) 54.dp * scale else 60.dp * scale
        Box(
            modifier = Modifier
                .graphicsLayer {
                    val pos = dragPositionProvider()
                    translationX = pos.x - with(density) { iconSizeDp.toPx() / 2 }
                    translationY = pos.y - with(density) { iconSizeDp.toPx() / 2 }
                }
                .size(iconSizeDp)
        ) {
            FolderIconItem(
                name = draggedWorkspaceItem.name,
                apps = draggedWorkspaceItem.packages.mapNotNull { pkg -> allApps.find { it.packageName == pkg } },
                showLabel = false,
                iconSizeOverride = iconSizeDp,
                onClick = {},
                onContextMenu = {}
            )
        }
    }
    else if (draggedWorkspaceItem is WorkspaceWidgetItem) {
        val cellWidth = if (gridSize.width > 0 && gridColumns > 0) (gridSize.width / gridColumns).toFloat() else 200f
        val cellHeight = if (gridSize.height > 0 && gridRows > 0) (gridSize.height / gridRows).toFloat() else 200f

        val widthPx = cellWidth * draggedWorkspaceItem.spanX
        val heightPx = cellHeight * draggedWorkspaceItem.spanY

        val widthDp = with(density) { widthPx.toDp() }
        val heightDp = with(density) { heightPx.toDp() }

        Box(
            modifier = Modifier
                .graphicsLayer {
                    val dragPosition = dragPositionProvider()
                    translationX = dragPosition.x - (widthPx / 2)
                    translationY = dragPosition.y - (heightPx / 2)
                    alpha = 0.8f
                }
                .size(widthDp, heightDp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Dashboard,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
    else if (draggedWidgetProvider != null) {
        val providerInfo = draggedWidgetProvider.second
        val cellWidth = if (gridSize.width > 0 && gridColumns > 0) (gridSize.width / gridColumns).toFloat() else 200f
        val cellHeight = if (gridSize.height > 0 && gridRows > 0) (gridSize.height / gridRows).toFloat() else 200f

        val (spanX, spanY) = WidgetManager.getDefaultSpans(providerInfo, gridColumns, gridRows)
        val widthPx = cellWidth * spanX
        val heightPx = cellHeight * spanY

        val widthDp = with(density) { widthPx.toDp() }
        val heightDp = with(density) { heightPx.toDp() }

        Box(
            modifier = Modifier
                .graphicsLayer {
                    val dragPosition = dragPositionProvider()
                    translationX = dragPosition.x - (widthPx / 2)
                    translationY = dragPosition.y - (heightPx / 2)
                    alpha = 0.8f
                }
                .size(widthDp, heightDp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Dashboard,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun DynamicPageIndicator(pagerState: androidx.compose.foundation.pager.PagerState, modifier: Modifier = Modifier) {
    val pageCount = pagerState.pageCount
    if (pageCount <= 1) return

    val isDark = LocalThemeSystemBars.current.isWallpaperDark.value
    val indicatorColor = if (isDark) Color.White else Color.Black

    Row(
        modifier = modifier
            .wrapContentWidth(Alignment.CenterHorizontally)
            .background(
                color = indicatorColor.copy(alpha = 0.2f),
                shape = CircleShape
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(pageCount) { index ->
            val isSelected = pagerState.currentPage == index
            val size by animateDpAsState(
                targetValue = if (isSelected) 10.dp else 7.dp,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                label = "pageIndicatorSize"
            )
            val alpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.4f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                label = "pageIndicatorAlpha"
            )
            Box(
                modifier = Modifier
                    .size(size)
                    .background(
                        color = indicatorColor.copy(alpha = alpha),
                        shape = CircleShape
                    )
            )
        }
    }
}

fun calculateDockInsertIndex(
    pointerPos: Offset,
    dockBarWindowPos: Offset,
    dockBarSize: IntSize,
    dockAppsCount: Int,
    maxDockApps: Int
): Int {
    if (dockBarSize == IntSize.Zero) return if (dockAppsCount == 0) 0 else -1
    val relativeX = pointerPos.x - dockBarWindowPos.x
    if (relativeX < 0 || relativeX > dockBarSize.width) return if (dockAppsCount == 0) 0 else -1
    val activeSlots = minOf(dockAppsCount + 1, maxDockApps)
    val cellWidth = dockBarSize.width.toFloat() / activeSlots
    val rawIndex = (relativeX / cellWidth).toInt()
    val fraction = (relativeX % cellWidth) / cellWidth
    val adjustedIndex = if (fraction > 0.5f) rawIndex + 1 else rawIndex
    return adjustedIndex.coerceIn(0, dockAppsCount)
}

fun isInDockBar(pos: Offset, dockBarWindowPos: Offset, dockBarSize: IntSize, isDockBarHidden: Boolean): Boolean {
    if (isDockBarHidden) return false
    if (dockBarSize == IntSize.Zero) return false
    val xIn = pos.x >= dockBarWindowPos.x && pos.x <= dockBarWindowPos.x + dockBarSize.width
    val yIn = pos.y >= dockBarWindowPos.y && pos.y <= dockBarWindowPos.y + dockBarSize.height
    return xIn && yIn
}