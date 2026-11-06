package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RiveraColorScheme = lightColorScheme(
    primary = ButtonPrimary,
    onPrimary = ButtonPrimaryText,
    primaryContainer = HotelHeaderNav,
    onPrimaryContainer = TextWhite,
    secondary = ButtonAccent,
    onSecondary = ButtonAccentText,
    background = Color(0xFFFFFFFF),
    onBackground = TextDark,
    surface = Color(0xFFFFFFFF),
    onSurface = TextDark,
    surfaceVariant = Color(0xFFF8FAFC),
    onSurfaceVariant = TextDarkSecondary,
    outline = BorderMedium,
    outlineVariant = BorderLight,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRedBg,
    onErrorContainer = ErrorRed
)

@Composable
fun HotelRiveraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RiveraColorScheme,
        typography = Typography,
        content = content
    )
}
