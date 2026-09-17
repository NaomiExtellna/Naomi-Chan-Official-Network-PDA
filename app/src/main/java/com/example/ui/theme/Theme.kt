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
    onSecondary = Color(0xFF221006),
    secondaryContainer = Color(0xFF52301F),
    onSecondaryContainer = Color(0xFFFFE7DB),
    tertiary = Color(0xFFD0A36E),
    onTertiary = Color(0xFF24180B),
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

private val LightColorScheme = lightColorScheme(
    primary = NaomiRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDD6),
    onPrimaryContainer = Color(0xFF491008),
    secondary = NaomiOrange,
    onSecondary = Color(0xFF2A1206),
    secondaryContainer = Color(0xFFFFDBCA),
    onSecondaryContainer = Color(0xFF351000),
    tertiary = NaomiDeepRed,
    onTertiary = Color.White,
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF181B20),
    surface = Color.White,
    onSurface = Color(0xFF181B20),
    surfaceVariant = Color(0xFFF0F2F5),
    onSurfaceVariant = Color(0xFF4E5661),
    outline = Color(0xFFD3D8DE),
    error = NaomiError,
    onError = Color.White
)

@Composable
fun NaomiChanTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
