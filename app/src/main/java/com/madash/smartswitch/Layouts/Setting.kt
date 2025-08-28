package com.madash.smartswitch.Layouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Switch
import androidx.navigation.NavHostController
import com.madash.smartswitch.SmartBottomBar
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    dynamicColour: Boolean,
    onDynamicColourChange: (Boolean) -> Unit
) {
    Scaffold (
        topBar = {
            TopAppBar(
                title = {  Text(
                    "Settings",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                ) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = { SmartBottomBar(navController) }
    ){ innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                
        ) {

            SettingsContent(
                dynamicColour = dynamicColour,
                onDynamicColourChange = onDynamicColourChange
            )
        
    }
}
}

@Composable
fun SettingsContent(
    dynamicColour: Boolean,
    onDynamicColourChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Dynamic Material Theme", modifier = Modifier.padding(end = 8.dp))
            Switch(
                checked = dynamicColour,
                onCheckedChange = onDynamicColourChange
            )
        }
    }
}
