package com.gluon.launcher.launcher.ui.screens.dashboard

import android.content.Context
import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun DashboardSystemDialogs(
    showDeleteScreenDialog: Boolean,
    showClearScreenDialog: Boolean,
    showAccessibilityDialog: Boolean,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissClear: () -> Unit,
    onConfirmClear: () -> Unit,
    onDismissAccessibility: () -> Unit,
    context: Context
) {
    if (showDeleteScreenDialog) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Удалить экран?", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Все элементы на этом экране будут безвозвратно удалены.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = onConfirmDelete) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) { Text("Отмена", color = MaterialTheme.colorScheme.primary) }
            }
        )
    }

    if (showClearScreenDialog) {
        AlertDialog(
            onDismissRequest = onDismissClear,
            title = { Text("Очистить экран?", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Все значки и виджеты на этом экране будут удалены.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = onConfirmClear) { Text("Очистить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = onDismissClear) { Text("Отмена", color = MaterialTheme.colorScheme.primary) }
            }
        )
    }

    if (showAccessibilityDialog) {
        AlertDialog(
            onDismissRequest = onDismissAccessibility,
            title = { Text("Необходимо разрешение", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Для открытия панели уведомлений жестом вниз нужно включить специальные возможности.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    onDismissAccessibility()
                    context.startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }) { Text("Включить", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = onDismissAccessibility) { Text("Отмена", color = MaterialTheme.colorScheme.primary) }
            }
        )
    }
}