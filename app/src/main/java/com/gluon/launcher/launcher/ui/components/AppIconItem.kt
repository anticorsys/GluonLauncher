// app/src/main/java/com/gluon/launcher/launcher/ui/components/AppIconItem.kt
package com.gluon.launcher.launcher.ui.components

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import coil.compose.AsyncImage
import com.gluon.launcher.core.data.AppModel
import com.gluon.launcher.core.data.states.NotificationState
import com.gluon.launcher.core.utils.launchApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppShortcutItem(
    val id: String,
    val label: String,
    val icon: android.graphics.drawable.Drawable?
)

@Composable
fun AppIconItem(
    app: AppModel,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
    showIconBorder: Boolean = false,
    isInDockBar: Boolean = false,
    isInGrid: Boolean = false,
    onAppClick: ((AppModel) -> Unit)? = null,
    onLongPress: ((AppModel) -> Unit)? = null,
    onDragStart: ((AppModel) -> Unit)? = null,
    onContextMenu: @Composable (dismiss: () -> Unit) -> Unit = {},
    enableShadow: Boolean = false,
    contextMenuOffset: DpOffset = DpOffset.Zero,
    iconSizeOverride: Dp? = null,
    showContextMenu: Boolean = true,
    textColor: Color? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current

    var showMenu by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(contextMenuOffset) }
    var lastLongPressTime by remember { mutableLongStateOf(0L) }

    var appShortcuts by remember { mutableStateOf<List<AppShortcutItem>>(emptyList()) }

    LaunchedEffect(showMenu) {
        if (showMenu && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            withContext(Dispatchers.IO) {
                try {
                    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                    val query = LauncherApps.ShortcutQuery()
                    query.setPackage(app.packageName)
                    query.setQueryFlags(
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                    )
                    val userHandle = Process.myUserHandle()
                    val fetchedShortcuts = launcherApps.getShortcuts(query, userHandle) ?: emptyList()

                    val displayDensity = configuration.densityDpi
                    val mapped = fetchedShortcuts.take(4).map { sc ->
                        val scIcon = try {
                            launcherApps.getShortcutIconDrawable(sc, displayDensity)
                        } catch (_: Exception) { null }

                        AppShortcutItem(
                            id = sc.id,
                            label = (sc.shortLabel ?: sc.id).toString(),
                            icon = scIcon
                        )
                    }
                    withContext(Dispatchers.Main) {
                        appShortcuts = mapped
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else if (!showMenu) {
            appShortcuts = emptyList()
        }
    }

    val globalPosRef = remember { FloatArray(2) }
    val iconBitmap: ImageBitmap? = app.iconBitmap

    val currentOnClick by rememberUpdatedState { (onAppClick ?: { appModel -> context.launchApp(appModel) })(app) }
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentShowContextMenu by rememberUpdatedState(showContextMenu)

    val iconShape = CircleShape
    val iconSize = iconSizeOverride ?: 60.dp

    val paddingModifier = remember(isInDockBar, isInGrid) {
        when {
            isInDockBar || isInGrid -> Modifier.padding(2.dp)
            else -> Modifier.padding(4.dp)
        }
    }

    var isPressed by remember { mutableStateOf(false) }

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "icon_scale_animation"
    )

    val cellInteractionModifier = Modifier
        .pointerInput(app.packageName) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                },
                onTap = {
                    val now = System.currentTimeMillis()
                    if (now - lastLongPressTime > 500 && !showMenu) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentOnClick()
                    }
                }
            )
        }
        .then(
            if (currentShowContextMenu || currentOnLongPress != null || currentOnDragStart != null) {
                Modifier.pointerInput(app.packageName, "drag") {
                    var dragStarted = false
                    var slopAccumulator = 0f

                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            dragStarted = false
                            slopAccumulator = 0f
                            lastLongPressTime = System.currentTimeMillis()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            val screenWidth = windowInfo.containerSize.width.toFloat()
                            val screenHeight = windowInfo.containerSize.height.toFloat()
                            val menuWidthPx = with(density) { 250.dp.toPx() }
                            val menuHeightPx = with(density) { 400.dp.toPx() }

                            var localX = offset.x
                            var localY = offset.y + with(density) { 16.dp.toPx() }

                            val absX = globalPosRef[0] + localX
                            val absY = globalPosRef[1] + localY

                            if (absX + menuWidthPx > screenWidth) {
                                localX -= (absX + menuWidthPx - screenWidth + with(density) { 16.dp.toPx() })
                            }
                            if (absY + menuHeightPx > screenHeight) {
                                localY -= (absY + menuHeightPx - screenHeight + with(density) { 24.dp.toPx() })
                            }

                            pressOffset = DpOffset(with(density) { localX.toDp() }, with(density) { localY.toDp() })
                            if (currentShowContextMenu) showMenu = true
                        },
                        onDrag = { change, dragAmount ->
                            if (!dragStarted) {
                                slopAccumulator += dragAmount.getDistance()
                                change.consume()
                                if (slopAccumulator > 60f) {
                                    dragStarted = true
                                    showMenu = false
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    currentOnDragStart?.invoke(app)
                                }
                            } else {
                                change.consume()
                            }
                        },
                        onDragEnd = {
                            if (!dragStarted) currentOnLongPress?.invoke(app)
                            dragStarted = false
                            isPressed = false
                        },
                        onDragCancel = {
                            dragStarted = false
                            isPressed = false
                        }
                    )
                }
            } else Modifier
        )

    Column(
        modifier = modifier
            .then(if (isInGrid) Modifier.fillMaxWidth() else Modifier)
            .then(cellInteractionModifier)
            .then(paddingModifier)
            .onGloballyPositioned {
                val pos = it.positionInWindow()
                globalPosRef[0] = pos.x
                globalPosRef[1] = pos.y
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .aspectRatio(1f)
                    .then(
                        if (enableShadow) Modifier.shadow(elevation = 0.75.dp, shape = iconShape, clip = true)
                        else Modifier
                    )
                    .then(
                        if (showIconBorder) Modifier.border(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), shape = iconShape)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(iconShape),
                        contentScale = if (isInDockBar) ContentScale.Crop else ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(iconShape)
                            .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), shape = iconShape)
                            .background(Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = app.label.take(1).uppercase(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }

                // ИСПРАВЛЕНИЕ: Индикатор уведомлений над иконкой
                val notifications by NotificationState.notifications.collectAsStateWithLifecycle()
                val hasNotification = (notifications[app.packageName] ?: 0) > 0
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
                val labelSpacing = if (isInDockBar || isInGrid) 4.dp else 8.dp
                val (fontSizeSp, lineHeightSp) = if (isInDockBar || isInGrid) 12f to 14f else 14f to 18f

                Spacer(modifier = Modifier.height(labelSpacing))

                Text(
                    text = app.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = fontSizeSp.sp,
                        lineHeight = lineHeightSp.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    ),
                    color = textColor ?: MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (currentShowContextMenu) {
            GluonAnimatedDropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                offset = pressOffset,
                modifier = Modifier.width(250.dp)
            ) {
                if (appShortcuts.isNotEmpty()) {
                    appShortcuts.forEachIndexed { index, shortcut ->
                        GluonPopupMenuCard(
                            onClick = {
                                showMenu = false
                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                                        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                                        launcherApps.startShortcut(app.packageName, shortcut.id, null, null, Process.myUserHandle())
                                    }
                                } catch (_: Exception) { }
                            }
                        ) {
                            if (shortcut.icon != null) {
                                AsyncImage(
                                    model = shortcut.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp).padding(end = 8.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.size(24.dp).padding(end = 8.dp))
                            }
                            GluonMarqueeText(
                                text = shortcut.label,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (index < appShortcuts.size - 1) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                }
                onContextMenu { showMenu = false }
            }
        }
    }
}