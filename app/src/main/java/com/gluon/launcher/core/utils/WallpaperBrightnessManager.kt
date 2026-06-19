package com.gluon.launcher.core.utils

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WallpaperBrightnessManager(context: Context) {
    private val appContext = context.applicationContext
    private val wallpaperManager = WallpaperManager.getInstance(appContext)

    // ОПТИМИЗАЦИЯ: Системные запросы WallpaperManager могут быть "тяжелыми",
    // поэтому используем Dispatchers.IO, чтобы избежать лагов UI (главного потока)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isDark = MutableStateFlow(true)
    val isDark: StateFlow<Boolean> = _isDark.asStateFlow()

    private var colorsChangedListener: WallpaperManager.OnColorsChangedListener? = null

    init {
        update()
        registerListener()
    }

    private fun registerListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            colorsChangedListener = WallpaperManager.OnColorsChangedListener { colors, _ ->
                updateDarkState(colors)
            }
            // Гарантируем, что слушатель будет работать в связке с главным потоком приложения
            wallpaperManager.addOnColorsChangedListener(
                colorsChangedListener!!,
                Handler(Looper.getMainLooper())
            )
        }
    }

    fun update() {
        scope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                // Асинхронно запрашиваем цвета (блокировка исключена благодаря scope)
                val colors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                updateDarkState(colors)
            } else {
                _isDark.value = true
            }
        }
    }

    // СТАБИЛИЗАЦИЯ И ИСПРАВЛЕНИЕ БАГА АПИ
    private fun updateDarkState(colors: WallpaperColors?) {
        if (colors != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Для Android 12 (API 31) и выше используем современные системные хинты
                val hints = colors.colorHints
                _isDark.value = (hints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT) == 0
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                // Фолбэк для Android 8.1 - 11 (API 27-30).
                // Вычисляем общую светимость основного цвета обоев (от 0.0 до 1.0).
                // Если меньше 0.5f, значит основной цвет темный, и нужно использовать светлую тему иконок.
                val primaryLuminance = colors.primaryColor.luminance()
                _isDark.value = primaryLuminance < 0.5f
            } else {
                _isDark.value = true
            }
        } else {
            _isDark.value = true
        }
    }

    fun onDispose() {
        // Убираем за собой слушатель для предотвращения утечек памяти (Memory Leaks)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            colorsChangedListener?.let {
                wallpaperManager.removeOnColorsChangedListener(it)
            }
        }
        scope.cancel()
    }
}