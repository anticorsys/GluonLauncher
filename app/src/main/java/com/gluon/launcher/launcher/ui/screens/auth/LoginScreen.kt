// app/src/main/java/com/gluon/launcher/launcher/ui/screens/auth/LoginScreen.kt
package com.gluon.launcher.launcher.ui.screens.auth

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockPerson
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gluon.launcher.core.auth.AuthManager
import com.gluon.launcher.core.utils.isValidEmail
import com.gluon.launcher.launcher.ui.components.GluonPageTitle
import com.gluon.launcher.launcher.ui.components.GluonScaffold
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authManager: AuthManager,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passVisible by remember { mutableStateOf(false) }
    var showResetSheet by remember { mutableStateOf(false) }

    val isEmailValid by remember { derivedStateOf { email.isValidEmail() } }
    val isEmailError by remember { derivedStateOf { email.isNotEmpty() && !isEmailValid } }

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
                GluonPageTitle("Вход")
                Spacer(Modifier.height(16.dp))
                Icon(Icons.Default.LockPerson, null, Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(20.dp))

                AuthGroupContainer {
                    AuthTextField(
                        email, { email = it.replace(" ", "").replace(",", ".") }, "Email", Icons.Default.Email,
                        keyboardType = KeyboardType.Email, enabled = !isLoading, isError = isEmailError,
                        supportingText = if (isEmailError) "Некорректный формат почты" else null
                    )
                    AuthTextField(
                        password, { password = it }, "Пароль", Icons.Default.Lock,
                        isPassword = true, passwordVisible = passVisible, onPasswordToggle = { passVisible = !passVisible },
                        imeAction = ImeAction.Done, enabled = !isLoading, onDone = { focusManager.clearFocus() }
                    )
                    TextButton(onClick = { if (!isLoading) showResetSheet = true }, modifier = Modifier.align(Alignment.End), enabled = !isLoading) {
                        Text("Забыли пароль?", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (isLoading) return@Button
                        scope.launch {
                            isLoading = true
                            authManager.login(email.trim(), password)
                                .onSuccess { onSuccess() }
                                .onFailure { throwable -> snackState.showSnackbar(throwable.message ?: "Ошибка входа") }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(28.dp),
                    enabled = !isLoading && isEmailValid && password.isNotEmpty(),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text("ВОЙТИ", fontWeight = FontWeight.Bold)
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

            if (showResetSheet) {
                ModalBottomSheet(onDismissRequest = { showResetSheet = false }, containerColor = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
                    ResetPasswordSheet(onDismiss = { showResetSheet = false }, authManager = authManager, snackbarHostState = snackState)
                }
            }
        }
    }
}

@Composable
fun ResetPasswordSheet(
    onDismiss: () -> Unit,
    authManager: AuthManager,
    snackbarHostState: SnackbarHostState
) {
    var email by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val isEmailValid by remember { derivedStateOf { email.isValidEmail() } }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .padding(32.dp)
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Восстановление",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { focusManager.clearFocus(); onDismiss() }) {
                Icon(Icons.Default.Close, null, Modifier.size(24.dp))
            }
        }
        Text(
            "Введите почту для получения инструкций",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.height(24.dp))
        AuthGroupContainer {
            AuthTextField(
                email,
                { email = it.replace(" ", "").replace(",", ".") },
                "Email",
                Icons.Default.Email,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
                enabled = !isSending,
                isError = email.isNotEmpty() && !isEmailValid,
                supportingText = if (email.isNotEmpty() && !isEmailValid) "Введите корректную почту" else null,
                onDone = { if (isEmailValid && !isSending) focusManager.clearFocus() }
            )
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                scope.launch {
                    isSending = true
                    authManager.sendPasswordResetEmail(email.trim())
                        .onSuccess {
                            onDismiss()
                            snackbarHostState.showSnackbar("Инструкции отправлены")
                        }
                        .onFailure {
                            snackbarHostState.showSnackbar(it.message ?: "Ошибка")
                        }
                    isSending = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            enabled = isEmailValid && !isSending,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            if (isSending) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            else Text("ОТПРАВИТЬ ССЫЛКУ", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(40.dp))
    }
}