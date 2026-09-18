package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val CorporateShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

private val LightColorScheme = lightColorScheme(
    primary = NaomiRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE7E2),
    onPrimaryContainer = NaomiDeepRed,
    secondary = NaomiOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFECE2),
    onSecondaryContainer = Color(0xFF5B2D18),
    tertiary = NaomiDeepRed,
    onTertiary = Color.White,
    background = NaomiDarkBg,
    onBackground = NaomiTextPrimary,
    surface = NaomiSurface,
    onSurface = NaomiTextPrimary,
    surfaceVariant = NaomiSurfaceVariant,
    onSurfaceVariant = NaomiTextSecondary,
    outline = NaomiBorder,
    outlineVariant = Color(0xFFE8EBEF),
    error = NaomiError,
    onError = Color.White
)

@Composable
fun NaomiChanTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = CorporateShapes,
        content = content
    )
}
