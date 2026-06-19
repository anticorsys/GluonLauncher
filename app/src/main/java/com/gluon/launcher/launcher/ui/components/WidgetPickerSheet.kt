// app/src/main/java/com/gluon/launcher/launcher/ui/components/WidgetPickerSheet.kt
package com.gluon.launcher.launcher.ui.components

import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gluon.launcher.core.widget.WidgetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WidgetPickerSheet(
    onlyGoogleSearch: Boolean = false,
    onWidgetSelected: (providerInfo: AppWidgetProviderInfo) -> Unit,
    @Suppress("UNUSED_PARAMETER") onWidgetDragStart: (appWidgetId: Int, providerInfo: AppWidgetProviderInfo, offset: androidx.compose.ui.geometry.Offset) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var groupedProviders by remember { mutableStateOf<Map<String, List<AppWidgetProviderInfo>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    BackHandler(enabled = true) { onDismiss() }

    LaunchedEffect(Unit) {
        // ИСПРАВЛЕНИЕ: Ждем 300мс, чтобы анимация появления шторки успела плавно завершиться до начала парсинга
        delay(300.milliseconds)
        withContext(Dispatchers.IO) {
            val all = WidgetManager.getGroupedWidgetProviders(context)
            val filtered = if (onlyGoogleSearch) {
                all.mapNotNull { (appName, providers) ->
                    val googleProviders = providers.filter { provider ->
                        provider.provider.packageName == "com.google.android.googlequicksearchbox"
                    }
                    if (googleProviders.isNotEmpty()) appName to googleProviders else null
                }.toMap()
            } else { all }

            withContext(Dispatchers.Main) {
                groupedProviders = filtered
                isLoading = false
            }
        }
    }

    var expandedGroups by remember { mutableStateOf<Set<String>>(emptySet()) }
    val listState = rememberLazyListState()

    GluonScaffold(
        isRoot = true,
        onNavigate = onDismiss
    ) { modifier ->
        Box(modifier = modifier.fillMaxSize()) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Загрузка виджетов...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            } else {
                val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 30.dp, end = 30.dp, top = 90.dp, bottom = systemBarsPadding.calculateBottomPadding() + 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "Title") {
                        GluonPageTitle(if (onlyGoogleSearch) "Виджет Google Поиск" else "Виджеты")
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    groupedProviders.forEach { (appName, providers) ->
                        item(key = appName) {
                            val isExpanded = expandedGroups.contains(appName)

                            var iconDrawable by remember(providers) { mutableStateOf<android.graphics.drawable.Drawable?>(null) }

                            LaunchedEffect(providers) {
                                withContext(Dispatchers.IO) {
                                    val icon = try { context.packageManager.getApplicationIcon(providers.first().provider.packageName) } catch(_: Exception) { null }
                                    withContext(Dispatchers.Main) {
                                        iconDrawable = icon
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shadowElevation = 0.dp,
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { expandedGroups = if (isExpanded) expandedGroups.minus(appName) else expandedGroups.plus(appName) }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (iconDrawable != null) {
                                            AsyncImage(model = iconDrawable, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape))
                                        } else {
                                            Box(
                                                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                                                contentAlignment = Alignment.Center
                                            ) { Text(text = appName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(text = appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                                        Icon(imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
                                    }

                                    AnimatedVisibility(
                                        visible = isExpanded,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).padding(bottom = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            maxItemsInEachRow = 2
                                        ) {
                                            providers.forEach { provider ->
                                                Box(modifier = Modifier.weight(1f)) {
                                                    WidgetPickerItem(providerInfo = provider, fallbackAppIcon = iconDrawable, onSelect = { onWidgetSelected(provider) })
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetPickerItem(
    providerInfo: AppWidgetProviderInfo,
    fallbackAppIcon: android.graphics.drawable.Drawable?,
    onSelect: () -> Unit
) {
    val context = LocalContext.current
    val label = remember(providerInfo) { providerInfo.loadLabel(context.packageManager) ?: "Виджет" }

    var previewDrawable by remember(providerInfo) { mutableStateOf<android.graphics.drawable.Drawable?>(null) }

    LaunchedEffect(providerInfo) {
        withContext(Dispatchers.IO) {
            val res = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) providerInfo.previewImage else 0
            if (res != 0) {
                val drawable = try { context.packageManager.getDrawable(providerInfo.provider.packageName, res, null) } catch(_: Exception) { null }
                withContext(Dispatchers.Main) {
                    previewDrawable = drawable
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 0.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (previewDrawable != null) {
                    AsyncImage(model = previewDrawable, contentDescription = label, modifier = Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit)
                } else if (fallbackAppIcon != null) {
                    AsyncImage(model = fallbackAppIcon, contentDescription = label, modifier = Modifier.size(48.dp).clip(CircleShape), contentScale = ContentScale.Fit)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}