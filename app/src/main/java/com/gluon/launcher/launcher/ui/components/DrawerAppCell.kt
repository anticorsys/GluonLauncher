package com.gluon.launcher.launcher.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.gluon.launcher.core.data.AppModel

@Composable
fun DrawerAppCell(
    app: AppModel,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    iconSize: Dp,
    showLabels: Boolean,
    showIconBorder: Boolean,
    isWorkspaceLocked: Boolean,
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
    onEnterSelectionMode: (AppModel) -> Unit,
    showCategoryOption: Boolean
) {
    Box(modifier = Modifier, contentAlignment = Alignment.TopCenter) {
        Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)) {
            AppIconItem(
                app = app,
                showLabel = showLabels,
                showIconBorder = showIconBorder,
                isInGrid = true,
                onAppClick = {
                    if (isSelectionMode) {
                        onToggleSelection(app)
                    } else {
                        onAppClick(app)
                    }
                },
                onDragStart = if (isWorkspaceLocked || isSelectionMode) null else { { onDragStart(app) } },
                iconSizeOverride = iconSize,
                showContextMenu = !isSelectionMode,
                onContextMenu = { dismiss ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (!isSelectionMode) {
                            GluonPopupMenuCard(onClick = { dismiss(); onEnterSelectionMode(app) }) {
                                Icon(Icons.Default.CreateNewFolder, null, Modifier.size(28.dp).padding(end = 10.dp), tint = MaterialTheme.colorScheme.onSurface)
                                GluonMarqueeText("В папку")
                            }
                        }
                        if (!isDockBarHidden) {
                            val isInDock = dockApps.contains(app.packageName)
                            val canAddToDock = isInDock || dockApps.size < maxDockApps
                            if (canAddToDock) {
                                GluonPopupMenuCard(onClick = { dismiss(); onToggleDockApp(app.packageName) }) {
                                    Icon(if (isInDock) Icons.Default.Close else Icons.Default.Add, null, Modifier.size(28.dp).padding(end = 10.dp), tint = MaterialTheme.colorScheme.onSurface)
                                    GluonMarqueeText(if (isInDock) "Убрать из док-бара" else "Добавить в док-бар")
                                }
                            }
                        }
                        if (showCategoryOption) {
                            GluonPopupMenuCard(onClick = { dismiss(); onCategorize(app) }) {
                                Icon(Icons.Default.Edit, null, Modifier.size(28.dp).padding(end = 10.dp), tint = MaterialTheme.colorScheme.onSurface)
                                GluonMarqueeText("Категория")
                            }
                        }
                        GluonPopupMenuCard(onClick = { dismiss(); onHideApp(app.packageName) }) {
                            Icon(Icons.Default.VisibilityOff, null, Modifier.size(28.dp).padding(end = 10.dp), tint = MaterialTheme.colorScheme.onSurface)
                            GluonMarqueeText("Скрыть")
                        }
                        GluonPopupMenuCard(onClick = { dismiss(); val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${app.packageName}".toUri()); context.startActivity(intent) }) {
                            Icon(Icons.Default.Info, null, Modifier.size(28.dp).padding(end = 10.dp), tint = MaterialTheme.colorScheme.onSurface)
                            GluonMarqueeText("О приложении")
                        }
                    }
                }
            )
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp).background(MaterialTheme.colorScheme.surface, CircleShape))
            }
        }
    }
}