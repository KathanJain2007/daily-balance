package com.spendtag.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Fallback Light Color Scheme (Deep Indigo primary + Warm Accent)
private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimaryLight,
    onPrimary = IndigoOnPrimaryLight,
    primaryContainer = IndigoPrimaryContainerLight,
    onPrimaryContainer = IndigoOnPrimaryContainerLight,
    secondary = WarmAccentLight,
    onSecondary = WarmOnAccentLight,
    secondaryContainer = WarmAccentContainerLight,
    onSecondaryContainer = WarmOnAccentContainerLight,
    tertiary = SlateCyanLight,
    onTertiary = SlateOnCyanLight,
    tertiaryContainer = SlateCyanContainerLight,
    onTertiaryContainer = SlateOnCyanContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

// Fallback Dark Color Scheme (True Dark, deep midnight black/navy, NOT grey)
private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = IndigoOnPrimaryDark,
    primaryContainer = IndigoPrimaryContainerDark,
    onPrimaryContainer = IndigoOnPrimaryContainerDark,
    secondary = WarmAccentDark,
    onSecondary = WarmOnAccentDark,
    secondaryContainer = WarmAccentContainerDark,
    onSecondaryContainer = WarmOnAccentContainerDark,
    tertiary = SlateCyanDark,
    onTertiary = SlateOnCyanDark,
    tertiaryContainer = SlateCyanContainerDark,
    onTertiaryContainer = SlateOnCyanContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

@Composable
fun SpendTagTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+ (API 31+)
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                // To preserve the "true dark" requirement even with dynamic color,
                // we customize dynamic dark with pure dark background & surface
                val dynamicDark = dynamicDarkColorScheme(context)
                dynamicDark.copy(
                    background = BackgroundDark,
                    surface = SurfaceDark,
                    surfaceVariant = SurfaceVariantDark
                )
            } else {
                dynamicLightColorScheme(context)
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Edge-to-edge transparent system bars setup with contrast icons
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            // If darkTheme, statusBar icons should be light (!darkTheme = false), otherwise dark (true)
            windowInsetsController.isAppearanceLightStatusBars = !darkTheme
            windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
