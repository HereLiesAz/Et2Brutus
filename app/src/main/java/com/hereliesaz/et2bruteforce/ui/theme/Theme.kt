package com.hereliesaz.et2bruteforce.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = MutedBlue,
    secondary = MutedPurple,
    tertiary = MutedGreen,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = OnMutedBlue,
    onSecondary = OnMutedPurple,
    onTertiary = OnMutedGreen,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    error = MutedRed,
    onError = OnMutedRed
)

@Composable
fun Et2BruteForceTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}