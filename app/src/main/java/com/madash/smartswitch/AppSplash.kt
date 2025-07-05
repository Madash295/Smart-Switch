package com.madash.smartswitch



import androidx.compose.runtime.*
import kotlinx.coroutines.delay

@Composable
fun AppSplash(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) { delay(3_000); onTimeout() }
    SmartSwitchScreen()
}
