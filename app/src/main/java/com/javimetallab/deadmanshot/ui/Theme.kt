package com.javimetallab.deadmanshot.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val RunningColor = Color(0xFF2E9E4F)
val PausedColor = Color(0xFFE09A1A)
val OffColor = Color(0xFF8A8A96)
val FiredColor = Color(0xFFD9443A)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4A9),
    onPrimary = Color(0xFF561E17),
    secondary = Color(0xFF9DD1A5),
    background = Color(0xFF14121A),
    surface = Color(0xFF1E1B24),
    surfaceVariant = Color(0xFF2A2631),
    error = Color(0xFFFF8A80),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF9C2A22),
    secondary = Color(0xFF2E7D4F),
    background = Color(0xFFF7F5F8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFECE7EE),
)

@Composable
fun DeadManShotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
