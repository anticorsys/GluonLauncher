package com.gluon.launcher.core.utils

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class FuzzySearchTest {
    @Test
    fun testExactMatch() {
        assertTrue(matchesFuzzyQuery("Telegram", "telegram"))
    }

    @Test
    fun testPartialMatch() {
        assertTrue(matchesFuzzyQuery("Google Chrome", "chrome"))
    }

    @Test
    fun testTransliteration() {
        // "tg" -> "telegram" через аббревиатуру
        assertTrue(matchesFuzzyQuery("Telegram", "тг"))
    }

    @Test
    fun testNoMatch() {
        assertFalse(matchesFuzzyQuery("YouTube", "zoom"))
    }
}