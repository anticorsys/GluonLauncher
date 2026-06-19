// app/src/main/java/com/gluon/launcher/core/utils/GridValidator.kt
package com.gluon.launcher.core.utils

import com.gluon.launcher.core.data.WorkspaceItem

object GridValidator {

    /**
     * ПРОФЕССИОНАЛЬНАЯ ОПТИМИЗАЦИЯ:
     * Убрана аллокация двумерного массива при каждой проверке.
     * Теперь используется математическая проверка пересечения прямоугольников (AABB).
     * Это полностью устраняет микрофризы (GC churn) при перетаскивании элементов.
     */
    fun isAreaVacant(
        items: List<WorkspaceItem>,
        screenId: Int,
        targetX: Int,
        targetY: Int,
        targetSpanX: Int,
        targetSpanY: Int,
        gridColumns: Int,
        gridRows: Int,
        ignoreItemIds: Set<String> = emptySet()
    ): Boolean {
        // Проверка выхода за границы экрана
        if (targetX < 0 || (targetX + targetSpanX) > gridColumns) return false
        if (targetY < 0 || (targetY + targetSpanY) > gridRows) return false

        // Математическая проверка пересечений (быстро и без аллокаций памяти)
        for (item in items) {
            if (item.screenId != screenId || item.id in ignoreItemIds) continue

            val overlapsX = targetX < (item.cellX + item.spanX) && (targetX + targetSpanX) > item.cellX
            val overlapsY = targetY < (item.cellY + item.spanY) && (targetY + targetSpanY) > item.cellY

            if (overlapsX && overlapsY) {
                return false // Найдено пересечение
            }
        }
        return true
    }

    fun findFirstVacantCell(
        items: List<WorkspaceItem>,
        screenId: Int,
        spanX: Int,
        spanY: Int,
        gridColumns: Int,
        gridRows: Int,
        ignoreItemIds: Set<String> = emptySet()
    ): Pair<Int, Int>? {
        for (y in 0 until (gridRows - spanY + 1)) {
            for (x in 0 until (gridColumns - spanX + 1)) {
                if (isAreaVacant(items, screenId, x, y, spanX, spanY, gridColumns, gridRows, ignoreItemIds)) {
                    return Pair(x, y)
                }
            }
        }
        return null
    }

    fun getOverlappingItems(
        items: List<WorkspaceItem>,
        screenId: Int,
        targetX: Int,
        targetY: Int,
        targetSpanX: Int,
        targetSpanY: Int,
        ignoreItemIds: Set<String> = emptySet()
    ): List<WorkspaceItem> {
        return items.filter { item ->
            if (item.screenId != screenId || item.id in ignoreItemIds) return@filter false

            val overlapsX = targetX < (item.cellX + item.spanX) && (targetX + targetSpanX) > item.cellX
            val overlapsY = targetY < (item.cellY + item.spanY) && (targetY + targetSpanY) > item.cellY

            overlapsX && overlapsY
        }
    }
}