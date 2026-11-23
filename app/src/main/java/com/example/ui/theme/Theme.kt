package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GoluDarkColorScheme =
    darkColorScheme(
        primary = GoluElectricBlue,
        onPrimary = Color.White,
        primaryContainer = GoluNavyCardAlt,
        onPrimaryContainer = GoluNeonCyan,
        secondary = GoluNeonCyan,
        onSecondary = GoluNavyDeep,
        secondaryContainer = Color(0xFF0D2E42),
        onSecondaryContainer = GoluSkyGlow,
        tertiary = GoluIndigoAccent,
        onTertiary = Color.White,
        background = GoluNavyDeep,
        onBackground = GoluTextPrimaryDark,
        surface = GoluNavySurface,
        onSurface = GoluTextPrimaryDark,
        surfaceVariant = GoluNavyCard,
        onSurfaceVariant = GoluTextSecondaryDark,
        outline = GoluOutlineDark,
        error = GoluCoralRed,
        onError = Color.White,
    )

private val GoluLightColorScheme =
    lightColorScheme(
        primary = GoluBluePrimaryLight,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD6E6FF),
        onPrimaryContainer = GoluBluePrimaryLight,
        secondary = GoluCyanSecondaryLight,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD5F5EE),
        onSecondaryContainer = GoluCyanSecondaryLight,
        tertiary = GoluIndigoAccent,
        onTertiary = Color.White,
        background = GoluLightBg,
        onBackground = GoluTextPrimaryLight,
        surface = GoluLightSurface,
        onSurface = GoluTextPrimaryLight,
        surfaceVariant = GoluLightCard,
        onSurfaceVariant = GoluTextSecondaryLight,
        outline = GoluOutlineLight,
        error = GoluCoralRed,
        onError = Color.White,
    )

@Composable
fun GoluTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) GoluDarkColorScheme else GoluLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = GoluTypography,
        content = content,
    )
}
