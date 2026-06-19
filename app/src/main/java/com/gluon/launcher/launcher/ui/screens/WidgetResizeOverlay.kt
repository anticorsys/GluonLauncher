package com.gluon.launcher.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun WidgetResizeOverlay(
    itemCellX: Int,
    itemCellY: Int,
    previewSpanX: Int,
    previewSpanY: Int,
    cellWidthDp: Dp,
    cellHeightDp: Dp,
    horizontalMargin: Dp,
    isValidSize: Boolean,
    onDragRightStart: () -> Unit,
    onDragRight: (deltaXDp: Float) -> Unit,
    onDragBottomStart: () -> Unit,
    onDragBottom: (deltaYDp: Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val markerSize = 24.dp

    Box(
        modifier = Modifier
            .absoluteOffset(horizontalMargin + cellWidthDp * itemCellX, cellHeightDp * itemCellY)
            .size(cellWidthDp * previewSpanX, cellHeightDp * previewSpanY)
            .background(
                if (isValidSize) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                RoundedCornerShape(16.dp)
            )
            .border(2.dp, if (isValidSize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, RoundedCornerShape(16.dp))
    ) {
        // Правый маркер изменения размера
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = markerSize / 2)
                .size(markerSize)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .border(2.dp, Color.White, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            onDragRightStart()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDragRight(dragAmount.x)
                        },
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel
                    )
                }
        )

        // Нижний маркер изменения размера
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = markerSize / 2)
                .size(markerSize)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .border(2.dp, Color.White, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            onDragBottomStart()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDragBottom(dragAmount.y)
                        },
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel
                    )
                }
        )
    }
}