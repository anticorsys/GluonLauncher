// app/src/main/java/com/gluon/launcher/core/data/states/LauncherStates.kt
package com.gluon.launcher.core.data.states

import kotlinx.coroutines.flow.MutableStateFlow

enum class AppState { WELCOME, LOGIN, REGISTER, VERIFY_EMAIL, MAIN, SETTINGS, EDIT_PROFILE }

data class SettingsState(
    val isProfileGlass: Boolean,
    val showProfileAvatar: Boolean,
    val isDynamicStyle: Boolean,
    val themeMode: Int,
    val dockApps: List<String>,
    val gridColumns: Int,
    val showLabels: Boolean,
    val showDockLabels: Boolean,
    val showIconBorder: Boolean,
    val hiddenApps: Set<String>,
    val isDockBarHidden: Boolean,
    val isPredictiveDockEnabled: Boolean,
    val isWorkspaceLocked: Boolean,
    val showWorkspaceLabels: Boolean,
    val isDynamicDockBar: Boolean
)

data class PendingWidgetConfig(
    val appWidgetId: Int,
    val spanX: Int,
    val spanY: Int,
    val screenId: Int,
    val cellX: Int? = null,
    val cellY: Int? = null
)

// Глобальное состояние для управления точками уведомлений (Notification Dots)
object NotificationState {
    val notifications = MutableStateFlow<Map<String, Int>>(emptyMap())
}