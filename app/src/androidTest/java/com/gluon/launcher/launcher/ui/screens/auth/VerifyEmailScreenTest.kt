package com.gluon.launcher.launcher.ui.screens.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.gluon.launcher.core.auth.AuthManager
import com.gluon.launcher.core.data.RetrofitClient
import org.junit.Rule
import org.junit.Test

class VerifyEmailScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun verifyEmailScreen_displaysTitle() {
        composeTestRule.setContent {
            VerifyEmailScreen(
                email = "test@example.com",
                authManager = AuthManager(
                    context = InstrumentationRegistry.getInstrumentation().targetContext,
                    retrofitClient = RetrofitClient()
                ),
                onVerified = {},
                onBack = {}
            )
        }
        composeTestRule.onNodeWithText("ВЕРИФИКАЦИЯ").assertExists()
    }

    @Test
    fun verifyEmailScreen_hasResendButton() {
        composeTestRule.setContent {
            VerifyEmailScreen(
                email = "test@example.com",
                authManager = AuthManager(
                    context = InstrumentationRegistry.getInstrumentation().targetContext,
                    retrofitClient = RetrofitClient()
                ),
                onVerified = {},
                onBack = {}
            )
        }
        // Используем substring = true, потому что кнопка может содержать таймер (например, "Отправить повторно (30с)")
        composeTestRule.onNodeWithText("Отправить повторно", substring = true).assertExists()
    }
}