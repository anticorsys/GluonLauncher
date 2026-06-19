// app/src/main/java/com/gluon/launcher/launcher/ui/screens/auth/WelcomeScreen.kt
package com.gluon.launcher.launcher.ui.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gluon.launcher.launcher.ui.components.GluonScaffold

@Composable
fun WelcomeScreen(
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onGuest: () -> Unit
) {
    GluonScaffold(
        isRoot = true,
        showCloseButton = false,
        onNavigate = null
    ) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 30.dp)
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.15f))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Добро\nпожаловать",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 55.sp,
                        lineHeight = 55.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    textAlign = TextAlign.Start
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Text(
                        text = "Gluon — частица, которая «склеивает» всё воедино. Идеальная метафора для лаунчера, который объединяет приложения.",
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 0.dp, shadowElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "GLUONCORE ID", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.primary)
                    Text(text = "Единый аккаунт экосистемы Gluon", textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp, bottom = 40.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onRegister, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(28.dp), elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)) {
                        Text(text = "СОЗДАТЬ АККАУНТ", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    FilledTonalButton(onClick = onLogin, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(28.dp)) {
                        Text(text = "ВОЙТИ", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            FilledTonalButton(
                onClick = onGuest,
                modifier = Modifier.padding(bottom = 40.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Text(text = "ВОЙТИ КАК ГОСТЬ", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold))
            }

            Spacer(modifier = Modifier.weight(0.1f))
        }
    }
}