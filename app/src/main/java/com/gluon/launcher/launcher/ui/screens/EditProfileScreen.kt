// app/src/main/java/com/gluon/launcher/launcher/ui/screens/EditProfileScreen.kt
package com.gluon.launcher.launcher.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockPerson
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.gluon.launcher.core.auth.AuthManager
import com.gluon.launcher.core.auth.ProfileManager
import com.gluon.launcher.core.utils.PermissionHelper
import com.gluon.launcher.core.utils.toast
import com.gluon.launcher.launcher.ui.components.GluonPageTitle
import com.gluon.launcher.launcher.ui.components.GluonScaffold
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    authManager: AuthManager,
    profileManager: ProfileManager,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    val user by profileManager.currentUser.collectAsStateWithLifecycle()

    val gluonIdStatic = remember(user) { user?.gluonId ?: "" }
    val emailStatic = remember(user) { user?.email ?: "" }

    var fullName by remember(user) { mutableStateOf(user?.fullName ?: "") }
    var bio by remember(user) { mutableStateOf(user?.bio ?: "") }
    var selectedGender by remember(user) { mutableStateOf(user?.gender ?: "Male") }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var avatarMarkedForDeletion by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var showPasswordSheet by remember { mutableStateOf(false) }

    LaunchedEffect(user?.avatar) {
        if (!user?.avatar.isNullOrEmpty()) avatarMarkedForDeletion = false
    }

    val serverAvatarUrl = remember(user, avatarMarkedForDeletion) {
        if (!avatarMarkedForDeletion) user?.getAvatarUrl() else null
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            avatarMarkedForDeletion = false
        }
    }

    val requestPermissionAndPick = PermissionHelper.rememberImagePickerPermissionLauncher(
        onPermissionGranted = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onPermissionDenied = { scope.launch { snackbarHostState.showSnackbar("Нет разрешения на чтение изображений") } }
    )

    val launchPicker = { if (!isLoading) { focusManager.clearFocus(); requestPermissionAndPick() } }

    GluonScaffold(
        isRoot = false,
        onNavigate = { focusManager.clearFocus(); onBack() }
    ) { modifier ->
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 30.dp)
                    .padding(top = 25.dp)
            ) {
                GluonPageTitle("Редактирование")
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier.size(130.dp).align(Alignment.CenterHorizontally).padding(bottom = 8.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize().clip(CircleShape).clickable(enabled = !isLoading) { launchPicker() },
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        when {
                            selectedImageUri != null -> AsyncImage(model = selectedImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            !serverAvatarUrl.isNullOrEmpty() -> AsyncImage(model = serverAvatarUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else -> Box(contentAlignment = Alignment.Center) { Text(text = fullName.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black) }
                        }
                    }

                    val hasPhotoOnServer = !user?.avatar.isNullOrEmpty() && !avatarMarkedForDeletion
                    val hasSelectedNew = selectedImageUri != null
                    val canDelete = hasPhotoOnServer || hasSelectedNew

                    IconButton(
                        onClick = {
                            if (!isLoading) {
                                when {
                                    hasSelectedNew -> selectedImageUri = null
                                    hasPhotoOnServer -> avatarMarkedForDeletion = !avatarMarkedForDeletion
                                    else -> launchPicker()
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp).background(if (canDelete) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    ) { Icon(imageVector = if (canDelete) Icons.Default.Delete else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (canDelete) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer) }
                }

                Spacer(Modifier.height(12.dp))

                EditSettingsGroup(title = "Аккаунт GluonCore", icon = Icons.Default.LockPerson) {
                    ReadOnlyField(label = "Gluon ID", value = gluonIdStatic, icon = Icons.Default.AlternateEmail)
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ReadOnlyField(label = "Электронная почта", value = emailStatic, icon = Icons.Default.Email)
                }

                EditSettingsGroup(title = "Личные данные", icon = Icons.Default.Edit) {
                    CompactTextField(value = fullName, onValueChange = { if (it.length <= 40) fullName = it }, label = "Ваше имя", icon = Icons.Default.Person, enabled = !isLoading)
                    CompactTextField(value = bio, onValueChange = { if (it.length <= 150) bio = it }, label = "О себе", icon = Icons.Default.Info, singleLine = false, supportingText = "${bio.length}/150", enabled = !isLoading)
                    GenderSelector(selectedGender = selectedGender, onGenderSelected = { if (!isLoading) selectedGender = it })
                }

                Spacer(Modifier.height(12.dp))

                FilledTonalButton(
                    onClick = { focusManager.clearFocus(); showPasswordSheet = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(28.dp),
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Security, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Изменить пароль", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        isLoading = true
                        scope.launch {
                            try {
                                if (avatarMarkedForDeletion) {
                                    profileManager.deleteAvatar(context).onFailure { e -> context.toast("Не удалось удалить старый аватар: ${e.message}") }
                                }
                                val result = profileManager.updateProfile(context, fullName.trim(), gluonIdStatic.trim(), selectedGender, bio.trim(), if (avatarMarkedForDeletion) null else selectedImageUri)
                                if (result.isSuccess) {
                                    context.toast("Профиль успешно обновлён")
                                    onSuccess()
                                } else {
                                    context.toast("Ошибка: ${result.exceptionOrNull()?.message}")
                                }
                            } catch (_: Exception) { context.toast("Неизвестная ошибка") } finally { isLoading = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    enabled = !isLoading && fullName.trim().isNotBlank(),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("СОХРАНИТЬ ИЗМЕНЕНИЯ", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
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

            if (showPasswordSheet) {
                ModalBottomSheet(
                    onDismissRequest = { if (!isLoading) showPasswordSheet = false },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    ChangePasswordSheet(
                        onDismiss = { showPasswordSheet = false },
                        onConfirm = { oldPassStr, newPassStr, confirmPassStr ->
                            scope.launch {
                                focusManager.clearFocus()
                                isLoading = true
                                val res = profileManager.changePassword(oldPassStr.trim(), newPassStr.trim(), confirmPassStr.trim())
                                if (res.isSuccess) {
                                    authManager.login(emailStatic, newPassStr.trim())
                                        .onSuccess { context.toast("Пароль изменён и сессия обновлена"); showPasswordSheet = false }
                                        .onFailure { context.toast("Пароль изменён, но войти не удалось.\nПопробуйте войти вручную."); showPasswordSheet = false }
                                } else {
                                    context.toast(res.exceptionOrNull()?.message ?: "Ошибка сервера")
                                    showPasswordSheet = false
                                }
                                isLoading = false
                            }
                        },
                        isLoading = isLoading
                    )
                }
            }
        }
    }
}

@Composable
fun EditSettingsGroup(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) { Column(modifier = Modifier.padding(20.dp), content = content) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    singleLine: Boolean = true,
    supportingText: String? = null,
    enabled: Boolean = true
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 14.sp) },
        leadingIcon = { Icon(icon, null, Modifier.size(22.dp)) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        singleLine = singleLine,
        enabled = enabled,
        supportingText = supportingText?.let { { Text(it, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, fontSize = 11.sp) } },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun ReadOnlyField(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun GenderSelector(selectedGender: String, onGenderSelected: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf("Male" to "Мужчина", "Female" to "Женщина").forEach { (value, label) ->
            val isSelected = selectedGender == value
            Surface(
                onClick = { onGenderSelected(value) },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            ) { Box(contentAlignment = Alignment.Center) { Text(text = label, fontWeight = FontWeight.Bold, fontSize = 14.sp) } }
        }
    }
}

@Composable
fun ChangePasswordSheet(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
    isLoading: Boolean = false
) {
    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    val isConfirmError = confirmPass.isNotEmpty() && newPass != confirmPass
    val isSameAsOld = newPass.isNotEmpty() && oldPass == newPass && oldPass.isNotEmpty()

    Column(
        modifier = Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(24.dp).verticalScroll(rememberScrollState())
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Безопасность", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
        Text("При смене пароля сессия будет обновлена автоматически", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(24.dp))
        TextField(
            value = oldPass, onValueChange = { if (!isLoading) oldPass = it }, label = { Text("Текущий пароль") },
            modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { IconButton(onClick = { if (!isLoading) visible = !visible }) { Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, modifier = Modifier.size(24.dp)) } },
            colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, focusedIndicatorColor = MaterialTheme.colorScheme.primary, unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant, cursorColor = MaterialTheme.colorScheme.primary),
            enabled = !isLoading
        )
        Spacer(Modifier.height(16.dp))
        TextField(
            value = newPass, onValueChange = { if (!isLoading) newPass = it }, label = { Text("Новый пароль (от 8 символов)") },
            modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, isError = isSameAsOld,
            supportingText = if (isSameAsOld) { { Text("Новый пароль не должен совпадать со старым") } } else null,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, focusedIndicatorColor = MaterialTheme.colorScheme.primary, unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant, errorIndicatorColor = MaterialTheme.colorScheme.error, cursorColor = MaterialTheme.colorScheme.primary),
            enabled = !isLoading
        )
        Spacer(Modifier.height(16.dp))
        TextField(
            value = confirmPass, onValueChange = { if (!isLoading) confirmPass = it }, label = { Text("Подтвердите пароль") },
            modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, isError = isConfirmError,
            supportingText = if (isConfirmError) { { Text("Пароли не совпадают") } } else null,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, focusedIndicatorColor = MaterialTheme.colorScheme.primary, unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant, errorIndicatorColor = MaterialTheme.colorScheme.error, cursorColor = MaterialTheme.colorScheme.primary),
            enabled = !isLoading
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = { onConfirm(oldPass, newPass, confirmPass) },
            modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(28.dp),
            enabled = !isLoading && oldPass.isNotBlank() && newPass.length >= 8 && newPass == confirmPass && !isSameAsOld
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.White)
            else Text("ОБНОВИТЬ И ВОЙТИ", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
    }
}