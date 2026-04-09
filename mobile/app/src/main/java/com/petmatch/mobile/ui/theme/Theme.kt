package com.petmatch.mobile.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary       = PrimaryPink,
    onPrimary     = Color.White,
    primaryContainer    = Color(0xFFFFDAE0),
    onPrimaryContainer  = Color(0xFF400011),
    secondary     = SecondaryOrange,
    onSecondary   = Color.White,
    secondaryContainer  = Color(0xFFFFDDD0),
    onSecondaryContainer = Color(0xFF3D1100),
    tertiary      = AccentPurple,
    onTertiary    = Color.White,
    background    = BackgroundLight,
    onBackground  = TextPrimary,
    surface       = SurfaceLight,
    onSurface     = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline       = Divider,
    error         = DislikeRed,
)

private val DarkColorScheme = darkColorScheme(
    primary       = Color(0xFFFFAFBA),
    onPrimary     = Color(0xFF680024),
    primaryContainer = PrimaryDark,
    onPrimaryContainer = Color(0xFFFFDAE0),
    secondary     = Color(0xFFFFB59A),
    onSecondary   = Color(0xFF5C1800),
    background    = BackgroundDark,
    onBackground  = Color(0xFFFFECEF),
    surface       = SurfaceDark,
    onSurface     = Color(0xFFFFECEF),
    surfaceVariant = SurfaceDarkVariant,
    onSurfaceVariant = Color(0xFFD4BEC3),
)

@Composable
fun PetMatchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}