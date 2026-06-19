// app/src/main/java/com/gluon/launcher/launcher/ui/components/DrawerSearchResults.kt
package com.gluon.launcher.launcher.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.gluon.launcher.core.data.AppModel
import com.gluon.launcher.core.search.SearchResultItem

@Composable
fun DrawerSearchResults(
    groupedSearchResults: Map<String, List<SearchResultItem>>,
    listState: LazyListState,
    searchHeight: Dp,
    topInternalPadding: Dp,
    searchBottomPadding: Dp,
    contentPadding: Dp,
    listBottomPadding: Dp,
    innerCardBackground: Color,
    onAppClick: (AppModel) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = searchHeight + topInternalPadding + searchBottomPadding)
            .padding(horizontal = contentPadding),
        contentPadding = PaddingValues(bottom = listBottomPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        groupedSearchResults.forEach { (category, results) ->
            item(key = "header_$category", contentType = "header") {
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp, top = 8.dp, start = 4.dp)
                )
            }
            if (category == "Приложения") {
                val appItems = results.filterIsInstance<SearchResultItem.AppItem>()
                val chunkedApps = appItems.chunked(2)
                items(chunkedApps, contentType = { "app_row" }) { rowApps ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowApps.forEach { result ->
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(innerCardBackground)
                                    .clickable { onAppClick(result.app) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIconItem(
                                    app = result.app,
                                    showLabel = false,
                                    iconSizeOverride = 48.dp,
                                    showContextMenu = false
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    result.app.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                        if (rowApps.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            } else {
                items(results, key = { it.hashCode() }, contentType = { "search_result" }) { result ->
                    Box(modifier = Modifier) {
                        when (result) {
                            is SearchResultItem.ContactItem -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(innerCardBackground)
                                        .clickable {
                                            val intent = Intent(Intent.ACTION_DIAL, "tel:${result.phoneNumber}".toUri())
                                            context.startActivity(intent)
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            result.name.take(1).uppercase(),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(result.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                        Text(result.phoneNumber, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            is SearchResultItem.WebSuggestionItem -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(innerCardBackground)
                                        .clickable {
                                            val intent = Intent(Intent.ACTION_VIEW, result.url.toUri())
                                            context.startActivity(intent)
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Text(result.query, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            is SearchResultItem.FileItem -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(innerCardBackground)
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Dashboard, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(result.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                        Text(result.mimeType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}