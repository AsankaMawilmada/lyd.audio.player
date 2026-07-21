package au.com.inoaspect.lyd.audio.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LydDarkColorScheme = darkColorScheme(
    primary = LydColors.Primary,
    onPrimary = LydColors.OnPrimary,
    primaryContainer = LydColors.PrimaryContainer,
    onPrimaryContainer = LydColors.OnPrimaryContainer,
    inversePrimary = LydColors.InversePrimary,
    secondary = LydColors.Secondary,
    onSecondary = LydColors.OnSecondary,
    secondaryContainer = LydColors.SecondaryContainer,
    onSecondaryContainer = LydColors.OnSecondaryContainer,
    tertiary = LydColors.Tertiary,
    onTertiary = LydColors.OnTertiary,
    tertiaryContainer = LydColors.TertiaryContainer,
    onTertiaryContainer = LydColors.OnTertiaryContainer,
    background = LydColors.Background,
    onBackground = LydColors.OnBackground,
    surface = LydColors.Surface,
    onSurface = LydColors.OnSurface,
    surfaceVariant = LydColors.SurfaceVariant,
    onSurfaceVariant = LydColors.OnSurfaceVariant,
    surfaceTint = LydColors.Primary,
    inverseSurface = LydColors.InverseSurface,
    inverseOnSurface = LydColors.InverseOnSurface,
    outline = LydColors.Outline,
    outlineVariant = LydColors.OutlineVariant,
    error = LydColors.Error,
    onError = LydColors.OnError,
    errorContainer = LydColors.ErrorContainer,
    onErrorContainer = LydColors.OnErrorContainer,
    surfaceContainerLowest = LydColors.SurfaceContainerLowest,
    surfaceContainerLow = LydColors.SurfaceContainerLow,
    surfaceContainer = LydColors.SurfaceContainer,
    surfaceContainerHigh = LydColors.SurfaceContainerHigh,
    surfaceContainerHighest = LydColors.SurfaceContainerHighest,
)

// All slots are mapped (not just the ones this app uses explicitly) so that any Material3
// component we don't style by hand — DropdownMenuItem, AlertDialog, Switch, etc. — still
// renders in Manrope instead of falling back to the platform default font.
private val LydTypography = Typography(
    displayLarge = LydType.display,
    displayMedium = LydType.displayMobile,
    displaySmall = LydType.headlineLg,
    headlineLarge = LydType.headlineLg,
    headlineMedium = LydType.headlineMd,
    headlineSmall = LydType.headlineMdMobile,
    titleLarge = LydType.headlineMdMobile,
    titleMedium = LydType.bodyLg,
    titleSmall = LydType.bodyMd,
    bodyLarge = LydType.bodyLg,
    bodyMedium = LydType.bodyMd,
    bodySmall = LydType.labelSm,
    labelLarge = LydType.bodyMd,
    labelMedium = LydType.labelSm,
    labelSmall = LydType.labelSm,
)

/**
 * "Lyd" is a single fixed dark ("Quiet Premium") theme per the designs — it does not
 * follow system light/dark mode.
 */
@Composable
fun LydTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LydDarkColorScheme,
        typography = LydTypography,
        shapes = MaterialTheme.shapes.copy(
            extraSmall = LydShapes.sm,
            small = LydShapes.sm,
            medium = LydShapes.default,
            large = LydShapes.md,
            extraLarge = LydShapes.lg,
        ),
        content = content,
    )
}
