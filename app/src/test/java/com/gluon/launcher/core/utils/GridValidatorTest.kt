package com.gluon.launcher.core.utils

import com.gluon.launcher.core.data.WorkspaceAppItem
import com.gluon.launcher.core.data.WorkspaceFolderItem
import com.gluon.launcher.core.data.WorkspaceWidgetItem
import org.junit.Assert.*
import org.junit.Test

class GridValidatorTest {

    private val cols = 4
    private val rows = 8

    // ----- isAreaVacant -----

    @Test
    fun `isAreaVacant - пустой список, любая область свободна`() {
        assertTrue(GridValidator.isAreaVacant(emptyList(), 0, 0, 0, 2, 2, cols, rows))
    }

    @Test
    fun `isAreaVacant - без пересечений`() {
        val items = listOf(
            WorkspaceAppItem("1", "a", "A", 0, 0, 0),
            WorkspaceWidgetItem("2", 99, 0, 1, 1, 1, 1)
        )
        assertTrue(GridValidator.isAreaVacant(items, 0, 2, 0, 1, 1, cols, rows))
    }

    @Test
    fun `isAreaVacant - с пересечением`() {
        val items = listOf(
            WorkspaceAppItem("1", "a", "A", 0, 1, 1, 2, 2)
        )
        assertFalse(GridValidator.isAreaVacant(items, 0, 0, 0, 3, 3, cols, rows))
    }

    @Test
    fun `isAreaVacant - выход за границы сетки`() {
        assertFalse(GridValidator.isAreaVacant(emptyList(), 0, 3, 0, 2, 1, cols, rows)) // spanX выходит за cols
        assertFalse(GridValidator.isAreaVacant(emptyList(), 0, 0, 7, 1, 2, cols, rows)) // spanY выходит за rows
    }

    @Test
    fun `isAreaVacant - игнорируемые ID не считаются пересечением`() {
        val items = listOf(
            WorkspaceAppItem("skip", "a", "A", 0, 0, 0, 2, 2)
        )
        assertTrue(GridValidator.isAreaVacant(items, 0, 0, 0, 2, 2, cols, rows, setOf("skip")))
    }

    // ----- getOverlappingItems -----

    @Test
    fun `getOverlappingItems - находит пересекающиеся элементы`() {
        val item1 = WorkspaceAppItem("1", "a", "A", 0, 0, 0, 2, 2)
        val item2 = WorkspaceWidgetItem("2", 99, 0, 1, 1, 1, 1)
        val items = listOf(item1, item2)
        val overlaps = GridValidator.getOverlappingItems(items, 0, 0, 0, 3, 3)
        assertEquals(2, overlaps.size)
        assertTrue(item1 in overlaps)
        assertTrue(item2 in overlaps)
    }

    @Test
    fun `getOverlappingItems - исключает элементы из ignoreItemIds`() {
        val item1 = WorkspaceAppItem("1", "a", "A", 0, 0, 0, 2, 2)
        val item2 = WorkspaceFolderItem("2", "folder", listOf("pkg"), 0, 0, 0)
        val items = listOf(item1, item2)
        val overlaps = GridValidator.getOverlappingItems(items, 0, 0, 0, 2, 2, setOf("1"))
        assertEquals(1, overlaps.size)
        assertEquals(item2, overlaps[0])
    }

    // ----- findFirstVacantCell -----

    @Test
    fun `findFirstVacantCell - пустая сетка, первая ячейка`() {
        val result = GridValidator.findFirstVacantCell(emptyList(), 0, 1, 1, cols, rows)
        assertNotNull(result)
        assertEquals(0 to 0, result)
    }

    @Test
    fun `findFirstVacantCell - занята первая строка, находит во второй`() {
        // Заполним всю первую строку
        val items = (0 until cols).map { col ->
            WorkspaceAppItem("$col", "pkg$col", "App$col", 0, col, 0)
        }
        val result = GridValidator.findFirstVacantCell(items, 0, 1, 1, cols, rows)
        assertNotNull(result)
        // Первая свободная ячейка должна быть (0, 1)
        assertEquals(0 to 1, result)
    }

    @Test
    fun `findFirstVacantCell - нет свободного места, возвращает null`() {
        // Заполним всю сетку элементами 1x1
        val items = (0 until rows).flatMap { y ->
            (0 until cols).map { x ->
                WorkspaceAppItem("${x}_${y}", "pkg", "App", 0, x, y)
            }
        }
        val result = GridValidator.findFirstVacantCell(items, 0, 1, 1, cols, rows)
        assertNull(result)
    }

    @Test
    fun `findFirstVacantCell - учитывает span размещаемого элемента`() {
        // Ставим элемент 2x2 в левый верхний угол
        val item = WorkspaceWidgetItem("big", 1, 0, 0, 0, 2, 2)
        val result = GridValidator.findFirstVacantCell(listOf(item), 0, 1, 1, cols, rows)
        // Первая свободная 1x1 должна быть в (2,0) или (0,2), смотря алгоритм
        // Ожидаем, что (2,0) будет найдена первой при обходе y=0, x=2
        assertNotNull(result)
        assertEquals(2 to 0, result)
    }
}