package com.madash.smartswitch

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.madash.smartswitch.NavGraph



private val LightColours = lightColorScheme(
    primary      = Color(0xFF00B2FF),
    onPrimary    = Color.White,
    background   = Color.White,
    onBackground = Color.Black,
    surface      = Color.White,
    onSurface    = Color.Black
)

private val DarkColours = darkColorScheme(
    primary      = Color(0xFF00B2FF),
    onPrimary    = Color.Black,
    background   = Color(0xFF121212),
    onBackground = Color.White,
    surface      = Color(0xFF121212),
    onSurface    = Color.White
)

/* ───────── THEME WITH OPTIONAL DYNAMIC M3 COLOURS ───────── */

@Composable
fun SmartSwitchTheme(
    useDarkTheme : Boolean = isSystemInDarkTheme(),
    dynamicColour: Boolean = false,
    content      : @Composable () -> Unit
) {
    val ctx = LocalContext.current
    val colours = when {
        dynamicColour && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (useDarkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        useDarkTheme -> DarkColours
        else         -> LightColours
    }

    MaterialTheme(
        colorScheme = colours,
        typography  = Typography(),
        content     = content
    )
}

/* ───────── APP ROOT ───────── */

@Composable
fun App(
    useDarkTheme : Boolean = isSystemInDarkTheme(), // follow system for now
    dynamicColour: Boolean = false                  // flip to true for Material‑You
) {
    SmartSwitchTheme(useDarkTheme, dynamicColour) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color    = MaterialTheme.colorScheme.background   // paints full window
        ) {
            NavGraph()    // Splash → Onboarding (once) → Home flow
        }
    }
}
