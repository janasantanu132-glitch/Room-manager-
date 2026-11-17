package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = PureWhite,
    primaryContainer = IceBlue,
    onPrimaryContainer = DeepBlue,
    secondary = VividPurple,
    onSecondary = PureWhite,
    secondaryContainer = LightPurpleTint,
    onSecondaryContainer = DeepPurple,
    tertiary = BrightBlue,
    onTertiary = PureWhite,
    tertiaryContainer = SoftLavender,
    onTertiaryContainer = DeepPurple,
    background = CoolOffWhite,
    onBackground = TextPrimaryLight,
    surface = PureWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = SubtleSurfaceLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = SoftBlueBorder,
    error = DangerRed,
    onError = PureWhite,
    errorContainer = DangerContainerLight,
    onErrorContainer = DangerRed
)

private val DarkColorScheme = darkColorScheme(
    primary = BrightBlue,
    onPrimary = PureWhite,
    primaryContainer = DeepBlue,
    onPrimaryContainer = IceBlue,
    secondary = VividPurple,
    onSecondary = PureWhite,
    secondaryContainer = DeepPurple,
    onSecondaryContainer = SoftLavender,
    tertiary = RoyalBlue,
    onTertiary = PureWhite,
    background = DarkNavyBg,
    onBackground = TextPrimaryDark,
    surface = DarkNavySurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkNavyCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkBorder,
    error = DangerRed,
    onError = PureWhite,
    errorContainer = DangerContainerLight,
    onErrorContainer = DangerRed
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RoomManagementTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
