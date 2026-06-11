package com.worldbarometer.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.worldbarometer.app.core.BrandPalette
import com.worldbarometer.app.core.NeutralPalette

private val LightColors = lightColorScheme(
    background = NeutralPalette.Light.background,
    surface = NeutralPalette.Light.card,
    onBackground = NeutralPalette.Light.text,
    onSurface = NeutralPalette.Light.text,
    onSurfaceVariant = NeutralPalette.Light.textSecondary,
    outline = NeutralPalette.Light.outline,
    outlineVariant = NeutralPalette.Light.outline,
    primary = BrandPalette.calmTeal,
    onPrimary = Color(0xFF1B1B1D),
    primaryContainer = BrandPalette.warmSand.copy(alpha = 0.5f),
    onPrimaryContainer = NeutralPalette.Light.text,
)

private val DarkColors = darkColorScheme(
    background = NeutralPalette.Dark.background,
    surface = NeutralPalette.Dark.card,
    onBackground = NeutralPalette.Dark.text,
    onSurface = NeutralPalette.Dark.text,
    onSurfaceVariant = NeutralPalette.Dark.textSecondary,
    outline = NeutralPalette.Dark.outline,
    outlineVariant = NeutralPalette.Dark.outline,
    primary = BrandPalette.calmTeal,
    onPrimary = Color(0xFF1B1B1D),
)

@Composable
fun BarometerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content,
    )
}
