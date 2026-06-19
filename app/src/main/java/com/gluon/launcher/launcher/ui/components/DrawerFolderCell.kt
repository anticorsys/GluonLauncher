// app/src/main/java/com/gluon/launcher/launcher/ui/components/DrawerFolderCell.kt
package com.gluon.launcher.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gluon.launcher.core.data.AppModel
import com.gluon.launcher.core.data.DrawerFolderItem

@Composable
fun DrawerFolderCell(
    folder: DrawerFolderItem,
    allApps: List<AppModel>,
    showLabels: Boolean,
    iconSize: Dp,
    isSelectionMode: Boolean,
    folderForAdd: DrawerFolderItem?,
    onOpenedFolderChange: (DrawerFolderItem?) -> Unit,
    onEnterSelectionModeForFolder: (DrawerFolderItem) -> Unit,
    onDeleteFolder: (String) -> Unit
) {
    val isFolderTarget = isSelectionMode && folderForAdd?.id == folder.id
    Box(
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (isFolderTarget) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent),
        contentAlignment = Alignment.TopCenter
    ) {
        FolderIconItem(
            name = folder.name,
            apps = folder.packages.mapNotNull { pkg -> allApps.find { it.packageName == pkg } },
            showLabel = showLabels,
            iconSizeOverride = iconSize,
            showContextMenu = !isSelectionMode,
            onClick = { if (!isSelectionMode) onOpenedFolderChange(folder) },
            onContextMenu = { dismiss ->
                if (!isSelectionMode) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        GluonPopupMenuCard(onClick = { dismiss(); onEnterSelectionModeForFolder(folder) }) {
                            Icon(Icons.Default.Add, null, Modifier.size(28.dp).padding(end = 10.dp), tint = MaterialTheme.colorScheme.onSurface)
                            GluonMarqueeText("Добавить приложения")
                        }
                        GluonPopupMenuCard(onClick = { dismiss(); onDeleteFolder(folder.id) }) {
                            Icon(Icons.Default.Delete, null, Modifier.size(28.dp).padding(end = 10.dp), tint = MaterialTheme.colorScheme.onSurface)
                            GluonMarqueeText("Удалить папку")
                        }
                    }
                }
            }
        )
    }
}