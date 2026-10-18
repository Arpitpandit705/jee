package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = FocusIndigoPrimary,
    onPrimary = TextPrimary,
    primaryContainer = FocusIndigoDark,
    onPrimaryContainer = TextPrimary,
    secondary = FocusEmerald,
    onSecondary = TextPrimary,
    secondaryContainer = FocusEmeraldContainer,
    error = FocusCrimson,
    errorContainer = FocusCrimsonContainer,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = FocusIndigoDark,
    onPrimary = TextPrimary,
    primaryContainer = FocusIndigoLight,
    secondary = FocusEmerald,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = DarkBackground,
    onSurface = DarkBackground
)

@Composable
fun FocusStudyTheme(
    darkTheme: Boolean = true, // Default to deep obsidian dark theme for optimal study ergonomics
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
