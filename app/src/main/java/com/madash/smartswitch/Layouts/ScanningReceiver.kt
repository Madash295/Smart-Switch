package com.madash.smartswitch.Layouts

import android.content.ContentUris
import android.net.Uri
import android.util.Log
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
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.madash.smartswitch.DataClass.DeviceStatus
import com.madash.smartswitch.LocalDynamicColour
import com.madash.smartswitch.R
import com.madash.smartswitch.Routes
import com.madash.smartswitch.SmartSwitchTheme
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.madash.smartswitch.Classes.LocalDeviceInfo
import com.madash.smartswitch.Classes.LocalNetworkConnectionManager
import com.madash.smartswitch.Sender.FileSenderService
import com.madash.smartswitch.Sender.FileSenderState

import com.madash.smartswitch.util.WiFiConnectionManager
import com.madash.smartswitch.util.ConnectionMode
import com.madash.smartswitch.util.WiFiBand
import com.madash.smartswitch.util.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import android.net.wifi.p2p.WifiP2pDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanningReceiver(
    navController: NavHostController,
    mediaCount: Int? = null,
    appCount: Int? = null,
    contactCount: Int? = null,
    fileCount: Int? = null,
    qrScanResult: String? = null // Add this parameter
) {
    // Replace the existing devices line with these new state variables:
    var discoveredDevices by remember { mutableStateOf<List<LocalDeviceInfo>>(emptyList()) }

    val totalCount = (mediaCount ?: 0) + (appCount ?: 0) + (contactCount ?: 0) + (fileCount ?: 0)

    // File transfer state
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Services
    var localNetworkManager by remember { mutableStateOf<LocalNetworkConnectionManager?>(null) }
    var wifiDirectManager by remember { mutableStateOf<WiFiConnectionManager?>(null) }
    var fileSenderService by remember { mutableStateOf<FileSenderService?>(null) }

    // Discovery state
    var isDiscovering by remember { mutableStateOf(false) }
    var discoveryError by remember { mutableStateOf<String?>(null) }

    // Transfer state
    var isTransferring by remember { mutableStateOf(false) }
    var transferProgress by remember { mutableStateOf(0) }
    var currentFileName by remember { mutableStateOf("") }
    var transferResult by remember { mutableStateOf<String?>(null) }
    var showTransferDialog by remember { mutableStateOf(false) }

    // Initialize services
    LaunchedEffect(Unit) {
        localNetworkManager = LocalNetworkConnectionManager(context)
        wifiDirectManager = WiFiConnectionManager(context)
        fileSenderService = FileSenderService(context)

        // Start device discovery for both local network and WiFi Direct
        startDeviceDiscovery(
            localNetworkManager = localNetworkManager,
            wifiDirectManager = wifiDirectManager,
            onDiscovering = { isDiscovering = it },
            onDevicesFound = { devices ->
                discoveredDevices = devices
                Log.d("DeviceDiscovery", "Found ${devices.size} devices")
            },
            onError = { error ->
                discoveryError = error
                Log.e("DeviceDiscovery", "Discovery error: $error")
            }
        )
    }

    // Cleanup WiFi Direct callback when component is disposed
    androidx.compose.runtime.DisposableEffect(wifiDirectManager) {
        onDispose {
            wifiDirectManager?.setOnPeersFoundCallback(null)
        }
    }

    // Handle QR scan result
    LaunchedEffect(qrScanResult) {
        if (qrScanResult != null) {
            handleQRScanResult(
                qrData = qrScanResult,
                fileSenderService = fileSenderService,
                context = context,
                mediaCount = mediaCount,
                appCount = appCount,
                contactCount = contactCount,
                fileCount = fileCount,
                scope = scope,
                onShowDialog = { showTransferDialog = true }
            )
        }
    }

    // Collect transfer state
    val transferState by (fileSenderService?.transferState?.collectAsStateWithLifecycle(
        FileSenderState.Idle)
        ?: remember { mutableStateOf(FileSenderState.Idle) })

    // Handle transfer state changes
    LaunchedEffect(transferState) {
        when (transferState) {
            is FileSenderState.Connecting -> {
                isTransferring = true
                transferProgress = 0
                currentFileName = "Connecting to receiver..."
                showTransferDialog = true
            }
            is FileSenderState.Connected -> {
                currentFileName = "Connected! Preparing files..."
            }
            is FileSenderState.Transferring -> {
                currentFileName = "Sending ${(transferState as FileSenderState.Transferring).fileName} (${(transferState as FileSenderState.Transferring).fileIndex}/${(transferState as FileSenderState.Transferring).totalFiles})"
                transferProgress = (transferState as FileSenderState.Transferring).progress
            }
            is FileSenderState.Success -> {
                isTransferring = false
                transferResult = "Successfully sent ${(transferState as FileSenderState.Success).filesSent} files!"
                currentFileName = "Transfer Complete"
                transferProgress = 100
            }
            is FileSenderState.PartialSuccess -> {
                isTransferring = false
                transferResult = "Sent ${(transferState as FileSenderState.PartialSuccess).filesSent}/${(transferState as FileSenderState.PartialSuccess).totalFiles} files"
                currentFileName = "Transfer Partially Complete"
            }
            is FileSenderState.Failed -> {
                isTransferring = false
                transferResult = "Transfer failed: ${(transferState as FileSenderState.Failed).error}"
                currentFileName = "Transfer Failed"
            }
            is FileSenderState.Stopped -> {
                isTransferring = false
                transferResult = "Transfer cancelled"
                currentFileName = "Transfer Stopped"
            }
            else -> { /* Handle other states */ }
        }
    }

    // Your existing Scaffold code remains exactly the same, but replace the LazyColumn items with:
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
                text = if (isDiscovering) "Scanning Devices..." else "Found ${discoveredDevices.size} Receivers",
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

                // Add discovery indicator
                if (isDiscovering) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Device List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Show discovered devices first
                items(discoveredDevices) { deviceInfo ->
                    DiscoveredDeviceCard(
                        deviceInfo = deviceInfo,
                        onClick = {
                            scope.launch {
                                startTransferToDevice(
                                    fileSenderService = fileSenderService,
                                    targetIP = deviceInfo.ipAddress,
                                    targetPort = deviceInfo.port,
                                    deviceName = deviceInfo.name,
                                    context = context,
                                    mediaCount = mediaCount,
                                    appCount = appCount,
                                    contactCount = contactCount,
                                    fileCount = fileCount,
                                    onShowDialog = { showTransferDialog = true }
                                )
                            }
                        }
                    )
                }

                // Show error message if discovery failed
                if (discoveryError != null && discoveredDevices.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = "Discovery Error: $discoveryError",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Show message when no devices found but no error
                if (discoveryError == null && discoveredDevices.isEmpty() && !isDiscovering) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No receivers found",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Make sure the receiving device is running SmartSwitch and connected to the same network, or use QR code scanning.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // QR Code Button - Keep exactly as is
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                QRCodeButton {
                    navController.navigate(Routes.ScanQr.route)
                }
            }
        }
    }

    // Transfer Progress Dialog
    if (showTransferDialog) {
        TransferProgressDialog(
            isTransferring = isTransferring,
            fileName = currentFileName,
            progress = transferProgress,
            result = transferResult,
            onDismiss = {
                showTransferDialog = false
                transferResult = null
                if (isTransferring) {
                    fileSenderService?.stopTransfer()
                }
            }
        )
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



@Composable
fun DiscoveredDeviceCard(
    deviceInfo: LocalDeviceInfo,
    onClick: () -> Unit
) {
    val dynamic = LocalDynamicColour.current
    val backgroundColor = if (dynamic) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
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
            // Real device icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF4CAF50)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.phoneicon),
                    contentDescription = deviceInfo.name,
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
                    text = deviceInfo.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "${deviceInfo.ipAddress}:${deviceInfo.port}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            // Online status for discovered devices
            StatusIndicator(status = DeviceStatus.ONLINE)
        }
    }
}

@Composable
fun TransferProgressDialog(
    isTransferring: Boolean,
    fileName: String,
    progress: Int,
    result: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isTransferring) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isTransferring) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isTransferring) "Transferring Files" else "Transfer Complete",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (isTransferring) {
                    LinearProgressIndicator(
                        progress = progress / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    Text(
                        text = "$progress%",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (result != null) {
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (result.contains("Successfully") || result.contains("Complete"))
                            Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            if (!isTransferring) {
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            }
        },
        dismissButton = {
            if (isTransferring) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

// Helper functions
private fun startDeviceDiscovery(
    localNetworkManager: LocalNetworkConnectionManager?,
    wifiDirectManager: WiFiConnectionManager?,
    onDiscovering: (Boolean) -> Unit,
    onDevicesFound: (List<LocalDeviceInfo>) -> Unit,
    onError: (String) -> Unit
) {
    CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        try {
            onDiscovering(true)
            var discoveredDevices = mutableListOf<LocalDeviceInfo>()

            // Discover local network devices
            localNetworkManager?.discoverDevices(
                onDevicesFound = { devices ->
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        discoveredDevices.addAll(devices)
                        onDevicesFound(discoveredDevices.toList())
                    }
                },
                onError = { error ->
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        Log.w("DeviceDiscovery", "Local network discovery error: $error")
                        // Don't treat local network errors as fatal - WiFi Direct might still work
                    }
                }
            )

            // Start WiFi Direct peer discovery
            try {
                // Set up callback to receive discovered peers
                wifiDirectManager?.setOnPeersFoundCallback { peers ->
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        // Convert WiFi Direct peers to LocalDeviceInfo
                        val wifiDirectDevices = peers.map { peer ->
                            LocalDeviceInfo(
                                name = peer.deviceName.ifEmpty { "WiFi Direct Device" },
                                ipAddress = "192.168.49.1", // WiFi Direct group owner IP
                                port = 8080,
                                timestamp = System.currentTimeMillis()
                            )
                        }

                        // Add WiFi Direct devices to discovered list (avoid duplicates)
                        wifiDirectDevices.forEach { device ->
                            if (discoveredDevices.none { it.name == device.name }) {
                                discoveredDevices.add(device)
                            }
                        }

                        onDevicesFound(discoveredDevices.toList())
                        Log.d(
                            "DeviceDiscovery",
                            "Added ${wifiDirectDevices.size} WiFi Direct peers"
                        )
                    }
                }

                wifiDirectManager?.startConnection(
                    mode = ConnectionMode.WIFI_DIRECT,
                    band = WiFiBand.BAND_2_4_GHZ,
                    onStateChange = { state ->
                        when (state) {
                            is ConnectionState.Connected -> {
                                Log.d("DeviceDiscovery", "WiFi Direct ready: ${state.deviceName}")
                                // The peer discovery callback will handle adding discovered devices
                            }

                            is ConnectionState.Error -> {
                                Log.w(
                                    "DeviceDiscovery",
                                    "WiFi Direct discovery error: ${state.message}"
                                )
                                // Don't treat WiFi Direct errors as fatal - local network might still work
                            }

                            else -> {
                                // Handle other states as needed
                            }
                        }
                    }
                )
            } catch (e: SecurityException) {
                Log.w("DeviceDiscovery", "WiFi Direct permissions not granted: ${e.message}")
                // Continue with local network only
            } catch (e: Exception) {
                Log.w("DeviceDiscovery", "WiFi Direct discovery failed: ${e.message}")
                // Continue with local network only
            }

            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                onDiscovering(false)
            }

        } catch (e: Exception) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                onError(e.message ?: "Unknown error")
                onDiscovering(false)
            }
        }
    }
}

private suspend fun handleQRScanResult(
    qrData: String,
    fileSenderService: FileSenderService?,
    context: android.content.Context,
    mediaCount: Int?,
    appCount: Int?,
    contactCount: Int?,
    fileCount: Int?,
    scope: CoroutineScope,
    onShowDialog: () -> Unit
) {
    try {
        Log.d("QRScan", "Processing QR data: $qrData")

        // Try parsing as WiFi Direct connection
        val wifiDirectInfo = com.madash.smartswitch.util.QRCodeGenerator.parseConnectionData(qrData)
        if (wifiDirectInfo != null) {
            Log.d("QRScan", "Found WiFi Direct connection: ${wifiDirectInfo.deviceName}")
            startTransferToDevice(
                fileSenderService = fileSenderService,
                targetIP = "192.168.49.1",
                targetPort = wifiDirectInfo.port,
                deviceName = wifiDirectInfo.deviceName,
                context = context,
                mediaCount = mediaCount,
                appCount = appCount,
                contactCount = contactCount,
                fileCount = fileCount,
                onShowDialog = onShowDialog
            )
            return
        }

        // Try parsing as Local Network connection
        val localInfo = com.madash.smartswitch.util.LocalNetworkQRGenerator.parseLocalConnectionData(qrData)
        if (localInfo != null) {
            Log.d("QRScan", "Found Local Network connection: ${localInfo.deviceName} at ${localInfo.ipAddress}")
            startTransferToDevice(
                fileSenderService = fileSenderService,
                targetIP = localInfo.ipAddress,
                targetPort = localInfo.port,
                deviceName = localInfo.deviceName,
                context = context,
                mediaCount = mediaCount,
                appCount = appCount,
                contactCount = contactCount,
                fileCount = fileCount,
                onShowDialog = onShowDialog
            )
            return
        }

        Log.w("QRScan", "Unknown QR code format")

    } catch (e: Exception) {
        Log.e("QRScan", "Error processing QR scan result", e)
    }
}

private suspend fun startTransferToDevice(
    fileSenderService: FileSenderService?,
    targetIP: String,
    targetPort: Int,
    deviceName: String,
    context: android.content.Context,
    mediaCount: Int?,
    appCount: Int?,
    contactCount: Int?,
    fileCount: Int?,
    onShowDialog: () -> Unit
) {
    try {
        val fileUris = createSampleFileUris(context, mediaCount, appCount, contactCount, fileCount)

        if (fileUris.isEmpty()) {
            Log.w("FileTransfer", "No files to transfer")
            return
        }

        Log.d("FileTransfer", "Starting transfer to $deviceName at $targetIP:$targetPort with ${fileUris.size} files")

        onShowDialog()

        fileSenderService?.sendFiles(
            targetIP = targetIP,
            targetPort = targetPort,
            fileUris = fileUris,
            onProgress = { progress, fileName ->
                Log.d("FileTransfer", "Progress: $progress% - $fileName")
            },
            onComplete = { success, message ->
                Log.d("FileTransfer", "Transfer completed: success=$success, message=$message")
            }
        )

    } catch (e: Exception) {
        Log.e("FileTransfer", "Error starting transfer to $deviceName", e)
    }
}

private fun createSampleFileUris(
    context: android.content.Context,
    mediaCount: Int?,
    appCount: Int?,
    contactCount: Int?,
    fileCount: Int?
): List<Uri> {
    val uris = mutableListOf<Uri>()

    try {
        if (mediaCount != null && mediaCount > 0) {
            val cursor = context.contentResolver.query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(android.provider.MediaStore.Images.Media._ID),
                null, null,
                "${android.provider.MediaStore.Images.Media.DATE_ADDED} DESC LIMIT $mediaCount"
            )
            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media._ID)
                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val uri = ContentUris.withAppendedId(
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                    )
                    uris.add(uri)
                }
            }
        }

        if (uris.isEmpty() && (mediaCount ?: 0) > 0) {
            repeat(mediaCount ?: 1) { index ->
                val mockUri = Uri.parse("content://demo/file$index")
                uris.add(mockUri)
            }
        }

        Log.d("FileTransfer", "Created ${uris.size} sample URIs for transfer")

    } catch (e: Exception) {
        Log.e("FileTransfer", "Error creating sample URIs", e)
    }

    return uris
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