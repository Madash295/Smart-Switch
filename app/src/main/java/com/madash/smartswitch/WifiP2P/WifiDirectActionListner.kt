package com.madash.smartswitch.WifiP2P

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager

interface WifiDirectActionListner : WifiP2pManager.ChannelListener{

    fun wifiP2pEnabled(isEnabled: Boolean)
    fun onConnectionInfoAvailable(wifiP2pInfo: WifiP2pInfo)

    fun onDisconnection()

    fun onSelfDeviceAvailable(device: WifiP2pDevice)

    fun onPeersAvailable(devices: List<WifiP2pDevice>)

}