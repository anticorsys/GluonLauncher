// app/src/main/java/com/gluon/launcher/launcher/components/ProfileGlassCard.kt
package com.gluon.launcher.launcher.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.gluon.launcher.launcher.ui.components.GluonAnimatedDropdownMenu
import com.gluon.launcher.launcher.ui.components.GluonMarqueeText
import com.gluon.launcher.launcher.ui.components.GluonPopupMenuCard

@Composable
fun ProfileGlassCard(
    userName: String,
    showAvatar: Boolean,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    onAvatarClick: () -> Unit = {},
    isInfoPanelVisible: Boolean = true,
    onToggleInfoPanel: (Boolean) -> Unit = {},
    isEditMode: Boolean = false,
    customHeight: Dp = 60.dp,
    isEditModePanel: Boolean = false,
    isWorkspaceLocked: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }
    var cardHeight by remember { mutableStateOf(0.dp) }
    var globalPos by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val haptic = LocalHapticFeedback.current

    if (!isInfoPanelVisible && !isEditMode) return
    if (isEditMode && !isInfoPanelVisible && isEditModePanel) return

    val shape = RoundedCornerShape(if (isEditMode && isEditModePanel) 50.dp else 100.dp)

    var isCardPressed by remember { mutableStateOf(false) }
    val cardAnimatedScale by animateFloatAsState(
        targetValue = if (isCardPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "cardScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(85.dp) // Жестко зафиксировано 85dp для идеальной симметрии
            .padding(0.dp)
            .onGloballyPositioned {
                cardHeight = with(density) { it.size.height.toDp() }
                globalPos = it.positionInWindow()
            },
        contentAlignment = Alignment.Center
    ) {
        if (isEditMode && isEditModePanel) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(customHeight)
                    .border(width = 2.dp, color = MaterialTheme.colorScheme.primary, shape = shape)
            )
        } else if (!isEditMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(85.dp) // Жестко зафиксировано 85dp
                    .graphicsLayer {
                        scaleX = cardAnimatedScale
                        scaleY = cardAnimatedScale
                    }
                    .shadow(8.dp, shape, ambientColor = Color.Black.copy(alpha = 0.5f))
                    .clip(shape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(85.dp) // Жестко зафиксировано 85dp
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isCardPressed = true
                                    tryAwaitRelease()
                                    isCardPressed = false
                                },
                                onLongPress = { offset ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val screenWidth = windowInfo.containerSize.width.toFloat()
                                    val screenHeight = windowInfo.containerSize.height.toFloat()
                                    val menuW = with(density) { 250.dp.toPx() }
                                    // Запас высоты меню инфо-панели
                                    val menuH = with(density) { 200.dp.toPx() }

                                    var localX = offset.x
                                    var localY = offset.y

                                    val absX = globalPos.x + localX
                                    val absY = globalPos.y + localY

                                    if (absX + menuW > screenWidth) localX -= (absX + menuW - screenWidth + with(density) { 16.dp.toPx() })
                                    if (absY + menuH > screenHeight) localY -= (absY + menuH - screenHeight + with(density) { 24.dp.toPx() })

                                    menuOffset = DpOffset(with(density) { localX.toDp() }, with(density) { localY.toDp() })
                                    showMenu = true
                                }
                            )
                        }
                        .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)), shape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(start = 14.dp)
                            .fillMaxWidth()
                            .height(85.dp), // Жестко зафиксировано 85dp
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showAvatar) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .pointerInput(Unit) {
                                        detectTapGestures(onTap = { onAvatarClick() })
                                    }
                            ) {
                                if (!avatarUrl.isNullOrEmpty()) {
                                    SubcomposeAsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(avatarUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Аватар профиля",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        loading = {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                            }
                                        },
                                        error = {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text(text = userName.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.ExtraBold)
                                            }
                                        }
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(text = userName.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                            VerticalDivider(
                                modifier = Modifier.height(40.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (!isEditMode) {
            GluonAnimatedDropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                offset = menuOffset,
                modifier = Modifier.width(250.dp)
            ) {
                InfoPanelControlPopup(
                    isVisible = isInfoPanelVisible,
                    isWorkspaceLocked = isWorkspaceLocked,
                    onToggleVisibility = { onToggleInfoPanel(it); showMenu = false },
                    onDismiss = { showMenu = false }
                )
            }
        }
    }
}

@Composable
fun InfoPanelControlPopup(
    isVisible: Boolean,
    isWorkspaceLocked: Boolean,
    onToggleVisibility: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        GluonPopupMenuCard(
            enabled = !isWorkspaceLocked,
            onClick = {
                if (!isWorkspaceLocked) {
                    onToggleVisibility(!isVisible)
                    onDismiss()
                }
            }
        ) {
            Icon(
                if (isWorkspaceLocked) Icons.Default.Lock else if (isVisible) Icons.Default.Dashboard else Icons.Default.VisibilityOff,
                contentDescription = null,
                modifier = Modifier.size(28.dp).padding(end = 10.dp),
                tint = LocalContentColor.current
            )
            GluonMarqueeText(
                text = if (isVisible) "Скрыть панель" else "Показать панель",
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = !isVisible,
                enabled = !isWorkspaceLocked,
                onCheckedChange = null,
                modifier = Modifier.scale(0.8f)
            )
        }
    }
}