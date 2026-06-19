// app/src/main/java/com/gluon/launcher/core/utils/Extensions.kt
package com.gluon.launcher.core.utils

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gluon.launcher.core.data.AppModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "GluonExtensions"

fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        showCustomToast(this.applicationContext, message, duration)
    } else {
        Handler(Looper.getMainLooper()).post {
            showCustomToast(this.applicationContext, message, duration)
        }
    }
}

private fun showCustomToast(context: Context, message: String, duration: Int) {
    try {
        val toast = Toast(context)
        toast.duration = duration

        val density = context.resources.displayMetrics.density
        val isNightMode = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val bgColor = if (isNightMode) "#2B2930".toColorInt() else "#E8EAED".toColorInt()
        val strokeColor = if (isNightMode) "#44464F".toColorInt() else "#C4C6D0".toColorInt()
        val textColor = if (isNightMode) "#E6E1E5".toColorInt() else "#1F1F1F".toColorInt()

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padH = (20 * density).toInt()
            val padV = (14 * density).toInt()
            setPadding(padH, padV, padH, padV)
            background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 28f * density
                setStroke((0.5f * density).toInt(), strokeColor)
            }
            elevation = 8f * density
        }

        val textView = TextView(context).apply {
            this.text = message
            setTextColor(textColor)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
        }

        layout.addView(textView)

        @Suppress("DEPRECATION")
        toast.view = layout
        toast.show()
    } catch (_: Exception) {
        Toast.makeText(context, message, duration).show()
    }
}

fun Context.launchApp(app: AppModel) {
    try {
        val intent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            startActivity(intent)
        } else {
            toast("Приложение ${app.label} не найдено")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Launch error for ${app.packageName}", e)
        toast("Ошибка запуска")
    }
}

fun String.isValidEmail(): Boolean {
    val target = this.trim()
    if (target.isBlank()) return false
    // Разрешаем TLD длиной от 1 буквы (для поддержки "x@y.z")
    val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{1,}$")
    return emailRegex.matches(target) && !target.contains(",")
}

val Context.dataStore by preferencesDataStore(name = "gluon_app_repository")

private val json = Json { ignoreUnknownKeys = true }

private val USAGE_COUNT_KEY = stringPreferencesKey("usage_counts")
private val HIDDEN_APPS_KEY = stringPreferencesKey("hidden_apps")

suspend fun DataStore<Preferences>.getUsageCounts(): Map<String, Int> {
    return try {
        val stored = data.firstOrNull()?.get(USAGE_COUNT_KEY) ?: "{}"
        json.decodeFromString<Map<String, Int>>(stored)
    } catch (_: Exception) {
        emptyMap()
    }
}

suspend fun DataStore<Preferences>.getHiddenApps(): Set<String> {
    return try {
        val stored = data.firstOrNull()?.get(HIDDEN_APPS_KEY) ?: "[]"
        json.decodeFromString<List<String>>(stored).toSet()
    } catch (_: Exception) {
        emptySet()
    }
}

suspend fun DataStore<Preferences>.addHiddenApp(packageName: String) {
    edit { prefs ->
        val stored = prefs[HIDDEN_APPS_KEY] ?: "[]"
        val current = try {
            json.decodeFromString<List<String>>(stored)
        } catch (_: Exception) {
            emptyList()
        }
        val updated = current + packageName
        prefs[HIDDEN_APPS_KEY] = json.encodeToString(updated)
    }
}

suspend fun DataStore<Preferences>.removeHiddenApp(packageName: String) {
    edit { prefs ->
        val stored = prefs[HIDDEN_APPS_KEY] ?: "[]"
        val current = try {
            json.decodeFromString<List<String>>(stored)
        } catch (_: Exception) {
            emptyList()
        }
        val updated = current.filter { it != packageName }
        prefs[HIDDEN_APPS_KEY] = json.encodeToString(updated)
    }
}