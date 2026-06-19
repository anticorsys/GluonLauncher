// app/src/main/java/com/gluon/launcher/launcher/ui/screens/dashboard/OpenedWorkspaceFolderOverlay.kt
package com.gluon.launcher.launcher.ui.screens.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.gluon.launcher.core.data.AppModel
import com.gluon.launcher.core.data.WorkspaceFolderItem
import com.gluon.launcher.core.theme.M3EShapes
import com.gluon.launcher.core.utils.launchApp
import com.gluon.launcher.launcher.ui.components.AppIconItem
import com.gluon.launcher.launcher.ui.components.GluonMarqueeText
import com.gluon.launcher.launcher.ui.components.GluonPopupMenuCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OpenedWorkspaceFolderOverlay(
    openedFolder: WorkspaceFolderItem,
    isFolderDragActive: Boolean,
    allApps: List<AppModel>,
    showWorkspaceLabels: Boolean,
    isWorkspaceLocked: Boolean,
    onClose: () -> Unit,
    onRename: (String, String) -> Unit,
    onRemoveApp: (String, String) -> Unit,
    onDragStartApp: (AppModel, String) -> Unit
) {
    val context = LocalContext.current
    val scale = remember { Animatable(0.8f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { scale.animateTo(1f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)) }
        launch { alphaAnim.animateTo(1f, animationSpec = spring(dampingRatio = 0.9f, stiffness = 300f)) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (isFolderDragActive) 0f else (0.5f * alphaAnim.value)))
            .zIndex(100f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !isFolderDragActive
            ) { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(350.dp) // Жесткая ширина для вместимости 3х иконок без принудительного переноса
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    alpha = if (isFolderDragActive) 0f else alphaAnim.value
                }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            shape = RoundedCornerShape(M3EShapes.ExtraLarge),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var folderName by remember(openedFolder.name) { mutableStateOf(openedFolder.name) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = folderName,
                        onValueChange = { folderName = it; onRename(openedFolder.id, it) },
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    maxItemsInEachRow = if (openedFolder.packages.size == 4) 2 else 3
                ) {
                    openedFolder.packages.forEach { pkg ->
                        val app = allApps.find { it.packageName == pkg }
                        if (app != null) {
                            Box(modifier = Modifier.width(76.dp), contentAlignment = Alignment.TopCenter) {
                                AppIconItem(
                                    app = app,
                                    showLabel = showWorkspaceLabels,
                                    isInGrid = true,
                                    showContextMenu = true,
                                    onAppClick = { context.launchApp(app) },
                                    onDragStart = if (isWorkspaceLocked) null else {
                                        { appModel -> onDragStartApp(appModel, openedFolder.id) }
                                    },
                                    onContextMenu = { dismiss ->
                                        Column(modifier = Modifier.width(250.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            GluonPopupMenuCard(
                                                enabled = !isWorkspaceLocked,
                                                onClick = {
                                                    if (!isWorkspaceLocked) {
                                                        dismiss()
                                                        onRemoveApp(openedFolder.id, pkg)
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    if (isWorkspaceLocked) Icons.Default.Lock else Icons.Default.Close,
                                                    null, Modifier.size(28.dp).padding(end = 10.dp), tint = LocalContentColor.current
                                                )
                                                GluonMarqueeText("Удалить из папки")
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}