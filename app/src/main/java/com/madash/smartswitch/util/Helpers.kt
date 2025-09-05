package com.madash.smartswitch.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.madash.smartswitch.DataClass.DeviceType

@Composable
fun getDeviceIconBackground(deviceType: DeviceType, dynamic: Boolean): Color {
    return if (dynamic) {
        when (deviceType) {
            DeviceType.PHONE -> MaterialTheme.colorScheme.primary
            DeviceType.TABLET -> MaterialTheme.colorScheme.secondary
            DeviceType.LAPTOP, DeviceType.DESKTOP -> MaterialTheme.colorScheme.tertiary
        }
    } else {
        when (deviceType) {
            DeviceType.PHONE -> Color(0xFF4CAF50)
            DeviceType.TABLET -> Color(0xFF2196F3)
            DeviceType.LAPTOP, DeviceType.DESKTOP -> Color(0xFF9C27B0)
        }
    }
}