package com.example.smartswitch

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/* ---------- THEME (Light / Dark / Dynamic) ---------- */

private val LightColours = lightColorScheme(
    primary       = Color(0xFF00B2FF),  // your cyan
    onPrimary     = Color.White,
    secondary     = Color(0xFF006C9C),
    onSecondary   = Color.White,
    background    = Color.White,
    onBackground  = Color.Black,
    surface       = Color.White,
    onSurface     = Color.Black
)

private val DarkColours = darkColorScheme(
    primary       = Color(0xFF00B2FF),
    onPrimary     = Color.Black,
    secondary     = Color(0xFF4FC9FF),
    onSecondary   = Color.Black,
    background    = Color(0xFF121212),
    onBackground  = Color.White,
    surface       = Color(0xFF121212),
    onSurface     = Color.White
)

@Composable
fun SmartSwitchTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColour: Boolean = false,
    content: @Composable () -> Unit
) {
    val colours = when {
        dynamicColour && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        useDarkTheme -> DarkColours
        else         -> LightColours
    }

    MaterialTheme(
        colorScheme = colours,
        typography  = Typography(),
        content     = content
    )
}

/* ---------- NAVIGATION ---------- */

sealed class Screen(val route: String) {
    object SmartSwitch : Screen("smartSwitch")
    object Second      : Screen("second")

}

@Composable
fun App(useDarkTheme: Boolean = isSystemInDarkTheme()) {
    SmartSwitchTheme(useDarkTheme = useDarkTheme) {


        Surface(
            modifier = Modifier.fillMaxSize(),
            color    = MaterialTheme.colorScheme.background
        ) {

            SmartSwitchScreen()
        }
    }
}

