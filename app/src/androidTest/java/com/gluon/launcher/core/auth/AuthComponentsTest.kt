package com.gluon.launcher.launcher.ui.screens.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class AuthComponentsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun authTextField_displaysLabel() {
        composeTestRule.setContent {
            AuthTextField(
                value = "",
                onValueChange = {},
                label = "Email",
                icon = Icons.Default.Email
            )
        }
        composeTestRule.onNodeWithText("Email").assertExists()
    }

    @Test
    fun authGroupContainer_displaysTitle() {
        composeTestRule.setContent {
            AuthGroupContainer(
                title = "Тестовая группа",
                icon = Icons.Default.Email,
                content = {}
            )
        }
        composeTestRule.onNodeWithText("ТЕСТОВАЯ ГРУППА").assertExists()
    }
}