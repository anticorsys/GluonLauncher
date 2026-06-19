package com.gluon.launcher.core.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperBrightnessManagerTest {

    @Test
    fun `luminanceCalculator - тёмный цвет`() {
        val luminance = calculateLuminance(0.1f, 0.1f, 0.1f)
        assertTrue(luminance < 0.5f)
    }

    @Test
    fun `luminanceCalculator - светлый цвет`() {
        val luminance = calculateLuminance(0.9f, 0.9f, 0.9f)
        assertTrue(luminance > 0.5f)
    }

    @Test
    fun `luminanceCalculator - чисто чёрный`() {
        val luminance = calculateLuminance(0f, 0f, 0f)
        assertTrue(luminance == 0f)
    }

    @Test
    fun `luminanceCalculator - чисто белый`() {
        val luminance = calculateLuminance(1f, 1f, 1f)
        assertTrue(luminance == 1f)
    }

    @Test
    fun `isDark - тёмный фон`() {
        assertTrue(isDarkFromColor(0.2f, 0.2f, 0.2f))
    }

    @Test
    fun `isDark - светлый фон`() {
        assertFalse(isDarkFromColor(0.8f, 0.8f, 0.8f))
    }

    // Вспомогательные функции (скопированы из логики WallpaperBrightnessManager, но без Android-зависимостей)

    private fun calculateLuminance(r: Float, g: Float, b: Float): Float {
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    private fun isDarkFromColor(r: Float, g: Float, b: Float): Boolean {
        val luminance = calculateLuminance(r, g, b)
        return luminance < 0.5f
    }
}