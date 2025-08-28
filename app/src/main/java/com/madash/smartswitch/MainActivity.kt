package com.madash.smartswitch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    private val dynamicColourState = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        dynamicColourState.value = prefs.getBoolean("dynamicColour", true)
        setContent {
            App(
                dynamicColour = dynamicColourState.value,
                onDynamicColourChange = {
                    dynamicColourState.value = it
                    prefs.edit().putBoolean("dynamicColour", it).apply()
                }
            )
        }
    }
}

@Composable
fun App(
    useDarkTheme : Boolean = isSystemInDarkTheme(), // follow system for now
    dynamicColour: Boolean = true,                  // flip to true for Material‑You
    onDynamicColourChange: (Boolean) -> Unit = {}
) {
    SmartSwitchTheme(useDarkTheme, dynamicColour) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color    = MaterialTheme.colorScheme.background   // paints full window
        ) {
            NavGraph(
                onDynamicColourChange = onDynamicColourChange,
                dynamicColour = dynamicColour
            )    // Splash → Onboarding (once) → Home flow
        }
    }
}

