// app/src/main/java/com/gluon/launcher/core/search/OmniSearchManager.kt
package com.gluon.launcher.core.search

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import com.gluon.launcher.core.data.AppModel
import com.gluon.launcher.core.utils.matchesFuzzyQuery
import com.gluon.launcher.core.utils.transliterate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface SearchResultItem {
    data class AppItem(val app: AppModel) : SearchResultItem
    data class ContactItem(val name: String, val phoneNumber: String) : SearchResultItem
    data class FileItem(val name: String, val path: String, val mimeType: String) : SearchResultItem
    data class WebSuggestionItem(val query: String, val url: String) : SearchResultItem
}

class OmniSearchManager(private val context: Context) {

    suspend fun performOmniSearch(
        query: String,
        installedApps: List<AppModel>
    ): List<SearchResultItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val results = mutableListOf<SearchResultItem>()
        val queryTrans = query.transliterate()
        val queryLen = query.length
        val queryTransLen = queryTrans.length

        // Оптимизация: Sequence предотвращает создание промежуточных листов
        val matchedApps = installedApps.asSequence().filter {
            matchesFuzzyQuery(it.label, query) ||
                    (queryLen > 2 && it.packageName.contains(query, ignoreCase = true)) ||
                    (queryTransLen > 2 && it.packageName.contains(queryTrans, ignoreCase = true))
        }.sortedByDescending {
            it.label.startsWith(query, ignoreCase = true) || it.label.startsWith(queryTrans, ignoreCase = true)
        }.map { SearchResultItem.AppItem(it) }
            .toList()

        results.addAll(matchedApps)

        try {
            val contactsCursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$query%"),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC LIMIT 3"
            )
            contactsCursor?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    if (nameIdx >= 0 && numIdx >= 0) {
                        results.add(SearchResultItem.ContactItem(cursor.getString(nameIdx), cursor.getString(numIdx)))
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            val fileUri: Uri = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.MIME_TYPE)
            val filesCursor: Cursor? = context.contentResolver.query(
                fileUri,
                projection,
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?",
                arrayOf("%$query%"),
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC LIMIT 3"
            )
            filesCursor?.use { cursor ->
                val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dataIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val mimeIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIdx)
                    val path = cursor.getString(dataIdx)
                    val mime = cursor.getString(mimeIdx) ?: "application/octet-stream"
                    results.add(SearchResultItem.FileItem(name, path, mime))
                }
            }
        } catch (_: Exception) {}

        val encoded = Uri.encode(query)
        results.add(SearchResultItem.WebSuggestionItem("Искать в Google: \"$query\"", "https://www.google.com/search?q=$encoded"))
        results.add(SearchResultItem.WebSuggestionItem("Искать в Яндекс: \"$query\"", "https://yandex.ru/search/?text=$encoded"))

        return@withContext results
    }
}