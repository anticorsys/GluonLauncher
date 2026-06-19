package com.gluon.launcher.core.utils

val EN_TO_RU = mapOf(
    'q' to 'й', 'w' to 'ц', 'e' to 'у', 'r' to 'к', 't' to 'е', 'y' to 'н', 'u' to 'г', 'i' to 'ш', 'o' to 'щ', 'p' to 'з', '[' to 'х', ']' to 'ъ',
    'a' to 'ф', 's' to 'ы', 'd' to 'в', 'f' to 'а', 'g' to 'п', 'h' to 'р', 'j' to 'о', 'k' to 'л', 'l' to 'д', ';' to 'ж', '\'' to 'э',
    'z' to 'я', 'x' to 'ч', 'c' to 'с', 'v' to 'м', 'b' to 'и', 'n' to 'т', 'm' to 'ь', ',' to 'б', '.' to 'ю'
)
val RU_TO_EN = EN_TO_RU.entries.associate { (k, v) -> v to k }

// Фонетическая транслитерация (т → t, г → g и т.д.)
private val PHONETIC_RU_TO_EN = mapOf(
    'а' to 'a', 'б' to 'b', 'в' to 'v', 'г' to 'g', 'д' to 'd', 'е' to 'e', 'ё' to 'e',
    'ж' to "zh", 'з' to 'z', 'и' to 'i', 'й' to 'y', 'к' to 'k', 'л' to 'l', 'м' to 'm',
    'н' to 'n', 'о' to 'o', 'п' to 'p', 'р' to 'r', 'с' to 's', 'т' to 't', 'у' to 'u',
    'ф' to 'f', 'х' to 'h', 'ц' to "ts", 'ч' to "ch", 'ш' to "sh", 'щ' to "sch",
    'ъ' to "", 'ы' to 'y', 'ь' to "", 'э' to 'e', 'ю' to "yu", 'я' to "ya"
)

private val REGEX_CLEAN = Regex("[^a-zа-я0-9 .-]")
private val REGEX_DOTS = Regex("[.-]{2,}")

private val ABBREVIATIONS = mapOf(
    "tg" to "telegram",
    "vk" to "vkontakte",
    "wa" to "whatsapp",
    "fb" to "facebook",
    "ig" to "instagram",
    "gm" to "gmail",
    "yt" to "youtube"
)

fun String.transliterate(): String {
    val sb = StringBuilder(this.length)
    for (i in this.indices) {
        val char = this[i]
        sb.append(EN_TO_RU[char] ?: RU_TO_EN[char] ?: char)
    }
    return sb.toString()
}

// Фонетическая транслитерация – заменяет кириллицу на латиницу «по звучанию»
private fun String.phoneticTransliterate(): String {
    val sb = StringBuilder(this.length)
    for (ch in this) {
        sb.append(PHONETIC_RU_TO_EN[ch] ?: ch)
    }
    return sb.toString()
}

fun String.normalizeForSearch(): String {
    var cleaned = this.lowercase().replace(REGEX_CLEAN, "")
    // Фонетическая транслитерация – теперь «тг» → «tg»
    cleaned = cleaned.phoneticTransliterate()
    val tokens = cleaned.split(" ")
    if (tokens.any { it in ABBREVIATIONS }) {
        cleaned = tokens.joinToString(" ") { ABBREVIATIONS[it] ?: it }
    }
    return cleaned.replace(REGEX_DOTS, ".")
}

fun matchesFuzzyQuery(appLabel: String, query: String): Boolean {
    if (query.isBlank()) return true

    val normalizedQuery = query.normalizeForSearch().trim()
    if (normalizedQuery.isEmpty()) return false
    val normalizedLabel = appLabel.normalizeForSearch()

    val transQuery = normalizedQuery.transliterate()

    if (normalizedLabel.contains(normalizedQuery) || normalizedLabel.contains(transQuery)) return true
    if (normalizedLabel.startsWith(normalizedQuery) || normalizedLabel.startsWith(transQuery)) return true
    if (normalizedLabel.contains(" $normalizedQuery") || normalizedLabel.contains(" $transQuery")) return true

    val queryWithoutDots = normalizedQuery.replace(".", " ")
    val transWithoutDots = transQuery.replace(".", " ")

    if (queryWithoutDots.isNotEmpty() && normalizedLabel.contains(queryWithoutDots)) return true
    if (transWithoutDots.isNotEmpty() && normalizedLabel.contains(transWithoutDots)) return true

    return false
}