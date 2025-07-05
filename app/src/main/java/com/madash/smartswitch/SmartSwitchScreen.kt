package com.madash.smartswitch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.madash.smartswitch.ui.theme.SmartSwitchTheme

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
            // Replace R.drawable.splashlogo with your actual SVG resource ID
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.splashlogo),
                contentDescription = "Logo",
                tint = MaterialTheme.colorScheme.primary)


        }

        Spacer(Modifier.height(25.dp))

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
