// app/src/main/java/com/gluon/launcher/launcher/ui/components/FolderIconItem.kt
package com.gluon.launcher.launcher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gluon.launcher.core.data.AppModel
import com.gluon.launcher.core.data.states.NotificationState

@Composable
fun FolderIconItem(
    name: String,
    apps: List<AppModel>,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    iconSizeOverride: Dp? = null,
    textColor: Color? = null,
    showContextMenu: Boolean = true,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    onContextMenu: @Composable (dismiss: () -> Unit) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current

    val iconSize = iconSizeOverride ?: 60.dp
    var showMenu by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
    var isPressed by remember { mutableStateOf(false) }
    val globalPosRef = remember { FloatArray(2) }

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "folder_scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(2.dp)
            .onGloballyPositioned {
                val pos = it.positionInWindow()
                globalPosRef[0] = pos.x
                globalPosRef[1] = pos.y
            }
            .pointerInput(name) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                )
            }
            .then(
                if (onLongPress != null || showContextMenu) {
                    Modifier.pointerInput(name, "drag") {
                        var dragStarted = false
                        var slopAccumulator = 0f
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                dragStarted = false
                                slopAccumulator = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                val screenW = windowInfo.containerSize.width.toFloat()
                                val screenH = windowInfo.containerSize.height.toFloat()
                                val menuW = with(density) { 250.dp.toPx() }
                                val menuH = with(density) { 200.dp.toPx() } // Запас для меню папки

                                var localX = offset.x
                                var localY = offset.y + with(density) { 16.dp.toPx() }
                                val absX = globalPosRef[0] + localX
                                val absY = globalPosRef[1] + localY

                                if (absX + menuW > screenW) localX -= (absX + menuW - screenW + with(density) { 16.dp.toPx() })
                                if (absY + menuH > screenH) localY -= (absY + menuH - screenH + with(density) { 24.dp.toPx() })

                                pressOffset = DpOffset(with(density) { localX.toDp() }, with(density) { localY.toDp() })
                                if (showContextMenu) showMenu = true
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (!dragStarted) {
                                    slopAccumulator += dragAmount.getDistance()
                                    if (slopAccumulator > 60f) {
                                        dragStarted = true
                                        showMenu = false
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onLongPress?.invoke()
                                    }
                                }
                            },
                            onDragEnd = { dragStarted = false; isPressed = false },
                            onDragCancel = { dragStarted = false; isPressed = false }
                        )
                    }
                } else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.graphicsLayer { scaleX = animatedScale; scaleY = animatedScale },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(iconSize * 0.95f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val previewApps = apps.take(9)
                val size = previewApps.size

                Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    when {
                        size <= 3 -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { previewApps.forEach { MiniIcon(it) } }
                        }
                        size == 4 -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { MiniIcon(previewApps[0]); MiniIcon(previewApps[1]) }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { MiniIcon(previewApps[2]); MiniIcon(previewApps[3]) }
                        }
                        size == 5 -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { MiniIcon(previewApps[0]); MiniIcon(previewApps[1]); MiniIcon(previewApps[2]) }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { MiniIcon(previewApps[3]); MiniIcon(previewApps[4]) }
                        }
                        size in 6..9 -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { MiniIcon(previewApps[0]); MiniIcon(previewApps[1]); MiniIcon(previewApps[2]) }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { MiniIcon(previewApps[3]); MiniIcon(previewApps[4]); MiniIcon(previewApps[5]) }
                            if (size > 6) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    for(i in 6 until size) MiniIcon(previewApps[i])
                                }
                            }
                        }
                    }
                }

                // ИСПРАВЛЕНИЕ: Индикатор уведомлений над папкой
                val notifications by NotificationState.notifications.collectAsStateWithLifecycle()
                val hasNotification = remember(apps, notifications) {
                    apps.any { (notifications[it.packageName] ?: 0) > 0 }
                }
                if (hasNotification) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(12.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }
            }

            if (showLabel) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
                    color = textColor ?: MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (showMenu) {
            GluonAnimatedDropdownMenu(expanded = true, onDismissRequest = { showMenu = false }, offset = pressOffset, modifier = Modifier.width(250.dp)) {
                onContextMenu { showMenu = false }
            }
        }
    }
}

@Composable
private fun MiniIcon(app: AppModel?) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(if (app == null) Color.Transparent else MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        if (app?.iconBitmap != null) {
            Image(bitmap = app.iconBitmap, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
        }
    }
}