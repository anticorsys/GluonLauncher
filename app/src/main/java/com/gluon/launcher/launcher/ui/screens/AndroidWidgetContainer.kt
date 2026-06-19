// app/src/main/java/com/gluon/launcher/launcher/ui/screens/AndroidWidgetContainer.kt
package com.gluon.launcher.launcher.ui.screens

import android.appwidget.AppWidgetHostView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.gluon.launcher.core.data.WorkspaceWidgetItem
import com.gluon.launcher.core.widget.WidgetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AndroidWidgetContainer(
    item: WorkspaceWidgetItem,
    modifier: Modifier = Modifier,
    cellPixelWidth: Int = 0,
    cellPixelHeight: Int = 0,
    isEditMode: Boolean = false
) {
    val context = LocalContext.current

    var hostView by remember { mutableStateOf<AppWidgetHostView?>(null) }
    var isLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(item.appWidgetId) {
        withContext(Dispatchers.IO) {
            val view = WidgetManager.getOrCreateView(context, item.appWidgetId)
            withContext(Dispatchers.Main) {
                hostView = view
                isLoaded = true
            }
        }
    }

    var lastWidth by remember { mutableIntStateOf(0) }
    var lastHeight by remember { mutableIntStateOf(0) }

    LaunchedEffect(item.spanX, item.spanY, cellPixelWidth, cellPixelHeight, hostView) {
        if (hostView != null && cellPixelWidth > 0 && cellPixelHeight > 0) {
            val targetWidth = cellPixelWidth * item.spanX
            val targetHeight = cellPixelHeight * item.spanY

            if (kotlin.math.abs(targetWidth - lastWidth) > 5 || kotlin.math.abs(targetHeight - lastHeight) > 5) {
                WidgetManager.updateWidgetSizePx(item.appWidgetId, targetWidth, targetHeight)
                // ИСПРАВЛЕНИЕ: Меньше лишних requestLayout
                lastWidth = targetWidth
                lastHeight = targetHeight
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (!isLoaded) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            }
        } else {
            if (hostView != null) {
                AndroidView(
                    factory = {
                        val view = hostView!!
                        (view.parent as? ViewGroup)?.removeView(view)
                        view.apply {
                            setPadding(0, 0, 0, 0)
                            clipChildren = false
                            clipToPadding = false
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                        }
                        view
                    },
                    update = { _ ->
                        // ИСПРАВЛЕНИЕ: Оставляем абсолютно пустым.
                        // Compose больше не будет дергать View при свайпах Pager'а, убирая лаги.
                    },
                    modifier = Modifier.fillMaxSize()
                )
                if (isEditMode) {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)))
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Ошибка виджета", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}