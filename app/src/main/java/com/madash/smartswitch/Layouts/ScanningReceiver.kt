package com.madash.smartswitch.Layouts

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
fun ScanningReceiver(
    navController: NavHostController,
    mediaCount: Int? = null,
    appCount: Int? = null,
    contactCount: Int? = null,
    fileCount: Int? = null
) {
    val devices = remember { getSampleDevices() }
    val totalCount = (mediaCount ?: 0) + (appCount ?: 0) + (contactCount ?: 0) + (fileCount ?: 0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {  Text(
                    "Send Files",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                ) },
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
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))




            ScanningAnimation()

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Scanning Devices",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Nearby Receivers Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Nearby Receivers",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Device List
            LazyColumn(
                modifier = Modifier
                    .weight(1f) // Allow LazyColumn to take available space
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(devices) { device ->
                    DeviceCard(device = device) {
                        // Handle device selection - you can navigate to transfer screen here
                        // The selected file counts are: media=$mediaCount, apps=$appCount, contacts=$contactCount, files=$fileCount
                    }
                }
            }

            // QR Code Button - Aligned to bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp), // 8dp from bottom
                contentAlignment = Alignment.BottomCenter // Align content to bottom center
            ) {
                QRCodeButton {
                    navController.navigate(Routes.ScanQr.route)
                }
            }
        }
    }
}

@Composable
fun ScanningAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val dynamic = LocalDynamicColour.current

    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer scanning rings
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

            val primaryColor = MaterialTheme.colorScheme.primary

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

        // Central device icon with gradient background
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primary
                ),
            contentAlignment = Alignment.Center
        ) {
            val iconTint = if (dynamic) MaterialTheme.colorScheme.primaryContainer else Color.White
            Icon(
                painter = painterResource(R.drawable.sendnew),
                contentDescription = "Phone",
                tint = iconTint,
                modifier = Modifier.size(48.dp)
            )
        }

    }
}


@Composable
fun DeviceCard(
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
            .clickable { onClick() },
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
            // Device Icon
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

                Text(
                    text = "${device.platform}${device.batteryLevel?.let { " • $it% Battery" } ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            // Status Indicator
            StatusIndicator(status = device.status)
        }
    }
}

@Composable
fun StatusIndicator(status: DeviceStatus) {
    val dynamic = LocalDynamicColour.current
    val (color, text) = when (status) {
        DeviceStatus.ONLINE -> Color(0xFF4CAF50) to "Online"
        DeviceStatus.AWAY -> Color(0xFFFF9800) to "Away"
        DeviceStatus.CHARGING -> Color(0xFFFF9800) to "Charging"
        DeviceStatus.OFFLINE -> {
            val offlineColor =
                if (dynamic) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f) else Color(
                    0xFF9E9E9E
                )
            offlineColor to "Offline"
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@Composable
fun QRCodeButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(12.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.qrscanner),
            contentDescription = "Scan QR Code",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "Scan QR Code",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}



fun getSampleDevices(): List<Device> {
    return listOf(
        Device(
            id = "1",
            name = "John's iPhone",
            deviceType = DeviceType.PHONE,
            platform = "Android",
            batteryLevel = 98,
            status = DeviceStatus.ONLINE,
            icon = R.drawable.phoneicon
        ),
        Device(
            id = "2",
            name = "Sarah's Tablet",
            deviceType = DeviceType.TABLET,
            platform = "Android",
            batteryLevel = 76,
            status = DeviceStatus.ONLINE,
            icon = R.drawable.tableticon
        ),
        Device(
            id = "3",
            name = "Mike's Laptop",
            deviceType = DeviceType.LAPTOP,
            platform = "Android",
            batteryLevel = null,
            status = DeviceStatus.AWAY,
            icon = R.drawable.laptopicon
        ),
    )
}

@Preview(name = "Light", showBackground = true, showSystemUi = true)
@Composable
fun ScanningReceiverLightPreview() {
    SmartSwitchTheme(useDarkTheme = false, dynamicColour = false) {
        ScanningReceiver(navController = NavHostController(LocalContext.current))
    }
}

@Preview(
    name = "Dark",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    showSystemUi = true
)
@Composable
fun ScanningReceiverDarkPreview() {
    SmartSwitchTheme(useDarkTheme = true, dynamicColour = false) {
        ScanningReceiver(navController = NavHostController(LocalContext.current))
    }
}

@Preview(name = "Dynamic Light", showBackground = true, showSystemUi = false)
@Composable
fun ScanningReceiverDynamicLightPreview() {
    SmartSwitchTheme(useDarkTheme = false, dynamicColour = true) {
        ScanningReceiver(navController = NavHostController(LocalContext.current))
    }
}

@Preview(name = "Dynamic Dark", showBackground = true, showSystemUi = true)
@Composable
fun ScanningReceiverDynamicDarkPreview() {
    SmartSwitchTheme(useDarkTheme = true, dynamicColour = true) {
        ScanningReceiver(navController = NavHostController(LocalContext.current))
    }
}