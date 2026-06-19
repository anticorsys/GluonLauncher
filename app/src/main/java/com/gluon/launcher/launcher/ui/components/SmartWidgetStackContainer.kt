// app/src/main/java/com/gluon/launcher/launcher/ui/components/SmartWidgetStackContainer.kt
package com.gluon.launcher.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.gluon.launcher.core.data.WorkspaceWidgetItem
import com.gluon.launcher.launcher.ui.screens.AndroidWidgetContainer

@Composable
fun SmartWidgetStackContainer(
    widgets: List<WorkspaceWidgetItem>,
    modifier: Modifier = Modifier,
    cellPixelWidth: Int = 0,
    cellPixelHeight: Int = 0,
    isEditMode: Boolean = false,
    onActiveWidgetChanged: (WorkspaceWidgetItem) -> Unit = {}
) {
    if (widgets.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { widgets.size })

    LaunchedEffect(pagerState.currentPage, widgets) {
        if (widgets.isNotEmpty()) {
            val safeIndex = pagerState.currentPage.coerceIn(0, maxOf(0, widgets.size - 1))
            onActiveWidgetChanged(widgets[safeIndex])
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isEditMode,
            // ИСПРАВЛЕНИЕ: Добавлен отступ между виджетами в стопке
            pageSpacing = 16.dp,
            // Держим все виджеты стопки в памяти, исключая их удаление и перерисовку при свайпе
            beyondViewportPageCount = widgets.size.coerceAtLeast(1),
            key = { page -> widgets.getOrNull(page)?.id ?: page }
        ) { page ->
            val safePage = page.coerceIn(0, maxOf(0, widgets.size - 1))
            AndroidWidgetContainer(
                item = widgets[safePage],
                modifier = Modifier.fillMaxSize(),
                cellPixelWidth = cellPixelWidth,
                cellPixelHeight = cellPixelHeight,
                isEditMode = isEditMode
            )
        }

        if (widgets.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(widgets.size) { index ->
                    val isSelected = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 10.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                    )
                }
            }
        }
    }
}