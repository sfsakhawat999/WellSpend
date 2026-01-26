package com.h2.wellspend.ui.theme

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF2563eb),
    secondary = Color(0xFF64748b),
    tertiary = Color(0xFFef4444),
    background = Color(0xFF020617),
    surface = Color(0xFF0f172a),
    surfaceContainer = Color(0xFF1e293b), // Slate 800 - lighter for cards
    surfaceContainerHigh = Color(0xFF334155), // Slate 700 - even lighter for elevated cards
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFf1f5f9),
    onSurface = Color(0xFFf1f5f9),
    surfaceVariant = Color(0xFF131c2e), // Tuned darker for Nav Bar
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2563eb),
    secondary = Color(0xFF64748b),
    tertiary = Color(0xFFef4444),
    background = Color(0xFFF1F4F7), // Midpoint between Slate 50 and previous dark
    surface = Color(0xFFffffff),
    surfaceContainer = Color(0xFFffffff), // White for cards in light mode
    surfaceContainerHigh = Color(0xFFffffff), // White for elevated cards in light mode
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0f172a), // Slate 900
    onSurface = Color(0xFF0f172a),
    surfaceVariant = Color(0xFFe2e8f0), // Slate 200 for Navigation Bar
)

@Composable
fun WellSpendTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disable dynamic color to enforce our design
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context).copy(surfaceContainerHigh = Color.White)
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surfaceVariant.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Returns the appropriate background color for cards.
 * Light mode: Always white for clean card appearance
 * Dark mode: Uses surfaceContainerHigh from theme (supports dynamic colors)
 */
@Composable
fun cardBackgroundColor(): Color {
    return MaterialTheme.colorScheme.surfaceContainerHigh
}
