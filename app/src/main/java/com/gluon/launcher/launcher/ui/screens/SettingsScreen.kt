// app/src/main/java/com/gluon/launcher/launcher/ui/screens/SettingsScreen.kt
package com.gluon.launcher.launcher.ui.screens

import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gluon.launcher.MainActivity
import com.gluon.launcher.core.auth.AuthManager
import com.gluon.launcher.core.data.AppModel
import com.gluon.launcher.core.theme.M3EShapes
import com.gluon.launcher.core.update.UpdateInfo
import com.gluon.launcher.launcher.ui.components.GluonPageTitle
import com.gluon.launcher.launcher.ui.components.GluonScaffold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SettingsScreen(
    authManager: AuthManager, onBack: () -> Unit, onLogout: () -> Unit,
    onEditProfile: () -> Unit, onDeleteAccount: () -> Unit,
    onNavigateToAuth: () -> Unit, onVerifyEmail: () -> Unit, isProfileGlass: Boolean, showProfileAvatar: Boolean,
    onToggleProfileGlass: (Boolean) -> Unit, onToggleShowAvatar: (Boolean) -> Unit, themeMode: Int, isDynamicStyle: Boolean, onThemeModeChange: (Int) -> Unit,
    onDynamicStyleToggle: (Boolean) -> Unit, gridColumns: Int, onGridColumnsChange: (Int) -> Unit, showDockLabels: Boolean = false, onShowDockLabelsChange: (Boolean) -> Unit = {}, showLabels: Boolean = true,
    onShowLabelsChange: (Boolean) -> Unit = {}, showIconBorder: Boolean = false, onShowIconBorderChange: (Boolean) -> Unit = {},
    hiddenApps: Set<String> = emptySet(), hiddenAppModels: Map<String, AppModel> = emptyMap(),
    onUnhideApp: (String) -> Unit = {},
    isDockBarHidden: Boolean = false, onDockBarHiddenToggle: (Boolean) -> Unit = {},
    isPredictiveDockEnabled: Boolean = false, onPredictiveDockToggle: (Boolean) -> Unit = {},
    isWorkspaceLocked: Boolean = false, onWorkspaceLockedToggle: (Boolean) -> Unit = {},
    showWorkspaceLabels: Boolean = true, onShowWorkspaceLabelsChange: (Boolean) -> Unit = {},
    isDynamicDockBar: Boolean = false, onDynamicDockBarToggle: (Boolean) -> Unit = {},
    updateInfo: UpdateInfo?, isCheckingUpdate: Boolean,
    onCheckUpdates: ((Boolean) -> Unit) -> Unit, onDownloadUpdate: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user by authManager.currentUser.collectAsStateWithLifecycle()
    val isGuest by authManager.isGuest.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeveloperPopup by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var pendingGridColumns by remember { mutableIntStateOf(gridColumns) }

    LaunchedEffect(pendingGridColumns) { if (pendingGridColumns != gridColumns) { delay(300.milliseconds); onGridColumnsChange(pendingGridColumns) } }
    LaunchedEffect(showDeveloperPopup) { if (showDeveloperPopup) { delay(2000.milliseconds); showDeveloperPopup = false } }

    var profileExpanded by remember { mutableStateOf(false) }
    var appearanceExpanded by remember { mutableStateOf(false) }
    var infoPanelExpanded by remember { mutableStateOf(false) }
    var workspaceExpanded by remember { mutableStateOf(false) }
    var dockExpanded by remember { mutableStateOf(false) }
    var drawerExpanded by remember { mutableStateOf(false) }
    var hiddenExpanded by remember { mutableStateOf(false) }
    var additionalExpanded by remember { mutableStateOf(false) }

    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    BackHandler { onBack() }

    GluonScaffold(
        isRoot = true,
        onNavigate = onBack
    ) { modifier ->
        Box(modifier = modifier.fillMaxSize()) {
            val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = 600.dp),
                contentPadding = PaddingValues(start = 30.dp, end = 30.dp, top = 90.dp, bottom = systemBarsPadding.calculateBottomPadding() + 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "Title") {
                    GluonPageTitle("Настройки")
                }

                item(key = "ProfileSection") {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ModernExpandableSection("Профиль", Icons.Default.AccountCircle, profileExpanded, { profileExpanded = !profileExpanded }, alwaysVisible = {
                            if (isGuest || user == null) GuestPlaceholder(onNavigateToAuth) else { ProfileHeader(user?.fullName ?: "Пользователь", user?.gluonId ?: "—", user?.getAvatarUrl()); if (user?.verified == false) VerificationBanner(onVerifyEmail) } }) {
                            if (!isGuest && user != null) { SettingsActionRow("Редактировать профиль", Icons.Default.Edit, onClick = onEditProfile); SettingsActionRow("Выйти из аккаунта", Icons.AutoMirrored.Filled.ExitToApp, MaterialTheme.colorScheme.primary, onClick = { showLogoutDialog = true }); SettingsActionRow("Удалить аккаунт", Icons.Default.DeleteForever, MaterialTheme.colorScheme.error, onClick = { showDeleteDialog = true }) }
                        }
                    }
                }

                item(key = "AppearanceSection") {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ModernExpandableSection("Оформление", Icons.Default.Palette, appearanceExpanded, { appearanceExpanded = !appearanceExpanded }) {
                            SettingsToggleRow("Динамическое\nоформление", Icons.Default.AutoAwesome, checked = isDynamicStyle && dynamicAvailable, enabled = dynamicAvailable, onCheckedChange = onDynamicStyleToggle)
                            ThemeModeSelector(themeMode, onThemeModeChange)
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                                Icon(Icons.Default.GridOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(16.dp))
                                Text("Сетка в системе", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            }
                            val options = listOf("4", "5")
                            val selectedIndex = pendingGridColumns - 4
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                options.forEachIndexed { index, label ->
                                    SegmentedButton(selected = index == selectedIndex, onClick = { pendingGridColumns = index + 4 }, shape = SegmentedButtonDefaults.itemShape(index, options.size)) {
                                        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "InfoPanelSection") {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ModernExpandableSection("Информационная панель", Icons.Default.Dashboard, infoPanelExpanded, { infoPanelExpanded = !infoPanelExpanded }) {
                            SettingsToggleRow("Показывать информационную панель", Icons.Default.Dashboard, isProfileGlass, onCheckedChange = onToggleProfileGlass)
                            SettingsToggleRow("Показывать профиль", Icons.Default.AccountCircle, showProfileAvatar, isProfileGlass, onToggleShowAvatar)
                        }
                    }
                }

                item(key = "WorkspaceSection") {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ModernExpandableSection("Рабочий стол", Icons.Default.Layers, workspaceExpanded, { workspaceExpanded = !workspaceExpanded }) {
                            SettingsToggleRow("Заблокировать рабочий стол", Icons.Default.Lock, isWorkspaceLocked, onCheckedChange = onWorkspaceLockedToggle)
                            Spacer(Modifier.height(8.dp))
                            SettingsToggleRow("Подписи приложений", Icons.Default.TextFields, showWorkspaceLabels, onCheckedChange = onShowWorkspaceLabelsChange)
                        }
                    }
                }

                item(key = "DockSection") {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ModernExpandableSection("Док-бар", Icons.AutoMirrored.Filled.List, dockExpanded, { dockExpanded = !dockExpanded }) {
                            SettingsToggleRow("Показывать док-бар", Icons.AutoMirrored.Filled.List, !isDockBarHidden, onCheckedChange = { onDockBarHiddenToggle(!it) })
                            SettingsToggleRow("Динамический док-бар", Icons.Default.AutoAwesome, isDynamicDockBar, enabled = !isDockBarHidden, onCheckedChange = onDynamicDockBarToggle)
                            SettingsToggleRow("Умный док-бар", Icons.Default.AutoAwesome, isPredictiveDockEnabled, enabled = !isDockBarHidden, onCheckedChange = onPredictiveDockToggle)
                            SettingsToggleRow("Подписи приложений", Icons.Default.TextFields, showDockLabels, !isDockBarHidden, onShowDockLabelsChange)
                        }
                    }
                }

                item(key = "DrawerSection") {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ModernExpandableSection("Меню приложений", Icons.Default.Apps, drawerExpanded, { drawerExpanded = !drawerExpanded }) {
                            SettingsToggleRow("Подписи приложений", Icons.Default.TextFields, showLabels, onCheckedChange = onShowLabelsChange)
                            Spacer(Modifier.height(8.dp))
                            SettingsToggleRow("Контур иконок", Icons.Default.RadioButtonUnchecked, showIconBorder, onCheckedChange = onShowIconBorderChange)
                        }
                    }
                }

                if (hiddenApps.isNotEmpty()) {
                    item(key = "HiddenAppsSection") {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            ModernExpandableSection("Скрытые приложения", Icons.Default.VisibilityOff, hiddenExpanded, { hiddenExpanded = !hiddenExpanded }) {
                                hiddenApps.forEach { pkg ->
                                    val model = hiddenAppModels[pkg]
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                            if (model?.iconBitmap != null) Image(bitmap = model.iconBitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                            else Text(model?.label?.take(1)?.uppercase() ?: "?", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Text(model?.label ?: pkg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.Visibility, "Показать", Modifier.size(24.dp).clickable { onUnhideApp(pkg) }, MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "AdditionalSection") {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ModernExpandableSection("Дополнительно", Icons.Default.SettingsApplications, additionalExpanded, { additionalExpanded = !additionalExpanded }) {

                            SettingsActionRow(
                                label = if (isCheckingUpdate) "Поиск обновлений..." else "Проверить обновления",
                                icon = Icons.Default.CloudDownload,
                                color = MaterialTheme.colorScheme.primary,
                                isLoading = isCheckingUpdate
                            ) {
                                onCheckUpdates { found ->
                                    if (found) showUpdateDialog = true
                                    else scope.launch { snackbarHostState.showSnackbar("У вас актуальная версия!") }
                                }
                            }

                            SettingsActionRow("Перезапустить лаунчер", Icons.Default.Refresh, MaterialTheme.colorScheme.error) {
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                                Runtime.getRuntime().exit(0)
                            }
                        }
                    }
                }

                item(key = "FooterSection") {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp).combinedClickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}, onLongClick = { showDeveloperPopup = true })) {
                            Text("Gluon Launcher", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(4.dp))

                            val pInfo = try { context.packageManager.getPackageInfo(context.packageName, 0) } catch (_: Exception) { null }
                            val version = pInfo?.versionName ?: "beta 1.0.3"
                            Text("Версия $version", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter)) { data ->
                androidx.compose.material3.Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(28.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    actionColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }
            AnimatedVisibility(visible = showDeveloperPopup, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp)) { Surface(modifier = Modifier.clip(RoundedCornerShape(M3EShapes.Large)), color = MaterialTheme.colorScheme.primaryContainer, shadowElevation = 8.dp) { Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)); Spacer(Modifier.width(12.dp)); Text("Разработчик REIMS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer) } } }
            if (showLogoutDialog) AlertDialog(onDismissRequest = { showLogoutDialog = false }, icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = MaterialTheme.colorScheme.primary) }, title = { Text("Выход из аккаунта", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }, text = { Text("Вы уверены, что хотите выйти?", color = MaterialTheme.colorScheme.onSurfaceVariant) }, confirmButton = { Button(onClick = { showLogoutDialog = false; onLogout() }, shape = RoundedCornerShape(12.dp)) { Text("ВЫЙТИ", fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("ОТМЕНА") } }, shape = RoundedCornerShape(M3EShapes.ExtraLarge))
            if (showDeleteDialog) AlertDialog(onDismissRequest = { showDeleteDialog = false }, icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) }, title = { Text("Удаление аккаунта", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }, text = { Text("Это действие необратимо.\nВсе данные GluonCore будут удалены.", color = MaterialTheme.colorScheme.onSurfaceVariant) }, confirmButton = { Button(onClick = { showDeleteDialog = false; onDeleteAccount() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), shape = RoundedCornerShape(12.dp)) { Text("УДАЛИТЬ", fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("ОТМЕНА") } }, shape = RoundedCornerShape(M3EShapes.ExtraLarge))

            if (showUpdateDialog && updateInfo != null) {
                AlertDialog(
                    onDismissRequest = { showUpdateDialog = false },
                    title = { Text("Доступно обновление", fontWeight = FontWeight.Bold) },
                    text = { Text("Версия: ${updateInfo.versionName}\nЧто нового: ${updateInfo.changelog}", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    confirmButton = {
                        Button(onClick = { showUpdateDialog = false; onDownloadUpdate() }, shape = RoundedCornerShape(12.dp)) {
                            Text("СКАЧАТЬ И ОБНОВИТЬ", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUpdateDialog = false }) { Text("ПОЗЖЕ") }
                    },
                    shape = RoundedCornerShape(M3EShapes.ExtraLarge)
                )
            }
        }
    }
}