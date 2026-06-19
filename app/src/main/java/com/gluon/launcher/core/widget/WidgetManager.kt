// app/src/main/java/com/gluon/launcher/core/widget/WidgetManager.kt
package com.gluon.launcher.core.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.roundToInt

object WidgetManager {
    private const val HOST_ID = 1024
    private lateinit var appContext: Context
    private lateinit var _appWidgetManager: AppWidgetManager
    private lateinit var _appWidgetHost: GluonAppWidgetHost

    private val viewCache = ConcurrentHashMap<Int, AppWidgetHostView>()

    @Volatile
    var isInitialized: Boolean = false
        private set

    val appWidgetManager: AppWidgetManager
        get() {
            check(isInitialized) { "WidgetManager is not initialized. Call init() first." }
            return _appWidgetManager
        }

    private val excludedWidgetPackages = setOf(
        "com.android.contacts", "com.android.incallui", "com.android.dialer",
        "com.google.android.dialer", "com.android.phone", "com.samsung.android.contacts",
        "com.samsung.android.dialer", "com.android.messaging", "com.google.android.apps.messaging",
        "com.android.providers.contacts", "com.android.incall", "com.android.server.telecom",
        "com.oneplus.contacts", "com.oneplus.dialer"
    )

    private class GluonAppWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {
        fun getAllWidgetIds(): IntArray {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) appWidgetIds else intArrayOf()
        }
    }

    fun init(context: Context) {
        if (isInitialized) return
        appContext = context.applicationContext
        _appWidgetManager = AppWidgetManager.getInstance(appContext)
        _appWidgetHost = GluonAppWidgetHost(appContext, HOST_ID)
        _appWidgetHost.startListening()
        isInitialized = true
    }

    fun destroy() {
        if (!isInitialized) return
        try {
            _appWidgetHost.stopListening()
            clearCache()
        } catch (_: Exception) {}
        isInitialized = false
    }

    fun clearCache() {
        viewCache.values.forEach { view ->
            try {
                (view.parent as? ViewGroup)?.removeView(view)
                // Удалено: view.removeAllViews() - вызывало баг с прозрачностью (разрушение RemoteViews)
                view.setPadding(0, 0, 0, 0)
            } catch (_: Exception) {}
        }
        viewCache.clear()
    }

    fun bindWidget(appWidgetId: Int, providerInfo: AppWidgetProviderInfo): Boolean {
        if (!isInitialized) return false
        return try {
            _appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, providerInfo.provider)
        } catch (_: SecurityException) { false }
    }

    fun createBindWidgetIntent(appWidgetId: Int, providerInfo: AppWidgetProviderInfo): Intent =
        Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, providerInfo.provider)
        }

    fun getOrCreateView(context: Context, appWidgetId: Int): AppWidgetHostView? {
        if (!isInitialized) return null
        viewCache[appWidgetId]?.let { return it }

        val info = _appWidgetManager.getAppWidgetInfo(appWidgetId) ?: return null
        return try {
            val hostView = _appWidgetHost.createView(context, appWidgetId, info)
            hostView.setPadding(0, 0, 0, 0)
            hostView.clipChildren = false
            hostView.clipToPadding = false
            viewCache[appWidgetId] = hostView
            hostView
        } catch (_: Exception) { null }
    }

    fun removeViewFromCache(appWidgetId: Int) {
        val view = viewCache.remove(appWidgetId)
        try {
            (view?.parent as? ViewGroup)?.removeView(view)
            // Удалено: view?.removeAllViews() - вызывало баг с прозрачностью
        } catch (_: Exception) {}
    }

    @Suppress("DEPRECATION")
    fun updateWidgetSizePx(appWidgetId: Int, widthPx: Int, heightPx: Int) {
        val view = viewCache[appWidgetId] ?: return

        val density = appContext.resources.displayMetrics.density
        val widthDp = (widthPx / density).roundToInt()
        val heightDp = (heightPx / density).roundToInt()

        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.updateAppWidgetOptions(options)
        } else {
            view.updateAppWidgetSize(null, widthDp, heightDp, widthDp, heightDp)
        }
    }

    fun deleteWidget(appWidgetId: Int) {
        removeViewFromCache(appWidgetId)
        if (isInitialized) {
            try {
                _appWidgetHost.deleteAppWidgetId(appWidgetId)
            } catch (_: Exception) {}
        }
    }

    fun allocateAppWidgetId(): Int {
        check(isInitialized)
        return _appWidgetHost.allocateAppWidgetId()
    }

    fun cleanOrphanWidgets(validIds: Set<Int>) {
        if (!isInitialized || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        for (id in _appWidgetHost.getAllWidgetIds()) {
            if (id !in validIds) deleteWidget(id)
        }
    }

    fun getGroupedWidgetProviders(context: Context): Map<String, List<AppWidgetProviderInfo>> {
        if (!isInitialized) return emptyMap()
        return try {
            val valid = _appWidgetManager.installedProviders?.filter {
                it.provider.packageName !in excludedWidgetPackages
            } ?: emptyList()
            valid.groupBy { provider ->
                try {
                    val appInfo = context.packageManager.getApplicationInfo(provider.provider.packageName, 0)
                    context.packageManager.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) { provider.provider.packageName }
            }.entries.sortedBy { it.key.lowercase() }.associate { it.key to it.value }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun needsConfiguration(provider: AppWidgetProviderInfo) = provider.configure != null

    fun createConfigurationIntent(appWidgetId: Int, provider: AppWidgetProviderInfo): Intent? {
        val configure = provider.configure ?: return null
        return Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
            component = configure
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
    }

    fun getDefaultSpans(provider: AppWidgetProviderInfo, maxCols: Int = 4, maxRows: Int = 8): Pair<Int, Int> {
        val spanX = maxOf(1, ceil((provider.minWidth + 30) / 70.0).toInt()).coerceIn(1, maxCols)
        val spanY = maxOf(1, ceil((provider.minHeight + 30) / 70.0).toInt()).coerceIn(1, maxRows / 2)
        return Pair(spanX, spanY)
    }

    fun getMinSpans(): Pair<Int, Int> {
        return Pair(1, 1)
    }

    fun getMaxSpans(maxCols: Int = 4, maxRows: Int = 8): Pair<Int, Int> {
        return Pair(maxCols, maxRows)
    }
}