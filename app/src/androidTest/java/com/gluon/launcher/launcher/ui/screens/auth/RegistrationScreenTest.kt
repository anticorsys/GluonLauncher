package com.gluon.launcher.launcher.ui.screens.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class RegistrationScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun registrationScreen_hasCreateAccountButton() {
        composeTestRule.setContent {
            RegistrationScreen(
                authManager = com.gluon.launcher.core.auth.AuthManager(
                    context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext,
                    retrofitClient = com.gluon.launcher.core.data.RetrofitClient()
                ), // используем реальный, но в тесте он не вызывается
                onBack = {},
                onRegistrationSuccess = {}
            )
        }
        composeTestRule.onNodeWithText("СОЗДАТЬ АККАУНТ").assertExists()
    }

    @Test
    fun registrationScreen_hasNameField() {
        composeTestRule.setContent {
            RegistrationScreen(
                authManager = com.gluon.launcher.core.auth.AuthManager(
                    context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext,
                    retrofitClient = com.gluon.launcher.core.data.RetrofitClient()
                ),
                onBack = {},
                onRegistrationSuccess = {}
            )
        }
        composeTestRule.onNodeWithText("ФИО").assertExists()
    }

    @Test
    fun registrationScreen_hasEmailField() {
        composeTestRule.setContent {
            RegistrationScreen(
                authManager = com.gluon.launcher.core.auth.AuthManager(
                    context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext,
                    retrofitClient = com.gluon.launcher.core.data.RetrofitClient()
                ),
                onBack = {},
                onRegistrationSuccess = {}
            )
        }
        composeTestRule.onNodeWithText("Email").assertExists()
    }

    @Test
    fun registrationScreen_hasPasswordField() {
        composeTestRule.setContent {
            RegistrationScreen(
                authManager = com.gluon.launcher.core.auth.AuthManager(
                    context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext,
                    retrofitClient = com.gluon.launcher.core.data.RetrofitClient()
                ),
                onBack = {},
                onRegistrationSuccess = {}
            )
        }
        composeTestRule.onNodeWithText("Пароль").assertExists()
    }
}