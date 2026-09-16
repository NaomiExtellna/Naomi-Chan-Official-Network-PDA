package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NaomiRed,
    onPrimary = Color.White,
    primaryContainer = NaomiDeepRed,
    onPrimaryContainer = Color(0xFFFFDAD4),
    secondary = NaomiOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF5D2005),
    onSecondaryContainer = Color(0xFFFFDBCF),
    tertiary = NaomiOrange,
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

private val LightColorScheme = lightColorScheme(
    primary = NaomiRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD4),
    onPrimaryContainer = NaomiDeepRed,
    secondary = NaomiOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBCF),
    onSecondaryContainer = Color(0xFF380C00),
    tertiary = NaomiDeepRed,
    onTertiary = Color.White,
    background = Color(0xFFFBF8F7),
    onBackground = Color(0xFF1E1A19),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E1A19),
    surfaceVariant = Color(0xFFF3EDEC),
    onSurfaceVariant = Color(0xFF514341),
    outline = Color(0xFFD5C3C0),
    error = NaomiError,
    onError = Color.White
)

@Composable
fun NaomiChanTheme(
    darkTheme: Boolean = true, // Default to dark DJ terminal aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
