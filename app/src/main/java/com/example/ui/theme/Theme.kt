package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.ThemeMode

@Immutable
data class GlassTokens(
    val isDark: Boolean,
    val backdropTop: Color,
    val backdropMid: Color,
    val backdropBottom: Color,
    val glassFillTop: Color,
    val glassFillBottom: Color,
    val glassRailFill: Color,
    val glassPillActive: Color,
    val specularBorderTop: Color,
    val specularBorderBottom: Color,
    val subtleDivider: Color,
    val accentGlow: Color,
    val secondaryGlow: Color
)

private val LightGlassTokens = GlassTokens(
    isDark = false,
    backdropTop = Color(0xFFE7F2FA),
    backdropMid = Color(0xFFF2F6FC),
    backdropBottom = Color(0xFFE2ECF8),
    glassFillTop = Color(0xBDFFFFFF),
    glassFillBottom = Color(0x8AF4F8FF),
    glassRailFill = Color(0x5CEAF3FC),
    glassPillActive = Color(0x3D007E7A),
    specularBorderTop = Color(0xF2FFFFFF),
    specularBorderBottom = Color(0x6EB8C9DF),
    subtleDivider = Color(0x2E52627A),
    accentGlow = Color(0x30009B8D),
    secondaryGlow = Color(0x241A56DB)
)

private val DarkGlassTokens = GlassTokens(
    isDark = true,
    backdropTop = Color(0xFF060A13),
    backdropMid = Color(0xFF0B1324),
    backdropBottom = Color(0xFF070C17),
    glassFillTop = Color(0x9E18253E),
    glassFillBottom = Color(0x730E1729),
    glassRailFill = Color(0x52101A2E),
    glassPillActive = Color(0x402CEAA3),
    specularBorderTop = Color(0x6BFFFFFF),
    specularBorderBottom = Color(0x2B5CA8FF),
    subtleDivider = Color(0x3394A6C2),
    accentGlow = Color(0x2E2CEAA3),
    secondaryGlow = Color(0x265CA8FF)
)

val LocalGlassTokens = staticCompositionLocalOf { DarkGlassTokens }

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    tertiary = DarkTertiary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    tertiary = LightTertiary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

private val VoltMotorShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun VoltMotorTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val glassTokens = if (isDark) DarkGlassTokens else LightGlassTokens

    CompositionLocalProvider(LocalGlassTokens provides glassTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VoltMotorTypography,
            shapes = VoltMotorShapes,
            content = content
        )
    }
}
