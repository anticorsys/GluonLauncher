package com.gluon.launcher.launcher.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.gluon.launcher.core.auth.AuthManager
import com.gluon.launcher.core.data.RetrofitClient
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsScreen_hasProfileSection() {
        composeTestRule.setContent {
            SettingsScreen(
                authManager = AuthManager(
                    context = InstrumentationRegistry.getInstrumentation().targetContext,
                    retrofitClient = RetrofitClient()
                ),
                onBack = {},
                onLogout = {},
                onEditProfile = {},
                onDeleteAccount = {},
                onNavigateToAuth = {},
                onVerifyEmail = {},
                isProfileGlass = true,
                showProfileAvatar = true,
                onToggleProfileGlass = {},
                onToggleShowAvatar = {},
                themeMode = 0,
                isDynamicStyle = false,
                onThemeModeChange = {},
                onDynamicStyleToggle = {},
                gridColumns = 4,
                onGridColumnsChange = {},
                showDockLabels = false,
                onShowDockLabelsChange = {},
                showLabels = true,
                onShowLabelsChange = {},
                showIconBorder = false,
                onShowIconBorderChange = {},
                hiddenApps = emptySet(),
                hiddenAppModels = emptyMap(),
                onUnhideApp = {},
                isDockBarHidden = false,
                onDockBarHiddenToggle = {},
                isPredictiveDockEnabled = false,
                onPredictiveDockToggle = {},
                isWorkspaceLocked = false,
                onWorkspaceLockedToggle = {},
                showWorkspaceLabels = true,
                onShowWorkspaceLabelsChange = {},
                isDynamicDockBar = false,
                onDynamicDockBarToggle = {},
                updateInfo = null,
                isCheckingUpdate = false,
                onCheckUpdates = { callback -> callback(false) },
                onDownloadUpdate = {}
            )
        }
        // Заголовки в ModernExpandableSection преобразуются в uppercase
        composeTestRule.onNodeWithText("ПРОФИЛЬ").assertExists()
    }

    @Test
    fun settingsScreen_hasAppearanceSection() {
        composeTestRule.setContent {
            SettingsScreen(
                authManager = AuthManager(
                    context = InstrumentationRegistry.getInstrumentation().targetContext,
                    retrofitClient = RetrofitClient()
                ),
                onBack = {},
                onLogout = {},
                onEditProfile = {},
                onDeleteAccount = {},
                onNavigateToAuth = {},
                onVerifyEmail = {},
                isProfileGlass = true,
                showProfileAvatar = true,
                onToggleProfileGlass = {},
                onToggleShowAvatar = {},
                themeMode = 0,
                isDynamicStyle = false,
                onThemeModeChange = {},
                onDynamicStyleToggle = {},
                gridColumns = 4,
                onGridColumnsChange = {},
                showDockLabels = false,
                onShowDockLabelsChange = {},
                showLabels = true,
                onShowLabelsChange = {},
                showIconBorder = false,
                onShowIconBorderChange = {},
                hiddenApps = emptySet(),
                hiddenAppModels = emptyMap(),
                onUnhideApp = {},
                isDockBarHidden = false,
                onDockBarHiddenToggle = {},
                isPredictiveDockEnabled = false,
                onPredictiveDockToggle = {},
                isWorkspaceLocked = false,
                onWorkspaceLockedToggle = {},
                showWorkspaceLabels = true,
                onShowWorkspaceLabelsChange = {},
                isDynamicDockBar = false,
                onDynamicDockBarToggle = {},
                updateInfo = null,
                isCheckingUpdate = false,
                onCheckUpdates = { callback -> callback(false) },
                onDownloadUpdate = {}
            )
        }
        composeTestRule.onNodeWithText("ОФОРМЛЕНИЕ").assertExists()
    }
}