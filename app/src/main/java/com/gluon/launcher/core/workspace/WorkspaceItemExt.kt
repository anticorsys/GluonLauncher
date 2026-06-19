// app/src/main/java/com/gluon/launcher/core/workspace/WorkspaceItemExt.kt
package com.gluon.launcher.core.workspace

import android.appwidget.AppWidgetProviderInfo
import com.gluon.launcher.core.data.AppModel
import com.gluon.launcher.core.data.WorkspaceAppItem
import com.gluon.launcher.core.data.WorkspaceFolderItem
import com.gluon.launcher.core.data.WorkspaceItem
import com.gluon.launcher.core.data.WorkspaceWidgetItem
import com.gluon.launcher.core.data.states.PendingWidgetConfig
import com.gluon.launcher.core.utils.GridValidator
import com.gluon.launcher.core.widget.WidgetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

internal fun WorkspaceController.displaceItemsIfNeeded(currentItems: List<WorkspaceItem>, screenId: Int, targetX: Int, targetY: Int, targetSpanX: Int, targetSpanY: Int, ignoreItemId: String?, currentGridRows: Int): Boolean {
    val overlapping = currentItems.filter { it.screenId == screenId && it.id != ignoreItemId && !(targetX >= it.cellX + it.spanX || targetX + targetSpanX <= it.cellX || targetY >= it.cellY + it.spanY || targetY + targetSpanY <= it.cellY) }
    if (overlapping.isEmpty()) return true

    val remainingItems = currentItems.filter { it !in overlapping }.toMutableList()
    val reservedItem = WorkspaceAppItem("reserved", "", "", screenId, targetX, targetY, spanX = targetSpanX, spanY = targetSpanY)
    remainingItems.add(reservedItem)

    val newItems = currentItems.toMutableList()
    for (item in overlapping) {
        val vac = GridValidator.findFirstVacantCell(remainingItems, screenId, item.spanX, item.spanY, themeManager.gridColumns.value, currentGridRows)
        if (vac != null) {
            val moved = when (item) {
                is WorkspaceAppItem -> item.copy(cellX = vac.first, cellY = vac.second)
                is WorkspaceWidgetItem -> item.copy(cellX = vac.first, cellY = vac.second)
                is WorkspaceFolderItem -> item.copy(cellX = vac.first, cellY = vac.second)
            }
            newItems[newItems.indexOf(item)] = moved
            remainingItems.add(moved)
        } else return false
    }

    mutableWorkspaceItems.value = newItems
    return true
}

internal fun updateItemLocation(item: WorkspaceItem, screenId: Int, cx: Int, cy: Int, spanX: Int, spanY: Int): WorkspaceItem {
    return when (item) {
        is WorkspaceAppItem -> item.copy(screenId = screenId, cellX = cx, cellY = cy, spanX = spanX, spanY = spanY)
        is WorkspaceWidgetItem -> item.copy(screenId = screenId, cellX = cx, cellY = cy, spanX = spanX, spanY = spanY)
        is WorkspaceFolderItem -> item.copy(screenId = screenId, cellX = cx, cellY = cy, spanX = spanX, spanY = spanY)
    }
}

fun WorkspaceController.removeHiddenWorkspaceApps(packageName: String) {
    mutableWorkspaceItems.value = mutableWorkspaceItems.value.filterNot {
        it is WorkspaceAppItem && it.packageName == packageName
    }
}

fun WorkspaceController.moveAppFromFolderToWorkspace(folderId: String, packageName: String, screenId: Int, cellX: Int, cellY: Int, currentGridRows: Int) {
    removeFromFolder(folderId, packageName)
    val app = allApps.value.find { it.packageName == packageName }
    if (app != null) {
        addAppToWorkspace(app, cellX, cellY, screenId, currentGridRows)
    }
}

fun WorkspaceController.addAppToWorkspace(app: AppModel, preferredCellX: Int? = null, preferredCellY: Int? = null, targetScreen: Int = 0, currentGridRows: Int) {
    scope.launch(Dispatchers.IO) {
        var placedCell: Pair<Int, Int>? = null

        if (preferredCellX != null && preferredCellY != null) {
            if (GridValidator.isAreaVacant(mutableWorkspaceItems.value, targetScreen, preferredCellX, preferredCellY, 1, 1, themeManager.gridColumns.value, currentGridRows)) {
                placedCell = preferredCellX to preferredCellY
            } else if (displaceItemsIfNeeded(mutableWorkspaceItems.value, targetScreen, preferredCellX, preferredCellY, 1, 1, null, currentGridRows)) {
                placedCell = preferredCellX to preferredCellY
            }
        }

        if (placedCell == null) {
            placedCell = GridValidator.findFirstVacantCell(mutableWorkspaceItems.value, targetScreen, 1, 1, themeManager.gridColumns.value, currentGridRows)
        }

        var finalScreen = targetScreen
        if (placedCell == null && mutableCurrentScreenCount.value < 5) {
            finalScreen = mutableCurrentScreenCount.value
            mutableCurrentScreenCount.value += 1
            placedCell = Pair(0, 0)
        }

        if (placedCell != null) {
            mutableWorkspaceItems.value += WorkspaceAppItem(UUID.randomUUID().toString(), app.packageName, app.label, finalScreen, placedCell.first, placedCell.second)
            updateScreenCountFromItems(mutableWorkspaceItems.value)
        }
    }
}

fun WorkspaceController.tryAddWidget(providerInfo: AppWidgetProviderInfo, screenId: Int, currentGridRows: Int) {
    scope.launch(Dispatchers.IO) {
        val appWidgetId = WidgetManager.allocateAppWidgetId()
        val spans = WidgetManager.getDefaultSpans(providerInfo, themeManager.gridColumns.value, currentGridRows)
        val bound = WidgetManager.bindWidget(appWidgetId, providerInfo)

        if (bound) {
            if (WidgetManager.needsConfiguration(providerInfo)) {
                onWidgetConfigPending(PendingWidgetConfig(appWidgetId, spans.first, spans.second, screenId))
                setAwaitingWidgetConfigure(true)
                val intent = WidgetManager.createConfigurationIntent(appWidgetId, providerInfo)
                if (intent != null) {
                    onWidgetConfigureRequest(intent)
                } else {
                    addWidgetToWorkspace(appWidgetId, spans.first, spans.second, screenId, currentGridRows = currentGridRows)
                }
            } else {
                addWidgetToWorkspace(appWidgetId, spans.first, spans.second, screenId, currentGridRows = currentGridRows)
            }
        } else {
            onWidgetConfigPending(PendingWidgetConfig(appWidgetId, spans.first, spans.second, screenId))
            onWidgetBindingRequest(WidgetManager.createBindWidgetIntent(appWidgetId, providerInfo))
        }
    }
}

fun WorkspaceController.addWidgetToWorkspace(appWidgetId: Int, spanX: Int, spanY: Int, preferredScreenId: Int = 0, preferredCellX: Int? = null, preferredCellY: Int? = null, currentGridRows: Int) {
    scope.launch(Dispatchers.IO) {
        val providerInfo = WidgetManager.appWidgetManager.getAppWidgetInfo(appWidgetId)
        if (providerInfo == null) {
            WidgetManager.deleteWidget(appWidgetId)
            return@launch
        }

        val finalSpanX = spanX.coerceIn(1, themeManager.gridColumns.value)
        val finalSpanY = spanY.coerceIn(1, currentGridRows)

        var targetScreen = preferredScreenId
        var placedCell: Pair<Int, Int>? = null

        if (preferredCellX != null && preferredCellY != null) {
            val overlap = GridValidator.getOverlappingItems(mutableWorkspaceItems.value, targetScreen, preferredCellX, preferredCellY, finalSpanX, finalSpanY)
            val canStack = overlap.isNotEmpty() && overlap.all { it is WorkspaceWidgetItem } && overlap.size < 5

            if (canStack) {
                val anchor = overlap.first() as WorkspaceWidgetItem
                mutableWorkspaceItems.value += WorkspaceWidgetItem(UUID.randomUUID().toString(), appWidgetId, targetScreen, anchor.cellX, anchor.cellY, anchor.spanX, anchor.spanY)
                return@launch
            } else if (GridValidator.isAreaVacant(mutableWorkspaceItems.value, targetScreen, preferredCellX, preferredCellY, finalSpanX, finalSpanY, themeManager.gridColumns.value, currentGridRows)) {
                placedCell = Pair(preferredCellX, preferredCellY)
            } else if (displaceItemsIfNeeded(mutableWorkspaceItems.value, targetScreen, preferredCellX, preferredCellY, finalSpanX, finalSpanY, null, currentGridRows)) {
                placedCell = Pair(preferredCellX, preferredCellY)
            }
        }

        if (placedCell == null) {
            placedCell = GridValidator.findFirstVacantCell(mutableWorkspaceItems.value, targetScreen, finalSpanX, finalSpanY, themeManager.gridColumns.value, currentGridRows)
        }
        if (placedCell == null) {
            for (screen in 0 until mutableCurrentScreenCount.value) {
                if (screen == targetScreen) continue
                val cell = GridValidator.findFirstVacantCell(mutableWorkspaceItems.value, screen, finalSpanX, finalSpanY, themeManager.gridColumns.value, currentGridRows)
                if (cell != null) {
                    placedCell = cell
                    targetScreen = screen
                    break
                }
            }
        }
        if (placedCell == null && mutableCurrentScreenCount.value < 5) {
            targetScreen = mutableCurrentScreenCount.value
            placedCell = Pair(0, 0)
            mutableCurrentScreenCount.value += 1
        }
        if (placedCell == null) {
            for (screen in 0 until mutableCurrentScreenCount.value) {
                if (displaceItemsIfNeeded(mutableWorkspaceItems.value, screen, 0, 0, finalSpanX, finalSpanY, null, currentGridRows)) {
                    val cell = GridValidator.findFirstVacantCell(mutableWorkspaceItems.value, screen, finalSpanX, finalSpanY, themeManager.gridColumns.value, currentGridRows)
                    if (cell != null) {
                        placedCell = cell
                        targetScreen = screen
                        break
                    }
                }
            }
            if (placedCell == null) {
                WidgetManager.deleteWidget(appWidgetId)
                return@launch
            }
        }

        mutableWorkspaceItems.value += WorkspaceWidgetItem(UUID.randomUUID().toString(), appWidgetId, targetScreen, placedCell.first, placedCell.second, finalSpanX, finalSpanY)
        updateScreenCountFromItems(mutableWorkspaceItems.value)
    }
}

fun WorkspaceController.moveWorkspaceItem(itemId: String, screenId: Int, cellX: Int, cellY: Int, currentGridRows: Int): Boolean {
    val currentItems = mutableWorkspaceItems.value
    val item = currentItems.find { it.id == itemId } ?: return false

    if (screenId >= mutableCurrentScreenCount.value) {
        if (mutableCurrentScreenCount.value < 5) mutableCurrentScreenCount.value = screenId + 1 else return false
    }

    val clampedX = cellX.coerceIn(0, maxOf(0, themeManager.gridColumns.value - item.spanX))
    val clampedY = cellY.coerceIn(0, maxOf(0, currentGridRows - item.spanY))

    val overlap = GridValidator.getOverlappingItems(currentItems, screenId, clampedX, clampedY, item.spanX, item.spanY, setOf(itemId))
    val isStacking = item is WorkspaceWidgetItem && overlap.isNotEmpty() && overlap.all { it is WorkspaceWidgetItem } && overlap.size < 5

    if (overlap.isEmpty() || isStacking) {
        val anchor = overlap.firstOrNull() as? WorkspaceWidgetItem
        val finalX = anchor?.cellX ?: clampedX
        val finalY = anchor?.cellY ?: clampedY
        val finalSpanX = anchor?.spanX ?: item.spanX
        val finalSpanY = anchor?.spanY ?: item.spanY

        mutableWorkspaceItems.value = currentItems.map {
            if (it.id == itemId) {
                when (it) {
                    is WorkspaceAppItem -> it.copy(screenId = screenId, cellX = finalX, cellY = finalY)
                    is WorkspaceWidgetItem -> it.copy(screenId = screenId, cellX = finalX, cellY = finalY, spanX = finalSpanX, spanY = finalSpanY)
                    is WorkspaceFolderItem -> it.copy(screenId = screenId, cellX = finalX, cellY = finalY)
                }
            } else it
        }
        updateScreenCountFromItems(mutableWorkspaceItems.value)
        return true
    } else if (displaceItemsIfNeeded(currentItems, screenId, clampedX, clampedY, item.spanX, item.spanY, itemId, currentGridRows)) {
        mutableWorkspaceItems.value = mutableWorkspaceItems.value.map {
            if (it.id == itemId) {
                when (it) {
                    is WorkspaceAppItem -> it.copy(screenId = screenId, cellX = clampedX, cellY = clampedY)
                    is WorkspaceWidgetItem -> it.copy(screenId = screenId, cellX = clampedX, cellY = clampedY)
                    is WorkspaceFolderItem -> it.copy(screenId = screenId, cellX = clampedX, cellY = clampedY)
                }
            } else it
        }
        updateScreenCountFromItems(mutableWorkspaceItems.value)
        return true
    }
    return false
}

fun WorkspaceController.revalidateWorkspaceItemsBounds(newCols: Int, currentGridRows: Int) {
    scope.launch(Dispatchers.IO) {
        val currentItems = mutableWorkspaceItems.value.toMutableList()
        val itemsToRelocate = currentItems.filter { it.cellX + it.spanX > newCols }
        currentItems.removeAll(itemsToRelocate)

        for (item in itemsToRelocate) {
            val newSpanX = item.spanX.coerceAtMost(newCols)

            val initialCell = GridValidator.findFirstVacantCell(currentItems, item.screenId, newSpanX, item.spanY, newCols, currentGridRows)
            if (initialCell != null) {
                currentItems.add(updateItemLocation(item, item.screenId, initialCell.first, initialCell.second, newSpanX, item.spanY))
            } else {
                var placedInFallback = false
                for (s in 0 until mutableCurrentScreenCount.value) {
                    val fallbackCell = GridValidator.findFirstVacantCell(currentItems, s, newSpanX, item.spanY, newCols, currentGridRows)
                    if (fallbackCell != null) {
                        currentItems.add(updateItemLocation(item, s, fallbackCell.first, fallbackCell.second, newSpanX, item.spanY))
                        placedInFallback = true
                        break
                    }
                }
                if (!placedInFallback) {
                    if (mutableCurrentScreenCount.value < 5) {
                        val newScreenId = mutableCurrentScreenCount.value
                        mutableCurrentScreenCount.value += 1
                        currentItems.add(updateItemLocation(item, newScreenId, 0, 0, newSpanX, item.spanY))
                    } else {
                        if (item is WorkspaceWidgetItem) {
                            try { WidgetManager.deleteWidget(item.appWidgetId) } catch (_: Exception) { }
                        }
                    }
                }
            }
        }
        mutableWorkspaceItems.value = currentItems
        updateScreenCountFromItems(currentItems)
    }
}

fun WorkspaceController.revalidateWorkspaceItemsRows(newRows: Int) {
    mutableCurrentGridRows.value = newRows
    scope.launch(Dispatchers.IO) {
        val currentItems = mutableWorkspaceItems.value.toMutableList()
        val itemsToRelocate = currentItems.filter { it.cellY + it.spanY > newRows }
        currentItems.removeAll(itemsToRelocate)

        for (item in itemsToRelocate) {
            val newSpanY = item.spanY.coerceAtMost(newRows)

            val initialCell = GridValidator.findFirstVacantCell(currentItems, item.screenId, item.spanX, newSpanY, themeManager.gridColumns.value, newRows)
            if (initialCell != null) {
                currentItems.add(updateItemLocation(item, item.screenId, initialCell.first, initialCell.second, item.spanX, newSpanY))
            } else {
                var placedInFallback = false
                for (s in 0 until mutableCurrentScreenCount.value) {
                    val fallbackCell = GridValidator.findFirstVacantCell(currentItems, s, item.spanX, newSpanY, themeManager.gridColumns.value, newRows)
                    if (fallbackCell != null) {
                        currentItems.add(updateItemLocation(item, s, fallbackCell.first, fallbackCell.second, item.spanX, newSpanY))
                        placedInFallback = true
                        break
                    }
                }
                if (!placedInFallback) {
                    if (mutableCurrentScreenCount.value < 5) {
                        val newScreenId = mutableCurrentScreenCount.value
                        mutableCurrentScreenCount.value += 1
                        currentItems.add(updateItemLocation(item, newScreenId, 0, 0, item.spanX, newSpanY))
                    } else {
                        if (item is WorkspaceWidgetItem) {
                            try { WidgetManager.deleteWidget(item.appWidgetId) } catch (_: Exception) { }
                        }
                    }
                }
            }
        }
        mutableWorkspaceItems.value = currentItems
        updateScreenCountFromItems(currentItems)
    }
}

fun WorkspaceController.removeWorkspaceItem(itemId: String, scatterFolder: Boolean = false, currentGridRows: Int = 6) {
    scope.launch(Dispatchers.IO) {
        val currentItemsList = mutableWorkspaceItems.value.toMutableList()
        val itemToRemove = currentItemsList.find { it.id == itemId } ?: return@launch
        if (itemToRemove is WorkspaceWidgetItem) {
            try { WidgetManager.deleteWidget(itemToRemove.appWidgetId) } catch (_: Exception) { }
        }

        currentItemsList.remove(itemToRemove)

        if (scatterFolder && itemToRemove is WorkspaceFolderItem) {
            val appsToAdd = itemToRemove.packages.mapNotNull { pkg -> allApps.value.find { it.packageName == pkg } }
            appsToAdd.forEach { app ->
                var placedCell = GridValidator.findFirstVacantCell(currentItemsList, itemToRemove.screenId, 1, 1, themeManager.gridColumns.value, currentGridRows)
                var targetScreen = itemToRemove.screenId
                if (placedCell == null) {
                    for (s in 0 until mutableCurrentScreenCount.value) {
                        placedCell = GridValidator.findFirstVacantCell(currentItemsList, s, 1, 1, themeManager.gridColumns.value, currentGridRows)
                        if (placedCell != null) {
                            targetScreen = s
                            break
                        }
                    }
                }
                if (placedCell == null && mutableCurrentScreenCount.value < 5) {
                    targetScreen = mutableCurrentScreenCount.value
                    mutableCurrentScreenCount.value += 1
                    placedCell = Pair(0, 0)
                }
                if (placedCell != null) {
                    currentItemsList.add(WorkspaceAppItem(UUID.randomUUID().toString(), app.packageName, app.label, targetScreen, placedCell.first, placedCell.second))
                }
            }
        }

        mutableWorkspaceItems.value = currentItemsList
        updateScreenCountFromItems(currentItemsList)
    }
}

fun WorkspaceController.resizeWidget(itemId: String, newSpanX: Int, newSpanY: Int, currentGridRows: Int): Boolean {
    val currentItems = mutableWorkspaceItems.value
    val item = currentItems.find { it.id == itemId } as? WorkspaceWidgetItem ?: return false

    val clampedSpanX = newSpanX.coerceIn(1, themeManager.gridColumns.value)
    val clampedSpanY = newSpanY.coerceIn(1, currentGridRows)

    val stackIds = currentItems.filter {
        it is WorkspaceWidgetItem && it.cellX == item.cellX && it.cellY == item.cellY && it.screenId == item.screenId
    }.map { it.id }.toSet()

    val isValid = GridValidator.isAreaVacant(currentItems, item.screenId, item.cellX, item.cellY, clampedSpanX, clampedSpanY, themeManager.gridColumns.value, currentGridRows, stackIds)
    if (!isValid) return false

    mutableWorkspaceItems.value = currentItems.map {
        if (it.id in stackIds && it is WorkspaceWidgetItem) {
            it.copy(spanX = clampedSpanX, spanY = clampedSpanY)
        } else it
    }

    return true
}