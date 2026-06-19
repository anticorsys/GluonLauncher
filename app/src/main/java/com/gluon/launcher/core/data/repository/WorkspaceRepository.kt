package com.gluon.launcher.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gluon.launcher.core.data.DrawerFolderItem
import com.gluon.launcher.core.data.WorkspaceAppItem
import com.gluon.launcher.core.data.WorkspaceFolderItem
import com.gluon.launcher.core.data.WorkspaceItem
import com.gluon.launcher.core.data.WorkspaceWidgetItem
import com.gluon.launcher.core.utils.dataStore
import com.gluon.launcher.core.widget.WidgetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class WorkspaceRepository(private val context: Context) {
    private val workspaceItemsKey = stringPreferencesKey("workspace_items")
    private val drawerFoldersKey = stringPreferencesKey("drawer_folders")

    suspend fun saveWorkspaceItems(items: List<WorkspaceItem>) = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray()
            items.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("screenId", item.screenId)
                    put("cellX", item.cellX)
                    put("cellY", item.cellY)
                    put("spanX", item.spanX)
                    put("spanY", item.spanY)
                    when (item) {
                        is WorkspaceAppItem -> {
                            put("type", "app")
                            put("packageName", item.packageName)
                            put("label", item.label)
                        }
                        is WorkspaceWidgetItem -> {
                            put("type", "widget")
                            put("appWidgetId", item.appWidgetId)
                        }
                        is WorkspaceFolderItem -> {
                            put("type", "folder")
                            put("name", item.name)
                            put("packages", JSONArray(item.packages))
                        }
                    }
                }
                jsonArray.put(obj)
            }
            context.dataStore.edit { prefs -> prefs[workspaceItemsKey] = jsonArray.toString() }
        } catch (_: Exception) {}
    }

    suspend fun loadWorkspaceItems(): List<WorkspaceItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<WorkspaceItem>()
        try {
            val jsonString = context.dataStore.data.firstOrNull()?.get(workspaceItemsKey) ?: return@withContext items
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                when (obj.getString("type")) {
                    "app" -> items.add(
                        WorkspaceAppItem(
                            obj.getString("id"),
                            obj.getString("packageName"),
                            obj.getString("label"),
                            obj.getInt("screenId"),
                            obj.getInt("cellX"),
                            obj.getInt("cellY")
                        )
                    )
                    "widget" -> {
                        val id = obj.getInt("appWidgetId")
                        if (try { WidgetManager.appWidgetManager.getAppWidgetInfo(id) != null } catch (_: Exception) { false }) {
                            items.add(
                                WorkspaceWidgetItem(
                                    obj.getString("id"),
                                    id,
                                    obj.getInt("screenId"),
                                    obj.getInt("cellX"),
                                    obj.getInt("cellY"),
                                    obj.optInt("spanX", 1),
                                    obj.optInt("spanY", 1)
                                )
                            )
                        } else {
                            cleanInvalidWidgetFromStorage(id)
                        }
                    }
                    "folder" -> {
                        val pkgs = obj.optJSONArray("packages")?.let { arr -> List(arr.length()) { arr.getString(it) } } ?: emptyList()
                        items.add(
                            WorkspaceFolderItem(
                                obj.getString("id"),
                                obj.optString("name", "Папка"),
                                pkgs,
                                obj.getInt("screenId"),
                                obj.getInt("cellX"),
                                obj.getInt("cellY")
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return@withContext items
    }

    suspend fun saveDrawerFolders(items: List<DrawerFolderItem>) = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray()
            items.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("packages", JSONArray(item.packages))
                }
                jsonArray.put(obj)
            }
            context.dataStore.edit { prefs -> prefs[drawerFoldersKey] = jsonArray.toString() }
        } catch (_: Exception) {}
    }

    suspend fun loadDrawerFolders(): List<DrawerFolderItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<DrawerFolderItem>()
        try {
            val jsonString = context.dataStore.data.firstOrNull()?.get(drawerFoldersKey) ?: "[]"
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val pkgs = obj.optJSONArray("packages")?.let { arr -> List(arr.length()) { arr.getString(it) } } ?: emptyList()
                items.add(DrawerFolderItem(obj.getString("id"), obj.getString("name"), pkgs))
            }
        } catch (_: Exception) {}
        return@withContext items
    }

    private suspend fun cleanInvalidWidgetFromStorage(appWidgetId: Int) = withContext(Dispatchers.IO) {
        try {
            val json = context.dataStore.data.firstOrNull()?.get(workspaceItemsKey) ?: return@withContext
            val arr = JSONArray(json)
            val newArr = JSONArray()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.optString("type") == "widget" && obj.optInt("appWidgetId") == appWidgetId) continue
                newArr.put(obj)
            }
            context.dataStore.edit { it[workspaceItemsKey] = newArr.toString() }
        } catch (_: Exception) {}
    }
}