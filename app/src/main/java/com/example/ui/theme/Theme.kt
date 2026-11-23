package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ScanKitDarkColorScheme = darkColorScheme(
    primary = SkyCobaltPrimary,
    onPrimary = SkyCobaltOnPrimary,
    primaryContainer = SkyCobaltContainer,
    onPrimaryContainer = OnSkyCobaltContainer,
    secondary = NeonCyanSecondary,
    onSecondary = NeonCyanOnSecondary,
    secondaryContainer = NeonCyanContainer,
    onSecondaryContainer = OnNeonCyanContainer,
    tertiary = SignalEmeraldTertiary,
    onTertiary = SignalEmeraldOnTertiary,
    tertiaryContainer = SignalEmeraldContainer,
    onTertiaryContainer = OnSignalEmeraldContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = ErrorRedLight,
    onError = Color(0xFF450A0A)
)

private val ScanKitLightColorScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = CobaltOnPrimary,
    primaryContainer = CobaltContainer,
    onPrimaryContainer = OnCobaltContainer,
    secondary = CyanSecondary,
    onSecondary = CyanOnSecondary,
    secondaryContainer = CyanContainer,
    onSecondaryContainer = OnCyanContainer,
    tertiary = EmeraldTertiary,
    onTertiary = EmeraldOnTertiary,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = OnEmeraldContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = ErrorRed,
    onError = Color.White
)

val ScanKitShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ScanKitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ScanKitDarkColorScheme else ScanKitLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ScanKitTypography,
        shapes = ScanKitShapes,
        content = content
    )
}
