// app/src/main/java/com/gluon/launcher/animation/SpringAnimations.kt
package com.gluon.launcher.animation

import androidx.compose.animation.core.spring

object SpringAnimations {
    // ОПТИМИЗАЦИЯ ПОД 120 Гц: Повышена жесткость (Stiffness) и затухание (Damping).
    // Интерфейс больше не "желейный", а отзывчивый, как в Pixel Launcher.
    val standard = spring<Float>(
        dampingRatio = 0.9f,
        stiffness = 450f
    )

    // Максимально быстрый и точный отклик для иконок при тапе/нажатии
    val icon = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = 550f
    )
}