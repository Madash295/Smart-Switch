package com.madash.smartswitch.Layouts

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.madash.smartswitch.DataClass.Device
import com.madash.smartswitch.DataClass.DeviceStatus
import com.madash.smartswitch.DataClass.DeviceType
import com.madash.smartswitch.LocalDynamicColour
import com.madash.smartswitch.R
import com.madash.smartswitch.Routes
import com.madash.smartswitch.SmartSwitchTheme
import com.madash.smartswitch.util.getDeviceIconBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReceiver(
    navController: NavHostController
) {
    val devices = remember { getSampleSenderDevices() }
    val dynamic = LocalDynamicColour.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "File Transfer",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (dynamic) MaterialTheme.colorScheme.primary else Color(0xFF8965CC) ,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            // Receiving Animation - similar to ScanningAnimation but with receive icon
            ReceivingAnimation()

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Ready to Receive",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Select the device Name or Scan QR",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Large QR Scanner Area
            Box(
                modifier = Modifier
                    .size(200.dp) // Make it a square
                    .border(
                        width = 2.dp,
                        color = if (dynamic) MaterialTheme.colorScheme.primary else Color(0xFF8965CC),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clip(RoundedCornerShape(20.dp)) // Clip content to rounded corners
                    .clickable {
                        navController.navigate(Routes.ScanQr.route)
                    },
                contentAlignment = Alignment.Center
            ) {
                // Loading Circle
                CircularProgressIndicator(
                    modifier = Modifier.size(60.dp), // Adjust size as needed
                    color =if (dynamic) MaterialTheme.colorScheme.primary else Color(0xFF8965CC),
                    strokeWidth = 3.dp
                )
                // QR Icon (optional, can be overlaid or replaced by the circle)

            }

            Spacer(modifier = Modifier.height(24.dp))

            // Display only the first device in the list for now
            // In a real scenario, you might iterate through a list of devices
            // or have a more sophisticated way to select/display the relevant device.
            if (devices.isNotEmpty()) {
                ReceiverDeviceCard(device = devices.first()) { }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                ConnectionSettingsButton {
                    // Handle connection settings
                }
            }
        }
    }
}

@Composable
fun ReceivingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "receiving")
    val dynamic = LocalDynamicColour.current

    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer receiving rings - similar to scanning animation
        repeat(3) { index ->
            val delay = index * 667 // Stagger the animations
            val ringScale by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, delayMillis = delay, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "ring$index"
            )

            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 0.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, delayMillis = delay, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "alpha$index"
            )

            val primaryColor =if (dynamic) MaterialTheme.colorScheme.primary else Color(0xFF8965CC)

            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val center = center
                val radius = size.minDimension / 2 * ringScale

                drawCircle(
                    color = primaryColor.copy(alpha = alpha * 0.5f),
                    radius = radius,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Central device icon with gradient background - using receive icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    if (dynamic) MaterialTheme.colorScheme.primary else Color(0xFF8965CC)
                ),
            contentAlignment = Alignment.Center
        ) {
            val iconTint = if (dynamic) MaterialTheme.colorScheme.primaryContainer else Color.White
            Icon(
                painter = painterResource(R.drawable.receivenew),
                contentDescription = "Receive",
                tint = iconTint,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun ReceiverDeviceCard(
    device: Device,
    onClick: () -> Unit
) {
    val dynamic = LocalDynamicColour.current
    val backgroundColor = if (dynamic) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device Icon with dynamic background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(getDeviceIconBackground(device.deviceType, dynamic)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(device.icon),
                    contentDescription = device.name,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Device Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WiFi SSID: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Wildirect-9s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }

                Text(
                    text = "Password: 12345678",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
    }
}


@Composable
fun ConnectionSettingsButton(onClick: () -> Unit) {
    val dynamic = LocalDynamicColour.current
    Row(
        modifier = Modifier
            .padding(16.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Settings,
            contentDescription = "Connection Settings",
            tint = if (dynamic) MaterialTheme.colorScheme.primary else Color(0xFF8965CC),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "Connection Setting",
            style = MaterialTheme.typography.bodyMedium,
            color = if (dynamic) MaterialTheme.colorScheme.primary else Color(0xFF8965CC)
        )
    }
}

fun getSampleSenderDevices(): List<Device> {
    return listOf(
        Device(
            id = "1",
            name = "John Redmi Note 9s",
            deviceType = DeviceType.PHONE,
            platform = "Android",
            batteryLevel = null,
            status = DeviceStatus.ONLINE,
            icon = R.drawable.phoneicon
        )
    )
}

@Preview(name = "Light", showBackground = true, showSystemUi = true)
@Composable
fun CreateReceiverLightPreview() {
    SmartSwitchTheme(useDarkTheme = false, dynamicColour = false) {
        CreateReceiver(navController = NavHostController(LocalContext.current))
    }
}

@Preview(
    name = "Dark",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    showSystemUi = true
)
@Composable
fun CreateReceiverDarkPreview() {
    SmartSwitchTheme(useDarkTheme = true, dynamicColour = false) {
        CreateReceiver(navController = NavHostController(LocalContext.current))
    }
}

@Preview(name = "Dynamic Light", showBackground = true, showSystemUi = false)
@Composable
fun CreateReceiverDynamicLightPreview() {
    SmartSwitchTheme(useDarkTheme = false, dynamicColour = true) {
        CreateReceiver(navController = NavHostController(LocalContext.current))
    }
}

@Preview(name = "Dynamic Dark", showBackground = true, showSystemUi = true)
@Composable
fun CreateReceiverDynamicDarkPreview() {
    SmartSwitchTheme(useDarkTheme = true, dynamicColour = true) {
        CreateReceiver(navController = NavHostController(LocalContext.current))
    }
}
