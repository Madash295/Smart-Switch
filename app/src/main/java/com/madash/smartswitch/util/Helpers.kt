package com.madash.smartswitch.util

import android.Manifest
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.madash.smartswitch.DataClass.DeviceType
import com.madash.smartswitch.Layouts.MediaType
import java.util.Calendar

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


fun getRequiredPermissions(tabIndex: Int): List<String> {
    return when (tabIndex) {
        0, 1, 2, 5, 6 -> { // Photos, Videos, Music, Documents, Archives
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                when (tabIndex) {
                    0 -> listOf(Manifest.permission.READ_MEDIA_IMAGES)
                    1 -> listOf(Manifest.permission.READ_MEDIA_VIDEO)
                    2 -> listOf(Manifest.permission.READ_MEDIA_AUDIO)
                    else -> listOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.READ_MEDIA_AUDIO
                    )
                }
            } else {
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        3 -> listOf(Manifest.permission.READ_CONTACTS) // Contacts
        7 -> { // Files
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                listOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
            } else {
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        else -> emptyList() // Apps don't need special permissions
    }
}

fun getMediaTypeIcon(mediaType: MediaType): ImageVector {
    return when (mediaType) {
        MediaType.MUSIC -> Icons.Filled.MusicNote
        MediaType.DOCUMENT -> Icons.Filled.Description
        MediaType.ARCHIVE -> Icons.Filled.Archive
        MediaType.APK -> Icons.Filled.Android
        MediaType.CONTACT -> Icons.Filled.Person
        MediaType.FOLDER -> Icons.Filled.Folder
        else -> Icons.AutoMirrored.Filled.List
    }
}

fun getMediaTypeColor(mediaType: MediaType): Color {
    return when (mediaType) {
        MediaType.PHOTO -> Color(0xFFE3F2FD)
        MediaType.VIDEO -> Color(0xFFE8F5E8)
        MediaType.MUSIC -> Color(0xFFFFF3E0)
        MediaType.DOCUMENT -> Color(0xFFF3E5F5)
        MediaType.ARCHIVE -> Color(0xFFE0F2F1)
        MediaType.APK -> Color(0xFFE8F5E8)
        MediaType.CONTACT -> Color(0xFFE8F5E8)
        MediaType.FOLDER -> Color(0xFFFFF8E1)
        else -> Color(0xFFE5FAFF)
    }
}

fun getMediaTypeIconTint(mediaType: MediaType): Color {
    return when (mediaType) {
        MediaType.PHOTO -> Color(0xFF2196F3)
        MediaType.VIDEO -> Color(0xFF4CAF50)
        MediaType.MUSIC -> Color(0xFFFF9800)
        MediaType.DOCUMENT -> Color(0xFF9C27B0)
        MediaType.ARCHIVE -> Color(0xFF009688)
        MediaType.APK -> Color(0xFF4CAF50)
        MediaType.CONTACT -> Color(0xFF4CAF50)
        MediaType.FOLDER -> Color(0xFFFFC107)
        else -> Color(0xFF04C2FB)
    }
}

fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}