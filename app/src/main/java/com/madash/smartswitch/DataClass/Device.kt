package com.madash.smartswitch.DataClass

data class Device(
    val id: String,
    val name: String,
    val deviceType: DeviceType,
    val platform: String,
    val batteryLevel: Int? = null,
    val status: DeviceStatus,
    val icon: Int
)

enum class DeviceType {
    PHONE, TABLET, LAPTOP, DESKTOP
}

enum class DeviceStatus {
    ONLINE, AWAY, CHARGING, OFFLINE
}

data class SelectedFilesData(
    val mediaItems: Set<Int> = emptySet(),
    val appItems: Set<String> = emptySet(),
    val contactItems: Set<Long> = emptySet(),
    val fileItems: Set<String> = emptySet()
) {
    fun getTotalCount(): Int {
        return mediaItems.size + appItems.size + contactItems.size + fileItems.size
    }

    fun isEmpty(): Boolean {
        return mediaItems.isEmpty() && appItems.isEmpty() &&
                contactItems.isEmpty() && fileItems.isEmpty()
    }
}