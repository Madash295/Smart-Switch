package com.madash.smartswitch.util

import android.content.Context
import android.os.Build
import android.provider.Settings

/**
 * Represents the current state of connection
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()

    data class Connected(
        val deviceName: String,
        val ssid: String,
        val password: String = "",
        val ipAddress: String? = null,
        val port: Int? = null
    ) : ConnectionState()

    data class Error(val message: String) : ConnectionState()
}

/**
 * Enum for different connection modes
 */
enum class ConnectionMode {
    AUTOMATIC,      // Both WiFi Direct and Local Network
    WIFI_DIRECT,    // WiFi Direct only
    LOCAL_NETWORK   // Local Network only
}

/**
 * Enhanced WiFi bands with 6GHz support
 */
enum class WiFiBand(val frequency: String, val bandId: Int) {
    BAND_2_4_GHZ("2.4 GHz", 1),
    BAND_5_GHZ("5 GHz", 2),
    BAND_6_GHZ("6 GHz", 4); // WiFi 6E support

    fun isSupported(context: Context): Boolean {
        return when (this) {
            BAND_2_4_GHZ -> true // Always supported
            BAND_5_GHZ -> {
                // Check if device supports 5GHz
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
                        wifiManager.is5GHzBandSupported
                    } catch (e: Exception) {
                        false
                    }
                } else {
                    false
                }
            }
            BAND_6_GHZ -> {
                // Check if device supports 6GHz (WiFi 6E)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
                        // Use reflection to check for 6GHz support since API might not be available
                        try {
                            val method = wifiManager.javaClass.getMethod("is6GHzBandSupported")
                            method.invoke(wifiManager) as? Boolean ?: false
                        } catch (e: Exception) {
                            // Fallback: check if device has WiFi 6E support through other means
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                    context.packageManager.hasSystemFeature("android.hardware.wifi.wifi6e")
                        }
                    } catch (e: Exception) {
                        false
                    }
                } else {
                    false
                }
            }
        }
    }

    /**
     * Get the recommended use case for this band
     */
    fun getRecommendation(): String {
        return when (this) {
            BAND_2_4_GHZ -> "Best compatibility and range"
            BAND_5_GHZ -> "Faster speeds, less interference"
            BAND_6_GHZ -> "Highest speeds, latest standard"
        }
    }

    /**
     * Get the typical range for this band
     */
    fun getRange(): String {
        return when (this) {
            BAND_2_4_GHZ -> "~50m indoors, ~150m outdoors"
            BAND_5_GHZ -> "~30m indoors, ~100m outdoors"
            BAND_6_GHZ -> "~20m indoors, ~80m outdoors"
        }
    }
}

/**
 * Get device name from system settings
 */
fun getDeviceName(context: Context): String {
    return try {
        // Try to get device name from global settings
        val globalName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Settings.Global.getString(context.contentResolver, "device_name")
        } else {
            null
        }

        globalName ?: run {
            // Fallback to secure settings
            val secureName = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            "SmartSwitch_${secureName?.take(8) ?: "Device"}"
        }
    } catch (e: Exception) {
        // Final fallback
        "SmartSwitch_${Build.MODEL.replace(" ", "_")}"
    }
}

/**
 * Extension function to check if connection state indicates an active connection
 */
fun ConnectionState.isConnected(): Boolean {
    return this is ConnectionState.Connected
}

/**
 * Extension function to check if connection state indicates an error
 */
fun ConnectionState.isError(): Boolean {
    return this is ConnectionState.Error
}

/**
 * Extension function to get connection details as a formatted string
 */
fun ConnectionState.Connected.getConnectionInfo(): String {
    return buildString {
        append("Device: $deviceName\n")
        if (ssid.isNotEmpty() && ssid != "AUTO_MODE") {
            append("Network: $ssid\n")
        }
        if (password.isNotEmpty() && password != "DUAL_CONNECTION") {
            append("Password: $password\n")
        }
        ipAddress?.let { append("IP: $it\n") }
        port?.let { append("Port: $it") }
    }.trim()
}

/**
 * Get the best WiFi band for the device
 */
fun getBestAvailableBand(context: Context): WiFiBand {
    return when {
        WiFiBand.BAND_6_GHZ.isSupported(context) -> WiFiBand.BAND_6_GHZ
        WiFiBand.BAND_5_GHZ.isSupported(context) -> WiFiBand.BAND_5_GHZ
        else -> WiFiBand.BAND_2_4_GHZ
    }
}

/**
 * Get all supported bands for the device
 */
fun getSupportedBands(context: Context): List<WiFiBand> {
    return WiFiBand.values().filter { it.isSupported(context) }
}

/**
 * Connection configuration data class
 */
data class ConnectionConfig(
    val mode: ConnectionMode,
    val band: WiFiBand,
    val timeout: Long = 15000L,
    val retryAttempts: Int = 3
)

/**
 * Default connection configuration
 */
fun getDefaultConnectionConfig(context: Context): ConnectionConfig {
    return ConnectionConfig(
        mode = ConnectionMode.WIFI_DIRECT, // Changed from AUTOMATIC to LOCAL_NETWORK
        band = getBestAvailableBand(context)
    )
}