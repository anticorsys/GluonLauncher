package com.gluon.launcher.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 10-ступенчатая шкала скруглений Material 3 Expressive.
 * Используется во всех контейнерах, карточках, кнопках и полях ввода.
 */
object M3EShapes {
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Large = 16.dp
    val ExtraLarge = 28.dp
    val ExtraExtraLarge = 48.dp
}

/**
 * Глобальные формы Material 3 с M3E-радиусами.
 */
val GluonShapes = Shapes(
    extraSmall = RoundedCornerShape(M3EShapes.ExtraSmall),   // 4dp
    small = RoundedCornerShape(M3EShapes.Small),             // 8dp
    medium = RoundedCornerShape(M3EShapes.Large),            // 16dp (medium увеличен)
    large = RoundedCornerShape(M3EShapes.ExtraLarge),        // 28dp
    extraLarge = RoundedCornerShape(M3EShapes.ExtraExtraLarge) // 48dp
)