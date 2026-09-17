package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NaomiRed,
    onPrimary = Color.White,
    primaryContainer = NaomiDeepRed,
    onPrimaryContainer = Color(0xFFFFE4DF),
    secondary = NaomiOrange,
    onSecondary = Color.White,
    background = Color(0xFF0D0F13),
    onBackground = Color(0xFFF5F7FA),
    surface = Color(0xFF171A20),
    onSurface = Color(0xFFF5F7FA),
    surfaceVariant = Color(0xFF20242C),
    onSurfaceVariant = Color(0xFFB6BDC8),
    outline = Color(0xFF303640),
    error = NaomiError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = NaomiRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE3DD),
    onPrimaryContainer = Color(0xFF4A1510),
    secondary = NaomiOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE8DC),
    onSecondaryContainer = Color(0xFF4B2412),
    tertiary = NaomiDeepRed,
    onTertiary = Color.White,
    background = NaomiDarkBg,
    onBackground = NaomiTextPrimary,
    surface = NaomiSurface,
    onSurface = NaomiTextPrimary,
    surfaceVariant = NaomiSurfaceVariant,
    onSurfaceVariant = NaomiTextSecondary,
    outline = NaomiBorder,
    error = NaomiError,
    onError = Color.White
)

@Composable
fun NaomiChanTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
