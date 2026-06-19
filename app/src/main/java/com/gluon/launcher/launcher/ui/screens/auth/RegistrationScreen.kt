// app/src/main/java/com/gluon/launcher/launcher/ui/screens/auth/RegistrationScreen.kt
package com.gluon.launcher.launcher.ui.screens.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gluon.launcher.core.auth.AuthManager
import com.gluon.launcher.core.utils.PermissionHelper
import com.gluon.launcher.core.utils.isValidEmail
import com.gluon.launcher.launcher.ui.components.GluonPageTitle
import com.gluon.launcher.launcher.ui.components.GluonScaffold
import kotlinx.coroutines.launch

@Composable
fun RegistrationScreen(
    authManager: AuthManager,
    onBack: () -> Unit,
    onRegistrationSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var passVisible by remember { mutableStateOf(false) }

    val isEmailValid by remember { derivedStateOf { email.isValidEmail() } }
    val isEmailError by remember { derivedStateOf { email.isNotEmpty() && !isEmailValid } }
    val isConfirmError by remember { derivedStateOf { confirmPassword.isNotEmpty() && confirmPassword != password } }
    val isValid by remember { derivedStateOf { name.trim().isNotEmpty() && isEmailValid && password.length >= 8 && password == confirmPassword } }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) avatarUri = uri }

    val requestPermissionAndPick = PermissionHelper.rememberImagePickerPermissionLauncher(
        onPermissionGranted = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onPermissionDenied = { scope.launch { snackState.showSnackbar("Нет разрешения на чтение изображений") } }
    )

    val launchPicker = { if (!isLoading) requestPermissionAndPick() }

    GluonScaffold(
        isRoot = false,
        onNavigate = onBack
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
                    .padding(top = 25.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GluonPageTitle("Регистрация")
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.BottomEnd) {
                    Surface(
                        modifier = Modifier.fillMaxSize().clip(CircleShape).clickable(enabled = !isLoading) { launchPicker() },
                        color = MaterialTheme.colorScheme.surfaceContainerHighest, tonalElevation = 0.dp, shadowElevation = 0.dp
                    ) {
                        if (avatarUri != null) AsyncImage(model = avatarUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AddAPhoto, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary.copy(0.4f)) }
                    }
                    IconButton(
                        onClick = { if (avatarUri == null) launchPicker() else avatarUri = null },
                        modifier = Modifier.size(36.dp).background(if (avatarUri == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer, CircleShape)
                    ) { Icon(if (avatarUri == null) Icons.Default.Add else Icons.Default.Delete, null, Modifier.size(18.dp), tint = if (avatarUri == null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer) }
                }

                Spacer(Modifier.height(12.dp))

                AuthGroupContainer(title = "Данные профиля", icon = Icons.Default.Badge) {
                    AuthTextField(name, { if (it.length <= 40) name = it }, "ФИО", Icons.Default.Person, enabled = !isLoading)
                    AuthTextField(email, { email = it.replace(" ", "").replace(",", ".") }, "Email", Icons.Default.Email, keyboardType = KeyboardType.Email, enabled = !isLoading, isError = isEmailError, supportingText = if (isEmailError) "Некорректный формат почты" else null)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("Male" to "Мужчина", "Female" to "Женщина").forEach { (value, label) ->
                            val selected = gender == value
                            Surface(onClick = { if (!isLoading) gender = value }, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(12.dp), color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 0.dp, shadowElevation = 0.dp, contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, enabled = !isLoading) { Box(contentAlignment = Alignment.Center) { Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp) } }
                        }
                    }
                }

                AuthGroupContainer(title = "Безопасность", icon = Icons.Default.Lock) {
                    AuthTextField(password, { password = it }, "Пароль", Icons.Default.Lock, isPassword = true, passwordVisible = passVisible, onPasswordToggle = { passVisible = !passVisible }, enabled = !isLoading, isError = password.isNotEmpty() && password.length < 8, supportingText = if (password.isNotEmpty() && password.length < 8) "Минимум 8 символов" else null)
                    AuthTextField(confirmPassword, { confirmPassword = it }, "Повтор пароля", Icons.Default.Lock, isPassword = true, passwordVisible = passVisible, onPasswordToggle = { passVisible = !passVisible }, imeAction = ImeAction.Done, enabled = !isLoading, isError = isConfirmError, supportingText = if (isConfirmError) "Пароли не совпадают" else null, onDone = { focusManager.clearFocus() })
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (isLoading) return@Button
                        scope.launch {
                            isLoading = true
                            val registerResult = authManager.register(email.trim(), password, name.trim(), gender, avatarUri)
                            if (registerResult.isSuccess) {
                                val loginResult = authManager.login(email.trim(), password)
                                if (loginResult.isSuccess) onRegistrationSuccess()
                                else { snackState.showSnackbar("Аккаунт создан, но автоматический вход не удался.\nПожалуйста, войдите вручную."); onBack() }
                            } else { snackState.showSnackbar(registerResult.exceptionOrNull()?.message ?: "Ошибка регистрации") }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(28.dp), enabled = !isLoading && isValid, elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text("СОЗДАТЬ АККАУНТ", fontWeight = FontWeight.Bold)
                }
            }
            SnackbarHost(hostState = snackState, modifier = Modifier.align(Alignment.BottomCenter)) { data ->
                androidx.compose.material3.Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(28.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    actionColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}