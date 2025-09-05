package com.madash.smartswitch

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AppTheme(
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
            )   
        }
    }
}