// app/src/main/java/com/gluon/launcher/launcher/ui/components/DrawerAppGrid.kt
package com.gluon.launcher.launcher.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gluon.launcher.core.data.AppModel
import com.gluon.launcher.core.data.DrawerFolderItem

@Composable
fun DrawerAppGrid(
    gridState: LazyGridState,
    gridColumns: Int,
    searchHeight: Dp,
    topInternalPadding: Dp,
    searchBottomPadding: Dp,
    contentPadding: Dp,
    listBottomPadding: Dp,
    sortMode: Int,
    drawerFolders: List<DrawerFolderItem>,
    aiCategorizedApps: List<Pair<String, List<AppModel>>>,
    sortedApps: List<AppModel>,
    allApps: List<AppModel>,
    showLabels: Boolean,
    iconSize: Dp,
    isSelectionMode: Boolean,
    isWorkspaceLocked: Boolean,
    folderForAdd: DrawerFolderItem?,
    onOpenedFolderChange: (DrawerFolderItem?) -> Unit,
    onEnterSelectionModeForFolder: (DrawerFolderItem) -> Unit,
    onDeleteFolder: (String) -> Unit,
    selectedApps: Set<AppModel>,
    showIconBorder: Boolean,
    isDockBarHidden: Boolean,
    dockApps: List<String>,
    maxDockApps: Int,
    onToggleDockApp: (String) -> Unit,
    onCategorize: (AppModel) -> Unit,
    onHideApp: (String) -> Unit,
    context: Context,
    onAppClick: (AppModel) -> Unit,
    onDragStart: (AppModel) -> Unit,
    onToggleSelection: (AppModel) -> Unit,
    onEnterSelectionMode: (AppModel) -> Unit
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(gridColumns),
        modifier = Modifier
            .fillMaxSize()
            .padding(top = searchHeight + topInternalPadding + searchBottomPadding)
            .padding(horizontal = contentPadding),
        contentPadding = PaddingValues(bottom = listBottomPadding),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        // ИСПРАВЛЕНИЕ: Уменьшаем отступы по вертикали в режиме сортировки "Категории"
        verticalArrangement = if (sortMode == 2) Arrangement.spacedBy(8.dp) else Arrangement.spacedBy(24.dp)
    ) {
        // ИСПРАВЛЕНИЕ: Композитные ключи (sortMode + ID) навсегда решают проблему лагов при смене сортировок
        if (sortMode == 2) {
            if (drawerFolders.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "sm${sortMode}_header_Папки", contentType = "header") {
                    Text(
                        text = "Папки",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp, top = 16.dp, start = 8.dp)
                    )
                }
                items(drawerFolders, key = { "sm${sortMode}_folder_${it.id}" }, contentType = { "folder" }) { folder ->
                    DrawerFolderCell(
                        folder = folder,
                        allApps = allApps,
                        showLabels = showLabels,
                        iconSize = iconSize,
                        isSelectionMode = isSelectionMode,
                        folderForAdd = folderForAdd,
                        onOpenedFolderChange = onOpenedFolderChange,
                        onEnterSelectionModeForFolder = onEnterSelectionModeForFolder,
                        onDeleteFolder = onDeleteFolder
                    )
                }
            }
            aiCategorizedApps.forEach { (category, apps) ->
                item(span = { GridItemSpan(maxLineSpan) }, key = "sm${sortMode}_header_$category", contentType = "header") {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp, top = 16.dp, start = 8.dp)
                    )
                }
                items(apps, key = { "sm${sortMode}_app_${it.packageName}" }, contentType = { "app" }) { app ->
                    DrawerAppCell(
                        app = app,
                        isSelected = selectedApps.contains(app),
                        isSelectionMode = isSelectionMode,
                        iconSize = iconSize,
                        showLabels = showLabels,
                        showIconBorder = showIconBorder,
                        isWorkspaceLocked = isWorkspaceLocked,
                        isDockBarHidden = isDockBarHidden,
                        dockApps = dockApps,
                        maxDockApps = maxDockApps,
                        onToggleDockApp = onToggleDockApp,
                        onCategorize = onCategorize,
                        onHideApp = onHideApp,
                        context = context,
                        onAppClick = onAppClick,
                        onDragStart = onDragStart,
                        onToggleSelection = onToggleSelection,
                        onEnterSelectionMode = onEnterSelectionMode,
                        showCategoryOption = true
                    )
                }
            }
        } else {
            if (drawerFolders.isNotEmpty()) {
                items(drawerFolders, key = { "sm${sortMode}_folder_${it.id}" }, contentType = { "folder" }) { folder ->
                    DrawerFolderCell(
                        folder = folder,
                        allApps = allApps,
                        showLabels = showLabels,
                        iconSize = iconSize,
                        isSelectionMode = isSelectionMode,
                        folderForAdd = folderForAdd,
                        onOpenedFolderChange = onOpenedFolderChange,
                        onEnterSelectionModeForFolder = onEnterSelectionModeForFolder,
                        onDeleteFolder = onDeleteFolder
                    )
                }
            }
            items(sortedApps, key = { "sm${sortMode}_app_${it.packageName}" }, contentType = { "app" }) { app ->
                DrawerAppCell(
                    app = app,
                    isSelected = selectedApps.contains(app),
                    isSelectionMode = isSelectionMode,
                    iconSize = iconSize,
                    showLabels = showLabels,
                    showIconBorder = showIconBorder,
                    isWorkspaceLocked = isWorkspaceLocked,
                    isDockBarHidden = isDockBarHidden,
                    dockApps = dockApps,
                    maxDockApps = maxDockApps,
                    onToggleDockApp = onToggleDockApp,
                    onCategorize = onCategorize,
                    onHideApp = onHideApp,
                    context = context,
                    onAppClick = onAppClick,
                    onDragStart = onDragStart,
                    onToggleSelection = onToggleSelection,
                    onEnterSelectionMode = onEnterSelectionMode,
                    showCategoryOption = false
                )
            }
        }
    }
}