package com.javimetallab.deadmanshot.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// One look only: near-black with blood red, like the launcher icon. The app is always dark.

/** Running and every "all is well" element: the app's own red. */
val RunningColor = Color(0xFFE53935)
val PausedColor = Color(0xFFFFB74D)
val OffColor = Color(0xFF7D7174)
/** The lever has fired: a brighter, alarming red. */
val FiredColor = Color(0xFFFF1744)

/** The big "I'm OK" button. */
val CheckInColor = Color(0xFFC62828)

private val Night = Color(0xFF120D0E)

private val Colors = darkColorScheme(
    primary = Color(0xFFE53935),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5E1414),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFFF8A80),
    onSecondary = Color(0xFF3B0907),
    secondaryContainer = Color(0xFF4A1A1C),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = PausedColor,
    background = Night,
    onBackground = Color(0xFFF3E6E6),
    surface = Night,
    onSurface = Color(0xFFF3E6E6),
    surfaceVariant = Color(0xFF2B1F21),
    onSurfaceVariant = Color(0xFFC9B3B4),
    surfaceContainerLowest = Color(0xFF0C0809),
    surfaceContainerLow = Color(0xFF181113),
    surfaceContainer = Color(0xFF1D1517),
    surfaceContainerHigh = Color(0xFF261B1D),
    surfaceContainerHighest = Color(0xFF312326),
    outline = Color(0xFF7A5F62),
    outlineVariant = Color(0xFF3F2D30),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun DeadManShotTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
