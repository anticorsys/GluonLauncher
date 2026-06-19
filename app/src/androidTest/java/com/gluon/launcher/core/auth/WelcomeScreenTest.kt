package com.gluon.launcher.launcher.ui.screens.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class WelcomeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun welcomeScreen_hasCreateAccountButton() {
        composeTestRule.setContent {
            WelcomeScreen(onLogin = {}, onRegister = {}, onGuest = {})
        }
        composeTestRule.onNodeWithText("СОЗДАТЬ АККАУНТ").assertExists()
    }

    @Test
    fun welcomeScreen_hasLoginButton() {
        composeTestRule.setContent {
            WelcomeScreen(onLogin = {}, onRegister = {}, onGuest = {})
        }
        composeTestRule.onNodeWithText("ВОЙТИ").assertExists()
    }

    @Test
    fun welcomeScreen_hasGuestButton() {
        composeTestRule.setContent {
            WelcomeScreen(onLogin = {}, onRegister = {}, onGuest = {})
        }
        composeTestRule.onNodeWithText("ВОЙТИ КАК ГОСТЬ").assertExists()
    }
}