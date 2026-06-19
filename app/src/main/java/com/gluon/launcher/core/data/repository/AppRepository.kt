package com.gluon.launcher.core.data.repository

import android.content.BroadcastReceiver
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.gluon.launcher.core.data.AppModel
import com.gluon.launcher.core.utils.addHiddenApp
import com.gluon.launcher.core.utils.dataStore
import com.gluon.launcher.core.utils.getHiddenApps
import com.gluon.launcher.core.utils.getUsageCounts
import com.gluon.launcher.core.utils.removeHiddenApp
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.time.Duration.Companion.milliseconds

class AppRepository(private val appContext: Context) : ComponentCallbacks2 {

    private val packageManager = appContext.packageManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("AppRepo"))
    private val dataStore = appContext.dataStore

    private val _appsFlow = MutableStateFlow<List<AppModel>>(emptyList())
    val appsFlow: StateFlow<List<AppModel>> = _appsFlow.asStateFlow()

    private var packageReceiver: BroadcastReceiver? = null
    private var refreshJob: Job? = null

    private val iconCache = LruCache<String, ImageBitmap>(600)

    init {
        appContext.registerComponentCallbacks(this)
        scope.launch { refreshApps() }
        observePackageChanges()
    }

    override fun onTrimMemory(level: Int) {
        // ИСПРАВЛЕНИЕ: Очищаем кэш только при реальной угрозе нехватки памяти, а не при каждом сворачивании UI
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            iconCache.evictAll()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {}

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() {
        iconCache.evictAll()
    }

    private suspend fun loadAppIcon(packageName: String, resolveInfo: android.content.pm.ResolveInfo): ImageBitmap? {
        iconCache[packageName]?.let { return it }

        return withContext(Dispatchers.Default) {
            try {
                val drawable = resolveInfo.loadIcon(packageManager)

                if (drawable is BitmapDrawable && drawable.bitmap != null) {
                    val imageBitmap = drawable.bitmap.asImageBitmap()
                    iconCache.put(packageName, imageBitmap)
                    return@withContext imageBitmap
                }

                val w = drawable.intrinsicWidth.coerceAtLeast(1).coerceAtMost(128)
                val h = drawable.intrinsicHeight.coerceAtLeast(1).coerceAtMost(128)

                val imageBitmap = drawable.toBitmap(w, h, Bitmap.Config.ARGB_8888).asImageBitmap()
                iconCache.put(packageName, imageBitmap)
                imageBitmap
            } catch (_: Exception) { null }
        }
    }

    suspend fun refreshApps() {
        withContext(Dispatchers.IO) {
            val hidden = dataStore.getHiddenApps()
            val usage = dataStore.getUsageCounts()
            val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, 0)
            }

            // ИСПРАВЛЕНИЕ: Сохраняем текущие иконки, чтобы они не пропадали при сборке мусора (GC)
            val currentAppsMap = _appsFlow.value.associateBy { it.packageName }

            val initialApps = resolveInfos.mapNotNull { info ->
                val pkgName = info.activityInfo.packageName
                if (pkgName == appContext.packageName || pkgName in hidden) return@mapNotNull null

                val appLabel = info.loadLabel(packageManager).toString()

                // Берем иконку из кэша. Если кэш был очищен, спасаем иконку из текущего отображаемого списка.
                val existingIcon = iconCache[pkgName] ?: currentAppsMap[pkgName]?.iconBitmap

                AppModel(
                    label = appLabel,
                    packageName = pkgName,
                    installTime = currentAppsMap[pkgName]?.installTime ?: 0L,
                    isSystem = (info.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                    usageCount = usage[pkgName] ?: 0,
                    iconBitmap = existingIcon
                )
            }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }

            _appsFlow.update { initialApps }

            scope.launch {
                val updatedApps = initialApps.toMutableList()
                val batchSize = 50

                resolveInfos.filter { info ->
                    val pkgName = info.activityInfo.packageName
                    pkgName != appContext.packageName && pkgName !in hidden
                }.chunked(batchSize).forEach { batch ->
                    val deferredList = batch.map { info ->
                        async {
                            val pkgName = info.activityInfo.packageName
                            val pInfo = try { packageManager.getPackageInfo(pkgName, 0) } catch(_: Exception) { null }
                            val installTime = pInfo?.firstInstallTime ?: 0L
                            val icon = loadAppIcon(pkgName, info)
                            Triple(pkgName, icon, installTime)
                        }
                    }

                    val results = deferredList.awaitAll()
                    var hasUpdates = false

                    for ((pkgName, icon, installTime) in results) {
                        val index = updatedApps.indexOfFirst { it.packageName == pkgName }
                        if (index != -1) {
                            val currentApp = updatedApps[index]
                            if (currentApp.iconBitmap == null || currentApp.installTime == 0L) {
                                updatedApps[index] = currentApp.copy(
                                    iconBitmap = icon ?: currentApp.iconBitmap,
                                    installTime = installTime
                                )
                                hasUpdates = true
                            }
                        }
                    }

                    if (hasUpdates) {
                        _appsFlow.update { updatedApps.toList() }
                        yield()
                    }
                }
            }
        }
    }

    suspend fun hideApp(packageName: String) {
        withContext(Dispatchers.IO) {
            dataStore.addHiddenApp(packageName)
            // ИСПРАВЛЕНИЕ: Мы не удаляем иконку из кэша и не вызываем refreshApps().
            // Просто мгновенно фильтруем текущий список в памяти.
            _appsFlow.update { currentList ->
                currentList.filter { it.packageName != packageName }
            }
        }
    }

    suspend fun unhideApp(packageName: String) {
        withContext(Dispatchers.IO) { dataStore.removeHiddenApp(packageName) }
        refreshApps()
    }

    suspend fun getHiddenApps(): Set<String> = withContext(Dispatchers.IO) { dataStore.getHiddenApps() }

    suspend fun getHiddenAppModels(): Map<String, AppModel> = withContext(Dispatchers.IO) {
        val pkgNames = dataStore.getHiddenApps()
        val deferredJobs = pkgNames.map { pkg ->
            scope.async {
                try {
                    val appInfo = packageManager.getApplicationInfo(pkg, 0)
                    val label = packageManager.getApplicationLabel(appInfo).toString()
                    val drawable = packageManager.getApplicationIcon(appInfo)
                    val bitmap = try {
                        if (drawable is BitmapDrawable && drawable.bitmap != null) {
                            drawable.bitmap.asImageBitmap()
                        } else {
                            val w = drawable.intrinsicWidth.coerceAtLeast(1).coerceAtMost(128)
                            val h = drawable.intrinsicHeight.coerceAtLeast(1).coerceAtMost(128)
                            drawable.toBitmap(w, h, Bitmap.Config.ARGB_8888).asImageBitmap()
                        }
                    } catch (_: Exception) { null }

                    pkg to AppModel(label = label, packageName = pkg, iconBitmap = bitmap)
                } catch (_: Exception) { null }
            }
        }
        deferredJobs.awaitAll().filterNotNull().toMap()
    }

    private fun observePackageChanges() {
        if (packageReceiver != null) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED) // ИСПРАВЛЕНИЕ: Ловим обновления приложений
            addDataScheme("package")
        }
        packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val pkgName = intent?.data?.schemeSpecificPart ?: return
                if (pkgName == appContext.packageName) return

                refreshJob?.cancel()
                refreshJob = scope.launch {
                    // ИСПРАВЛЕНИЕ: Даем системе 1 секунду на окончательную регистрацию новых иконок
                    delay(1000.milliseconds)
                    refreshApps()
                }
            }
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ContextCompat.RECEIVER_EXPORTED else 0
        ContextCompat.registerReceiver(appContext, packageReceiver, filter, flags)
    }

    fun cleanup() {
        try {
            packageReceiver?.let { appContext.unregisterReceiver(it) }
            appContext.unregisterComponentCallbacks(this)
            packageReceiver = null
            refreshJob?.cancel()
            scope.cancel()
            iconCache.evictAll()
        } catch (_: Exception) {}
    }
}