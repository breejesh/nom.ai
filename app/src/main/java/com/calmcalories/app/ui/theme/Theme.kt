package com.calmcalories.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Mode Base Colors
val BrandDarkLight = Color(0xFF1E1A16)
val BrandCardLight = Color(0xFFFAF8F5)
val BrandSurfaceLight = Color(0xFFF3EFE9)
val BrandSecondaryLight = Color(0xFF59524A)
val TextMutedLight = Color(0xFF8F8880)
val TextFaintLight = Color(0xFFC7BEB4)
val DividerLightColor = Color(0xFFE5DFD4)
val DividerLightLight = Color(0xFFEFECE5)

// Dark Mode Base Colors (Mindful, deep warm night shades)
val BrandDarkDark = Color(0xFFF5F1E9)      // Cream white text
val BrandCardDark = Color(0xFF1B1714)      // Warm black card surface
val BrandSurfaceDark = Color(0xFF120F0D)   // Midnight espresso background
val BrandSecondaryDark = Color(0xFFB5AD9F)  // Warm beige/crema text
val TextMutedDark = Color(0xFF918A7D)       // Soft sand grey text
val TextFaintDark = Color(0xFF474039)       // Deep charcoal/sand text
val DividerDarkColor = Color(0xFF28231E)    // Deep warm line
val DividerLightDark = Color(0xFF1E1A17)    // Faint dark separator line

class AppColors(
    val isDark: Boolean,
    val brandDark: Color,
    val brandCard: Color,
    val brandSurface: Color,
    val brandSecondary: Color,
    val textMuted: Color,
    val textFaint: Color,
    val divider: Color,
    val dividerLight: Color
)

private val LightColors = AppColors(
    isDark = false,
    brandDark = BrandDarkLight,
    brandCard = BrandCardLight,
    brandSurface = BrandSurfaceLight,
    brandSecondary = BrandSecondaryLight,
    textMuted = TextMutedLight,
    textFaint = TextFaintLight,
    divider = DividerLightColor,
    dividerLight = DividerLightLight
)

private val DarkColors = AppColors(
    isDark = true,
    brandDark = BrandDarkDark,
    brandCard = BrandCardDark,
    brandSurface = BrandSurfaceDark,
    brandSecondary = BrandSecondaryDark,
    textMuted = TextMutedDark,
    textFaint = TextFaintDark,
    divider = DividerDarkColor,
    dividerLight = DividerLightDark
)

val LocalAppColors = staticCompositionLocalOf { LightColors }

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

@Composable
fun CalmCaloriesTheme(isDark: Boolean, content: @Composable () -> Unit) {
    val activeColors = if (isDark) DarkColors else LightColors
    CompositionLocalProvider(LocalAppColors provides activeColors) {
        content()
    }
}

// Dynamic theme-aware status colors
val Emerald: Color
    @Composable
    @ReadOnlyComposable
    get() = if (AppTheme.colors.isDark) Color(0xFF85B09A) else Color(0xFF637D6E)

val Amber: Color
    @Composable
    @ReadOnlyComposable
    get() = if (AppTheme.colors.isDark) Color(0xFFE3C9A8) else Color(0xFFD9B991)

val BrandRed: Color
    @Composable
    @ReadOnlyComposable
    get() = if (AppTheme.colors.isDark) Color(0xFFC57E75) else Color(0xFF99625A)

// Dynamically resolved color values at composition call-site
val BrandDark: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.brandDark

val BrandCard: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.brandCard

val BrandSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.brandSurface

val BrandSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.brandSecondary

val TextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.textMuted

val TextFaint: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.textFaint

val Divider: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.divider

val DividerLight: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.dividerLight

enum class Tab { Home, Stats, Coach, Journal, Settings }
