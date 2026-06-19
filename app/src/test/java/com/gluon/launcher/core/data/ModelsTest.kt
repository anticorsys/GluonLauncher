package com.gluon.launcher.core.data

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Test
    fun `UserItem - десериализация из JSON`() {
        val raw = """
        {
            "id": "user123",
            "email": "user@example.com",
            "verified": true,
            "full_name": "John Doe",
            "gender": "Male",
            "gluon_id": "john_doe_1234",
            "bio": "Hello world",
            "avatar": "avatar.jpg",
            "collectionId": "col1",
            "collectionName": "Gluon_Database",
            "created": "2023-01-01",
            "updated": "1672531200"
        }
        """.trimIndent()

        val user = json.decodeFromString<UserItem>(raw)
        assertEquals("user123", user.id)
        assertEquals("user@example.com", user.email)
        assertTrue(user.verified)
        assertEquals("John Doe", user.fullName)
        assertEquals("Male", user.gender)
        assertEquals("john_doe_1234", user.gluonId)
        assertEquals("Hello world", user.bio)
        assertEquals("avatar.jpg", user.avatar)
    }

    @Test
    fun `UserItem - getAvatarUrl с валидными данными`() {
        val user = UserItem(
            id = "user123",
            avatar = "avatar.jpg",
            collectionName = "Gluon_Database",
            updated = "1672531200"
        )
        val url = user.getAvatarUrl()
        assertNotNull(url)
        // Проверяем, что url содержит необходимые части
        assertTrue(url!!.contains("api/files"))
        assertTrue(url.contains("Gluon_Database"))
        assertTrue(url.contains("user123"))
        assertTrue(url.contains("avatar.jpg"))
        assertTrue(url.contains("v="))  // параметр версии из updated
    }

    @Test
    fun `UserItem - getAvatarUrl возвращает null если avatar пустой`() {
        val user = UserItem(
            id = "user123",
            avatar = ""
        )
        assertNull(user.getAvatarUrl())
    }

    @Test
    fun `UserItem - getAvatarUrl возвращает null если id пустой`() {
        val user = UserItem(
            id = "",
            avatar = "pic.png"
        )
        assertNull(user.getAvatarUrl())
    }

    @Test
    fun `AuthResponse - десериализация`() {
        val raw = """
        {
            "token": "abc123",
            "record": {
                "id": "user456",
                "email": "a@b.com",
                "full_name": "Alice"
            }
        }
        """.trimIndent()

        val auth = json.decodeFromString<AuthResponse>(raw)
        assertEquals("abc123", auth.token)
        assertEquals("user456", auth.record.id)
        assertEquals("Alice", auth.record.fullName)
    }

    @Test
    fun `WorkspaceAppItem - структура и копирование`() {
        val item = WorkspaceAppItem(
            id = "id1",
            packageName = "com.example",
            label = "Example",
            screenId = 0,
            cellX = 1,
            cellY = 2,
            spanX = 2,
            spanY = 1
        )
        // Проверяем свойства
        assertEquals("id1", item.id)
        assertEquals(2, item.spanX)
        assertEquals(1, item.spanY)

        // copy
        val moved = item.copy(screenId = 1, cellX = 3, cellY = 4)
        assertEquals(1, moved.screenId)
        assertEquals(3, moved.cellX)
        assertEquals(4, moved.cellY)
        assertEquals("id1", moved.id) // ID не изменился
    }

    @Test
    fun `WorkspaceFolderItem - пустой список пакетов`() {
        val folder = WorkspaceFolderItem(
            id = "f1",
            name = "Test",
            packages = emptyList(),
            screenId = 0,
            cellX = 0,
            cellY = 0
        )
        assertTrue(folder.packages.isEmpty())
        assertEquals("Test", folder.name)
    }
}