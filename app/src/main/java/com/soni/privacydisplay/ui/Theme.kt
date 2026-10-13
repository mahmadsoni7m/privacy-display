package com.soni.privacydisplay.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Charcoal = Color(0xFF1C1C1E)
val CharcoalLight = Color(0xFF2C2C2E)
val Accent = Color(0xFF3E91FF)
val ActiveGreen = Color(0xFF4CD964)
val MutedText = Color(0xFFB0B0B8)
val ErrorRed = Color(0xFFFF6B6B)

private val PrivacyColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = Accent,
    onSecondary = Color.White,
    secondaryContainer = CharcoalLight,
    onSecondaryContainer = Color.White,
    background = Color(0xFF0A0A0B),
    onBackground = Color.White,
    surface = Charcoal,
    onSurface = Color.White,
    surfaceVariant = CharcoalLight,
    onSurfaceVariant = MutedText,
    error = ErrorRed,
    onError = Color.Black,
    errorContainer = Color(0xFF3B1F1F),
    onErrorContainer = Color(0xFFFFD6D6),
    outline = Color(0xFF48484A),
)

private val PrivacyShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

/** The app is always dark (black/charcoal, white text, blue accent). */
@Composable
fun PrivacyDisplayTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PrivacyColors, shapes = PrivacyShapes, content = content)
}
