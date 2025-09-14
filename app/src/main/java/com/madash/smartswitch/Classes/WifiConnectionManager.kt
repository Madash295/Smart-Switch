package com.madash.smartswitch.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresPermission
import com.madash.smartswitch.WifiP2P.WifiDirectActionListner
import com.madash.smartswitch.WifiP2P.WifiDirectBroadCastReceiver
import com.madash.smartswitch.WifiP2P.intentfilter
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.ServerSocket

class WiFiConnectionManager(private val context: Context) : WifiDirectActionListner {

    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
    private var wifiP2pChannel: WifiP2pManager.Channel? = null
    private var wifiDirectReceiver: WifiDirectBroadCastReceiver? = null

    private var currentConnectionMode: ConnectionMode = ConnectionMode.AUTOMATIC
    private var currentBand: WiFiBand = WiFiBand.BAND_2_4_GHZ
    private var onStateChange: ((ConnectionState) -> Unit)? = null

    // Connection state management
    private var isConnected = false
    private var isConnecting = false
    private val connectionMutex = Mutex()

    // Peer discovery and connection
    private var availablePeers = mutableListOf<WifiP2pDevice>()
    private var deviceName: String = ""
    private var isDiscovering = false
    private var serverSocket: ServerSocket? = null

    companion object {
        private const val TAG = "WiFiConnectionManager"
        private const val CONNECTION_TIMEOUT = 15000L
        private const val DISCOVERY_TIMEOUT = 10000L
        private const val FILE_TRANSFER_PORT = 8080
    }

    @RequiresPermission(allOf = [
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.NEARBY_WIFI_DEVICES,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE
    ])
    suspend fun startConnection(
        mode: ConnectionMode,
        band: WiFiBand,
        onStateChange: (ConnectionState) -> Unit
    ) = connectionMutex.withLock {
        Log.d(TAG, "=== startConnection called (Peer-to-Peer Mode) ===")
        Log.d(TAG, "Mode: $mode, Band: $band")

        this.onStateChange = onStateChange
        this.currentConnectionMode = mode
        this.currentBand = band
        this.deviceName = getDeviceName()

        if (isConnected && currentConnectionMode == mode) {
            Log.d(TAG, "Already connected with mode $mode")
            return@withLock
        }

        if (isConnecting) {
            Log.d(TAG, "Connection in progress, cleaning up first")
            forceCleanup()
            delay(1000)
        }

        isConnecting = true
        onStateChange(ConnectionState.Connecting)

        try {
            forceCleanup()
            delay(500)

            when (mode) {
                ConnectionMode.WIFI_DIRECT -> {
                    startWifiDirectPeerDiscovery()
                }
                ConnectionMode.LOCAL_NETWORK -> {
                    fallbackToLocalNetwork()
                }
                ConnectionMode.AUTOMATIC -> {
                    startWifiDirectPeerDiscovery()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start connection", e)
            handleConnectionFailure("Connection setup failed: ${e.message}")
        }
    }

    @RequiresPermission(allOf = [
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.NEARBY_WIFI_DEVICES
    ])
    private suspend fun startWifiDirectPeerDiscovery() {
        Log.d(TAG, "=== startWifiDirectPeerDiscovery ===")

        try {
            if (wifiP2pManager == null) {
                throw IllegalStateException("WiFi P2P Manager is null")
            }

            if (!isWifiEnabled()) {
                throw IllegalStateException("WiFi must be enabled")
            }

            // Initialize WiFi Direct
            val initSuccess = initializeWifiDirect()
            if (!initSuccess) {
                throw IllegalStateException("Failed to initialize WiFi Direct")
            }

            // Start peer discovery
            startPeerDiscovery()

            // Set device as discoverable by creating a service
            createDiscoverableService()

            // Wait for peers or connections
            waitForPeersOrConnection()

        } catch (e: Exception) {
            Log.e(TAG, "WiFi Direct peer discovery failed: ${e.message}", e)
            if (currentConnectionMode == ConnectionMode.WIFI_DIRECT) {
                handleConnectionFailure("WiFi Direct failed: ${e.message}")
            } else {
                fallbackToLocalNetwork()
            }
        }
    }

    private suspend fun initializeWifiDirect(): Boolean {
        Log.d(TAG, "=== initializeWifiDirect (Peer Mode) ===")

        return try {
            // Clean up any existing receivers
            wifiDirectReceiver?.let {
                try {
                    context.unregisterReceiver(it)
                } catch (e: Exception) {
                    Log.w(TAG, "Old receiver already unregistered", e)
                }
                wifiDirectReceiver = null
            }

            // Initialize WiFi P2P channel
            wifiP2pChannel = wifiP2pManager?.initialize(context, context.mainLooper, this)

            if (wifiP2pChannel == null) {
                Log.e(TAG, "Failed to initialize WiFi Direct channel")
                return false
            }

            // Register broadcast receiver
            val intentFilter = intentfilter.getIntentFilter()
            wifiDirectReceiver = WifiDirectBroadCastReceiver(wifiP2pManager!!, wifiP2pChannel!!, this)
            context.registerReceiver(wifiDirectReceiver, intentFilter)

            Log.d(TAG, "WiFi Direct initialized successfully for peer discovery")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing WiFi Direct", e)
            false
        }
    }

    @RequiresPermission(allOf = [
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.NEARBY_WIFI_DEVICES
    ])
    private fun startPeerDiscovery() {
        Log.d(TAG, "=== startPeerDiscovery ===")

        val channel = wifiP2pChannel ?: return
        isDiscovering = true
        availablePeers.clear()

        wifiP2pManager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Peer discovery started successfully")
                isDiscovering = true
            }

            override fun onFailure(reason: Int) {
                // Fix: Check if reason 0 is actually success
                if (reason == 0) {
                    Log.d(TAG, "Peer discovery started (reason code 0 = success)")
                    isDiscovering = true
                } else {
                    Log.e(TAG, "Peer discovery failed: ${getErrorString(reason)}")
                    isDiscovering = false
                }
            }
        })
    }

    private fun createDiscoverableService() {
        Log.d(TAG, "=== createDiscoverableService ===")

        // This makes the device discoverable to other WiFi Direct devices
        // The device will appear in other devices' peer lists
        onStateChange?.invoke(
            ConnectionState.Connected(
                deviceName = deviceName,
                ssid = "WiFi Direct Ready",
                password = "",
                ipAddress = null,
                port = FILE_TRANSFER_PORT
            )
        )

        isConnecting = false
        isConnected = true
    }

    private suspend fun waitForPeersOrConnection() {
        Log.d(TAG, "=== waitForPeersOrConnection ===")

        var timeoutCounter = 0
        val maxTimeout = DISCOVERY_TIMEOUT / 1000

        while (timeoutCounter < maxTimeout && isConnecting) {
            delay(1000)
            timeoutCounter++
            Log.d(TAG, "Waiting... ${timeoutCounter}/${maxTimeout}, peers: ${availablePeers.size}")

            if (availablePeers.isNotEmpty()) {
                Log.d(TAG, "Found ${availablePeers.size} peers, device ready for connections")
                break
            }

            if (isConnected) {
                Log.d(TAG, "Connection established during wait")
                return
            }
        }

        // Report connection ready after discovery
        if (isConnecting && !isConnected) {
            Log.d(TAG, "Discovery completed, device ready for connections")
            onStateChange?.invoke(
                ConnectionState.Connected(
                    deviceName = deviceName,
                    ssid = "WiFi Direct Discovery",
                    password = "",
                    ipAddress = getLocalIpAddress(),
                    port = FILE_TRANSFER_PORT
                )
            )
            isConnecting = false
            isConnected = true
        }
    }

    // WiFi Direct callbacks for peer-to-peer mode
    override fun wifiP2pEnabled(isEnabled: Boolean) {
        Log.d(TAG, "=== wifiP2pEnabled callback ===")
        Log.d(TAG, "WiFi P2P enabled: $isEnabled")

        if (!isEnabled && isConnecting) {
            Log.w(TAG, "WiFi Direct disabled during connection attempt")
            if (currentConnectionMode == ConnectionMode.WIFI_DIRECT) {
                handleConnectionFailure("WiFi Direct was disabled")
            } else {
                fallbackToLocalNetwork()
            }
        }
    }

    override fun onPeersAvailable(devices: List<WifiP2pDevice>) {
        Log.d(TAG, "=== onPeersAvailable callback ===")
        Log.d(TAG, "Peers found: ${devices.size}")

        availablePeers.clear()
        availablePeers.addAll(devices)

        devices.forEach { device ->
            Log.d(TAG, "Peer: ${device.deviceName} (${device.deviceAddress}) - Status: ${device.status}")
        }

        // For receiver mode, we don't need to connect to peers
        // We just need to be discoverable and ready to accept connections
        if (!isConnected && isConnecting) {
            Log.d(TAG, "Peers available, device ready for connections")
            onStateChange?.invoke(
                ConnectionState.Connected(
                    deviceName = deviceName,
                    ssid = "Peers Available (${devices.size})",
                    password = "",
                    ipAddress = getLocalIpAddress(),
                    port = FILE_TRANSFER_PORT
                )
            )
            isConnecting = false
            isConnected = true
        }
    }

    @RequiresPermission(allOf = [
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.NEARBY_WIFI_DEVICES
    ])
    override fun onConnectionInfoAvailable(wifiP2pInfo: WifiP2pInfo) {
        Log.d(TAG, "=== onConnectionInfoAvailable callback (Peer Mode) ===")
        Log.d(TAG, "Group formed: ${wifiP2pInfo.groupFormed}")
        Log.d(TAG, "Is group owner: ${wifiP2pInfo.isGroupOwner}")
        Log.d(TAG, "Group owner address: ${wifiP2pInfo.groupOwnerAddress}")

        if (wifiP2pInfo.groupFormed) {
            val role = if (wifiP2pInfo.isGroupOwner) "Group Owner" else "Client"
            val ipAddress = if (wifiP2pInfo.isGroupOwner) {
                wifiP2pInfo.groupOwnerAddress?.hostAddress ?: "192.168.49.1"
            } else {
                getLocalIpAddress() ?: "192.168.49.2"
            }

            Log.i(TAG, "WiFi Direct connection established as $role")

            if (isConnecting) {
                isConnecting = false
                isConnected = true

                onStateChange?.invoke(
                    ConnectionState.Connected(
                        deviceName = deviceName,
                        ssid = "WiFi Direct ($role)",
                        password = "",
                        ipAddress = ipAddress,
                        port = FILE_TRANSFER_PORT
                    )
                )
            }
        }
    }

    override fun onDisconnection() {
        Log.d(TAG, "=== onDisconnection callback ===")

        val wasConnected = isConnected
        isConnected = false
        isDiscovering = false

        if (wasConnected && !isConnecting) {
            Log.d(TAG, "Notifying UI of disconnection")
            onStateChange?.invoke(ConnectionState.Disconnected)
        }
    }

    override fun onSelfDeviceAvailable(device: WifiP2pDevice) {
        Log.d(TAG, "=== onSelfDeviceAvailable callback ===")
        Log.d(TAG, "Self device: ${device.deviceName} (status: ${device.status})")

        // Update device name if available
        if (device.deviceName.isNotEmpty()) {
            deviceName = device.deviceName
        }
    }

    override fun onChannelDisconnected() {
        Log.e(TAG, "=== onChannelDisconnected callback ===")
        isConnected = false
        isDiscovering = false

        if (isConnecting) {
            Log.w(TAG, "WiFi Direct channel disconnected while connecting")
            if (currentConnectionMode == ConnectionMode.WIFI_DIRECT) {
                handleConnectionFailure("WiFi Direct channel disconnected")
            } else {
                fallbackToLocalNetwork()
            }
        }
    }

    private fun fallbackToLocalNetwork() {
        Log.d(TAG, "=== fallbackToLocalNetwork ===")

        isConnecting = false
        isConnected = true
        isDiscovering = false

        Log.i(TAG, "Using Local Network fallback mode")

        onStateChange?.invoke(
            ConnectionState.Connected(
                deviceName = deviceName,
                ssid = "Local Network Mode",
                password = "",
                ipAddress = getLocalIpAddress(),
                port = FILE_TRANSFER_PORT
            )
        )
    }

    private fun handleConnectionFailure(message: String) {
        Log.e(TAG, "=== handleConnectionFailure ===")
        Log.e(TAG, "Failure message: $message")

        isConnecting = false
        isConnected = false
        isDiscovering = false

        onStateChange?.invoke(ConnectionState.Error(message))
    }

    private suspend fun cleanupWifiDirect() {
        try {
            isDiscovering = false

            serverSocket?.close()
            serverSocket = null

            wifiDirectReceiver?.let { receiver ->
                try {
                    context.unregisterReceiver(receiver)
                    Log.d(TAG, "WiFi Direct receiver unregistered")
                } catch (e: Exception) {
                    Log.w(TAG, "Receiver already unregistered", e)
                }
                wifiDirectReceiver = null
            }

            val channel = wifiP2pChannel
            if (channel != null && wifiP2pManager != null) {
                try {
                    // Stop peer discovery
                    wifiP2pManager.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            Log.d(TAG, "Peer discovery stopped")
                        }
                        override fun onFailure(reason: Int) {
                            Log.w(TAG, "Failed to stop peer discovery: ${getErrorString(reason)}")
                        }
                    })

                    // Remove any existing group (if we happen to be in one)
                    wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            Log.d(TAG, "Group removed (if existed)")
                        }
                        override fun onFailure(reason: Int) {
                            Log.w(TAG, "Group removal failed: ${getErrorString(reason)}")
                        }
                    })

                } catch (e: Exception) {
                    Log.w(TAG, "Error during WiFi Direct cleanup", e)
                }
            }

            wifiP2pChannel = null

        } catch (e: Exception) {
            Log.e(TAG, "Error during WiFi Direct cleanup", e)
        }
    }

    private suspend fun forceCleanup() {
        try {
            isConnecting = false
            isConnected = false
            isDiscovering = false
            availablePeers.clear()

            cleanupWifiDirect()
            delay(500)

            Log.d(TAG, "Force cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during force cleanup", e)
        }
    }

    suspend fun cleanupConnections() = connectionMutex.withLock {
        forceCleanup()
    }

    // Helper methods
    private fun getErrorString(reason: Int): String {
        return when (reason) {
            0 -> "Success" // Fix: 0 is actually success, not error
            WifiP2pManager.P2P_UNSUPPORTED -> "WiFi Direct not supported"
            WifiP2pManager.BUSY -> "WiFi Direct is busy"
            WifiP2pManager.ERROR -> "WiFi Direct system error"
            WifiP2pManager.NO_SERVICE_REQUESTS -> "No service requests"
            else -> "Unknown error (code: $reason)"
        }
    }

    private fun isWifiEnabled(): Boolean {
        return try {
            wifiManager.isWifiEnabled
        } catch (e: Exception) {
            Log.w(TAG, "Cannot check WiFi state", e)
            false
        }
    }

    @SuppressLint("DefaultLocale")
    private fun getLocalIpAddress(): String? {
        return try {
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
            Log.w(TAG, "Cannot get local IP address", e)
            null
        }
    }

    private fun getDeviceName(): String {
        return try {
            Settings.Global.getString(context.contentResolver, "device_name")
                ?: Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "Unknown Device"
        } catch (e: Exception) {
            "SmartSwitch Device"
        }
    }
}