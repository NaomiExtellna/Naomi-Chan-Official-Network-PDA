package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    // The SUNMI app intentionally uses one stable retail-light scheme. Keeping a
    // single palette avoids mixed dark/light legacy surfaces on Android 7.1.
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
