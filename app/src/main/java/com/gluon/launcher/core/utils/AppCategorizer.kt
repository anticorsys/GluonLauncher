package com.gluon.launcher.core.utils

import com.gluon.launcher.core.data.AppModel
import kotlinx.coroutines.yield

object AppCategorizer {
    val DEFAULT_CATEGORIES = listOf("Связь", "Почта", "Браузеры", "Мультимедиа", "Умный дом", "Финансы", "Игры", "Инструменты", "Прочее")

    suspend fun categorizeApps(
        apps: List<AppModel>,
        customCategories: Map<String, String>
    ): List<Pair<String, List<AppModel>>> {
        val categories = mutableMapOf<String, MutableList<AppModel>>()
        DEFAULT_CATEGORIES.forEach { categories[it] = mutableListOf() }

        apps.forEachIndexed { index, app ->
            if (index % 25 == 0) yield() // Оптимизация: не блокируем поток, даем время на рендер
            val pkg = app.packageName.lowercase()
            val lbl = app.label.lowercase()

            if (customCategories.containsKey(app.packageName)) {
                val customCat = customCategories[app.packageName]!!
                if (!categories.containsKey(customCat)) categories[customCat] = mutableListOf()
                categories[customCat]!!.add(app)
            } else {
                when {
                    (pkg.contains("vk") || pkg.contains("telegram") || pkg.contains("whatsapp") || pkg.contains("viber") || pkg.contains("chat") || pkg.contains("messenger") || pkg.contains("sms") || pkg.contains("dialer") || pkg.contains("phone") || pkg.contains("contact") || lbl.contains("связь") || lbl.contains("смс") || lbl.contains("телефон") || lbl.contains("контакты") || lbl.contains("сообщения")) && !pkg.contains("mail") -> categories["Связь"]!!.add(app)
                    pkg == "com.google.android.gm" || pkg.contains("mail") || lbl.contains("почта") || lbl.contains("gmail") -> categories["Почта"]!!.add(app)
                    pkg.contains("music") || pkg.contains("youtube") || pkg.contains("camera") || pkg.contains("gallery") || pkg.contains("video") || pkg.contains("player") || pkg.contains("twitch") || pkg.contains("rutube") || pkg.contains("kinopoisk") || lbl.contains("камера") || lbl.contains("музыка") || lbl.contains("галерея") || lbl.contains("плеер") || lbl.contains("кино") -> categories["Мультимедиа"]!!.add(app)
                    pkg.contains("browser") || pkg.contains("chrome") || pkg.contains("opera") || pkg.contains("firefox") || (pkg.contains("yandex") && !pkg.contains("disk") && !pkg.contains("maps") && !pkg.contains("taxi")) || lbl.contains("браузер") -> categories["Браузеры"]!!.add(app)
                    pkg == "com.google.android.apps.chromecast.app" || pkg.contains("thermex") || (pkg.contains("home") && !pkg.contains("chrome")) || pkg.contains("iot") || pkg.contains("mihome") || pkg.contains("smart") || lbl.contains("умный дом") -> categories["Умный дом"]!!.add(app)
                    pkg.contains("bank") || pkg.contains("finance") || pkg.contains("pay") || pkg.contains("wallet") || pkg.contains("sber") || pkg.contains("tinkoff") || lbl.contains("банк") || lbl.contains("оплата") || lbl.contains("деньги") || lbl.contains("кошелек") || lbl.contains("финансы") -> categories["Финансы"]!!.add(app)
                    pkg.contains("game") || pkg.contains("play") || lbl.contains("игра") || lbl.contains("игры") -> categories["Игры"]!!.add(app)
                    pkg.contains("calculator") || pkg.contains("clock") || pkg.contains("settings") || pkg.contains("weather") || pkg.contains("note") || lbl.contains("настройки") || lbl.contains("инструменты") || lbl.contains("часы") || lbl.contains("калькулятор") || lbl.contains("заметки") -> categories["Инструменты"]!!.add(app)
                    else -> categories["Прочее"]!!.add(app)
                }
            }
        }

        return categories.filterValues { it.isNotEmpty() }
            .toList()
            .sortedBy { if (it.first == "Прочее") 1 else 0 }
    }
}