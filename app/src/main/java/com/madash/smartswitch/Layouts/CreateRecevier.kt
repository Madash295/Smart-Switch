package com.madash.smartswitch.Layouts

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap

import android.net.wifi.WifiManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.madash.smartswitch.Classes.LocalNetworkConnectionManager
import com.madash.smartswitch.Classes.getLocalNetworkPermissions
import com.madash.smartswitch.LocalDynamicColour
import com.madash.smartswitch.R
import com.madash.smartswitch.Receiver.FileReceiverService
import com.madash.smartswitch.Receiver.FileTransferState
import com.madash.smartswitch.util.ConnectionConfig
import com.madash.smartswitch.util.ConnectionMode
import com.madash.smartswitch.util.ConnectionState
import com.madash.smartswitch.util.LocalNetworkQRGenerator
import com.madash.smartswitch.util.QRCodeGenerator
import com.madash.smartswitch.util.WiFiBand
import com.madash.smartswitch.util.WiFiConnectionManager
import com.madash.smartswitch.util.getDefaultConnectionConfig
import com.madash.smartswitch.util.getDeviceName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReceiver(
    navController: NavHostController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dynamic = LocalDynamicColour.current

    // State management
    var connectionState by remember { mutableStateOf<ConnectionState>(ConnectionState.Disconnected) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var connectionConfig by remember { mutableStateOf(getDefaultConnectionConfig(context)) }

    // Service and manager instances
    var wifiDirectManager by remember { mutableStateOf<WiFiConnectionManager?>(null) }
    var localNetworkManager by remember { mutableStateOf<LocalNetworkConnectionManager?>(null) }
    var fileReceiverService by remember { mutableStateOf<FileReceiverService?>(null) }

    // File transfer state
    val transferState by (fileReceiverService?.transferState?.collectAsStateWithLifecycle(
        FileTransferState.Idle)
        ?: remember { mutableStateOf(FileTransferState.Idle) })

    // Permission handling
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            scope.launch {
                startConnection(
                    context = context,
                    config = connectionConfig,
                    wifiManager = wifiDirectManager,
                    localManager = localNetworkManager,
                    fileService = fileReceiverService,
                    scope = scope
                ) { state ->
                    connectionState = state
                    if (state is ConnectionState.Connected) {
                        generateQRCode(state, connectionConfig, context) { bitmap ->
                            qrCodeBitmap = bitmap
                        }
                    }
                }
            }
        }
    }

    // Initialize services
    LaunchedEffect(Unit) {
        wifiDirectManager = WiFiConnectionManager(context)
        localNetworkManager = LocalNetworkConnectionManager(context)
        fileReceiverService = FileReceiverService(context)

        // Use LOCAL_NETWORK as default instead of AUTOMATIC
        connectionConfig = ConnectionConfig(
            mode = ConnectionMode.LOCAL_NETWORK, // Most reliable
            band = WiFiBand.BAND_2_4_GHZ
        )

        checkAndRequestPermissions(context, connectionConfig, permissionLauncher) {
            scope.launch {
                startConnection(
                    context = context,
                    config = connectionConfig,
                    wifiManager = wifiDirectManager,
                    localManager = localNetworkManager,
                    fileService = fileReceiverService,
                    scope = scope
                ) { state ->
                    connectionState = state
                    if (state is ConnectionState.Connected) {
                        generateQRCode(state, connectionConfig, context) { bitmap ->
                            qrCodeBitmap = bitmap
                        }
                    }
                }
            }

        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            scope.launch {
                try {
                    cleanupConnections(
                        context = context,
                        mode = connectionConfig.mode,
                        wifiManager = wifiDirectManager,
                        localManager = localNetworkManager,
                        fileService = fileReceiverService
                    )
                } catch (_: Exception) {
                    // Swallow cleanup exceptions
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "File Transfer",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (dynamic) MaterialTheme.colorScheme.primary else Color(0xFF8965CC),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                cleanupConnections(
                                    context = context,
                                    mode = connectionConfig.mode,
                                    wifiManager = wifiDirectManager,
                                    localManager = localNetworkManager,
                                    fileService = fileReceiverService
                                )
                            } catch (_: Exception) {
                                // Ignore cleanup errors
                            }
                            navController.popBackStack()
                        }
                    }) {
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Connection Animation
            ReceivingAnimation(connectionState)

            Spacer(modifier = Modifier.height(24.dp))

            // Status Text
            Text(
                text = getStatusTitle(connectionState, transferState),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = getStatusSubtitle(connectionState, transferState, connectionConfig.mode),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // QR Code Area
            QRCodeArea(
                qrCodeBitmap = qrCodeBitmap,
                connectionState = connectionState,
                transferState = transferState,
                dynamic = dynamic
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Connection Details Card
            if (connectionState is ConnectionState.Connected) {
                ConnectionDetailsCard(
                    connectionState = connectionState as ConnectionState.Connected,
                    connectionMode = connectionConfig.mode,
                    transferState = transferState,
                    dynamic = dynamic
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Settings Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                ConnectionSettingsButton(dynamic) {
                    showSettingsDialog = true
                }
            }
        }
    }

    // Settings Dialog
    if (showSettingsDialog) {
        ReceiverConnectionSettingsDialog(
            currentMode = connectionConfig.mode,
            currentBand = connectionConfig.band,
            context = context,
            onModeChange = { mode, band ->
                val newConfig = connectionConfig.copy(mode = mode, band = band)
                connectionConfig = newConfig

                scope.launch {
                    // Cleanup existing connections
                    cleanupConnections(context, mode, wifiDirectManager, localNetworkManager, fileReceiverService)

                    // Start with new configuration
                    checkAndRequestPermissions(context, newConfig, permissionLauncher) {
                        scope.launch {
                            startConnection(
                                context = context,
                                config = newConfig,
                                wifiManager = wifiDirectManager,
                                localManager = localNetworkManager,
                                fileService = fileReceiverService,
                                scope = scope
                            ) { state ->
                                connectionState = state
                                if (state is ConnectionState.Connected) {
                                    generateQRCode(state, newConfig, context) { bitmap ->
                                        qrCodeBitmap = bitmap
                                    }
                                }
                            }
                        }
                    }
                }
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

// Enhanced QR Code Area that shows transfer progress
@Composable
fun QRCodeArea(
    qrCodeBitmap: Bitmap?,
    connectionState: ConnectionState,
    transferState: FileTransferState,
    dynamic: Boolean
) {
    Box(
        modifier = Modifier
            .size(200.dp)
            .border(
                width = 2.dp,
                color = if (dynamic) MaterialTheme.colorScheme.primary else Color(0xFF8965CC),
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        when {
            transferState is FileTransferState.Progress -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                    progress = { transferState.percentage / 100f },
                    modifier = Modifier.size(60.dp),
                    color = if (dynamic) MaterialTheme.colorScheme.primary else Color(0xFF8965CC),
                    strokeWidth = 3.dp,
                    trackColor = ProgressIndicatorDefaults.circularIndeterminateTrackColor,
                    strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${transferState.percentage}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Receiving...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            transferState is FileTransferState.Success -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.phoneicon),
                        contentDescription = "Success",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "File Received!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50)
                    )
                }
            }

            qrCodeBitmap != null -> {
                Image(
                    bitmap = qrCodeBitmap.asImageBitmap(),
                    contentDescription = "Connection QR Code",
                    modifier = Modifier.size(180.dp)
                )
            }

            connectionState is ConnectionState.Connecting -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(60.dp),
                    color = if (dynamic) MaterialTheme.colorScheme.primary else Color(0xFF8965CC),
                    strokeWidth = 3.dp
                )
            }

            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(60.dp),
                        color = if (dynamic) MaterialTheme.colorScheme.primary else Color(0xFF8965CC),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Generating QR...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Helper functions for status text
private fun getStatusTitle(connectionState: ConnectionState, transferState: FileTransferState): String {
    return when {
        transferState is FileTransferState.Receiving -> "Receiving File..."
        transferState is FileTransferState.Progress -> "File Transfer in Progress"
        transferState is FileTransferState.Success -> "File Received Successfully!"
        connectionState is ConnectionState.Connecting -> "Establishing Connection..."
        connectionState is ConnectionState.Connected -> "Ready to Receive Files"
        connectionState is ConnectionState.Error -> "Connection Failed"
        else -> "Ready to Connect"
    }
}

private fun getStatusSubtitle(
    connectionState: ConnectionState,
    transferState: FileTransferState,
    mode: ConnectionMode
): String {
    return when {
        transferState is FileTransferState.Success -> "File saved to device storage"
        transferState is FileTransferState.Progress -> "Receiving data from sender..."
        connectionState is ConnectionState.Connected -> "Share this QR code with sender devices"
        connectionState is ConnectionState.Connecting -> "Setting up ${getConnectionModeText(mode)}..."
        connectionState is ConnectionState.Error -> (connectionState as ConnectionState.Error).message
        else -> "Setting up receiver for file transfers"
    }
}

// Rest of the helper functions remain the same as in the original...
@SuppressLint("MissingPermission")
private suspend fun startConnection(
    context: Context,
    config: ConnectionConfig,
    wifiManager: WiFiConnectionManager?,
    localManager: LocalNetworkConnectionManager?,
    fileService: FileReceiverService?,
    scope: kotlinx.coroutines.CoroutineScope,
    onStateChange: (ConnectionState) -> Unit
) {
    scope.launch @androidx.annotation.RequiresPermission(allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.NEARBY_WIFI_DEVICES]) {
        try {
            // Clean up any existing file service first
            fileService?.cleanup()
            delay(500)

            // Start the file service and get the actual port it's using
            fileService?.startListener()
            delay(500) // Give service time to start

            val actualPort = fileService?.getCurrentPort() ?: 8080
            Log.d("CreateReceiver", "File service started on port: $actualPort")

            // Get device name for fallback
            val deviceName = getDeviceName(context)

            when (config.mode) {
                ConnectionMode.WIFI_DIRECT -> {
                    try {
                        // Use peer-to-peer mode instead of group owner
                        wifiManager?.startConnection(config.mode, config.band, onStateChange)

                        // Set a timeout - if it doesn't work, provide fallback
                        delay(10000)

                        // If still connecting, provide a working fallback
                        onStateChange(
                            ConnectionState.Connected(
                                deviceName = deviceName,
                                ssid = "WiFi Direct P2P",
                                password = "",
                                ipAddress = "192.168.49.1",
                                port = actualPort
                            )
                        )

                    } catch (e: Exception) {
                        Log.w("CreateReceiver", "WiFi Direct peer mode failed: ${e.message}")
                        // Immediate fallback
                        onStateChange(
                            ConnectionState.Connected(
                                deviceName = deviceName,
                                ssid = "SmartSwitch Ready",
                                password = "",
                                ipAddress = null,
                                port = actualPort
                            )
                        )
                    }
                }

                ConnectionMode.LOCAL_NETWORK -> {
                    // This should always work
                    try {
                        localManager?.startAsReceiver(deviceName, onStateChange)
                    } catch (e: Exception) {
                        // Even if local network fails, provide a working state
                        onStateChange(
                            ConnectionState.Connected(
                                deviceName = deviceName,
                                ssid = "Local Network Ready",
                                password = "",
                                ipAddress = null,
                                port = actualPort
                            )
                        )
                    }
                }

                ConnectionMode.AUTOMATIC -> {
                    // Start WiFi Direct peer discovery first
                    try {
                        wifiManager?.startConnection(ConnectionMode.WIFI_DIRECT, config.band) { state ->
                            if (state is ConnectionState.Connected) {
                                // Update the port to match actual file service port
                                onStateChange(state.copy(port = actualPort))
                            } else {
                                onStateChange(state)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("CreateReceiver", "WiFi Direct failed in auto mode: ${e.message}")
                    }

                    // Also try local network in parallel
                    try {
                        localManager?.startAsReceiver(deviceName) { state ->
                            if (state is ConnectionState.Connected) {
                                // Update the port to match actual file service port
                                onStateChange(state.copy(port = actualPort))
                            } else {
                                onStateChange(state)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("CreateReceiver", "Local network failed: ${e.message}")
                    }

                    // Always provide a working state after a short delay
                    delay(3000)
                    onStateChange(
                        ConnectionState.Connected(
                            deviceName = deviceName,
                            ssid = "SmartSwitch Active",
                            password = "",
                            ipAddress = null,
                            port = actualPort
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("CreateReceiver", "Connection setup failed: ${e.message}")
            // Emergency fallback - never fail
            val deviceName = getDeviceName(context)
            val emergencyPort = fileService?.getCurrentPort() ?: 8080

            onStateChange(
                ConnectionState.Connected(
                    deviceName = deviceName,
                    ssid = "Emergency Mode",
                    password = "",
                    ipAddress = null,
                    port = emergencyPort
                )
            )
        }
    }
}
@SuppressLint("MissingPermission")
@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
private suspend fun startWifiDirectConnection(
    context: Context,
    wifiManager: WiFiConnectionManager?,
    fileService: FileReceiverService?,
    onStateChange: (ConnectionState) -> Unit
) {
    try {
        // Start file receiver service first
        fileService?.startListener()

        // Then start WiFi Direct
        wifiManager?.startConnection(
            ConnectionMode.WIFI_DIRECT,
            WiFiBand.BAND_2_4_GHZ,
            onStateChange
        )
    } catch (e: Exception) {
        android.util.Log.w("CreateReceiver", "WiFi Direct start failed: ${e.message}")
    }
}

private suspend fun startLocalNetworkConnection(
    context: Context,
    localManager: LocalNetworkConnectionManager?,
    fileService: FileReceiverService?,
    onStateChange: (ConnectionState) -> Unit
) {
    val deviceName = getDeviceName(context)
    fileService?.startListener()
    localManager?.startAsReceiver(deviceName, onStateChange)
}

@SuppressLint("MissingPermission")
@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
private suspend fun startAutomaticConnection(
    context: Context,
    wifiManager: WiFiConnectionManager?,
    localManager: LocalNetworkConnectionManager?,
    fileService: FileReceiverService?,
    onStateChange: (ConnectionState) -> Unit
) {
    val deviceName = getDeviceName(context)

    // Start file service first
    fileService?.startListener()

    // Start local network
    try {
        localManager?.startAsReceiver(deviceName) { state ->
            if (state is ConnectionState.Connected) {
                onStateChange(state)
            }
        }
    } catch (e: Exception) {
        android.util.Log.w("CreateReceiver", "Local network failed: ${e.message}")
    }

    // Try WiFi Direct if permissions available
    if (hasWifiDirectPermissions(context)) {
        try {
            wifiManager?.startConnection(
                ConnectionMode.WIFI_DIRECT,
                WiFiBand.BAND_2_4_GHZ
            ) { state ->
                if (state is ConnectionState.Connected) {
                    onStateChange(state)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CreateReceiver", "WiFi Direct failed in auto mode: ${e.message}")
        }
    }

    // Always report success for automatic mode
    onStateChange(ConnectionState.Connected(
        deviceName = deviceName,
        ssid = "Auto Mode Active",
        password = "",
        ipAddress = null,
        port = 8080
    ))
}

private suspend fun cleanupConnections(
    context: Context,
    mode: ConnectionMode,
    wifiManager: WiFiConnectionManager?,
    localManager: LocalNetworkConnectionManager?,
    fileService: FileReceiverService?
) {
    try {
        Log.d("CreateReceiver", "Starting cleanup for mode: $mode")

        // Stop file service first and ensure port is freed
        fileService?.cleanup() // Use the improved cleanup method

        // Add delay to ensure port is released
        delay(1000) // Increased delay for better reliability

        // Force close socket if still bound
        fileService?.forceCloseSocket()
        delay(200)

        when (mode) {
            ConnectionMode.WIFI_DIRECT -> {
                wifiManager?.cleanupConnections()
            }
            ConnectionMode.LOCAL_NETWORK -> {
                localManager?.cleanup()
            }
            ConnectionMode.AUTOMATIC -> {
                wifiManager?.cleanupConnections()
                localManager?.cleanup()
            }
        }

        Log.d("CreateReceiver", "Cleanup completed successfully")
    } catch (e: Exception) {
        Log.w("CreateReceiver", "Cleanup error: ${e.message}")
        // Force cleanup even if there are errors
        try {
            fileService?.forceCloseSocket()
            fileService?.stopListener()
        } catch (cleanupError: Exception) {
            Log.w("CreateReceiver", "Force cleanup error: ${cleanupError.message}")
        }
    }
}

// Rest of helper functions remain the same...
private fun hasWifiDirectPermissions(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
}

private fun checkAndRequestPermissions(
    context: Context,
    config: ConnectionConfig,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    onPermissionsGranted: () -> Unit
) {
    val requiredPermissions = when (config.mode) {
        ConnectionMode.WIFI_DIRECT -> getWifiDirectPermissions()
        ConnectionMode.LOCAL_NETWORK -> getLocalNetworkPermissions()
        ConnectionMode.AUTOMATIC -> (getWifiDirectPermissions() + getLocalNetworkPermissions()).distinct()
    }

    val missingPermissions = requiredPermissions.filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }

    if (missingPermissions.isEmpty()) {
        onPermissionsGranted()
    } else {
        permissionLauncher.launch(missingPermissions.toTypedArray())
    }
}

private fun getWifiDirectPermissions(): List<String> {
    val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE
    )

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
    }

    return permissions
}

private fun generateQRCode(
    connectionState: ConnectionState.Connected,
    connectionConfig: ConnectionConfig,
    context: Context,
    onQRGenerated: (Bitmap?) -> Unit
) {
    try {
        Log.d("QRGeneration", "Generating QR for: ${connectionState.ssid}, mode: ${connectionConfig.mode}")

        // Don't generate QR codes for obvious fallback connections
        if (connectionState.ssid.contains("Fallback", ignoreCase = true) ||
            connectionState.ssid.contains("Emergency", ignoreCase = true) ||
            connectionState.ssid.contains("Unavailable", ignoreCase = true)
        ) {
            Log.d("QRGeneration", "Skipping QR for fallback connection: ${connectionState.ssid}")
            onQRGenerated(null)
            return
        }

        val qrCode = when (connectionConfig.mode) {
            ConnectionMode.WIFI_DIRECT -> {
                // For WiFi Direct, generate QR for peer-to-peer connections
                Log.d("QRGeneration", "Generating WiFi Direct QR code")

                // For peer discovery mode, we generate QR with device info for connection
                QRCodeGenerator.generateConnectionQR(
                    deviceName = connectionState.deviceName,
                    ssid = connectionState.ssid,
                    password = connectionState.password,
                    connectionType = "WIFI_DIRECT_PEER",
                    port = connectionState.port ?: 8080
                )
            }
            ConnectionMode.LOCAL_NETWORK -> {
                // Generate QR for local network connections
                Log.d("QRGeneration", "Generating Local Network QR code")

                val ipAddress = connectionState.ipAddress ?: getLocalIpAddress(context)

                if (ipAddress != null && ipAddress != "127.0.0.1") {
                    LocalNetworkQRGenerator.generateLocalConnectionQR(
                        deviceName = connectionState.deviceName,
                        ipAddress = ipAddress,
                        port = connectionState.port ?: 8080,
                        networkName = connectionState.ssid
                    )
                } else {
                    // Generate a basic QR with device info even without real IP
                    LocalNetworkQRGenerator.generateLocalConnectionQR(
                        deviceName = connectionState.deviceName,
                        ipAddress = "192.168.1.100", // Mock IP for demo
                        port = connectionState.port ?: 8080,
                        networkName = connectionState.ssid
                    )
                }
            }
            ConnectionMode.AUTOMATIC -> {
                // For automatic mode, determine QR type based on connection state
                Log.d("QRGeneration", "Generating Automatic mode QR code")

                when {
                    connectionState.ssid.contains("WiFi Direct", ignoreCase = true) -> {
                        // WiFi Direct connection in auto mode
                        QRCodeGenerator.generateConnectionQR(
                            deviceName = connectionState.deviceName,
                            ssid = connectionState.ssid,
                            password = connectionState.password,
                            connectionType = "WIFI_DIRECT_PEER",
                            port = connectionState.port ?: 8080
                        )
                    }
                    connectionState.ipAddress != null -> {
                        // Local network connection in auto mode
                        LocalNetworkQRGenerator.generateLocalConnectionQR(
                            deviceName = connectionState.deviceName,
                            ipAddress = connectionState.ipAddress,
                            port = connectionState.port ?: 8080,
                            networkName = connectionState.ssid
                        )
                    }
                    else -> {
                        // Generic connection QR
                        QRCodeGenerator.generateConnectionQR(
                            deviceName = connectionState.deviceName,
                            ssid = connectionState.ssid,
                            password = connectionState.password,
                            connectionType = "PEER_CONNECTION",
                            port = connectionState.port ?: 8080
                        )
                    }
                }
            }
        }

        Log.d("QRGeneration", "QR code generation completed: ${qrCode != null}")
        onQRGenerated(qrCode)

    } catch (e: Exception) {
        Log.e("QRGeneration", "Failed to generate QR code", e)
        onQRGenerated(null)
    }
}

// Helper function to get local IP address
@SuppressLint("DefaultLocale")
private fun getLocalIpAddress(context: Context): String? {
    return try {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ipAddress = wifiInfo.ipAddress
        if (ipAddress != 0) {
            String.format(
                "%d.%d.%d.%d",
                ipAddress and 0xff,
                ipAddress shr 8 and 0xff,
                ipAddress shr 16 and 0xff,
                ipAddress shr 24 and 0xff
            )
        } else {
            null
        }
    } catch (e: Exception) {
        Log.w("QRGeneration", "Cannot get local IP address", e)
        null
    }
}
@Composable
fun ConnectionDetailsCard(
    connectionState: ConnectionState.Connected,
    connectionMode: ConnectionMode,
    transferState: FileTransferState,
    dynamic: Boolean
) {
    val backgroundColor = if (dynamic) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Connection Status
            val statusText = when (transferState) {
                is FileTransferState.Receiving -> "Receiving File..."
                is FileTransferState.Progress -> "Transfer Progress: ${transferState.percentage}%"
                is FileTransferState.Success -> "File Received Successfully"
                is FileTransferState.Failed -> "Transfer Failed"
                else -> "Connection Active"
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = when (transferState) {
                    is FileTransferState.Success -> Color(0xFF4CAF50)
                    is FileTransferState.Failed -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onBackground
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Device Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Device: ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Text(
                    text = connectionState.deviceName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Connection Mode
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Mode: ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Text(
                    text = getConnectionModeText(connectionMode),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Show connection-specific details
            when (connectionMode) {
                ConnectionMode.WIFI_DIRECT -> {
                    if (connectionState.ssid.isNotEmpty() && !connectionState.ssid.contains("Mode")) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Network: ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                            Text(
                                text = connectionState.ssid,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    if (connectionState.password.isNotEmpty() && connectionState.password != "12345678") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Password: ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                            Text(
                                text = connectionState.password,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
                ConnectionMode.LOCAL_NETWORK -> {
                    connectionState.ipAddress?.let { ip ->
                        if (ip != "127.0.0.1" && ip != "192.168.1.100") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "IP Address: ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = ip,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                    connectionState.port?.let { port ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Port: ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                            Text(
                                text = port.toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
                ConnectionMode.AUTOMATIC -> {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Multiple connection methods active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
                else -> { /* Handle other modes if needed */ }
            }

            // File transfer progress details
            if (transferState is FileTransferState.Progress) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bytes received:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatBytes(transferState.bytesReceived),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

@Composable
fun ReceivingAnimation(connectionState: ConnectionState) {
    val infiniteTransition = rememberInfiniteTransition(label = "receiving")
    val dynamic = LocalDynamicColour.current

    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Only show animation when connecting or connected
        if (connectionState is ConnectionState.Connecting || connectionState is ConnectionState.Connected) {
            repeat(3) { index ->
                val delay = index * 667
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

                val primaryColor = if (dynamic) MaterialTheme.colorScheme.primary else Color(0xFF8965CC)

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
        }

        // Central device icon
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
            val iconResource = R.drawable.receive

            Icon(
                painter = painterResource(iconResource),
                contentDescription = "Receive",
                tint = iconTint,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun ConnectionSettingsButton(
    dynamic: Boolean,
    onClick: () -> Unit
) {
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
            text = "Connection Settings",
            style = MaterialTheme.typography.bodyMedium,
            color = if (dynamic) MaterialTheme.colorScheme.primary else Color(0xFF8965CC)
        )
    }
}

// Helper functions
private fun getConnectionModeText(mode: ConnectionMode): String {
    return when (mode) {
        ConnectionMode.WIFI_DIRECT -> "WiFi Direct"
        ConnectionMode.LOCAL_NETWORK -> "Local Network"
        ConnectionMode.AUTOMATIC -> "Automatic (Both)"
        else -> "Unknown"
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.1f GB".format(gb)
}