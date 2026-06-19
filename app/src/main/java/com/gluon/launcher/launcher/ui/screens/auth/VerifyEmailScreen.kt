// app/src/main/java/com/gluon/launcher/launcher/ui/screens/auth/VerifyEmailScreen.kt
package com.gluon.launcher.launcher.ui.screens.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.gluon.launcher.core.auth.AuthManager
import com.gluon.launcher.launcher.ui.components.GluonPageTitle
import com.gluon.launcher.launcher.ui.components.GluonScaffold
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun VerifyEmailScreen(
    email: String,
    authManager: AuthManager,
    onVerified: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    var isVerifiedServerSide by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    var cooldownSeconds by remember { mutableIntStateOf(30) }
    var initialEmailSent by remember { mutableStateOf(false) }

    val iconColor by animateColorAsState(
        targetValue = if (isVerifiedServerSide) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
        animationSpec = tween(600), label = "iconColor"
    )

    LaunchedEffect(Unit) {
        while (cooldownSeconds > 0) {
            delay(1.seconds)
            cooldownSeconds--
        }
    }

    LaunchedEffect(Unit) {
        if (!initialEmailSent) {
            initialEmailSent = true
            authManager.resendVerificationEmail(email)
        }

        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive && !isVerifiedServerSide) {
                authManager.checkEmailVerificationStatus(email).onSuccess { verified ->
                    if (verified) {
                        isVerifiedServerSide = true
                    }
                }
                if (!isVerifiedServerSide) {
                    delay(4000.milliseconds)
                }
            }
        }
    }

    LaunchedEffect(isVerifiedServerSide) {
        if (isVerifiedServerSide) {
            delay(1500.milliseconds)
            onVerified()
        }
    }

    GluonScaffold(
        isRoot = false,
        onNavigate = onBack
    ) { modifier ->
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .imePadding()
                    .padding(horizontal = 30.dp)
                    .padding(top = 25.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GluonPageTitle("Верификация")

                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(iconColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isVerifiedServerSide) Icons.Default.MarkEmailRead else Icons.Default.MarkEmailUnread,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = iconColor
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = if (isVerifiedServerSide) "ГОТОВО" else "ВЕРИФИКАЦИЯ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = if (isVerifiedServerSide) iconColor else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (isVerifiedServerSide) {
                        "Аккаунт подтверждён!\nПеренаправление через секунду..."
                    } else {
                        "Мы отправили письмо на почту:\n$email\n\nПерейдите по ссылке для активации аккаунта."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(32.dp))

                if (!isVerifiedServerSide) {
                    Button(
                        onClick = {
                            if (cooldownSeconds <= 0 && !isResending) {
                                isResending = true
                                scope.launch {
                                    authManager.resendVerificationEmail(email)
                                        .onSuccess {
                                            snackState.showSnackbar("Письмо успешно отправлено повторно")
                                            cooldownSeconds = 30
                                        }
                                        .onFailure {
                                            snackState.showSnackbar("Ошибка при отправке письма")
                                        }
                                    isResending = false
                                }
                            }
                        },
                        enabled = cooldownSeconds <= 0 && !isResending,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isResending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(if (cooldownSeconds > 0) "Отправить повторно (${cooldownSeconds}с)" else "Отправить повторно")
                        }
                    }
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