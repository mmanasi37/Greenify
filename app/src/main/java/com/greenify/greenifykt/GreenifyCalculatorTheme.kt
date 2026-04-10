package com.greenify.greenifykt

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF1B5E20),
    background = Color(0xFFF4F8F4),
    onBackground = Color(0xFF1F2A1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F2A1F),
    surfaceVariant = Color(0xFFE8F5E9),
    onSurfaceVariant = Color(0xFF1B4332)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF0B2A12),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFD7F5DB),
    background = Color(0xFF0F1411),
    onBackground = Color(0xFFE3F1E5),
    surface = Color(0xFF16201A),
    onSurface = Color(0xFFE3F1E5),
    surfaceVariant = Color(0xFF233228),
    onSurfaceVariant = Color(0xFFCFE7D2)
)

@Composable
fun GreenifyCalculatorTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
