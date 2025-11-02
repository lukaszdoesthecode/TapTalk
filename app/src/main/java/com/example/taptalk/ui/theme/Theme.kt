package com.example.taptalk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

/**
 * Defines the color palette for the light theme of the application.
 * This includes colors for backgrounds, surfaces, and the text/icons drawn on top of them.
 */
private val LightColors = lightColorScheme(
    background = Color.White,
    surface = Color(0xFFF8F8F8),
    onBackground = Color.Black,
    onSurface = Color.Black,
)

/**
 * A dark color scheme for the TapTalk application.
 *
 * This color palette is used when the application is in dark mode.
 * It defines specific colors for UI elements like backgrounds and surfaces
 * to ensure a consistent and visually appealing dark theme.
 */
private val DarkColors = darkColorScheme(
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color.White,
    onSurface = Color.White,
)

/**
 * A custom theme for the TapTalk application that wraps MaterialTheme.
 *
 * This theme applies a specific color scheme (light or dark) and can adjust typography for
 * improved accessibility (low vision).
 *
 * @param darkMode Whether the application should be in dark mode. Defaults to the system's
 *   dark theme setting.
 * @param lowVision Whether to enable low vision mode, which increases the base font size
 *   for better readability. Defaults to false.
 * @param content The Composable content to be themed.
 */
@Composable
fun TapTalkTheme(
    darkMode: Boolean = isSystemInDarkTheme(),
    lowVision: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkMode) DarkColors else LightColors
    val fontSizeBoost = if (lowVision) 18.sp else 14.sp

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSizeBoost)
        ),
        content = content
    )
}
