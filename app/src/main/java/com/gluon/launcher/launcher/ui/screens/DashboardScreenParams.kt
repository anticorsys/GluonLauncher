// app/src/main/java/com/gluon/launcher/launcher/ui/screens/DashboardScreenParams.kt
package com.gluon.launcher.launcher.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.gluon.launcher.MainViewModel
import com.gluon.launcher.core.data.AppModel
import com.gluon.launcher.core.data.WorkspaceItem
import kotlin.math.roundToInt

data class DashboardScreenParams(
    val viewModel: MainViewModel, val dockApps: List<String>, val allApps: List<AppModel>, val gridColumns: Int,
    val showLabels: Boolean, val showDockLabels: Boolean, val showIconBorder: Boolean, val isDockBarHidden: Boolean,
    val onDockAppsChange: (List<String>) -> Unit, val onHideApp: (String) -> Unit, val onOpenSettings: () -> Unit,
    val onEditProfile: () -> Unit, val onToggleDockBarHidden: (Boolean) -> Unit,
    val homePressCount: Int, val isProfileGlass: Boolean, val showProfileAvatar: Boolean,
    val userName: String, val avatarUrl: String?, val onToggleInfoPanel: (Boolean) -> Unit, val isWorkspaceLocked: Boolean,
    val showWorkspaceLabels: Boolean, val isDynamicDockBar: Boolean
)

fun updateTargetCellLocal(
    pointerPos: Offset, gridWindowPos: Offset, gridSize: IntSize, gridColumns: Int, gridRows: Int, draggedWorkspaceItem: WorkspaceItem?, density: Density
): Rect? {
    if (gridSize == IntSize.Zero) return null
    val localX = pointerPos.x - gridWindowPos.x
    val localY = pointerPos.y - gridWindowPos.y
    val marginY = 150f
    if (localX !in -50f..(gridSize.width + 50f) || localY !in -50f..(gridSize.height + marginY)) return null

    val marginX = with(density) { 20.dp.toPx() }
    val gridWidth = gridSize.width - (marginX * 2)

    val cellWidth = gridWidth / gridColumns
    val cellHeight = gridSize.height.toFloat() / gridRows

    val spanX = draggedWorkspaceItem?.spanX ?: 1
    val spanY = draggedWorkspaceItem?.spanY ?: 1

    val startCol = ((localX - marginX - (cellWidth * spanX) / 2) / cellWidth).roundToInt().coerceIn(0, maxOf(0, gridColumns - spanX))
    val startRow = ((localY - (cellHeight * spanY) / 2) / cellHeight).roundToInt().coerceIn(0, maxOf(0, gridRows - spanY))
    val left = gridWindowPos.x + marginX + startCol * cellWidth
    val top = gridWindowPos.y + startRow * cellHeight
    return Rect(left, top, left + cellWidth * spanX, top + cellHeight * spanY)
}