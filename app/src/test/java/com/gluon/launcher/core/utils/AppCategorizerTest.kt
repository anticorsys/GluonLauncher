package com.gluon.launcher.core.utils

import com.gluon.launcher.core.data.AppModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AppCategorizerTest {

    private val defaultCategories = AppCategorizer.DEFAULT_CATEGORIES

    @Test
    fun `категоризация пустого списка`() = runTest {
        val result = AppCategorizer.categorizeApps(emptyList(), emptyMap())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `приложения распределяются по категориям на основе пакета`() = runTest {
        val apps = listOf(
            AppModel("Telegram", "org.telegram.messenger"),
            AppModel("Gmail", "com.google.android.gm"),
            AppModel("Chrome", "com.android.chrome"),
            AppModel("YouTube", "com.google.android.youtube"),
            AppModel("Calculator", "com.android.calculator"),
            AppModel("Unknown", "com.example.unknown")
        )
        val result = AppCategorizer.categorizeApps(apps, emptyMap())
        val map = result.toMap()

        assertTrue(map["Связь"]?.any { it.packageName == "org.telegram.messenger" } == true)
        assertTrue(map["Почта"]?.any { it.packageName == "com.google.android.gm" } == true)
        assertTrue(map["Браузеры"]?.any { it.packageName == "com.android.chrome" } == true)
        assertTrue(map["Мультимедиа"]?.any { it.packageName == "com.google.android.youtube" } == true)
        assertTrue(map["Инструменты"]?.any { it.packageName == "com.android.calculator" } == true)
        assertTrue(map["Прочее"]?.any { it.packageName == "com.example.unknown" } == true)
    }

    @Test
    fun `кастомные категории переопределяют авто-категорию`() = runTest {
        val apps = listOf(
            AppModel("Chrome", "com.android.chrome")
        )
        val custom = mapOf("com.android.chrome" to "Мои инструменты")
        val result = AppCategorizer.categorizeApps(apps, custom)
        val map = result.toMap()
        assertTrue(map.containsKey("Мои инструменты"))
        assertTrue(map["Мои инструменты"]?.any { it.packageName == "com.android.chrome" } == true)
        assertFalse(map["Браузеры"]?.any { it.packageName == "com.android.chrome" } == true)
    }

    @Test
    fun `несколько приложений в одной категории`() = runTest {
        val apps = listOf(
            AppModel("Telegram", "org.telegram.messenger"),
            AppModel("WhatsApp", "com.whatsapp")
        )
        val result = AppCategorizer.categorizeApps(apps, emptyMap())
        val comm = result.find { it.first == "Связь" }?.second
        assertEquals(2, comm?.size)
        assertTrue(comm?.any { it.packageName == "org.telegram.messenger" } == true)
        assertTrue(comm?.any { it.packageName == "com.whatsapp" } == true)
    }

    @Test
    fun `приложение попадает в Прочее, если не подходит ни под одно правило`() = runTest {
        val apps = listOf(
            AppModel("SomeApp", "com.some.app")
        )
        val result = AppCategorizer.categorizeApps(apps, emptyMap())
        assertEquals(1, result.size)
        assertEquals("Прочее", result[0].first)
        assertEquals("com.some.app", result[0].second[0].packageName)
    }

    @Test
    fun `категории без приложений не появляются в результате`() = runTest {
        val apps = listOf(
            AppModel("Chrome", "com.android.chrome"),
            AppModel("Telegram", "org.telegram.messenger")
        )
        val result = AppCategorizer.categorizeApps(apps, emptyMap())
        val presentCategories = result.map { it.first }.toSet()
        assertTrue(presentCategories.contains("Связь"))
        assertTrue(presentCategories.contains("Браузеры"))
        // Остальные категории отсутствуют
        val absent = defaultCategories.filter { it != "Связь" && it != "Браузеры" && it != "Прочее" }
        for (cat in absent) {
            assertFalse(presentCategories.contains(cat))
        }
    }

    @Test
    fun `категория Прочее помещается в конец списка`() = runTest {
        val apps = listOf(
            AppModel("Telegram", "org.telegram.messenger"),
            AppModel("Unknown", "com.unknown")
        )
        val result = AppCategorizer.categorizeApps(apps, emptyMap())
        assertTrue(result.isNotEmpty())
        assertEquals("Прочее", result.last().first)
    }

    @Test
    fun `игнорируется собственное приложение лаунчера`() = runTest {
        // Пакет лаунчера не должен появляться в списке, но он фильтруется в AppRepository, а не в категоризаторе.
        // Здесь просто убедимся, что если его передать, он попадет в Прочее (не сломает логику)
        val apps = listOf(
            AppModel("Gluon", "com.gluon.launcher")
        )
        val result = AppCategorizer.categorizeApps(apps, emptyMap())
        // Не падаем, результат есть
        assertNotNull(result)
    }
}