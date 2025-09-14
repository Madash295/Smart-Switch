package com.madash.smartswitch.util



import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

/**
 * QR Code Generator for Smart Switch connection information
 * CHANGE THIS CLASS TO MODIFY QR CODE GENERATION
 */
object QRCodeGenerator {

    private const val QR_CODE_SIZE = 512
    private const val QR_CODE_MARGIN = 2

    /**
     * Generate QR code containing connection information
     * CHANGE THIS METHOD TO MODIFY QR DATA FORMAT
     */
    fun generateConnectionQR(
        deviceName: String,
        ssid: String,
        password: String,
        connectionType: String = "WIFI_DIRECT",
        port: Int = 8080,
        size: Int = QR_CODE_SIZE
    ): Bitmap? {
        try {
            // Create connection data JSON
            val connectionData = createConnectionData(
                deviceName, ssid, password, connectionType, port
            )

            // Generate QR code
            return generateQRBitmap(connectionData, size)

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Create connection data JSON object
     * CHANGE THIS METHOD TO MODIFY CONNECTION DATA STRUCTURE
     */
    private fun createConnectionData(
        deviceName: String,
        ssid: String,
        password: String,
        connectionType: String,
        port: Int
    ): String {
        val jsonObject = JSONObject().apply {
            put("type", "SMARTSWITCH_CONNECTION")
            put("version", "1.0")
            put("device_name", deviceName)
            put("connection_type", connectionType)
            put("ssid", ssid)
            put("password", password)
            put("port", port)
            put("timestamp", System.currentTimeMillis())

            // Additional metadata
            put("app_name", "Smart Switch")
            put("protocol", "TCP")
            put("security", "WPA2")
        }

        return jsonObject.toString()
    }

    /**
     * Generate QR code bitmap from data string
     * CHANGE THIS METHOD TO MODIFY QR CODE APPEARANCE
     */
    private fun generateQRBitmap(data: String, size: Int): Bitmap? {
        try {
            val writer = QRCodeWriter()

            // Configure QR code hints
            val hints = mapOf(
                EncodeHintType.MARGIN to QR_CODE_MARGIN,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )

            // Generate bit matrix
            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size, hints)

            // Create bitmap
            val bitmap = createBitmap(size, size, Bitmap.Config.RGB_565)

            // Fill bitmap with QR code pattern
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                }
            }

            return bitmap

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Parse connection data from QR code string
     * CHANGE THIS METHOD TO MODIFY QR DATA PARSING
     */
    fun parseConnectionData(qrData: String): ConnectionInfo? {
        try {
            val jsonObject = JSONObject(qrData)

            // Validate QR code type
            if (jsonObject.getString("type") != "SMARTSWITCH_CONNECTION") {
                return null
            }

            return ConnectionInfo(
                deviceName = jsonObject.getString("device_name"),
                connectionType = jsonObject.getString("connection_type"),
                ssid = jsonObject.getString("ssid"),
                password = jsonObject.getString("password"),
                port = jsonObject.getInt("port"),
                timestamp = jsonObject.getLong("timestamp")
            )

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Generate WiFi QR code (standard format)
     * CHANGE THIS METHOD TO MODIFY WIFI QR FORMAT
     */
    fun generateWiFiQR(
        ssid: String,
        password: String,
        security: String = "WPA",
        hidden: Boolean = false,
        size: Int = QR_CODE_SIZE
    ): Bitmap? {
        try {
            // WiFi QR code format: WIFI:T:WPA;S:mynetwork;P:mypass;H:false;;
            val wifiString = "WIFI:T:$security;S:$ssid;P:$password;H:$hidden;;"

            return generateQRBitmap(wifiString, size)

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}

/**
 * Data class for connection information
 * CHANGE THIS CLASS TO MODIFY CONNECTION INFO STRUCTURE
 */
data class ConnectionInfo(
    val deviceName: String,
    val connectionType: String,
    val ssid: String,
    val password: String,
    val port: Int,
    val timestamp: Long
)

/**
 * Extension function for easy QR generation
 * CHANGE THIS FUNCTION TO MODIFY CONVENIENCE METHOD
 */
fun generateQRCode(
    deviceName: String,
    ssid: String,
    password: String,
    connectionType: String = "WIFI_DIRECT"
): Bitmap? {
    return QRCodeGenerator.generateConnectionQR(
        deviceName = deviceName,
        ssid = ssid,
        password = password,
        connectionType = connectionType
    )
}



/**
 * QR Code Generator for Local Network connections
 * Creates QR codes with device IP and connection info
 */
object LocalNetworkQRGenerator {

    private const val QR_CODE_SIZE = 512
    private const val QR_CODE_MARGIN = 2

    /**
     * Generate QR code for local network connection
     */
    fun generateLocalConnectionQR(
        deviceName: String,
        ipAddress: String,
        port: Int = 8080,
        networkName: String? = null,
        size: Int = QR_CODE_SIZE
    ): Bitmap? {
        try {
            val connectionData = createLocalConnectionData(
                deviceName, ipAddress, port, networkName
            )

            return generateQRBitmap(connectionData, size)

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Create connection data for local network
     */
    private fun createLocalConnectionData(
        deviceName: String,
        ipAddress: String,
        port: Int,
        networkName: String?
    ): String {
        val jsonObject = JSONObject().apply {
            put("type", "SMARTSWITCH_LOCAL")
            put("version", "2.0")
            put("device_name", deviceName)
            put("ip_address", ipAddress)
            put("port", port)
            put("network_name", networkName ?: "Local Network")
            put("timestamp", System.currentTimeMillis())
            put("connection_type", "LOCAL_NETWORK")
            put("protocol", "TCP")
        }

        return jsonObject.toString()
    }

    /**
     * Generate QR code bitmap from data string
     */
    private fun generateQRBitmap(data: String, size: Int): Bitmap? {
        try {
            val writer = QRCodeWriter()

            val hints = mapOf(
                EncodeHintType.MARGIN to QR_CODE_MARGIN,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )

            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size, hints)
            val bitmap = createBitmap(size, size, Bitmap.Config.RGB_565)

            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                }
            }

            return bitmap

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Parse local connection data from QR code
     */
    fun parseLocalConnectionData(qrData: String): LocalConnectionInfo? {
        try {
            val jsonObject = JSONObject(qrData)

            if (jsonObject.getString("type") != "SMARTSWITCH_LOCAL") {
                return null
            }

            return LocalConnectionInfo(
                deviceName = jsonObject.getString("device_name"),
                ipAddress = jsonObject.getString("ip_address"),
                port = jsonObject.getInt("port"),
                networkName = jsonObject.optString("network_name", "Local Network"),
                timestamp = jsonObject.getLong("timestamp")
            )

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}

/**
 * Data class for local connection information
 */
data class LocalConnectionInfo(
    val deviceName: String,
    val ipAddress: String,
    val port: Int,
    val networkName: String,
    val timestamp: Long
)