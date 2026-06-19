// app/src/main/java/com/gluon/launcher/launcher/ui/components/MainDockBar.kt
package com.gluon.launcher.launcher.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.gluon.launcher.core.data.AppModel
import com.gluon.launcher.core.theme.M3EShapes

@Composable
fun MainDockBar(
    modifier: Modifier = Modifier,
    dockPackages: List<String>,
    allApps: List<AppModel>,
    showIconBorder: Boolean,
    showDockLabels: Boolean = false,
    onAppClick: (AppModel) -> Unit,
    onRemoveFromDock: (String) -> Unit,
    onMoveLeft: (String) -> Unit,
    onMoveRight: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") onHideApp: (String) -> Unit = {},
    isDockBarHidden: Boolean = false,
    onToggleDockBarHidden: (Boolean) -> Unit = {},
    maxDockApps: Int = 5,
    isEditMode: Boolean = false,
    customHeight: Dp = 85.dp,
    isEditModePanel: Boolean = false,
    onDragStart: (AppModel) -> Unit = {},
    isHoveringDockBar: Boolean = false,
    draggedAppPackage: String? = null,
    isDraggingFromDock: Boolean = false,
    targetInsertIndex: Int = -1,
    isPredictiveEnabled: Boolean = false,
    onDisablePredictive: () -> Unit = {},
    isDynamicDockBar: Boolean = false,
    isWorkspaceLocked: Boolean = false
) {
    val effectiveVisible = !isDockBarHidden
    val showEditFrame = isEditMode && isEditModePanel
    if (!effectiveVisible) return

    val uniquePackages = dockPackages.distinct().take(maxDockApps)
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val haptic = LocalHapticFeedback.current

    val renderItems = mutableListOf<AppModel?>()
    uniquePackages.forEach { pkg ->
        if (isDraggingFromDock && draggedAppPackage == pkg) {
            // пропускаем
        } else {
            allApps.find { it.packageName == pkg }?.let { renderItems.add(it) }
        }
    }
    if (isHoveringDockBar && renderItems.size < maxDockApps && targetInsertIndex != -1) {
        val safeIndex = targetInsertIndex.coerceIn(0, renderItems.size)
        renderItems.add(safeIndex, null)
    }

    val shape = RoundedCornerShape(if (isEditMode && isEditModePanel) 50.dp else M3EShapes.ExtraExtraLarge)

    var showDockMenu by remember { mutableStateOf(false) }
    var dockMenuOffset by remember { mutableStateOf(DpOffset.Zero) }
    var globalPos by remember { mutableStateOf(Offset.Zero) }

    val dockTextColor = MaterialTheme.colorScheme.onSurface

    var isDockPressed by remember { mutableStateOf(false) }
    val dockAnimatedScale by animateFloatAsState(
        targetValue = if (isDockPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "dockScale"
    )

    BoxWithConstraints(
        modifier = modifier
            .wrapContentWidth(Alignment.CenterHorizontally)
            .then(if (showEditFrame) Modifier.fillMaxWidth() else Modifier.wrapContentWidth())
            .onGloballyPositioned { globalPos = it.positionInWindow() }
            .pointerInput(Unit) {
                if (!isEditMode) {
                    detectTapGestures(
                        onPress = {
                            isDockPressed = true
                            tryAwaitRelease()
                            isDockPressed = false
                        },
                        onLongPress = { offset ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val screenWidth = windowInfo.containerSize.width.toFloat()
                            val screenHeight = windowInfo.containerSize.height.toFloat()
                            val menuW = with(density) { 250.dp.toPx() }
                            val menuH = with(density) { 150.dp.toPx() }

                            var localX = offset.x
                            var localY = offset.y

                            val absX = globalPos.x + localX
                            val absY = globalPos.y + localY

                            if (absX + menuW > screenWidth) localX -= (absX + menuW - screenWidth + with(density) { 16.dp.toPx() })
                            if (absY + menuH > screenHeight) localY -= (absY + menuH - screenHeight + with(density) { 24.dp.toPx() })

                            dockMenuOffset = DpOffset(with(density) { localX.toDp() }, with(density) { localY.toDp() })
                            showDockMenu = true
                        }
                    )
                }
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        val maxAvailableWidth = this.maxWidth

        val iconSize = if (maxDockApps >= 5) 54.dp else 60.dp
        val padding = 12.dp

        val verticalPadding = maxOf(padding, 10.dp)
        val itemsCount = renderItems.size

        val targetDockWidth = if (isDynamicDockBar && itemsCount > 0) {
            val spacing = 16.dp
            val sidePaddings = 24.dp
            (iconSize * itemsCount) + (spacing * (itemsCount - 1)) + sidePaddings
        } else {
            maxAvailableWidth
        }

        val animatedDockWidth by animateDpAsState(
            targetValue = targetDockWidth,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
            label = "dockWidthAnim"
        )

        val backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh
        val borderStroke = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        CompositionLocalProvider(LocalContentColor provides dockTextColor) {
            if (showEditFrame) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(85.dp) // Жестко зафиксировано 85dp
                        .padding(vertical = 8.dp)
                        .background(Color.Transparent, shape)
                        .border(width = 2.dp, color = MaterialTheme.colorScheme.primary, shape = shape)
                        .clip(shape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(customHeight))
                }
            } else {
                Box(
                    modifier = Modifier
                        .width(animatedDockWidth)
                        .height(85.dp) // Жестко зафиксировано 85dp
                        .graphicsLayer {
                            scaleX = dockAnimatedScale
                            scaleY = dockAnimatedScale
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .width(animatedDockWidth)
                            .height(85.dp) // Жестко зафиксировано 85dp для идеальной симметрии
                            .shadow(elevation = 8.dp, shape = shape, clip = false)
                            .border(borderStroke, shape)
                            .background(backgroundColor, shape)
                            .padding(horizontal = padding, vertical = verticalPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        if (itemsCount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Top
                            ) {
                                renderItems.forEach { app ->
                                    key(app?.packageName ?: "drop_target") {
                                        Box(
                                            modifier = Modifier.weight(1f).height(85.dp),
                                            contentAlignment = Alignment.TopCenter
                                        ) {
                                            if (app == null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(iconSize)
                                                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                                )
                                            } else {
                                                AppIconItem(
                                                    app = app,
                                                    showLabel = showDockLabels,
                                                    showIconBorder = showIconBorder,
                                                    isInDockBar = true,
                                                    onAppClick = onAppClick,
                                                    onDragStart = { if (!isWorkspaceLocked) onDragStart(app) },
                                                    modifier = Modifier,
                                                    enableShadow = true,
                                                    contextMenuOffset = DpOffset(16.dp, 0.dp),
                                                    textColor = dockTextColor,
                                                    iconSizeOverride = iconSize,
                                                    onContextMenu = { dismiss ->
                                                        Column(
                                                            modifier = Modifier.width(250.dp),
                                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            if (isPredictiveEnabled) {
                                                                GluonPopupMenuCard(
                                                                    enabled = !isWorkspaceLocked,
                                                                    onClick = { if (!isWorkspaceLocked) { dismiss(); onDisablePredictive() } }
                                                                ) {
                                                                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(28.dp).padding(end = 10.dp), tint = LocalContentColor.current)
                                                                    GluonMarqueeText("Отключить умный док-бар")
                                                                }
                                                            } else {
                                                                val currentIndex = renderItems.indexOf(app)
                                                                if (currentIndex > 0) {
                                                                    GluonPopupMenuCard(
                                                                        enabled = !isWorkspaceLocked,
                                                                        onClick = { if (!isWorkspaceLocked) { dismiss(); onMoveLeft(app.packageName) } }
                                                                    ) {
                                                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(28.dp).padding(end = 10.dp), tint = LocalContentColor.current)
                                                                        GluonMarqueeText("Переместить влево")
                                                                    }
                                                                }
                                                                if (currentIndex < renderItems.size - 1 && currentIndex != -1) {
                                                                    GluonPopupMenuCard(
                                                                        enabled = !isWorkspaceLocked,
                                                                        onClick = { if (!isWorkspaceLocked) { dismiss(); onMoveRight(app.packageName) } }
                                                                    ) {
                                                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(28.dp).padding(end = 10.dp), tint = LocalContentColor.current)
                                                                        GluonMarqueeText("Переместить вправо")
                                                                    }
                                                                }
                                                                GluonPopupMenuCard(
                                                                    enabled = !isWorkspaceLocked,
                                                                    onClick = { if (!isWorkspaceLocked) { dismiss(); onRemoveFromDock(app.packageName) } }
                                                                ) {
                                                                    Icon(Icons.Default.Close, null, Modifier.size(28.dp).padding(end = 10.dp), tint = LocalContentColor.current)
                                                                    GluonMarqueeText("Убрать из док-бара")
                                                                }
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Нет приложений в док-баре", style = MaterialTheme.typography.bodySmall, color = dockTextColor.copy(alpha = 0.6f), textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }

        if (!isEditMode) {
            GluonAnimatedDropdownMenu(
                expanded = showDockMenu,
                onDismissRequest = { showDockMenu = false },
                offset = DpOffset(dockMenuOffset.x, dockMenuOffset.y),
                modifier = Modifier.width(250.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    GluonPopupMenuCard(
                        enabled = !isWorkspaceLocked,
                        onClick = {
                            if (!isWorkspaceLocked) {
                                onToggleDockBarHidden(!isDockBarHidden)
                                showDockMenu = false
                            }
                        }
                    ) {
                        Icon(
                            if (isWorkspaceLocked) Icons.Default.Lock else Icons.Default.Home,
                            null, Modifier.size(28.dp).padding(end = 10.dp), tint = LocalContentColor.current
                        )
                        GluonMarqueeText("Скрыть док-бар", Modifier.weight(1f))
                        Switch(
                            checked = isDockBarHidden,
                            enabled = !isWorkspaceLocked,
                            onCheckedChange = null,
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }
            }
        }
    }
}