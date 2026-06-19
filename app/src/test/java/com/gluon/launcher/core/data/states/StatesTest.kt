package com.gluon.launcher.core.data.states

import org.junit.Assert.*
import org.junit.Test

class StatesTest {

    @Test
    fun `NotificationState - начальное состояние`() {
        // Уведомления изначально должны быть пустыми
        assertTrue(NotificationState.notifications.value.isEmpty())
    }

    @Test
    fun `PendingWidgetConfig - создание и поля`() {
        val config = PendingWidgetConfig(
            appWidgetId = 42,
            spanX = 3,
            spanY = 2,
            screenId = 1,
            cellX = 0,
            cellY = 1
        )
        assertEquals(42, config.appWidgetId)
        assertEquals(3, config.spanX)
        assertEquals(2, config.spanY)
        assertEquals(1, config.screenId)
        assertEquals(0, config.cellX)
        assertEquals(1, config.cellY)
    }

    @Test
    fun `AppState - все состояния`() {
        // Просто проверяем, что enum содержит нужные значения
        val states = AppState.entries
        assertTrue(states.contains(AppState.WELCOME))
        assertTrue(states.contains(AppState.LOGIN))
        assertTrue(states.contains(AppState.REGISTER))
        assertTrue(states.contains(AppState.VERIFY_EMAIL))
        assertTrue(states.contains(AppState.MAIN))
        assertTrue(states.contains(AppState.SETTINGS))
        assertTrue(states.contains(AppState.EDIT_PROFILE))
    }
}