package com.example.smartswitch

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Smartphone
@Composable
fun SmartSwitchScreen(
    onNavigateNext: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement  = Arrangement.Center
    ) {
        /* --- phone = phone row --- */
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.PhoneIphone, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp))

            Text("=", color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp))

            Icon(Icons.Outlined.PhoneIphone, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp))
        }

        Spacer(Modifier.height(32.dp))

        /* --- title texts --- */
        Text("Smart",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground)

        Text("Switch",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary)

        Spacer(Modifier.height(48.dp))



    }
}

@Preview(showBackground = true)
@Composable fun SmartSwitchPreview() = SmartSwitchTheme { SmartSwitchScreen() }
