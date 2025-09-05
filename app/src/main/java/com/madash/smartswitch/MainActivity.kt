package com.madash.smartswitch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {
    private val dynamicColourState = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        dynamicColourState.value = prefs.getBoolean("dynamicColour", true)
        setContent {
            AppTheme(
                dynamicColour = dynamicColourState.value,
                onDynamicColourChange = {
                    dynamicColourState.value = it
                    prefs.edit().putBoolean("dynamicColour", it).apply()
                }
            )
        }
    }
}



