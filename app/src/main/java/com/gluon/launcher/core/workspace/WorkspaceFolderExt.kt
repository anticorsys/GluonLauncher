// app/src/main/java/com/gluon/launcher/core/workspace/WorkspaceFolderExt.kt
package com.gluon.launcher.core.workspace

import com.gluon.launcher.core.data.DrawerFolderItem
import com.gluon.launcher.core.data.WorkspaceAppItem
import com.gluon.launcher.core.data.WorkspaceFolderItem
import com.gluon.launcher.core.data.WorkspaceItem
import com.gluon.launcher.core.data.WorkspaceWidgetItem
import com.gluon.launcher.core.widget.WidgetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

internal fun WorkspaceController.updateScreenCountFromItems(items: List<WorkspaceItem>) {
    if (items.isEmpty()) {
        if (mutableCurrentScreenCount.value < 1) mutableCurrentScreenCount.value = 1
        return
    }
    val maxScreen = items.maxOf { it.screenId }
    if (mutableCurrentScreenCount.value <= maxScreen + 1) mutableCurrentScreenCount.value = maxScreen + 1
}

fun WorkspaceController.increaseScreenCount() {
    if (mutableCurrentScreenCount.value < 5) mutableCurrentScreenCount.value += 1
}

fun WorkspaceController.clearScreen(screenId: Int) {
    scope.launch(Dispatchers.IO) {
        val itemsToRemove = mutableWorkspaceItems.value.filter { it.screenId == screenId }
        itemsToRemove.forEach { item ->
            if (item is WorkspaceWidgetItem) {
                try { WidgetManager.deleteWidget(item.appWidgetId) } catch (_: Exception) { }
            }
        }
        mutableWorkspaceItems.value = mutableWorkspaceItems.value.filterNot { it.screenId == screenId }
    }
}

fun WorkspaceController.deleteScreen(screenId: Int) {
    if (mutableCurrentScreenCount.value <= 1) return
    scope.launch(Dispatchers.IO) {
        val itemsToRemove = mutableWorkspaceItems.value.filter { it.screenId == screenId }
        itemsToRemove.forEach { item ->
            if (item is WorkspaceWidgetItem) {
                try { WidgetManager.deleteWidget(item.appWidgetId) } catch (_: Exception) { }
            }
        }

        val afterRemoval = mutableWorkspaceItems.value.filterNot { it.screenId == screenId }
        val renumbered = afterRemoval.map { item ->
            if (item.screenId > screenId) {
                when (item) {
                    is WorkspaceAppItem -> item.copy(screenId = item.screenId - 1)
                    is WorkspaceWidgetItem -> item.copy(screenId = item.screenId - 1)
                    is WorkspaceFolderItem -> item.copy(screenId = item.screenId - 1)
                }
            } else item
        }
        mutableWorkspaceItems.value = renumbered
        mutableCurrentScreenCount.value -= 1
    }
}

fun WorkspaceController.createDrawerFolder(name: String, packages: List<String>) {
    val current = mutableDrawerFolders.value.toMutableList()
    current.add(DrawerFolderItem(UUID.randomUUID().toString(), name, packages))
    mutableDrawerFolders.value = current
    scope.launch { workspaceRepository.saveDrawerFolders(current) }
}

fun WorkspaceController.updateDrawerFolder(folderId: String, packages: List<String>) {
    val current = mutableDrawerFolders.value.toMutableList()
    val index = current.indexOfFirst { it.id == folderId }
    if (index != -1) {
        current[index] = current[index].copy(packages = packages)
        mutableDrawerFolders.value = current
        scope.launch { workspaceRepository.saveDrawerFolders(current) }
    }
}

fun WorkspaceController.deleteDrawerFolder(folderId: String) {
    val current = mutableDrawerFolders.value.toMutableList()
    current.removeAll { it.id == folderId }
    mutableDrawerFolders.value = current
    scope.launch { workspaceRepository.saveDrawerFolders(current) }
}

fun WorkspaceController.removeFromDrawerFolder(folderId: String, packageName: String) {
    val current = mutableDrawerFolders.value.toMutableList()
    val index = current.indexOfFirst { it.id == folderId }
    if (index != -1) {
        val folder = current[index]
        val newPackages = folder.packages - packageName
        if (newPackages.isEmpty()) {
            current.removeAt(index)
        } else {
            current[index] = folder.copy(packages = newPackages)
        }
        mutableDrawerFolders.value = current
        scope.launch { workspaceRepository.saveDrawerFolders(current) }
    }
}

fun WorkspaceController.renameDrawerFolder(folderId: String, newName: String) {
    val current = mutableDrawerFolders.value.toMutableList()
    val index = current.indexOfFirst { it.id == folderId }
    if (index != -1) {
        current[index] = current[index].copy(name = newName)
        mutableDrawerFolders.value = current
        scope.launch { workspaceRepository.saveDrawerFolders(current) }
    }
}

fun WorkspaceController.mergeIntoFolder(sourceItemId: String?, sourceAppPkg: String?, screenId: Int, cellX: Int, cellY: Int): Boolean {
    val currentItems = mutableWorkspaceItems.value.toMutableList()
    val targetItem = currentItems.find { it.screenId == screenId && it.cellX == cellX && it.cellY == cellY } ?: return false
    val pkgToAdd = sourceAppPkg ?: (currentItems.find { it.id == sourceItemId } as? WorkspaceAppItem)?.packageName ?: return false

    if (targetItem is WorkspaceFolderItem) {
        if (targetItem.packages.size >= 9) return false
        if (pkgToAdd !in targetItem.packages) {
            val newFolder = targetItem.copy(packages = targetItem.packages + pkgToAdd)
            currentItems[currentItems.indexOf(targetItem)] = newFolder
        } else return false
    } else if (targetItem is WorkspaceAppItem) {
        if (targetItem.packageName != pkgToAdd) {
            val newFolder = WorkspaceFolderItem(
                id = UUID.randomUUID().toString(),
                name = "Папка",
                packages = listOf(targetItem.packageName, pkgToAdd),
                screenId = screenId,
                cellX = cellX,
                cellY = cellY
            )
            currentItems.remove(targetItem)
            currentItems.add(newFolder)
        } else return false
    } else return false

    if (sourceItemId != null) {
        currentItems.removeAll { it.id == sourceItemId }
    }

    mutableWorkspaceItems.value = currentItems
    scheduleSave()
    return true
}

fun WorkspaceController.renameFolder(folderId: String, newName: String) {
    val currentItems = mutableWorkspaceItems.value.toMutableList()
    val index = currentItems.indexOfFirst { it.id == folderId }
    if (index != -1 && currentItems[index] is WorkspaceFolderItem) {
        currentItems[index] = (currentItems[index] as WorkspaceFolderItem).copy(name = newName)
        mutableWorkspaceItems.value = currentItems
        scheduleSave()
    }
}

fun WorkspaceController.removeFromFolder(folderId: String, packageName: String) {
    val current = mutableWorkspaceItems.value.toMutableList()
    val folder = current.find { it.id == folderId } as? WorkspaceFolderItem ?: return
    val newPackages = folder.packages - packageName
    current.remove(folder)

    if (newPackages.size == 1) {
        val app = allApps.value.find { it.packageName == newPackages[0] }
        if (app != null) {
            current.add(WorkspaceAppItem(UUID.randomUUID().toString(), app.packageName, app.label, folder.screenId, folder.cellX, folder.cellY))
        }
    } else if (newPackages.isNotEmpty()) {
        current.add(folder.copy(packages = newPackages))
    }

    mutableWorkspaceItems.value = current
    scheduleSave()
}

fun WorkspaceController.removeFromFolderAndMerge(folderId: String, packageName: String, screenId: Int, cellX: Int, cellY: Int) {
    removeFromFolder(folderId, packageName)
    mergeIntoFolder(null, packageName, screenId, cellX, cellY)
}