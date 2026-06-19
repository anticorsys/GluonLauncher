// app/src/main/java/com/gluon/launcher/core/data/WorkspaceModel.kt
package com.gluon.launcher.core.data

import androidx.compose.runtime.Immutable

sealed interface WorkspaceItem {
    val id: String
    val screenId: Int
    val cellX: Int
    val cellY: Int
    val spanX: Int
    val spanY: Int
}

@Immutable
data class WorkspaceAppItem(
    override val id: String,
    val packageName: String,
    val label: String,
    override val screenId: Int,
    override val cellX: Int,
    override val cellY: Int,
    override val spanX: Int = 1,
    override val spanY: Int = 1
) : WorkspaceItem

@Immutable
data class WorkspaceWidgetItem(
    override val id: String,
    val appWidgetId: Int,
    override val screenId: Int,
    override val cellX: Int,
    override val cellY: Int,
    override val spanX: Int,
    override val spanY: Int
) : WorkspaceItem

@Immutable
data class WorkspaceFolderItem(
    override val id: String,
    val name: String,
    val packages: List<String>,
    override val screenId: Int,
    override val cellX: Int,
    override val cellY: Int,
    override val spanX: Int = 1,
    override val spanY: Int = 1
) : WorkspaceItem

@Immutable
data class DrawerFolderItem(
    val id: String,
    val name: String,
    val packages: List<String>
)