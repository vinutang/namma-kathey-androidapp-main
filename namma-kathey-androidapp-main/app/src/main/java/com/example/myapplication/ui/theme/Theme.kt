package com.example.myapplication.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = StorybookCream,
    primaryContainer = TempleGold.copy(alpha = 0.35f),
    onPrimaryContainer = StorybookDeepGreen,
    secondary = DeepSaffron,
    onSecondary = StorybookInk,
    secondaryContainer = DeepSaffron.copy(alpha = 0.25f),
    onSecondaryContainer = StorybookDeepGreen,
    tertiary = TempleGold,
    onTertiary = StorybookInk,
    background = StorybookCream,
    onBackground = StorybookInk,
    surface = StorybookCream,
    onSurface = StorybookInk,
    surfaceVariant = TempleGold.copy(alpha = 0.22f),
    onSurfaceVariant = StorybookDeepGreen,
    outline = ForestGreen.copy(alpha = 0.35f),
)

private val DarkColorScheme = darkColorScheme(
    primary = TempleGold,
    onPrimary = StorybookDeepGreen,
    primaryContainer = ForestGreen,
    onPrimaryContainer = StorybookCream,
    secondary = DeepSaffron,
    onSecondary = StorybookInk,
    secondaryContainer = DeepSaffron.copy(alpha = 0.35f),
    onSecondaryContainer = StorybookCream,
    tertiary = ForestGreen,
    onTertiary = StorybookCream,
    background = StorybookDeepGreen,
    onBackground = StorybookCream,
    surface = StorybookDeepGreen,
    onSurface = StorybookCream,
    surfaceVariant = ForestGreen.copy(alpha = 0.45f),
    onSurfaceVariant = StorybookCream,
    outline = TempleGold.copy(alpha = 0.35f),
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
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
        typography = Typography,
        shapes = StorybookShapes,
        content = content,
    )
}
