package com.gluon.launcher.launcher.ui.screens.dashboard

import android.content.Context
import android.view.ViewConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import com.gluon.launcher.core.data.WorkspaceItem
import com.gluon.launcher.core.data.WorkspaceWidgetItem

fun Modifier.dashboardSwipeGestures(
    context: Context,
    density: androidx.compose.ui.unit.Density,
    gridSize: IntSize,
    gridWindowPosRef: FloatArray,
    currentCellPixelWidth: Int,
    currentCellPixelHeight: Int,
    currentWorkspaceItems: List<WorkspaceItem>,
    currentPage: Int,
    haptic: HapticFeedback,
    wasMenuOpenAtStartProvider: () -> Boolean,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit
): Modifier = this.pointerInput(gridSize) {
    val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    val swipeThresholdSq = (touchSlop * 1.5f) * (touchSlop * 1.5f)

    awaitPointerEventScope {
        while (true) {
            var event = awaitPointerEvent(PointerEventPass.Main)
            var downChange = event.changes.firstOrNull { it.pressed }

            while (downChange == null || downChange.isConsumed) {
                event = awaitPointerEvent(PointerEventPass.Main)
                downChange = event.changes.firstOrNull { it.pressed }
            }

            val wasMenuOpenAtStart = wasMenuOpenAtStartProvider()

            val startY = downChange.position.y
            val startX = downChange.position.x

            val marginPx = with(density) { 12.dp.toPx() }
            val gridLocalX = startX - gridWindowPosRef[0] - marginPx
            val gridLocalY = startY - gridWindowPosRef[1]

            val overWidget = if (gridLocalX >= 0 && gridLocalY >= 0 && currentCellPixelWidth > 0 && currentCellPixelHeight > 0) {
                val cX = (gridLocalX / currentCellPixelWidth).toInt()
                val cY = (gridLocalY / currentCellPixelHeight).toInt()
                currentWorkspaceItems.any {
                    it is WorkspaceWidgetItem &&
                            it.screenId == currentPage &&
                            cX >= it.cellX && cX < it.cellX + it.spanX &&
                            cY >= it.cellY && cY < it.cellY + it.spanY
                }
            } else false

            if (wasMenuOpenAtStart || overWidget) {
                while (event.changes.any { it.pressed }) {
                    event = awaitPointerEvent(PointerEventPass.Main)
                }
                continue
            }

            val bottomDeadZone = with(density) { 35.dp.toPx() }
            val screenHeight = size.height.toFloat()

            if (startY > screenHeight - bottomDeadZone) {
                while (event.changes.any { it.pressed }) {
                    event = awaitPointerEvent(PointerEventPass.Main)
                }
                continue
            }

            while (event.changes.any { it.pressed }) {
                event = awaitPointerEvent(PointerEventPass.Main)
                val moveChange = event.changes.firstOrNull() ?: break

                if (moveChange.isConsumed) break

                val dx = moveChange.position.x - startX
                val dy = moveChange.position.y - startY
                val distanceSq = dx * dx + dy * dy

                if (distanceSq > swipeThresholdSq) {
                    if (kotlin.math.abs(dy) > kotlin.math.abs(dx) * 0.8f) {
                        if (dy < 0) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSwipeUp()
                        } else {
                            onSwipeDown()
                        }
                        moveChange.consume()
                    }
                    break
                }
            }

            while (event.changes.any { it.pressed }) {
                event = awaitPointerEvent(PointerEventPass.Main)
            }
        }
    }
}