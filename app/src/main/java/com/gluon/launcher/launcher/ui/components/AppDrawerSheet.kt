package com.gluon.launcher.launcher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gluon.launcher.core.data.AppModel
import com.gluon.launcher.core.data.DrawerFolderItem
import com.gluon.launcher.core.search.SearchResultItem
import com.gluon.launcher.core.theme.M3EShapes
import com.gluon.launcher.core.utils.AppCategorizer
import com.gluon.launcher.core.utils.matchesFuzzyQuery
import com.gluon.launcher.core.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val DEFAULT_CATEGORIES = AppCategorizer.DEFAULT_CATEGORIES

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun AppDrawerSheet(
    allApps: List<AppModel>,
    drawerFolders: List<DrawerFolderItem>,
    onCreateDrawerFolder: (String, List<String>) -> Unit,
    onDeleteDrawerFolder: (String) -> Unit,
    onRenameDrawerFolder: (String, String) -> Unit,
    onRemoveFromDrawerFolder: (String, String) -> Unit,
    onUpdateDrawerFolder: (String, List<String>) -> Unit,
    gridColumns: Int,
    showLabels: Boolean,
    showIconBorder: Boolean,
    onAppClick: (AppModel) -> Unit,
    onHideApp: (String) -> Unit,
    onToggleDockApp: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dockApps: List<String> = emptyList(),
    searchQuery: String = "",
    searchResults: List<SearchResultItem> = emptyList(),
    onSearchQueryChange: (String) -> Unit = {},
    onDragStart: (AppModel) -> Unit = {},
    maxDockApps: Int = 5,
    sortMode: Int = 0,
    onCycleSortMode: () -> Unit = {},
    customAppCategories: Map<String, String> = emptyMap(),
    onSetAppCategory: (String, String) -> Unit = { _, _ -> },
    isWorkspaceLocked: Boolean = false,
    isDockBarHidden: Boolean = false
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val searchHeight = 60.dp
    val contentPadding = 24.dp
    val topInternalPadding = 70.dp
    val searchBottomPadding = 32.dp

    val navBarBottom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val listBottomPadding = contentPadding + navBarBottom + 16.dp

    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    val cardShape = RoundedCornerShape(topStart = M3EShapes.ExtraExtraLarge, topEnd = M3EShapes.ExtraExtraLarge)

    var appToCategorize by remember { mutableStateOf<AppModel?>(null) }
    var openedDrawerFolderId by remember { mutableStateOf<String?>(null) }

    val openedDrawerFolder = remember(openedDrawerFolderId, drawerFolders) {
        drawerFolders.find { it.id == openedDrawerFolderId }
    }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedApps by remember { mutableStateOf(setOf<AppModel>()) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderForAdd by remember { mutableStateOf<DrawerFolderItem?>(null) }
    var sortedApps by remember { mutableStateOf<List<AppModel>>(emptyList()) }
    var aiCategorizedApps by remember { mutableStateOf<List<Pair<String, List<AppModel>>>>(emptyList()) }

    BackHandler(enabled = isSelectionMode || openedDrawerFolder != null) {
        if (showCreateFolderDialog) {
            showCreateFolderDialog = false
        } else if (isSelectionMode) {
            isSelectionMode = false
            selectedApps = emptySet()
            folderForAdd = null
        } else if (openedDrawerFolder != null) {
            openedDrawerFolderId = null
        }
    }

    LaunchedEffect(allApps, searchQuery, sortMode, customAppCategories, drawerFolders) {
        withContext(Dispatchers.Default) {
            val appsInFolders = drawerFolders.flatMap { it.packages }.toSet()
            val displayApps = if (searchQuery.isBlank()) {
                allApps.filter { it.packageName !in appsInFolders }
            } else {
                allApps.filter { matchesFuzzyQuery(it.label, searchQuery) }
            }

            val comparator = Comparator<AppModel> { a, b ->
                val aFirst = a.label.firstOrNull() ?: ' '
                val bFirst = b.label.firstOrNull() ?: ' '
                val aCyrillic = aFirst in 'а'..'я' || aFirst in 'А'..'Я' || aFirst == 'ё' || aFirst == 'Ё'
                val bCyrillic = bFirst in 'а'..'я' || bFirst in 'А'..'Я' || bFirst == 'ё' || bFirst == 'Ё'

                if (aCyrillic && !bCyrillic) -1
                else if (!aCyrillic && bCyrillic) 1
                else a.label.lowercase().compareTo(b.label.lowercase())
            }

            sortedApps = when (sortMode) {
                1 -> displayApps.sortedByDescending { it.usageCount }
                else -> displayApps.sortedWith(comparator)
            }

            if (sortMode == 2) {
                aiCategorizedApps = AppCategorizer.categorizeApps(displayApps, customAppCategories)
            }
        }
    }

    val groupedSearchResults = remember(searchResults) {
        searchResults.groupBy {
            when (it) {
                is SearchResultItem.AppItem -> "Приложения"
                is SearchResultItem.ContactItem -> "Контакты"
                is SearchResultItem.FileItem -> "Файлы"
                is SearchResultItem.WebSuggestionItem -> "Поиск в интернете"
            }
        }
    }

    val nestedScrollConnection = remember(sortMode, searchQuery) {
        object : NestedScrollConnection {
            var overscrollAccumulator = 0f
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val isAtTop = if (searchQuery.isNotEmpty()) {
                    listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                } else {
                    gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
                }

                if (isAtTop && available.y > 0 && source == NestedScrollSource.UserInput) {
                    overscrollAccumulator += available.y
                    val threshold = if (searchQuery.isNotEmpty()) 150f else 80f

                    if (overscrollAccumulator > threshold) {
                        keyboardController?.hide()
                        onDismiss()
                        overscrollAccumulator = 0f
                    }
                    return Offset(0f, available.y)
                } else if (available.y < 0) {
                    overscrollAccumulator = 0f
                }
                return Offset.Zero
            }
        }
    }

    val onToggleSelection = { appModel: AppModel ->
        if (selectedApps.contains(appModel)) {
            selectedApps = selectedApps - appModel
            if (selectedApps.isEmpty() && folderForAdd == null) isSelectionMode = false
        } else {
            if (selectedApps.size < 9) {
                selectedApps = selectedApps + appModel
            } else {
                context.toast("Максимум 9 приложений в папке")
            }
        }
    }

    val onEnterSelectionMode = { appModel: AppModel ->
        isSelectionMode = true
        selectedApps = setOf(appModel)
    }

    Box(modifier = modifier.fillMaxSize().nestedScroll(nestedScrollConnection)) {
        val iconSize = if (gridColumns >= 5) 54.dp else 60.dp

        Box(modifier = Modifier.fillMaxSize().clip(cardShape)) {
            val backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh
            val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            val innerCardBackground = Color.Transparent

            Surface(
                modifier = Modifier.fillMaxSize(), shape = cardShape, color = backgroundColor,
                border = BorderStroke(0.5.dp, borderColor), tonalElevation = 1.dp, shadowElevation = 6.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (searchQuery.isNotEmpty()) {
                        DrawerSearchResults(
                            groupedSearchResults = groupedSearchResults,
                            listState = listState,
                            searchHeight = searchHeight,
                            topInternalPadding = topInternalPadding,
                            searchBottomPadding = searchBottomPadding,
                            contentPadding = contentPadding,
                            listBottomPadding = listBottomPadding,
                            innerCardBackground = innerCardBackground,
                            onAppClick = onAppClick
                        )
                    } else {
                        DrawerAppGrid(
                            gridState = gridState,
                            gridColumns = gridColumns,
                            searchHeight = searchHeight,
                            topInternalPadding = topInternalPadding,
                            searchBottomPadding = searchBottomPadding,
                            contentPadding = contentPadding,
                            listBottomPadding = listBottomPadding,
                            sortMode = sortMode,
                            drawerFolders = drawerFolders,
                            aiCategorizedApps = aiCategorizedApps,
                            sortedApps = sortedApps,
                            allApps = allApps,
                            showLabels = showLabels,
                            iconSize = iconSize,
                            isSelectionMode = isSelectionMode,
                            isWorkspaceLocked = isWorkspaceLocked,
                            folderForAdd = folderForAdd,
                            onOpenedFolderChange = { openedDrawerFolderId = it?.id },
                            onEnterSelectionModeForFolder = { f ->
                                folderForAdd = f
                                isSelectionMode = true
                                selectedApps = f.packages.mapNotNull { pkg -> allApps.find { it.packageName == pkg } }.toSet()
                                openedDrawerFolderId = null
                            },
                            onDeleteFolder = { onDeleteDrawerFolder(it) },
                            selectedApps = selectedApps,
                            showIconBorder = showIconBorder,
                            isDockBarHidden = isDockBarHidden,
                            dockApps = dockApps,
                            maxDockApps = maxDockApps,
                            onToggleDockApp = onToggleDockApp,
                            onCategorize = { appToCategorize = it },
                            onHideApp = onHideApp,
                            context = context,
                            onAppClick = onAppClick,
                            onDragStart = onDragStart,
                            onToggleSelection = onToggleSelection,
                            onEnterSelectionMode = onEnterSelectionMode
                        )
                    }

                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = contentPadding).padding(top = topInternalPadding, bottom = searchBottomPadding).align(Alignment.TopCenter)) {
                        Row(modifier = Modifier.fillMaxWidth().height(searchHeight), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(M3EShapes.ExtraLarge), color = MaterialTheme.colorScheme.surfaceVariant, shadowElevation = 4.dp
                            ) {
                                if (isSelectionMode) {
                                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        TextButton(onClick = { isSelectionMode = false; selectedApps = emptySet(); folderForAdd = null }) { Text("Отмена", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        Text("Выбрано: ${selectedApps.size}/9", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        TextButton(
                                            onClick = {
                                                if (folderForAdd != null) {
                                                    onUpdateDrawerFolder(folderForAdd!!.id, selectedApps.map { it.packageName })
                                                    folderForAdd = null
                                                    isSelectionMode = false
                                                    selectedApps = emptySet()
                                                } else {
                                                    showCreateFolderDialog = true
                                                }
                                            },
                                            enabled = selectedApps.isNotEmpty()
                                        ) { Text("Готово", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                                    }
                                } else {
                                    Row(modifier = Modifier.padding(horizontal = 16.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                                        Spacer(Modifier.width(12.dp))
                                        BasicTextField(
                                            value = searchQuery,
                                            onValueChange = onSearchQueryChange,
                                            modifier = Modifier.weight(1f),
                                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, textDecoration = androidx.compose.ui.text.style.TextDecoration.None),
                                            singleLine = true,
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            decorationBox = { innerTextField ->
                                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                                                    if (searchQuery.isEmpty()) Text("Поиск...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                                    innerTextField()
                                                }
                                            }
                                        )
                                        if (searchQuery.isNotEmpty()) {
                                            Spacer(Modifier.width(4.dp))
                                            IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, "Очистить", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) }
                                        }
                                        Spacer(Modifier.width(4.dp))
                                        IconButton(onClick = onCycleSortMode, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Смена сортировки", tint = if (sortMode != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
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

    if (appToCategorize != null) {
        ChangeCategoryDialog(
            app = appToCategorize!!,
            currentCategory = customAppCategories[appToCategorize!!.packageName],
            onDismiss = { appToCategorize = null },
            onSave = { category ->
                onSetAppCategory(appToCategorize!!.packageName, category)
                appToCategorize = null
            }
        )
    }

    if (showCreateFolderDialog) {
        var newFolderName by remember { mutableStateOf("Новая папка") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Создать папку", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Имя папки") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFolderName.isNotBlank() && selectedApps.isNotEmpty()) {
                        onCreateDrawerFolder(newFolderName.trim(), selectedApps.map { it.packageName })
                        showCreateFolderDialog = false
                        isSelectionMode = false
                        selectedApps = emptySet()
                    }
                }) { Text("Готово") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (openedDrawerFolder != null) {
        OpenedDrawerFolderOverlay(
            openedDrawerFolder = openedDrawerFolder,
            allApps = allApps,
            showLabels = showLabels,
            onClose = { openedDrawerFolderId = null },
            onRename = onRenameDrawerFolder,
            onRemoveApp = onRemoveFromDrawerFolder,
            onDragStartApp = { appModel ->
                onDragStart(appModel)
                openedDrawerFolderId = null
            }
        )
    }
}