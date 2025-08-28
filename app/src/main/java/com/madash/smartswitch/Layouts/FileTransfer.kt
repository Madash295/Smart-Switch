package com.madash.smartswitch.Layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.madash.smartswitch.LocalDynamicColour
import com.madash.smartswitch.NativeAdBlock
import com.madash.smartswitch.R
import com.madash.smartswitch.SmartBottomBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileTransfer(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {  Text(
                    "File Transfer",
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
    ) { innerPadding ->
        Column (
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),


        ){
         Maincontent()
        }

    }

}


@Composable
fun Maincontent() {
    val dynamic = LocalDynamicColour.current
    val dynamicSenderColors = listOf(
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
    )
    val staticSenderGradient = listOf(
        Color(0xFF34B3FE),
        Color(0xFF2260E8)
    )
    val dynamicReceiverColors = listOf(
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
    )
    val staticReceiverGradient = listOf(
        Color(0xFF8965CC),
        Color(0xFF471E94)
    )

    Column(
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Rounded icon/file placeholder
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.filetransfer))
        val dynamicProperties = if (dynamic) {
            rememberLottieDynamicProperties(
                // Example: Change color of a specific layer (e.g., "Shape Layer 1")
                com.airbnb.lottie.compose.rememberLottieDynamicProperty(
                    com.airbnb.lottie.LottieProperty.COLOR,
                    MaterialTheme.colorScheme.primary.hashCode(),
                    "**" // Target all layers
                ),
                com.airbnb.lottie.compose.rememberLottieDynamicProperty(
                    com.airbnb.lottie.LottieProperty.STROKE_COLOR,
                    MaterialTheme.colorScheme.onPrimary.hashCode(),
                    "**" // Target all layers
                )
                // Add more rememberLottieDynamicProperty calls for other layers
                // if you want to apply different colors to different parts
                // or use "**" to apply a single color to everything if that's desired.
            )
        } else {
            // Optionally, define static properties or return null if no dynamic changes are needed
            // For this example, we'll return null, meaning no dynamic properties are applied
            // when 'dynamic' is false.
            null
        }

        Box(
            modifier = Modifier
                .size(220.dp) // Reduced size
                .padding(8.dp)
                .align(Alignment.CenterHorizontally)
                .background(
                    color = Color.Transparent, // Make background transparent
                )
        ){
            LottieAnimation(
                composition = composition,
                dynamicProperties = dynamicProperties, // Apply dynamic properties for theming
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.8f) // Fill the Box
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            "Transfer Everything",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Choose Mode",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.height(10.dp))

      Column(modifier = Modifier.padding(horizontal = 32.dp)) {
          GradientButton(
              text = "Sender",
              subtitle = "Send data from this device",
              gradient = if (dynamic) dynamicSenderColors else staticSenderGradient,
              icon = painterResource(R.drawable.send) // placeholder icon
          )
          Spacer(modifier = Modifier.height(10.dp))
          // Receiver Button
          GradientButton(
              text = "Receiver",
              subtitle = "Receive data on this device",
              gradient = if (dynamic) dynamicReceiverColors else staticReceiverGradient,
              icon = painterResource(R.drawable.receive)
          )
          Spacer(modifier = Modifier.height(10.dp))
          // Speed Up Banner


      }
//        NativeAdBlock()
      }

}

// Gradient button used for Sender/Receiver
@Composable
fun GradientButton(
    text: String,
    subtitle: String,
    gradient: List<Color>,
    icon: Painter
) {
    val dynamic = LocalDynamicColour.current
    val iconTint = if (dynamic) MaterialTheme.colorScheme.onSecondaryContainer else  Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .background(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(gradient),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center // Center the Row within the Box
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth() // Allow Row to take full width of the Box
                .padding(horizontal = 16.dp), // Keep some horizontal padding within the Row
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start // Align items to the start of the Row
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .size(40.dp) // Adjusted icon size
                    .padding(end = 12.dp) // Adjusted padding
            )
            Column(
                modifier = Modifier
                    .weight(1f), // Ensure text takes available space
                horizontalAlignment = Alignment.Start, // Align text to the start of the Column
                verticalArrangement = Arrangement.Center // Center text vertically within the column
            ) {
                Text(
                    text,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (dynamic) MaterialTheme.colorScheme.onSecondaryContainer else Color.White,
                    // Modifier.align(Alignment.Start) removed as Column handles alignment
                )
                Text(
                    subtitle,

                    style = MaterialTheme.typography.bodySmall,
                    color = if (dynamic) MaterialTheme.colorScheme.onSecondaryContainer.copy(0.9f) else Color.White,
                    // Modifier.align(Alignment.Start) removed as Column handles alignment
                )
            }
        }
    }
}


@Composable
@Preview
fun FileTransferPreview(){
    FileTransfer(navController = NavHostController(LocalContext.current))
}