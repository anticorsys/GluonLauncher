// app/src/main/java/com/gluon/launcher/launcher/ui/AppNavigation.kt
@file:Suppress("PackageDirectoryMismatch")
package com.gluon.launcher.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gluon.launcher.MainViewModel
import com.gluon.launcher.animation.SpringAnimations
import com.gluon.launcher.core.auth.AuthManager
import com.gluon.launcher.core.auth.ProfileManager
import com.gluon.launcher.core.data.AppModel
import com.gluon.launcher.core.data.states.AppState
import com.gluon.launcher.core.data.states.SettingsState
import com.gluon.launcher.core.theme.LocalThemeSystemBars
import com.gluon.launcher.launcher.ui.screens.DashboardScreen
import com.gluon.launcher.launcher.ui.screens.DashboardScreenParams
import com.gluon.launcher.launcher.ui.screens.EditProfileScreen
import com.gluon.launcher.launcher.ui.screens.SettingsScreen
import com.gluon.launcher.launcher.ui.screens.auth.LoginScreen
import com.gluon.launcher.launcher.ui.screens.auth.RegistrationScreen
import com.gluon.launcher.launcher.ui.screens.auth.VerifyEmailScreen
import com.gluon.launcher.launcher.ui.screens.auth.WelcomeScreen
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppNavigation(
    appState: AppState,
    viewModel: MainViewModel,
    authManager: AuthManager,
    profileManager: ProfileManager,
    settingsState: SettingsState,
    allApps: List<AppModel>,
    onStateChange: (AppState) -> Unit
) {
    val scope = rememberCoroutineScope()
    val user by authManager.currentUser.collectAsStateWithLifecycle()
    val isGuest by authManager.isGuest.collectAsStateWithLifecycle()
    var pendingEmail by remember { mutableStateOf("") }
    val hiddenAppModels by viewModel.hiddenAppModels.collectAsStateWithLifecycle()
    val systemBars = LocalThemeSystemBars.current
    val homePressCount by viewModel.homePressCount.collectAsStateWithLifecycle()
    val isDockBarHidden by viewModel.isDockBarHidden.collectAsStateWithLifecycle()

    // Обновления
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsStateWithLifecycle()

    LaunchedEffect(appState) {
        systemBars.isDashboard.value = (appState == AppState.MAIN)
    }

    BackHandler(enabled = appState != AppState.MAIN && appState != AppState.WELCOME) {
        when (appState) {
            AppState.SETTINGS -> onStateChange(AppState.MAIN)
            AppState.EDIT_PROFILE -> onStateChange(AppState.SETTINGS)
            AppState.VERIFY_EMAIL -> onStateChange(AppState.SETTINGS)
            AppState.LOGIN -> onStateChange(AppState.WELCOME)
            AppState.REGISTER -> onStateChange(AppState.WELCOME)
            else -> onStateChange(AppState.MAIN)
        }
    }

    BackHandler(enabled = appState == AppState.MAIN) { }

    val transitionSpec: AnimatedContentTransitionScope<AppState>.() -> ContentTransform = {
        (fadeIn(animationSpec = SpringAnimations.standard) + scaleIn(initialScale = 0.90f, animationSpec = SpringAnimations.standard))
            .togetherWith(fadeOut(animationSpec = SpringAnimations.standard) + scaleOut(targetScale = 0.96f, animationSpec = SpringAnimations.standard))
            .using(SizeTransform(clip = false))
    }

    AnimatedContent(targetState = appState, transitionSpec = transitionSpec, label = "GluonNavigation", modifier = Modifier.fillMaxSize()) { targetAppState ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (targetAppState) {
                AppState.WELCOME -> WelcomeScreen(onLogin = { onStateChange(AppState.LOGIN) }, onRegister = { onStateChange(AppState.REGISTER) }, onGuest = { viewModel.setGuestMode(true); onStateChange(AppState.MAIN) })
                AppState.LOGIN -> LoginScreen(authManager = authManager, onBack = { onStateChange(AppState.WELCOME) }, onSuccess = { onStateChange(AppState.MAIN) })
                AppState.REGISTER -> RegistrationScreen(authManager = authManager, onBack = { onStateChange(AppState.WELCOME) }, onRegistrationSuccess = { onStateChange(AppState.MAIN) })
                AppState.VERIFY_EMAIL -> VerifyEmailScreen(email = pendingEmail, authManager = authManager, onVerified = { onStateChange(AppState.SETTINGS) }, onBack = { onStateChange(AppState.SETTINGS) })
                AppState.MAIN -> DashboardScreen(
                    params = DashboardScreenParams(
                        viewModel = viewModel,
                        dockApps = settingsState.dockApps,
                        allApps = allApps,
                        gridColumns = settingsState.gridColumns,
                        showLabels = settingsState.showLabels,
                        showDockLabels = settingsState.showDockLabels,
                        showIconBorder = settingsState.showIconBorder,
                        isDockBarHidden = isDockBarHidden,
                        onDockAppsChange = viewModel::onDockAppsChange,
                        onHideApp = viewModel::hideApp,
                        onOpenSettings = { onStateChange(AppState.SETTINGS) },
                        onEditProfile = { if (!isGuest) onStateChange(AppState.EDIT_PROFILE) else onStateChange(AppState.SETTINGS) },
                        onToggleDockBarHidden = viewModel::onDockBarHiddenToggle,
                        homePressCount = homePressCount,
                        isProfileGlass = settingsState.isProfileGlass,
                        showProfileAvatar = settingsState.showProfileAvatar,
                        userName = user?.fullName ?: "Гость",
                        avatarUrl = user?.getAvatarUrl(),
                        onToggleInfoPanel = viewModel::onProfileGlassToggle,
                        isWorkspaceLocked = settingsState.isWorkspaceLocked,
                        showWorkspaceLabels = settingsState.showWorkspaceLabels,
                        isDynamicDockBar = settingsState.isDynamicDockBar
                    )
                )
                AppState.SETTINGS -> SettingsScreen(
                    authManager = authManager,
                    onBack = { onStateChange(AppState.MAIN) },
                    onLogout = { scope.launch { withContext(NonCancellable) { viewModel.logout() } } },
                    onEditProfile = { if (!isGuest) onStateChange(AppState.EDIT_PROFILE) },
                    onDeleteAccount = { scope.launch { viewModel.deleteAccount {} } },
                    onNavigateToAuth = { onStateChange(AppState.WELCOME) },
                    onVerifyEmail = { pendingEmail = user?.email ?: ""; onStateChange(AppState.VERIFY_EMAIL) },
                    isProfileGlass = settingsState.isProfileGlass,
                    showProfileAvatar = settingsState.showProfileAvatar,
                    onToggleProfileGlass = viewModel::onProfileGlassToggle,
                    onToggleShowAvatar = viewModel::onProfileAvatarToggle,
                    themeMode = settingsState.themeMode,
                    isDynamicStyle = settingsState.isDynamicStyle,
                    onThemeModeChange = viewModel::onThemeModeChange,
                    onDynamicStyleToggle = viewModel::onDynamicStyleToggle,
                    gridColumns = settingsState.gridColumns,
                    onGridColumnsChange = { cols -> viewModel.onGridColumnsChange(cols, viewModel.currentGridRows.value) },
                    showDockLabels = settingsState.showDockLabels,
                    onShowDockLabelsChange = viewModel::onShowDockLabelsChange,
                    showLabels = settingsState.showLabels,
                    onShowLabelsChange = viewModel::onShowLabelsChange,
                    showIconBorder = settingsState.showIconBorder,
                    onShowIconBorderChange = viewModel::onShowIconBorderChange,
                    hiddenApps = settingsState.hiddenApps,
                    hiddenAppModels = hiddenAppModels,
                    onUnhideApp = viewModel::onUnhideApp,
                    isDockBarHidden = isDockBarHidden,
                    onDockBarHiddenToggle = viewModel::onDockBarHiddenToggle,
                    isPredictiveDockEnabled = viewModel.predictiveDockManager.isPredictiveEnabled.collectAsStateWithLifecycle().value,
                    onPredictiveDockToggle = viewModel.predictiveDockManager::setPredictiveEnabled,
                    isWorkspaceLocked = settingsState.isWorkspaceLocked,
                    onWorkspaceLockedToggle = viewModel::onWorkspaceLockedToggle,
                    showWorkspaceLabels = settingsState.showWorkspaceLabels,
                    onShowWorkspaceLabelsChange = viewModel::onShowWorkspaceLabelsChange,
                    isDynamicDockBar = settingsState.isDynamicDockBar,
                    onDynamicDockBarToggle = viewModel::onDynamicDockBarToggle,
                    updateInfo = updateInfo,
                    isCheckingUpdate = isCheckingUpdate,
                    onCheckUpdates = viewModel::checkForUpdatesManual,
                    onDownloadUpdate = viewModel::downloadUpdate
                )
                AppState.EDIT_PROFILE -> {
                    if (isGuest) { LaunchedEffect(Unit) { onStateChange(AppState.MAIN) } }
                    else { EditProfileScreen(authManager, profileManager, onBack = { onStateChange(AppState.SETTINGS) }, onSuccess = { onStateChange(AppState.SETTINGS) }) }
                }
            }
        }
    }
}