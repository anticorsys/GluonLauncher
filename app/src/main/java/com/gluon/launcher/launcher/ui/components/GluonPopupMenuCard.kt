// app/src/main/java/com/gluon/launcher/launcher/ui/components/GluonPopupMenuCard.kt
package com.gluon.launcher.launcher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.gluon.launcher.core.theme.LocalThemeSystemBars
import com.gluon.launcher.core.theme.M3EShapes

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GluonPopupMenuCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    height: Dp = 48.dp,
    horizontalPadding: Dp = 16.dp,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(M3EShapes.Large)

    Surface(
        modifier = modifier
            .height(height)
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        border = null,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowScope.GluonMarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    fontWeight: FontWeight = FontWeight.Bold,
    applyWeight: Boolean = true
) {
    Text(
        text = text,
        modifier = modifier.then(if (applyWeight) Modifier.weight(1f) else Modifier).basicMarquee(iterations = Int.MAX_VALUE),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodyLarge,
        color = color,
        fontWeight = fontWeight
    )
}

@Composable
fun GluonAnimatedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
    content: @Composable ColumnScope.() -> Unit
) {
    val density = LocalDensity.current
    val systemBarState = LocalThemeSystemBars.current

    // ИСПРАВЛЕНИЕ: MutableTransitionState синхронизирует жизненный цикл Popup с анимацией
    val transitionState = remember { MutableTransitionState(expanded) }
    transitionState.targetState = expanded

    DisposableEffect(transitionState.currentState, transitionState.targetState) {
        val isVisible = transitionState.currentState || transitionState.targetState
        if (isVisible) {
            systemBarState.openMenuCount.intValue++
        }
        onDispose {
            if (isVisible) {
                systemBarState.openMenuCount.intValue--
            }
        }
    }

    if (transitionState.currentState || transitionState.targetState) {
        Popup(
            alignment = Alignment.TopStart,
            offset = with(density) { IntOffset((offset.x - 32.dp).roundToPx(), (offset.y - 32.dp).roundToPx()) },
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(
                focusable = true,
                dismissOnClickOutside = true,
                clippingEnabled = true
            )
        ) {
            Box(modifier = Modifier.padding(32.dp)) {
                AnimatedVisibility(
                    visibleState = transitionState,
                    enter = fadeIn(animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)) +
                            scaleIn(
                                initialScale = 0.8f,
                                transformOrigin = TransformOrigin(0.5f, 0f),
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                            ),
                    exit = fadeOut(animationSpec = spring(dampingRatio = 0.9f, stiffness = 500f)) +
                            scaleOut(
                                targetScale = 0.8f,
                                transformOrigin = TransformOrigin(0.5f, 0f),
                                animationSpec = spring(dampingRatio = 0.9f, stiffness = 500f)
                            )
                ) {
                    Surface(
                        modifier = modifier,
                        shape = RoundedCornerShape(M3EShapes.ExtraLarge),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 8.dp,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            content = content
                        )
                    }
                }
            }
        }
    }
}