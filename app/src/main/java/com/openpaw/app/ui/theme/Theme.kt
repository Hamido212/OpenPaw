package com.openpaw.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ── Modern color palette ────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary            = Color(0xFF7C9AFF),
    onPrimary          = Color(0xFF001B5E),
    primaryContainer   = Color(0xFF1E3A7A),
    onPrimaryContainer = Color(0xFFD6E2FF),
    secondary          = Color(0xFFA8C8FF),
    onSecondary        = Color(0xFF0D3068),
    secondaryContainer = Color(0xFF1A2744),
    onSecondaryContainer = Color(0xFFD6E2FF),
    tertiary           = Color(0xFF7DDAC3),
    onTertiary         = Color(0xFF003829),
    tertiaryContainer  = Color(0xFF0D2E24),
    onTertiaryContainer = Color(0xFFA8F0DA),
    error              = Color(0xFFFFB4AB),
    onError            = Color(0xFF690005),
    errorContainer     = Color(0xFF3B0607),
    onErrorContainer   = Color(0xFFFFDAD6),
    background         = Color(0xFF0D1117),
    onBackground       = Color(0xFFE6E1E5),
    surface            = Color(0xFF0D1117),
    onSurface          = Color(0xFFE6E1E5),
    surfaceVariant     = Color(0xFF1C2333),
    onSurfaceVariant   = Color(0xFFC4C6D0),
    outline            = Color(0xFF8E9099),
    outlineVariant     = Color(0xFF44474F),
    inverseSurface     = Color(0xFFE6E1E5),
    inverseOnSurface   = Color(0xFF1B1B1F),
    surfaceTint        = Color(0xFF7C9AFF)
)

private val LightColorScheme = lightColorScheme(
    primary            = Color(0xFF3B63D4),
    onPrimary          = Color(0xFFFFFFFF),
    primaryContainer   = Color(0xFFDCE4FF),
    onPrimaryContainer = Color(0xFF001A5E),
    secondary          = Color(0xFF4A6FA5),
    onSecondary        = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD6E4FF),
    onSecondaryContainer = Color(0xFF001B3D),
    tertiary           = Color(0xFF006B55),
    onTertiary         = Color(0xFFFFFFFF),
    tertiaryContainer  = Color(0xFF7CF8D8),
    onTertiaryContainer = Color(0xFF002018),
    error              = Color(0xFFBA1A1A),
    onError            = Color(0xFFFFFFFF),
    errorContainer     = Color(0xFFFFDAD6),
    onErrorContainer   = Color(0xFF410002),
    background         = Color(0xFFF8F9FC),
    onBackground       = Color(0xFF1B1B1F),
    surface            = Color(0xFFF8F9FC),
    onSurface          = Color(0xFF1B1B1F),
    surfaceVariant     = Color(0xFFE7E8F0),
    onSurfaceVariant   = Color(0xFF44474F),
    outline            = Color(0xFF74777F),
    outlineVariant     = Color(0xFFC4C6D0),
    inverseSurface     = Color(0xFF303034),
    inverseOnSurface   = Color(0xFFF2F0F4),
    surfaceTint        = Color(0xFF3B63D4)
)

// ── Custom Typography ───────────────────────────────────────────────────────

private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.25).sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun OpenPawTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
