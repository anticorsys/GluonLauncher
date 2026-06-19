// app/src/main/java/com/gluon/launcher/launcher/ui/components/GluonScaffold.kt
package com.gluon.launcher.launcher.ui.components

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.gluon.launcher.core.theme.LocalThemeSystemBars

@Composable
fun GluonPageTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            // Отступ справа гарантирует, что длинный текст не налезет под навигационную кнопку
            .padding(bottom = 24.dp, end = 80.dp),
        textAlign = TextAlign.Start
    )
}

@Composable
fun GluonScaffold(
    isRoot: Boolean = false,
    showCloseButton: Boolean = true,
    onNavigate: (() -> Unit)? = null,
    isDashboard: Boolean = false,
    content: @Composable (Modifier) -> Unit
) {
    val systemBars = LocalThemeSystemBars.current
    val context = LocalContext.current

    LaunchedEffect(isDashboard) {
        systemBars.isDashboard.value = isDashboard
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDashboard) Color.Transparent else MaterialTheme.colorScheme.background)
    ) {
        content(Modifier.fillMaxSize().imePadding())

        if (!isDashboard && showCloseButton) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 20.dp, end = 30.dp)
                    .zIndex(10f)
            ) {
                Surface(
                    modifier = Modifier
                        .size(45.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (onNavigate != null) {
                                onNavigate()
                            } else if (isRoot) {
                                (context as? Activity)?.finish()
                            }
                        },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = if (isRoot) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isRoot) "Закрыть" else "Назад",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}