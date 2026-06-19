@file:Suppress("PackageDirectoryMismatch")

package com.gluon.launcher.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// === Светлая тема (статический стиль) ===
val LightPrimary = Color(0xFF1565C0)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFD3E3FD)
val LightOnPrimaryContainer = Color(0xFF001B3D)

val LightSecondary = Color(0xFF5F6368)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFE8EAED)
val LightOnSecondaryContainer = Color(0xFF1F1F1F)

val LightTertiary = Color(0xFF007B83)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFA0EFF5)
val LightOnTertiaryContainer = Color(0xFF002022)

val LightBackground = Color(0xFFF8F9FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE8EAED)
val LightOnSurface = Color(0xFF1F1F1F)
val LightOnSurfaceVariant = Color(0xFF44464F)

val LightSurfaceContainerLow = Color(0xFFFFFFFF)
val LightSurfaceContainer = Color(0xFFF2F2F2)
val LightSurfaceContainerHigh = Color(0xFFE8EAED)
val LightSurfaceContainerHighest = Color(0xFFDEE0E3)

val LightError = Color(0xFFB3261E)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFF9DEDC)
val LightOnErrorContainer = Color(0xFF410E0B)

val LightOutline = Color(0xFF74777F)
val LightOutlineVariant = Color(0xFFC4C6D0)

// === Тёмная тема (статический стиль) ===
val DarkPrimary = Color(0xFF8AB4F8)
val DarkOnPrimary = Color(0xFF001D36)
val DarkPrimaryContainer = Color(0xFF004A77)
val DarkOnPrimaryContainer = Color(0xFFD3E3FD)

val DarkSecondary = Color(0xFFC4C6D0)
val DarkOnSecondary = Color(0xFF303134)
val DarkSecondaryContainer = Color(0xFF44464F)
val DarkOnSecondaryContainer = Color(0xFFE8EAED)

val DarkTertiary = Color(0xFF80DEEA)
val DarkOnTertiary = Color(0xFF00363A)
val DarkTertiaryContainer = Color(0xFF004F53)
val DarkOnTertiaryContainer = Color(0xFFA0EFF5)

// ✅ Исправлен фон – теперь он комфортный, а не "угольный"
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF3E3E45)
val DarkOnSurface = Color(0xFFE6E1E5)
val DarkOnSurfaceVariant = Color(0xFFC4C6D0)

val DarkSurfaceContainerLow = Color(0xFF1D1B20)
val DarkSurfaceContainer = Color(0xFF242229)
val DarkSurfaceContainerHigh = Color(0xFF2B2930)
val DarkSurfaceContainerHighest = Color(0xFF34333A)

val DarkError = Color(0xFFF2B8B5)
val DarkOnError = Color(0xFF601410)
val DarkErrorContainer = Color(0xFF8C1D18)
val DarkOnErrorContainer = Color(0xFFF9DEDC)

val DarkOutline = Color(0xFF8E918F)
val DarkOutlineVariant = Color(0xFF44464F)

// Готовые схемы
val GluonLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant
)

val GluonDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)