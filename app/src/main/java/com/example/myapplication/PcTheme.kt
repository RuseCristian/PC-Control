package com.example.myapplication

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun PcControlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: Color = Color(0xFF00F5A0),
    backgroundColor: Color? = null,
    content: @Composable () -> Unit
) {
    val defaultBg = if (darkTheme) Color(0xFF0B0E11) else Color(0xFFF0F2F5)
    val bg = backgroundColor ?: defaultBg
    
    val surfaceColor = if (darkTheme) {
        Color(
            red = (bg.red + 0.05f).coerceAtMost(1f),
            green = (bg.green + 0.05f).coerceAtMost(1f),
            blue = (bg.blue + 0.05f).coerceAtMost(1f)
        )
    } else {
        Color(
            red = (bg.red - 0.15f).coerceAtLeast(0f),
            green = (bg.green - 0.15f).coerceAtLeast(0f),
            blue = (bg.blue - 0.15f).coerceAtLeast(0f)
        )
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accentColor,
            background = bg,
            surface = surfaceColor,
            onBackground = Color.White,
            onSurface = Color.White,
            surfaceVariant = surfaceColor.copy(alpha = 0.7f)
        )
    } else {
        lightColorScheme(
            primary = accentColor,
            background = bg,
            surface = surfaceColor,
            onBackground = Color(0xFF1A1C1E),
            onSurface = Color(0xFF1A1C1E),
            surfaceVariant = surfaceColor.copy(alpha = 0.7f)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
