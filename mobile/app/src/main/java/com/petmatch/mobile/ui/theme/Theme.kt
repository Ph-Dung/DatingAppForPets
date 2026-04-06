package com.petmatch.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val PetMatchColorScheme = lightColorScheme(
    primary = PrimaryPink,
    onPrimary = White,
    primaryContainer = LightPink,
    onPrimaryContainer = PrimaryPinkDark,
    secondary = AccentPink,
    onSecondary = White,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = White,
    onSurface = TextPrimary,
    error = ActionRed,
    onError = White,
)

@Composable
fun PetMatchTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PetMatchColorScheme,
        typography = Typography,
        content = content
    )
}