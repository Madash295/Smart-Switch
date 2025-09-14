package com.madash.smartswitch.util

import android.Manifest
import android.content.Context
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresPermission
import com.madash.smartswitch.WifiP2P.WifiDirectActionListner
import com.madash.smartswitch.WifiP2P.WifiDirectBroadCastReceiver
import com.madash.smartswitch.WifiP2P.intentfilter
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    // Enhanced retry and error handling
    private var retryCount = 0
    private var maxRetries = 2
    private var isWifiDirectInitialized = false
    private var lastErrorReason = -1

    companion object {
        private const val TAG = "WiFiConnectionManager"
        private const val WIFI_DIRECT_GROUP_NAME = "SmartSwitch-Direct"
        private const val CONNECTION_TIMEOUT = 12000L // Reduced timeout
        private const val RETRY_DELAY = 3000L // 3 seconds between retries
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
        this.onStateChange = onStateChange
        this.currentConnectionMode = mode
        this.currentBand = band
        this.retryCount = 0

        // If already connected with same mode, don't restart
        if (isConnected && currentConnectionMode == mode) {
            Log.d(TAG, "Already connected with mode $mode, skipping restart")
            return@withLock
        }

        // If currently connecting, wait a bit then try cleanup
        if (isConnecting) {
            Log.d(TAG, "Connection in progress, cleaning up first")
            forceCleanup()
            delay(2000) // Give time for cleanup
        }

        isConnecting = true
        onStateChange(ConnectionState.Connecting)

        try {
            // Always cleanup before starting new connection
            forceCleanup()
            delay(1000) // Brief delay after cleanup

            when (mode) {
                ConnectionMode.AUTOMATIC -> {
                    startAutomaticConnection(band)
                }
                ConnectionMode.WIFI_DIRECT -> {
                    startWifiDirectConnection(band)
                }
                ConnectionMode.LOCAL_NETWORK -> {
                    // Local network doesn't use WiFi Direct, report success
                    val deviceName = getDeviceName()
                    isConnecting = false
                    isConnected = true
                    onStateChange(ConnectionState.Connected(
                        deviceName = deviceName,
                        ssid = "Local Network Mode",
                        password = "",
                        ipAddress = getLocalIpAddress(),
                        port = 8080
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start connection", e)
            handleConnectionFailure("Connection setup failed: ${e.message}")
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    private suspend fun startAutomaticConnection(band: WiFiBand) {
        // Check if band is supported
        if (!band.isSupported(context)) {
            Log.w(TAG, "Band ${band.frequency} not supported, falling back to 2.4GHz")
            currentBand = WiFiBand.BAND_2_4_GHZ
        }

        // Try WiFi Direct if supported and enabled
        if (isWifiDirectSupported() && isWifiEnabled()) {
            try {
                Log.d(TAG, "Attempting WiFi Direct connection in automatic mode")
                startWifiDirectConnection(currentBand)
                return
            } catch (e: Exception) {
                Log.w(TAG, "WiFi Direct failed in automatic mode, falling back", e)
            }
        } else {
            Log.w(
                TAG,
                "WiFi Direct not available - supported: ${isWifiDirectSupported()}, wifi enabled: ${isWifiEnabled()}"
            )
        }

        // Fallback to local network mode
        fallbackToLocalNetwork()
    }

    @RequiresPermission(allOf = [
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.NEARBY_WIFI_DEVICES
    ])
    private suspend fun startWifiDirectConnection(band: WiFiBand) {
        try {
            if (wifiP2pManager == null) {
                Log.w(TAG, "WiFi P2P Manager is null")
                throw IllegalStateException("WiFi Direct not supported on this device")
            }

            if (!isWifiEnabled()) {
                Log.w(TAG, "WiFi is disabled")
                throw IllegalStateException("WiFi must be enabled for WiFi Direct")
            }

            // Enhanced cleanup with proper error handling
            cleanupWifiDirectWithRetry()
            delay(1500) // Longer delay after cleanup

            // Initialize WiFi Direct with enhanced error handling
            if (!initializeWifiDirect()) {
                throw IllegalStateException("Failed to initialize WiFi Direct")
            }

            // Create group with enhanced retry mechanism
            createWifiDirectGroupWithRetry(band)

        } catch (e: Exception) {
            Log.w(TAG, "WiFi Direct setup failed: ${e.message}", e)
            if (currentConnectionMode == ConnectionMode.WIFI_DIRECT) {
                // In WiFi Direct only mode, show specific error
                handleConnectionFailure("WiFi Direct failed: ${e.message}")
            } else {
                // In automatic mode, fallback to local network
                fallbackToLocalNetwork()
            }
        }
    }

    private suspend fun initializeWifiDirect(): Boolean {
        return try {
            // Reset initialization flag
            isWifiDirectInitialized = false

            // Initialize channel
            wifiP2pChannel = wifiP2pManager?.initialize(context, context.mainLooper, this)

            if (wifiP2pChannel == null) {
                Log.e(TAG, "Failed to initialize WiFi Direct channel")
                return false
            }

            // Register broadcast receiver with enhanced error handling
            val intentFilter = intentfilter.getIntentFilter()
            wifiDirectReceiver =
                WifiDirectBroadCastReceiver(wifiP2pManager!!, wifiP2pChannel!!, this)

            try {
                context.registerReceiver(wifiDirectReceiver, intentFilter)
                Log.d(TAG, "WiFi Direct broadcast receiver registered")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register WiFi Direct receiver", e)
                return false
            }

            isWifiDirectInitialized = true
            Log.d(TAG, "WiFi Direct initialized successfully")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing WiFi Direct", e)
            false
        }
    }

    private suspend fun createWifiDirectGroupWithRetry(band: WiFiBand) {
        while (retryCount <= maxRetries && isConnecting && !isConnected) {
            Log.d(
                TAG,
                "Attempting WiFi Direct group creation (attempt ${retryCount + 1}/${maxRetries + 1})"
            )

            try {
                val success = createWifiDirectGroup(band)
                if (success) {
                    // Set timeout for group creation
                    setGroupCreationTimeout()
                    return
                }
            } catch (e: Exception) {
                Log.w(TAG, "Group creation attempt ${retryCount + 1} failed", e)
            }

            retryCount++
            if (retryCount <= maxRetries && isConnecting) {
                Log.d(TAG, "Retrying WiFi Direct group creation in ${RETRY_DELAY}ms")
                delay(RETRY_DELAY)

                // Clean up before retry
                try {
                    wifiP2pManager?.removeGroup(wifiP2pChannel, null)
                    delay(1000)
                } catch (e: Exception) {
                    Log.w(TAG, "Error cleaning up before retry", e)
                }
            }
        }

        // All retries exhausted
        if (isConnecting) {
            Log.w(TAG, "All WiFi Direct creation attempts failed")
            if (currentConnectionMode == ConnectionMode.WIFI_DIRECT) {
                handleConnectionFailure("WiFi Direct group creation failed after ${maxRetries + 1} attempts")
            } else {
                fallbackToLocalNetwork()
            }
        }
    }

    private suspend fun createWifiDirectGroup(band: WiFiBand): Boolean {
        val channel = wifiP2pChannel ?: return false

        val config = WifiP2pConfig().apply {
            // Force this device to be the group owner
            groupOwnerIntent = 15 // Maximum value

            // Set band preference if supported
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    when (band) {
                        WiFiBand.BAND_2_4_GHZ -> setBandPreference(this, 1)
                        WiFiBand.BAND_5_GHZ -> if (band.isSupported(context)) setBandPreference(
                            this,
                            2
                        )

                        WiFiBand.BAND_6_GHZ -> if (band.isSupported(context)) setBandPreference(
                            this,
                            4
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Band selection not available", e)
                }
            }
        }

        return try {
            var groupCreationResult = false
            val resultLock = Object()

            val actionListener = object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(TAG, "WiFi Direct group creation initiated successfully")
                    synchronized(resultLock) {
                        groupCreationResult = true
                        resultLock.notify()
                    }
                }

                override fun onFailure(reason: Int) {
                    Log.w(
                        TAG,
                        "WiFi Direct group creation failed with reason: ${getErrorString(reason)}"
                    )
                    lastErrorReason = reason
                    synchronized(resultLock) {
                        groupCreationResult = false
                        resultLock.notify()
                    }
                }
            }

            // Try to create group
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                wifiP2pManager?.createGroup(channel, config, actionListener)
            } else {
                wifiP2pManager?.createGroup(channel, actionListener)
            }

            // Wait for result with timeout
            synchronized(resultLock) {
                try {
                    resultLock.wait(5000) // 5 second timeout
                } catch (e: InterruptedException) {
                    Log.w(TAG, "Group creation wait interrupted", e)
                }
            }

            groupCreationResult

        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied for WiFi Direct group creation", e)
            false
        } catch (e: Exception) {
            Log.w(TAG, "Error creating WiFi Direct group", e)
            false
        }
    }

    private fun setGroupCreationTimeout() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (isConnecting && !isConnected) {
                Log.w(TAG, "WiFi Direct group creation timeout")
                if (currentConnectionMode == ConnectionMode.WIFI_DIRECT) {
                    handleConnectionFailure("WiFi Direct connection timeout")
                } else {
                    fallbackToLocalNetwork()
                }
            }
        }, CONNECTION_TIMEOUT)
    }

    private fun fallbackToLocalNetwork() {
        val deviceName = getDeviceName()
        isConnecting = false
        isConnected = true

        Log.i(TAG, "Using Local Network fallback mode")

        onStateChange?.invoke(
            ConnectionState.Connected(
                deviceName = deviceName,
                ssid = "Local Network Mode",
                password = "",
                ipAddress = getLocalIpAddress(),
                port = 8080
            )
        )
    }

    private fun handleConnectionFailure(message: String) {
        isConnecting = false
        isConnected = false

        Log.e(TAG, "Connection failed: $message")
        onStateChange?.invoke(ConnectionState.Error(message))
    }

    private suspend fun cleanupWifiDirectWithRetry() {
        var attempts = 0
        val maxCleanupAttempts = 2

        while (attempts < maxCleanupAttempts) {
            try {
                cleanupWifiDirect()
                Log.d(TAG, "WiFi Direct cleanup completed (attempt ${attempts + 1})")
                break
            } catch (e: Exception) {
                Log.w(TAG, "WiFi Direct cleanup attempt ${attempts + 1} failed", e)
                attempts++
                if (attempts < maxCleanupAttempts) {
                    delay(1000)
                }
            }
        }
    }

    private suspend fun cleanupWifiDirect() {
        try {
            // Unregister receiver
            wifiDirectReceiver?.let { receiver ->
                try {
                    context.unregisterReceiver(receiver)
                    Log.d(TAG, "WiFi Direct receiver unregistered")
                } catch (e: Exception) {
                    Log.w(TAG, "Receiver already unregistered", e)
                }
                wifiDirectReceiver = null
            }

            // Remove group with timeout
            val channel = wifiP2pChannel
            if (channel != null && wifiP2pManager != null && isWifiDirectInitialized) {
                try {
                    var removalComplete = false
                    val removalLock = Object()

                    wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            Log.d(TAG, "WiFi Direct group removed successfully")
                            synchronized(removalLock) {
                                removalComplete = true
                                removalLock.notify()
                            }
                        }

                        override fun onFailure(reason: Int) {
                            Log.w(
                                TAG,
                                "Failed to remove WiFi Direct group: ${getErrorString(reason)}"
                            )
                            synchronized(removalLock) {
                                removalComplete = true // Continue anyway
                                removalLock.notify()
                            }
                        }
                    })

                    // Wait for removal with timeout
                    synchronized(removalLock) {
                        try {
                            removalLock.wait(3000) // 3 second timeout
                        } catch (e: InterruptedException) {
                            Log.w(TAG, "Group removal wait interrupted", e)
                        }
                    }

                } catch (e: Exception) {
                    Log.w(TAG, "Error removing WiFi Direct group", e)
                }
            }

            wifiP2pChannel = null
            isWifiDirectInitialized = false

        } catch (e: Exception) {
            Log.e(TAG, "Error during WiFi Direct cleanup", e)
        }
    }

    private suspend fun forceCleanup() {
        try {
            isConnecting = false
            isConnected = false
            retryCount = 0

            // Cleanup WiFi Direct
            cleanupWifiDirect()

            Log.d(TAG, "Force cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during force cleanup", e)
        }
    }

    suspend fun cleanupConnections() = connectionMutex.withLock {
        forceCleanup()
    }

    private fun setBandPreference(config: WifiP2pConfig, band: Int) {
        Log.i(
            TAG,
            "Band preference ($band) noted but cannot be set due to API restrictions. System will use default band selection."
        )
    }

    private fun getErrorString(reason: Int): String {
        return when (reason) {
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

    // WiFi Direct callbacks - Enhanced with better error handling
    override fun wifiP2pEnabled(isEnabled: Boolean) {
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

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    override fun onConnectionInfoAvailable(wifiP2pInfo: WifiP2pInfo) {
        Log.d(
            TAG,
            "WiFi P2P connection info - Group formed: ${wifiP2pInfo.groupFormed}, Is owner: ${wifiP2pInfo.isGroupOwner}"
        )

        if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
            // We are the group owner, get group info
            wifiP2pManager?.requestGroupInfo(wifiP2pChannel) { group ->
                if (group != null && isConnecting) {
                    val deviceName = getDeviceName()
                    val ssid = group.networkName ?: WIFI_DIRECT_GROUP_NAME
                    val password = group.passphrase ?: "12345678"

                    isConnecting = false
                    isConnected = true
                    retryCount = 0 // Reset retry count on success

                    Log.i(TAG, "WiFi Direct group successfully created - SSID: $ssid")

                    onStateChange?.invoke(
                        ConnectionState.Connected(
                            deviceName = deviceName,
                            ssid = ssid,
                            password = password,
                            ipAddress = wifiP2pInfo.groupOwnerAddress?.hostAddress,
                            port = 8080
                        )
                    )
                } else if (isConnecting) {
                    Log.w(TAG, "Group info is null or connection no longer needed, retrying...")
                    // Retry getting group info after a short delay
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        if (isConnecting) {
                            wifiP2pManager?.requestGroupInfo(wifiP2pChannel) { retryGroup ->
                                if (retryGroup != null && isConnecting) {
                                    val deviceName = getDeviceName()
                                    val ssid = retryGroup.networkName ?: WIFI_DIRECT_GROUP_NAME
                                    val password = retryGroup.passphrase ?: "12345678"

                                    isConnecting = false
                                    isConnected = true

                                    Log.i(
                                        TAG,
                                        "WiFi Direct group info retrieved on retry - SSID: $ssid"
                                    )

                                    onStateChange?.invoke(
                                        ConnectionState.Connected(
                                            deviceName = deviceName,
                                            ssid = ssid,
                                            password = password,
                                            ipAddress = wifiP2pInfo.groupOwnerAddress?.hostAddress,
                                            port = 8080
                                        )
                                    )
                                } else if (isConnecting) {
                                    Log.w(TAG, "Group info still unavailable after retry")
                                    if (currentConnectionMode == ConnectionMode.WIFI_DIRECT) {
                                        handleConnectionFailure("WiFi Direct group info unavailable")
                                    } else {
                                        fallbackToLocalNetwork()
                                    }
                                }
                            }
                        }
                    }, 2000) // 2 second delay
                }
            }
        } else if (isConnecting) {
            Log.w(TAG, "WiFi Direct group not formed or not group owner")
            // This might be expected during group formation, so don't immediately fail
        }
    }

    override fun onDisconnection() {
        Log.d(TAG, "WiFi P2P disconnected")
        isConnected = false
        if (!isConnecting) {
            onStateChange?.invoke(ConnectionState.Disconnected)
        }
    }

    override fun onSelfDeviceAvailable(device: WifiP2pDevice) {
        Log.d(TAG, "Self device: ${device.deviceName} (status: ${device.status})")
    }

    override fun onPeersAvailable(devices: List<WifiP2pDevice>) {
        Log.d(TAG, "Peers available: ${devices.size}")
    }

    override fun onChannelDisconnected() {
        Log.w(TAG, "WiFi P2P channel disconnected")
        isConnected = false
        isWifiDirectInitialized = false

        if (isConnecting) {
            Log.w(TAG, "WiFi Direct channel disconnected while connecting")
            if (currentConnectionMode == ConnectionMode.WIFI_DIRECT) {
                handleConnectionFailure("WiFi Direct channel disconnected")
            } else {
                fallbackToLocalNetwork()
            }
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

    private fun isWifiDirectSupported(): Boolean {
        return context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_WIFI_DIRECT)
    }
}