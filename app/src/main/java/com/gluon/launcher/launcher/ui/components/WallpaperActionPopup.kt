// app/src/main/java/com/gluon/launcher/launcher/components/WallpaperActionPopup.kt
package com.gluon.launcher.launcher.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

enum class WorkspaceMenuState {
    MAIN, SCREENS
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WallpaperActionPopup(
    onWallpaper: () -> Unit,
    onSettings: () -> Unit,
    onAddWidget: () -> Unit = {},
    onAddScreen: () -> Unit = {},
    onRequestRemoveScreen: () -> Unit = {},
    onRequestClearScreen: () -> Unit = {},
    totalScreens: Int = 1,
    maxScreens: Int = 5,
    isWorkspaceLocked: Boolean = false,
    onToggleWorkspaceLock: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var menuState by remember { mutableStateOf(WorkspaceMenuState.MAIN) }
    val iconSize = 28.dp
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    AnimatedContent(
        targetState = menuState,
        transitionSpec = {
            (fadeIn(spring(dampingRatio = 0.8f, stiffness = 400f)) + slideInVertically(spring(dampingRatio = 0.8f, stiffness = 400f)) { it / 3 }) togetherWith
                    (fadeOut(spring(dampingRatio = 0.9f, stiffness = 500f)) + slideOutVertically(spring(dampingRatio = 0.9f, stiffness = 500f)) { it / 3 })
        },
        label = "MenuTransition"
    ) { state ->
        Column(
            modifier = Modifier
                .width(250.dp)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            when (state) {
                WorkspaceMenuState.MAIN -> {
                    GluonPopupMenuCard(
                        onClick = {
                            onWallpaper()
                            onDismiss()
                        }
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize).padding(end = 10.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        GluonMarqueeText("Обновить обои", modifier = Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        GluonPopupMenuCard(
                            modifier = Modifier.weight(1f),
                            enabled = !isWorkspaceLocked,
                            onClick = {
                                if (!isWorkspaceLocked) menuState = WorkspaceMenuState.SCREENS
                            }
                        ) {
                            Icon(
                                Icons.Default.Layers,
                                contentDescription = null,
                                modifier = Modifier.size(iconSize).padding(end = 10.dp),
                                tint = LocalContentColor.current
                            )
                            GluonMarqueeText("Рабочий стол", modifier = Modifier.weight(1f))
                        }

                        // ИСПРАВЛЕНИЕ: Уменьшена ширина до 52.dp. Активация перенесена на onLongClick.
                        GluonPopupMenuCard(
                            modifier = Modifier.width(52.dp),
                            enabled = true,
                            horizontalPadding = 0.dp,
                            onClick = {
                                Toast.makeText(context, "Удерживайте для переключения", Toast.LENGTH_SHORT).show()
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleWorkspaceLock()
                            }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isWorkspaceLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Блокировка",
                                    modifier = Modifier.size(24.dp),
                                    tint = if (isWorkspaceLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    GluonPopupMenuCard(
                        enabled = !isWorkspaceLocked,
                        onClick = {
                            if (!isWorkspaceLocked) {
                                onAddWidget()
                                onDismiss()
                            }
                        }
                    ) {
                        Icon(
                            if (isWorkspaceLocked) Icons.Default.Lock else Icons.Default.Widgets,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize).padding(end = 10.dp),
                            tint = LocalContentColor.current
                        )
                        GluonMarqueeText("Добавить виджет", modifier = Modifier.weight(1f))
                    }

                    GluonPopupMenuCard(
                        onClick = {
                            onSettings()
                            onDismiss()
                        }
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize).padding(end = 10.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        GluonMarqueeText("Настройки Gluon", modifier = Modifier.weight(1f))
                    }
                }

                WorkspaceMenuState.SCREENS -> {
                    GluonPopupMenuCard(
                        onClick = { menuState = WorkspaceMenuState.MAIN }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize).padding(end = 10.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        GluonMarqueeText("Назад", modifier = Modifier.weight(1f))
                    }

                    GluonPopupMenuCard(
                        enabled = !isWorkspaceLocked,
                        onClick = {
                            if (!isWorkspaceLocked) {
                                onRequestClearScreen()
                                onDismiss()
                            }
                        }
                    ) {
                        Icon(if (isWorkspaceLocked) Icons.Default.Lock else Icons.Default.Delete, null, Modifier.size(iconSize).padding(end = 10.dp), tint = LocalContentColor.current)
                        GluonMarqueeText("Очистить экран", modifier = Modifier.weight(1f))
                    }

                    val isAddEnabled = totalScreens < maxScreens && !isWorkspaceLocked
                    GluonPopupMenuCard(
                        enabled = isAddEnabled,
                        onClick = {
                            if (isAddEnabled) {
                                onAddScreen()
                                onDismiss()
                            }
                        }
                    ) {
                        Icon(
                            if (isWorkspaceLocked) Icons.Default.Lock else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize).padding(end = 10.dp),
                            tint = LocalContentColor.current
                        )
                        GluonMarqueeText("Создать экран", modifier = Modifier.weight(1f))
                    }

                    val isRemoveEnabled = totalScreens > 1 && !isWorkspaceLocked
                    GluonPopupMenuCard(
                        enabled = isRemoveEnabled,
                        onClick = {
                            if (isRemoveEnabled) {
                                onRequestRemoveScreen()
                                onDismiss()
                            }
                        }
                    ) {
                        Icon(
                            if (isWorkspaceLocked) Icons.Default.Lock else Icons.Default.Remove,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize).padding(end = 10.dp),
                            tint = LocalContentColor.current
                        )
                        GluonMarqueeText("Удалить экран", modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}