package com.lyd.player.core.design

import androidx.compose.ui.graphics.Color

// Tokens from designs/sonic_minimalist/DESIGN.md ("Quiet Premium" dark theme)
object LydColors {
    val Surface = Color(0xFF111318)
    val SurfaceDim = Color(0xFF111318)
    val SurfaceBright = Color(0xFF37393E)
    val SurfaceContainerLowest = Color(0xFF0C0E13)
    val SurfaceContainerLow = Color(0xFF191C20)
    val SurfaceContainer = Color(0xFF1E2025)
    val SurfaceContainerHigh = Color(0xFF282A2F)
    val SurfaceContainerHighest = Color(0xFF33353A)
    val OnSurface = Color(0xFFE2E2E9)
    val OnSurfaceVariant = Color(0xFFC6C6CB)
    val InverseSurface = Color(0xFFE2E2E9)
    val InverseOnSurface = Color(0xFF2E3036)
    val Outline = Color(0xFF909095)
    val OutlineVariant = Color(0xFF45474B)

    val Primary = Color(0xFFC6C6CC)
    val OnPrimary = Color(0xFF2F3035)
    val PrimaryContainer = Color(0xFF0F1115)
    val OnPrimaryContainer = Color(0xFF7B7C82)
    val InversePrimary = Color(0xFF5D5E63)

    val Secondary = Color(0xFFFFB59A)
    val OnSecondary = Color(0xFF5A1B00)
    val SecondaryContainer = Color(0xFFFF5E07)
    val OnSecondaryContainer = Color(0xFF531900)

    val Tertiary = Color(0xFFC6C6C7)
    val OnTertiary = Color(0xFF2F3131)
    val TertiaryContainer = Color(0xFF0F1112)
    val OnTertiaryContainer = Color(0xFF7B7D7D)

    val Error = Color(0xFFFFB4AB)
    val OnError = Color(0xFF690005)
    val ErrorContainer = Color(0xFF93000A)
    val OnErrorContainer = Color(0xFFFFDAD6)

    val Background = Color(0xFF111318)
    val OnBackground = Color(0xFFE2E2E9)
    val SurfaceVariant = Color(0xFF33353A)

    // Glassmorphism helpers
    val GlassSurface = SurfaceContainer.copy(alpha = 0.85f)
    val GlassBackground = Background.copy(alpha = 0.9f)
    val HairlineWhite = Color(0x1AFFFFFF) // white @ 10%

    // Sampled from logo.png — matches the "." in the "Lyd." wordmark
    val BrandWordmark = Color(0xFFFFFFFF)
    val BrandDot = Color(0xFFFF5100)
}
