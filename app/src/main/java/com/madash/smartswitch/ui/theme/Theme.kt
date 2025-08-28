package com.madash.smartswitch

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalDynamicColour = compositionLocalOf { false }

/* ---------- STATIC LIGHT / DARK ---------- */
private val LightColours = lightColorScheme(
    primary = Color(0xFF00B2FF),
    onPrimary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF6F8FA),   // <‑‑  pale grey card shade
    onSurfaceVariant = Color.Black
)

private val DarkColours = darkColorScheme(
    primary = Color(0xFF00B2FF),
    onPrimary = Color.Black,
    background = Color(0xFF121212),       // window
    onBackground = Color.White,
    surface = Color(0xFF121212),          // same as background
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E1E1E),   // <‑‑ lighter dark for cards
    onSurfaceVariant = Color.White
)



@Composable
fun SmartSwitchTheme(
    useDarkTheme : Boolean = isSystemInDarkTheme(),
    dynamicColour: Boolean = true,
    content      : @Composable () -> Unit
) {
    val ctx = LocalContext.current
    val colours = when {
        dynamicColour && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (useDarkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        useDarkTheme -> DarkColours
        else         -> LightColours
    }

    CompositionLocalProvider(LocalDynamicColour provides dynamicColour) {   // ← provide
        MaterialTheme(
            colorScheme = colours,
            typography  = Typography(),
            content     = content
        )
    }
}
