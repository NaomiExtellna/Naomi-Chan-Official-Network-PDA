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
    primary = NaomiTextPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF2F4F7),
    onPrimaryContainer = NaomiTextPrimary,
    secondary = NaomiOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEAF1F4),
    onSecondaryContainer = Color(0xFF213B4A),
    tertiary = NaomiTransPink,
    onTertiary = Color(0xFF6E3449),
    background = NaomiDarkBg,
    onBackground = NaomiTextPrimary,
    surface = NaomiSurface,
    onSurface = NaomiTextPrimary,
    surfaceVariant = NaomiSurfaceVariant,
    onSurfaceVariant = NaomiTextSecondary,
    outline = NaomiBorder,
    outlineVariant = Color(0xFFEEF1F4),
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
