// app/src/main/java/com/gluon/launcher/launcher/ui/screens/SettingsComponents.kt
package com.gluon.launcher.launcher.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.gluon.launcher.core.theme.M3EShapes

@Composable
fun ModernExpandableSection(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    alwaysVisible: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "arrowRotation"
    )

    val animatedElevation by animateDpAsState(
        targetValue = if (expanded) 12.dp else 4.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
        label = "elevationAnim"
    )

    val iconContainerColor by animateColorAsState(
        if (expanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        label = "iconContainerColor"
    )
    val iconTintColor by animateColorAsState(
        if (expanded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
        label = "iconTintColor"
    )

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    shadowElevation = animatedElevation.toPx()
                    shape = RoundedCornerShape(M3EShapes.ExtraLarge)
                    clip = true
                    ambientShadowColor = Color.Black.copy(alpha = 0.3f)
                    spotShadowColor = Color.Black.copy(alpha = 0.4f)
                }
                .background(if (expanded) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(M3EShapes.ExtraLarge))
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle() }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(iconContainerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, modifier = Modifier.size(24.dp), tint = iconTintColor)
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        title.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Свернуть" else "Развернуть",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp).rotate(rotationAngle)
                    )
                }
                alwaysVisible?.let {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        it()
                        if (expanded) Spacer(Modifier.height(12.dp))
                    }
                }
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(spring(stiffness = 300f)) + fadeIn(),
                    exit = shrinkVertically(spring(stiffness = 300f)) + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(name: String, gluonId: String, avatarUrl: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrEmpty()) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            } else {
                Text(
                    name.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE).fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text("Gluon ID: $gluonId", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ThemeModeSelector(currentMode: Int, onModeSelected: (Int) -> Unit) {
    val options = listOf("Система", "Светлая", "Тёмная")
    val selectedIndex = currentMode.coerceIn(0, 2)
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = index == selectedIndex,
                    onClick = { onModeSelected(index) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size)
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun SettingsToggleRow(
    label: String,
    icon: ImageVector? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(Modifier.width(14.dp))
        }
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun VerificationBanner(onVerify: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
        shape = RoundedCornerShape(M3EShapes.ExtraLarge),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
        onClick = onVerify
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MarkEmailUnread, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Почта не подтверждена", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onErrorContainer)
                Text("Нажмите, чтобы подтвердить почту", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f))
            }
        }
    }
}

@Composable
fun SettingsActionRow(
    label: String,
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.onSurface,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(M3EShapes.ExtraLarge)).clickable(enabled = !isLoading, onClick = onClick),
        color = Color.Transparent
    ) {
        Row(modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(24.dp), color = color.copy(alpha = 0.85f), strokeWidth = 2.dp)
            } else {
                Icon(icon, null, modifier = Modifier.size(24.dp), tint = color.copy(alpha = 0.85f))
            }
            Spacer(Modifier.width(16.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GuestPlaceholder(onNavigateToAuth: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Войдите, чтобы синхронизировать данные экосистемы Gluon", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onNavigateToAuth, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(28.dp), elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)) {
            Text("ВОЙТИ", fontWeight = FontWeight.Bold)
        }
    }
}