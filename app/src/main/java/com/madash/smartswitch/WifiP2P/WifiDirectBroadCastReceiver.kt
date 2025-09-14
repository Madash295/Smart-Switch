package com.madash.smartswitch.WifiP2P

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.annotation.RequiresPermission

class WifiDirectBroadCastReceiver(
    private val wifiP2pManager: WifiP2pManager,
    private val wifiP2pChannel: WifiP2pManager.Channel,
    private val directActionListener: WifiDirectActionListner
) : BroadcastReceiver() {

    companion object {
        private const val TAG = "WifiDirectReceiver"
    }

    @RequiresPermission(allOf = [
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.NEARBY_WIFI_DEVICES
    ])
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")

        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val enabled = intent.getIntExtra(
                    WifiP2pManager.EXTRA_WIFI_STATE,
                    -1
                ) == WifiP2pManager.WIFI_P2P_STATE_ENABLED

                Log.d(TAG, "WIFI_P2P_STATE_CHANGED_ACTION: $enabled")
                directActionListener.wifiP2pEnabled(enabled)

                if (!enabled) {
                    directActionListener.onPeersAvailable(emptyList())
                }
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                Log.d(TAG, "WIFI_P2P_PEERS_CHANGED_ACTION")
                try {
                    wifiP2pManager.requestPeers(wifiP2pChannel) { peers ->
                        val deviceList = peers?.deviceList?.toList() ?: emptyList()
                        Log.d(TAG, "Found ${deviceList.size} peers")
                        directActionListener.onPeersAvailable(deviceList)
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "Permission denied when requesting peers", e)
                    directActionListener.onPeersAvailable(emptyList())
                } catch (e: Exception) {
                    Log.e(TAG, "Error requesting peers", e)
                    directActionListener.onPeersAvailable(emptyList())
                }
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                val networkInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO, NetworkInfo::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                }

                Log.d(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION: connected=${networkInfo?.isConnected}")

                if (networkInfo != null && networkInfo.isConnected) {
                    try {
                        // Request connection info when connected
                        wifiP2pManager.requestConnectionInfo(wifiP2pChannel) { info ->
                            if (info != null) {
                                Log.d(TAG, "Connection info received - Group formed: ${info.groupFormed}, Is owner: ${info.isGroupOwner}")
                                directActionListener.onConnectionInfoAvailable(info)
                            } else {
                                Log.w(TAG, "Connection info is null")
                            }
                        }
                        Log.d(TAG, "P2P device connected")
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Permission denied when requesting connection info", e)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error requesting connection info", e)
                    }
                } else {
                    Log.d(TAG, "Disconnected from P2P device")
                    directActionListener.onDisconnection()
                }
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                val wifiP2pDevice = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE, WifiP2pDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                }

                if (wifiP2pDevice != null) {
                    Log.d(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION: ${wifiP2pDevice.deviceName} (${wifiP2pDevice.deviceAddress})")
                    directActionListener.onSelfDeviceAvailable(wifiP2pDevice)
                } else {
                    Log.w(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION: device is null")
                }
            }

            else -> {
                Log.d(TAG, "Unhandled action: ${intent.action}")
            }
        }
    }
}

object Logger {
    private const val TAG = "WifiP2P"

    fun log(any: Any?) {
        Log.d(TAG, any?.toString() ?: "null")
    }

    fun logError(any: Any?) {
        Log.e(TAG, any?.toString() ?: "null")
    }

    fun logWarning(any: Any?) {
        Log.w(TAG, any?.toString() ?: "null")
    }
}